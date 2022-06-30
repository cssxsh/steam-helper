package xyz.cssxsh.mirai.steam

import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import xyz.cssxsh.mirai.steam.data.*

public object SteamListenerHost : SimpleListenerHost() {

    @EventHandler
    public fun BotOnlineEvent.init() {
        for (friend in bot.friends) {
            if (friend.id in SteamAuthData.bind) {
                friend.steam
            }
        }
        for (group in bot.groups) {
            for (member in group.members) {
                if (member.id in SteamAuthData.bind) {
                    member.steam
                }
            }
        }
    }

    @EventHandler
    public suspend fun MessageEvent.bind() {
        val content = message.contentToString()
        when {
             "steam-bind" in content -> {
                 sender.steam.auth()
            }
            "steam-friend" in content -> {
                subject.sendMessage(message = buildString {
                    for (relation in sender.steam.relations) {
                        if (relation.steamID.isIndividualAccount.not()) continue
                        if (relation.persona == null) continue
                        appendLine("${relation.steamID} - ${relation.persona.name} - ${relation.persona.state}")
                    }
                }.ifEmpty { "列表为空" })
            }
        }
    }
}