package io.github.gnuf0rce.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin

object SteamHelperPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "io.github.gnuf0rce.steam-helper",
        name = "steam-helper",
        version = "0.1.0",
    ) {
        author("cssxsh")
    }
) {
    override fun onEnable() {
        //
    }
}