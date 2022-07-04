package xyz.cssxsh.mirai.steam.command

import `in`.dragonbra.javasteam.types.*
import net.mamoe.mirai.console.command.*
import xyz.cssxsh.mirai.steam.*

public object SteamFriendCommand : CompositeCommand(
    owner = SteamHelperPlugin,
    "steam-friend",
    description = "绑定 Steam 账号",
    overrideContext = SteamCommandArgumentContext
) {
    @SubCommand
    public suspend fun UserCommandSender.list() {
        sendMessage(message = buildString {
            for (relation in user.steam.relations) {
                if (relation.steamID.isIndividualAccount.not()) continue
                if (relation.persona == null) continue
                appendLine("${relation.steamID} - ${relation.nickname} - ${relation.persona.gameName}")
            }
        }.ifEmpty { "列表为空" })
    }

    @SubCommand
    public fun UserCommandSender.ignore(target: SteamID, ignore: Boolean = true) {
        user.steam.ignore(target, ignore)
    }

    @SubCommand
    public fun UserCommandSender.nickname(target: SteamID, nickname: String) {
        user.steam.nickname(target, nickname)
    }

    @SubCommand
    public fun UserCommandSender.add(target: SteamID) {
        user.steam.add(target)
    }

    @SubCommand
    public fun UserCommandSender.plus(target: String) {
        user.steam.add(target)
    }

    @SubCommand
    public fun UserCommandSender.remove(target: SteamID) {
        user.steam.remove(target)
    }
}