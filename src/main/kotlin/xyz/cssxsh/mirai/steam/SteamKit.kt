package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.enums.*
import `in`.dragonbra.javasteam.handlers.*
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.*
import `in`.dragonbra.javasteam.steam.steamclient.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.sync.*
import net.mamoe.mirai.contact.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.File

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

private val http by lazy {
    HttpClient(OkHttp)
}

private val mutex = Mutex()

public suspend fun PersonaState.avatar(): File {
    val hex = avatarHash.joinToString(separator = "") { byte -> "%02x".format(byte) }
    val folder = File(System.getProperty("xyz.cssxsh.mirai.steam.avatar", "."))
    val avatar = folder.resolve("${hex}_full.jpg")

    mutex.withLock {
        if (!avatar.exists()) {
            folder.mkdirs()

            val bytes: ByteArray = http.get("https://avatars.akamai.steamstatic.com/${hex}_full.jpg").body()

            avatar.writeBytes(bytes)
        }
    }

    return avatar
}

public val User.steam: SteamHelper by SteamHelper.Factory