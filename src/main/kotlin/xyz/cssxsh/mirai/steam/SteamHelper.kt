package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.enums.*
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.*
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.*
import `in`.dragonbra.javasteam.steam.handlers.steamuser.*
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.*
import `in`.dragonbra.javasteam.steam.steamclient.*
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.*
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.*
import `in`.dragonbra.javasteam.types.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.mirai.steam.data.*
import kotlin.coroutines.*
import kotlin.properties.*
import kotlin.reflect.*

public class SteamHelper(public val id: Long) : CoroutineScope {
    private val logger = MiraiLogger.Factory.create(this::class, identity = "steam.helper.${id}")
    override val coroutineContext: CoroutineContext =
        CoroutineName(name = "steam-helper-${id}") + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            logger.warning("$throwable in steam-helper-${id}", throwable)
        }

    private val configuration = SteamKitConfig.toSteamConfiguration()
    private val client: SteamClient = SteamClient(configuration)
    private val auth: SteamUser = client.handler()
    private val friends: SteamFriends = client.handler()

    private val logon: LogOnDetails = LogOnDetails().apply {
        clientLanguage = "chinese"
        isShouldRememberPassword = true
    }
    private val relationships: MutableMap<SteamID, EFriendRelationship> = HashMap()
    private val nicknames: MutableMap<SteamID, String> = HashMap()
    private val states: MutableMap<SteamID, PersonaState> = HashMap()
    private val state = Mutex()
    public val relations: Sequence<SteamRelation>
        get() = sequence {
            for ((id, relationship) in relationships) {
                yield(
                    SteamRelation(
                        steamID = id,
                        relationship = relationship,
                        nickname = nicknames[id],
                        persona = states[id]
                    )
                )
            }
        }

    private val listeners: MutableMap<String, Job> = HashMap()
    public var key: String by SteamAuthData
        private set
    public var name: String by SteamAuthData
        private set
    private val messages: SharedFlow<ICallbackMsg> = with(MutableSharedFlow<ICallbackMsg>()) {
        launch {
            while (isActive) {
                val message = runInterruptible(coroutineContext) {
                    client.waitForCallback(true)
                } ?: continue

                emit(message)
            }
        }
        asSharedFlow()
    }

    private fun log(callback: ICallbackMsg) {
        when (callback) {
            is LoggedOnCallback -> when (callback.result) {
                EResult.OK -> logger.info("Successfully logged on ${callback.clientSteamID} !")
                EResult.AccountLogonDenied -> logger.warning("Unable to logon to Steam: This account is SteamGuard protected.")
                else -> logger.warning("Unable to logon to Steam: ${callback.result}, ${callback.result.url()}")
            }
            is LoggedOffCallback -> when (callback.result) {
                EResult.OK -> logger.info("Successfully logged off $name !")
                else -> logger.warning("Logged off of Steam: ${callback.result}, ${callback.result.url()}")
            }
            is FriendAddedCallback -> logger.info("${callback.personaName} is now a friend for $name")
            is ConnectedCallback -> {
                // ignore
                // logger.verbose("Connected")
            }
            else -> logger.debug("${callback.javaClass.simpleName} - ${callback.jobID}")
        }
    }

    private fun connect(callback: ICallbackMsg) {
        when (callback) {
            is DisconnectedCallback -> {
                if (callback.isUserInitiated) {
                    logger.info("Disconnected from Steam")
                } else {
                    logger.warning("Disconnected from Steam, Try reconnect")
                    client.connect()
                }
            }
        }
    }

    private fun save(callback: ICallbackMsg) {
        launch {
            state.withLock {
                when (callback) {
                    is LoggedOnCallback -> {
                        when (callback.result) {
                            EResult.OK -> {
                                logger.debug("refresh login name ${logon.username} for ${callback.clientSteamID}")
                                name = logon.username
                            }
                            else -> {}
                        }
                    }
                    is LoginKeyCallback -> {
                        logger.debug("refresh login key ${callback.loginKey} - ${callback.uniqueID} for $name")
                        key = callback.loginKey
                    }
                    is LoggedOffCallback -> {}
                    is FriendsListCallback -> {
                        logger.debug("refresh friend list for $name")
                        callback.friendList.forEach { friend ->
                            relationships[friend.steamID] = friend.relationship
                        }
                    }
                    is NicknameListCallback -> {
                        logger.debug("refresh nicknames for $name")
                        callback.nicknames.forEach { nickname ->
                            nicknames[nickname.steamID] = nickname.nickname
                        }
                    }
                    is AccountInfoCallback -> {
                        logger.debug("refresh account info for $name")
                    }
                    is FriendAddedCallback -> {
                        // TODO: FriendAddedCallback
                    }
                    is PersonaStatesCallback -> {
                        callback.personaStates.forEach { state ->
                            states[state.friendID] = state
                        }
                    }
                }
            }
        }
    }

    private fun chat(callback: ICallbackMsg) {
        launch {
            when (callback) {
                is ChatMsgCallback -> {
                    val user = user()
                    val nickname = nicknames[callback.chatterID] ?: "<null>"
                    val forward = buildForwardMessage(user) {
                        user.bot named nickname says "${callback.chatMsgType} at ${callback.chatRoomID}"
                        user.bot named nickname says callback.message
                    }

                    user.sendMessage(message = forward)
                }
            }
        }
    }

    init {
        listeners["LOG"] = launch { messages.collect { callback -> log(callback) } }
        listeners["CONNECT"] = launch { messages.collect { callback -> connect(callback) } }
        listeners["STATE"] = launch { messages.collect { callback -> save(callback) } }
        listeners["CHAT"] = launch { messages.collect { callback -> chat(callback) } }
        listeners["REFRESH"] = launch {
            while (isActive) {
                if (!client.isConnected) messages.filterIsInstance<ConnectedCallback>().first()
                if (client.steamID == null && key.isNotEmpty()) {
                    logger.info("Connected, try auto refresh for $name")
                    refresh()
                }
                delay(60_000L)
            }
        }
        client.connect()
    }

    public fun user(): User {
        for (bot in Bot.instances) {
            for (friend in bot.friends) {
                if (friend.id == id) return friend
            }
            for (group in bot.groups) {
                for (member in group.members) {
                    if (member.id == id) return member
                }
            }
        }
        throw NoSuchElementException("User(${id})")
    }

    public suspend fun request(): MessageChain = coroutineScope {
        globalEventChannel().nextEvent<FriendMessageEvent> { event -> event.friend.id == id }.message
    }

    public fun auth() {
        launch {
            val user = user()

            user.sendMessage(message = "请输入账号密码（和令牌，如果有）")
            val match = """^(\S+)\s+(\S+)(?:\s+(\S+))?""".toRegex().find(request().contentToString())
                ?: throw IllegalArgumentException("格式错误")
            val (username, password, code) = match.destructured

            launch {
                when (val result = messages.filterIsInstance<LoggedOnCallback>().first().result) {
                    EResult.OK -> user.sendMessage(message = "登录成功")
                    EResult.AccountLoginDeniedNeedTwoFactor -> {
                        user.sendMessage(message = "登录需要令牌, 请输入")
                        auth.logOn(logon.apply {
                            this.username = username
                            this.password = password
                            this.twoFactorCode = request().contentToString()
                        })
                    }
                    EResult.NeedCaptcha -> {
                        user.sendMessage(message = "登录需要验证码, 请输入")
                        auth.logOn(logon.apply {
                            this.username = username
                            this.password = password
                            this.authCode = request().contentToString()
                        })
                    }
                    else -> "登录失败, ${result.url()}"
                }
            }

            auth.logOn(logon.apply {
                this.username = username
                this.password = password
                this.twoFactorCode = code
            })
        }
    }

    public fun refresh() {
        check(key.isNotEmpty()) { "LoginKey must not empty" }
        auth.logOn(logon.apply {
            this.username = name
            this.loginKey = key
        })
    }

    public fun state(value: EPersonaState) {
        friends.setPersonaState(value)
    }

    public companion object Factory : ReadOnlyProperty<User, SteamHelper> {

        private val cache: MutableMap<Long, SteamHelper> = HashMap()

        override fun getValue(thisRef: User, property: KProperty<*>): SteamHelper {
            return cache.getOrPut(thisRef.id) { SteamHelper(id = thisRef.id) }
        }
    }
}