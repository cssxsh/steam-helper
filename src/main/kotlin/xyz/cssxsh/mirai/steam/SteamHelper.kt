package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.ChatMsgCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendsListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.NicknameListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoginKeyCallback
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.mirai.steam.data.*
import java.io.Closeable
import kotlin.coroutines.*
import kotlin.properties.*
import kotlin.reflect.*

public class SteamHelper(public val id: Long) : CoroutineScope {
    private val logger = MiraiLogger.Factory.create(this::class, identity = "steam-helper-${id}")
    override val coroutineContext: CoroutineContext =
        CoroutineName(name = "steam-helper-${id}") + SupervisorJob() + CoroutineExceptionHandler { context, throwable ->
            logger.warning("$throwable in steam-helper-${id}", throwable)
        }

    private val configuration = SteamKitConfig.toSteamConfiguration()
    public val client: SteamClient = SteamClient(configuration)
    public val auth: SteamUser = client.handler()
    public val friends: SteamFriends = client.handler()
    public val manager: CallbackManager = client.manager()

    private val relationships: MutableMap<SteamID, EFriendRelationship> = HashMap()
    private val nicknames: MutableMap<SteamID, String> = HashMap()
    public val relationship: Sequence<SteamFriendRelationship>
        get() = sequence {
            for ((id, relationship) in relationships) {
                val nickname = nicknames[id] ?: "$id"
                yield(SteamFriendRelationship(
                    steamID = id,
                    relationship = relationship,
                    nickname = nickname
                ))
            }
        }

    private val listeners: MutableList<Closeable> = ArrayList()
    private var key: String by SteamAuthData
    private var name: String by SteamAuthData

    init {
        //
        listeners += manager.subscribe<ConnectedCallback> {
            if (auth.steamID == null && key.isNotEmpty()) {
                logger.info("Connected, try refresh $name")
                refresh()
            } else {
                logger.info("Connected")
            }
        }
        listeners += manager.subscribe<DisconnectedCallback> { callback ->
            if (callback.isUserInitiated) {
                logger.info("Disconnected from Steam")
            } else {
                logger.warning("Disconnected from Steam, Try reconnect")
                client.connect()
            }
        }
        //
        listeners += manager.subscribe<LoggedOnCallback> { callback ->
            when (callback.result) {
                EResult.OK -> logger.info("Successfully logged on ${callback.clientSteamID} !")
                EResult.AccountLogonDenied -> logger.warning("Unable to logon to Steam: This account is SteamGuard protected.")
                else -> logger.error("Unable to logon to Steam: ${callback.result}, https://steamerrors.com/${callback.result.code()}")
            }
        }
        listeners += manager.subscribe<LoginKeyCallback> { callback ->
            logger.debug("refresh login key $key for $name")
            key = callback.loginKey
        }
        listeners += manager.subscribe<LoggedOffCallback> { callback ->
            logger.warning("Logged off of Steam: ${callback.result}")
        }
        //
        listeners += manager.subscribe<FriendsListCallback> { callback ->
            logger.debug("refresh friends for $name")
            callback.friendList.forEach { friend -> relationships[friend.steamID] = friend.relationship }
            if (nicknames.isEmpty()) {
                friends.requestFriendInfo(relationships.keys.toList(), 0)
            }
        }
        listeners += manager.subscribe<NicknameListCallback> { callback ->
            logger.debug("refresh nicknames for $name")
            callback.nicknames.forEach { nickname -> nicknames[nickname.steamID] = nickname.nickname }
        }
        //
        listeners += manager.subscribe<ChatMsgCallback> { callback ->
            launch {
                val user = user
                val nickname = nicknames[callback.chatterID] ?: "anno"
                val forward = buildForwardMessage(user) {
                    user.bot named nickname says "${callback.chatMsgType} at ${callback.chatRoomID}"
                    user.bot named nickname says callback.message
                }

                user.sendMessage(message = forward)
            }
        }

        launch {
            client.connect()
            while (isActive) {
                manager.runWaitCallbacks(1000L)
            }
        }
    }

    public val user: User
        get() {
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

    public fun auth(username: String, password: String, code: String) {
        auth.logOn(LogOnDetails().apply {
            this.username = username
            this.password = password
            this.twoFactorCode = code
            this.clientLanguage = "chinese"
            this.isShouldRememberPassword = true

            name = this.username
        })
    }

    public fun refresh() {
        auth.logOn(LogOnDetails().apply {
            this.username = name
            this.loginKey = key
            this.clientLanguage = "chinese"
            this.isShouldRememberPassword = true
        })
    }

    public companion object Factory : ReadOnlyProperty<User, SteamHelper> {

        private val cache: MutableMap<Long, SteamHelper> = HashMap()

        override fun getValue(thisRef: User, property: KProperty<*>): SteamHelper {
            return cache.getOrPut(thisRef.id) { SteamHelper(id = thisRef.id) }
        }
    }
}