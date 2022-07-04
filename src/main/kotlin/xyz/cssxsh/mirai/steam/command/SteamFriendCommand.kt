package xyz.cssxsh.mirai.steam.command

import `in`.dragonbra.javasteam.types.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import xyz.cssxsh.mirai.steam.*

public object SteamFriendCommand : CompositeCommand(
    owner = SteamHelperPlugin,
    "steam-friend",
    description = "Steam 好友管理",
    overrideContext = SteamCommandArgumentContext
) {
    @SubCommand
    public suspend fun UserCommandSender.list() {
        sendMessage(message = buildMessageChain {
            appendLine("${user.steam.name}的好友列表")
            for (relation in user.steam.relations) {
                if (relation.steamID.isIndividualAccount.not()) continue
                if (relation.persona == null) continue
                try {
                    append(relation.persona.avatar().uploadAsImage(subject))
                } catch (_: Throwable) {
                    //
                }
                appendLine("${relation.steamID} - ${relation.nickname} - ${relation.persona.gameName}")
            }
        })
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
    public fun UserCommandSender.plus(name: String) {
        user.steam.add(name)
    }

    @SubCommand
    public fun UserCommandSender.remove(target: SteamID) {
        user.steam.remove(target)
    }
}