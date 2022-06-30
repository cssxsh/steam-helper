package xyz.cssxsh.mirai.steam.command

import `in`.dragonbra.javasteam.types.SteamID
import net.mamoe.mirai.console.command.descriptor.*

public val SteamCommandArgumentContext: CommandArgumentContext = buildCommandArgumentContext {
    SteamID::class with { SteamID().apply { setFromSteam3String(it) } }
}