package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.enums.*
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.*
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.*
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.*
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
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
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

    /**
     * @see save
     */
    private val jobs: MutableMap<JobID, (ICallbackMsg) -> Unit> = HashMap()
    private val logon: LogOnDetails = LogOnDetails().apply {
        loginID = id.hashCode()
        clientLanguage = "chinese"
        isShouldRememberPassword = true
    }
    private val relationships: MutableMap<SteamID, EFriendRelationship> = HashMap()
    private val nicknames: MutableMap<SteamID, String> = HashMap()
    private val personas: MutableMap<SteamID, PersonaState> = HashMap()
    private val state = Mutex()
    public val relations: Sequence<SteamRelation>
        get() = sequence {
            for ((id, relationship) in relationships) {
                yield(
                    SteamRelation(
                        steamID = id,
                        relationship = relationship,
                        nickname = nicknames[id] ?: personas[id]?.name ?: id.render(),
                        persona = personas[id]
                    )
                )
            }
        }

    private val listeners: MutableMap<String, Job> = HashMap()
    public var key: String by SteamAuthData
        private set
    public var name: String by SteamAuthData
        private set
    public var persona: String = ""
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

    private suspend inline fun <reified C : ICallbackMsg> receive(): C = messages.first { it is C } as C

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
                    is FriendAddedCallback -> {
                        logger.debug("friend ${callback.personaName} added for $name")
                        relationships[callback.steamID] = EFriendRelationship.Friend
                        nicknames[callback.steamID] = callback.personaName
                    }
                    is NicknameCallback -> {
                        jobs.remove(callback.jobID)?.invoke(callback)
                    }
                    is NicknameListCallback -> {
                        logger.debug("refresh nicknames for $name")
                        callback.nicknames.forEach { nickname ->
                            nicknames[nickname.steamID] = nickname.nickname
                        }
                    }
                    is AccountInfoCallback -> {
                        logger.debug("refresh account info for $name")
                        persona = callback.personaName
                    }
                    is ProfileInfoCallback -> {}
                    is PersonaStatesCallback -> {
                        callback.personaStates.forEach { state ->
                            personas[state.friendID] = state
                        }
                    }
                    is PersonaChangeCallback -> {
                        persona = callback.name
                    }
                }
            }
        }
    }

    private fun chat(callback: ICallbackMsg) {
        launch {
            when (callback) {
                is ChatMsgCallback -> {
                    logger.info { "${callback.chatRoomID}#${callback.chatterID}<${callback.chatMsgType}>: ${callback.message}" }
                }
                is FriendMsgCallback -> {
                    logger.info { "${callback.sender}<${callback.entryType}>: ${callback.message}" }

                    val sender = callback.sender
                    val user = user()
                    val nickname = nicknames[sender] ?: personas[sender]?.name ?: sender.render()
                    val avatar = try {
                        personas[sender].avatar().uploadAsImage(user)
                    } catch (_: Throwable) {
                        "【头像下载失败】".toPlainText()
                    }
                    when (callback.entryType) {
                        EChatEntryType.ChatMsg -> {
                            user.sendMessage(message = buildMessageChain {
                                append(avatar).appendLine(nickname)
                                append(callback.message)
                            })
                        }
                        else -> {}
                    }
                }
                is FriendMsgEchoCallback -> {
                    logger.info { "${callback.sender}<${callback.entryType}>: ${callback.message}" }
                }
                is FriendMsgHistoryCallback -> {
                    logger.info { "${callback.steamID} message history ${callback.messages.size}" }

                    val user = user()
                    val forward = buildForwardMessage(user) {
                        for (record in callback.messages) {
                            val sender = record.steamID
                            val nickname = nicknames[sender] ?: personas[sender]?.name ?: sender.render()
                            val avatar = try {
                                personas[sender].avatar().uploadAsImage(user)
                            } catch (_: Throwable) {
                                "【头像下载失败】".toPlainText()
                            }
                            val time = record.timestamp.toInstant().epochSecond.toInt()
                            user.bot named nickname at time says {
                                append(avatar).appendLine(nickname)
                                append(record.message)
                            }
                        }
                    }

                    user.sendMessage(message = forward)
                }
            }
        }
    }

    private fun notice(callback: ICallbackMsg) {
        launch {
            when (callback) {
                is UserNotificationsCallback -> {}
                is OfflineMessageNotificationCallback -> {}
                is ItemAnnouncementsCallback -> {}
                is CommentNotificationsCallback -> {}
            }
        }
    }

    init {
        listeners["LOG"] = launch { messages.collect { callback -> log(callback) } }
        listeners["CONNECT"] = launch { messages.collect { callback -> connect(callback) } }
        listeners["STATE"] = launch { messages.collect { callback -> save(callback) } }
        listeners["CHAT"] = launch { messages.collect { callback -> chat(callback) } }
        listeners["NOTICE"] = launch { messages.collect { callback -> notice(callback) } }
        listeners["REFRESH"] = launch {
            while (isActive) {
                if (!client.isConnected) receive<LoggedOnCallback>()
                if (client.steamID == null) {
                    if (key.isNotEmpty()) {
                        logger.info("Connected, try auto refresh for $name")
                        refresh()
                    }
                } else {
                    flush()
                }
                delay(600_000L)
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

    private suspend fun request(): MessageChain = coroutineScope {
        globalEventChannel().nextEvent<FriendMessageEvent> { event -> event.friend.id == id }.message
    }

    private fun logon(block: LogOnDetails.() -> Unit) {
        auth.logOn(logon.apply(block))
    }

    public fun auth(user: User = user()) {
        launch {
            user.sendMessage(message = "请输入账号密码（和令牌，如果有）")
            val match = """^(\S+)\s+(\S+)(?:\s+(\S+))?""".toRegex().find(request().contentToString())
                ?: throw IllegalArgumentException("格式错误")
            val (username, password, code) = match.destructured
            logger.info { "try logon $username" }

            launch {
                when (val result = receive<LoggedOnCallback>().result) {
                    EResult.OK -> user.sendMessage(message = "登录成功")
                    EResult.AccountLoginDeniedNeedTwoFactor -> {
                        user.sendMessage(message = "登录需要令牌, 请输入")
                        val twoFactorCode = request().contentToString()
                        logon {
                            this.username = username
                            this.password = password
                            this.twoFactorCode = twoFactorCode
                        }
                    }
                    EResult.TwoFactorCodeMismatch -> {
                        user.sendMessage(message = "令牌过期或不匹配, 请重新输入")
                        val authCode = request().contentToString()
                        auth.logOn(logon.apply {
                            this.username = username
                            this.password = password
                            this.twoFactorCode = authCode
                        })
                    }
                    EResult.NeedCaptcha -> {
                        user.sendMessage(message = "登录需要验证码, 请输入")
                        val authCode = request().contentToString()
                        logon {
                            this.username = username
                            this.password = password
                            this.authCode = authCode
                        }
                    }
                    else -> user.sendMessage(message = "登录失败, ${result.url()}")
                }
            }

            logon {
                this.username = username
                this.password = password
                this.twoFactorCode = code
            }
        }
    }

    public fun refresh() {
        check(key.isNotEmpty()) { "LoginKey must not empty" }
        auth.logOn(logon.apply {
            this.username = name
            this.loginKey = key
        })
    }

    public fun flush() {
        friends.requestFriendInfo(relationships.keys.toList(), 0)
    }

    public fun sendMessage(target: SteamID, message: String) {
        friends.sendChatMessage(target, EChatEntryType.ChatMsg, message)
    }

    public fun sendMessage(target: SteamID, message: Message) {
        friends.sendChatMessage(target, EChatEntryType.ChatMsg, message.contentToString())
    }

    public fun state(value: EPersonaState) {
        friends.setPersonaState(value)
    }

    public fun ignore(target: SteamID, ignore: Boolean) {
        friends.ignoreFriend(target, ignore)
    }

    public fun nickname(target: SteamID, nickname: String) {
        friends.setFriendNickname(target, nickname)
    }

    public fun add(target: SteamID) {
        friends.addFriend(target)
    }

    public fun add(target: String) {
        friends.addFriend(target)
    }

    public fun remove(target: SteamID) {
        friends.removeFriend(target)
    }

    public companion object Factory : ReadOnlyProperty<User, SteamHelper> {

        private val cache: MutableMap<Long, SteamHelper> = HashMap()

        override fun getValue(thisRef: User, property: KProperty<*>): SteamHelper {
            return cache.getOrPut(thisRef.id) { SteamHelper(id = thisRef.id) }
        }
    }
}