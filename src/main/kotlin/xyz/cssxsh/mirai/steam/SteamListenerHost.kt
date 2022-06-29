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
             content.startsWith("#steam-bind") -> {
                // TODO: steam bind
            }
            "#steam-friends" in content -> {
                subject.sendMessage(message = buildString {
                    for (friend in sender.steam.relationship) {
                        if (friend.steamID.isIndividualAccount.not()) continue
                        appendLine("${friend.steamID} - ${friend.nickname} - ${friend.relationship}")
                    }
                })
            }
        }
    }
}