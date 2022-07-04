package xyz.cssxsh.mirai.steam.command

import `in`.dragonbra.javasteam.types.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.message.*
import xyz.cssxsh.mirai.steam.*

public object SteamSendCommand : SimpleCommand(
    owner = SteamHelperPlugin,
    "steam-send",
    description = "绑定 Steam 账号",
    overrideContext = SteamCommandArgumentContext
) {
    @Handler
    public suspend fun CommandSenderOnMessage<*>.handle(target: SteamID) {
        this as UserCommandSender
        sendMessage(message = "请输入消息")
        val message = fromEvent.nextMessage()
        user.steam.sendMessage(target, message)
    }
}