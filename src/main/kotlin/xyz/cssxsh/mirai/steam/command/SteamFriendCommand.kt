package xyz.cssxsh.mirai.steam.command

import `in`.dragonbra.javasteam.types.SteamID
import net.mamoe.mirai.console.command.*
import xyz.cssxsh.mirai.steam.*

public class SteamFriendCommand : CompositeCommand(
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
                appendLine("${relation.steamID} - ${relation.nickname} - ${relation.persona.name}")
            }
        }.ifEmpty { "列表为空" })
    }

    @SubCommand
    public fun UserCommandSender.ignore(target: SteamID, ignore: Boolean = true) {
        user.steam.ignoreFriend(target, ignore)
    }
}