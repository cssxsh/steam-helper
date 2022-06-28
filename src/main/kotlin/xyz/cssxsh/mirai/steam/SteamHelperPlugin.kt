package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.util.log.LogManager
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.mirai.steam.data.*
import java.util.*

public object SteamHelperPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.steam-helper",
        name = "steam-helper",
        version = "1.0.0",
    ) {
        author("cssxsh")

        dependsOn("net.mamoe.mirai.mirai-slf4j-bridge", true)
    }
) {

    init {
        val cache: MutableMap<Class<*>, MiraiLogger> = WeakHashMap()
        LogManager.addListener { clazz, message, throwable ->
            cache.getOrPut(clazz) { MiraiLogger.Factory.create(clazz, clazz.name) }
                .debug(message, throwable)
        }
    }

    override fun onEnable() {
        SteamKitConfig.reload()
        SteamAuthData.reload()

        SteamListenerHost.registerTo(globalEventChannel())
    }

    override fun onDisable() {
        SteamListenerHost.cancelAll()
    }
}