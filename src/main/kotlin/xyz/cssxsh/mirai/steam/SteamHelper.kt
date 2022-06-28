package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.Friend as SteamFriend
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendsListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoginKeyCallback
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.*
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
    public val chat: SteamFriends = client.handler()
    public val manager: CallbackManager = client.manager()
    public var friends: List<SteamFriend> = ArrayList()
        private set

    private val listeners: MutableList<Closeable> = ArrayList()
    private var key: String by SteamAuthData
    private var name: String by SteamAuthData
    private var temp = ""

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
                logger.error("Disconnected from Steam, Try reconnect")
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
            key = callback.loginKey
            name = temp
        }
        listeners += manager.subscribe<LoggedOffCallback> { callback ->
            logger.warning("Logged off of Steam: ${callback.result}")
        }
        listeners += manager.subscribe<FriendsListCallback> { callback ->
            friends = if (!callback.isIncremental) friends + callback.friendList else callback.friendList
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

            temp = this.username
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