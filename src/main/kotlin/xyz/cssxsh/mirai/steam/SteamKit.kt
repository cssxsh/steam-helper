package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.ICallbackMsg
import net.mamoe.mirai.contact.User
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.Closeable

public inline fun <reified H : ClientMsgHandler> SteamClient.handler(): H = getHandler(H::class.java)

public fun SteamClient.manager(): CallbackManager = CallbackManager(this)

public inline fun <reified C : ICallbackMsg> CallbackManager.subscribe(noinline block: (callback: C) -> Unit): Closeable {
    return subscribe(C::class.java, block)
}

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

public val User.steam: SteamHelper by SteamHelper.Factory