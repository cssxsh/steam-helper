package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.enums.*
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.*
import `in`.dragonbra.javasteam.types.*

public data class SteamRelation(
    public val steamID: SteamID,
    public val relationship: EFriendRelationship,
    public val nickname: String?,
    public val persona: PersonaState?
)