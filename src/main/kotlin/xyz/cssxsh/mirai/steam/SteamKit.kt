package xyz.cssxsh.mirai.steam

import `in`.dragonbra.javasteam.enums.*
import `in`.dragonbra.javasteam.handlers.*
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.*
import `in`.dragonbra.javasteam.steam.steamclient.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
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
    HttpClient(OkHttp) {
        expectSuccess = true
        BrowserUserAgent()
        ContentEncoding()
    }
}

private val mutex = Mutex()

/**
 * @param size `<empty>`, `medium`, `full`
 */
public suspend fun PersonaState?.avatar(size: String = ""): File {
    val hex = this?.avatarHash?.joinToString(separator = "") { byte -> "%02x".format(byte) }
        .takeIf { it != "0000000000000000000000000000000000000000" }
        ?: "fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb"
    val folder = File(System.getProperty("xyz.cssxsh.mirai.steam.avatar", "."))
    val name = if (size.isEmpty()) "${hex}.jpg" else "${hex}_${size}.jpg"
    val avatar = folder.resolve(name)

    mutex.withLock {
        if (!avatar.exists()) {
            folder.mkdirs()

            avatar.writeBytes(http.get("https://avatars.akamai.steamstatic.com/$name").body())
        }
    }

    return avatar
}

public val User.steam: SteamHelper by SteamHelper.Factory