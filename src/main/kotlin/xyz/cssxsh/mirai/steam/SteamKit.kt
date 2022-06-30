package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.enums.*
import `in`.dragonbra.javasteam.handlers.*
import `in`.dragonbra.javasteam.steam.steamclient.*
import net.mamoe.mirai.contact.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps

public inline fun <reified H : ClientMsgHandler> SteamClient.handler(): H = getHandler(H::class.java)

public fun DnsOverHttps(url: String, ipv6: Boolean = false): DnsOverHttps {
    return DnsOverHttps.Builder()
        .client(okhttp3.OkHttpClient())
        .url(url.toHttpUrl())
        .post(true)
        .includeIPv6(ipv6)
        .resolvePrivateAddresses(false)
        .resolvePublicAddresses(true)
        .build()
}

public fun EResult.url(): String = "https://steamerrors.com/${code()}"

public val User.steam: SteamHelper by SteamHelper.Factory