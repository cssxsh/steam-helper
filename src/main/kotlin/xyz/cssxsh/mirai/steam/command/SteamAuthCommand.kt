package xyz.cssxsh.mirai.steam.command

import net.mamoe.mirai.console.command.*
import xyz.cssxsh.mirai.steam.*

public object SteamAuthCommand : SimpleCommand(
    owner = SteamHelperPlugin,
    "steam-auth",
    description = "绑定 Steam 账号",
    overrideContext = SteamCommandArgumentContext
) {
    @Handler
    public fun UserCommandSender.auth() {
        user.steam.auth(user = user)
    }
}