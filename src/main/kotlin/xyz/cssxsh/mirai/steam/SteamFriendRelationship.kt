package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.types.SteamID

public data class SteamFriendRelationship(
    public val steamID: SteamID,
    public val relationship: EFriendRelationship,
    public val nickname: String
)