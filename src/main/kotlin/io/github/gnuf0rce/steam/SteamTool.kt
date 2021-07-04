package io.github.gnuf0rce.steam

import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

internal val SteamJson = Json {
    isLenient = true
    ignoreUnknownKeys = true
    allowSpecialFloatingPointValues = true
    useArrayPolymorphism = false
}

@Serializable
data class EditThisCookie(
    @SerialName("domain")
    val domain: String,
    @SerialName("expirationDate")
    val expirationDate: Double? = null,
    @SerialName("hostOnly")
    val hostOnly: Boolean,
    @SerialName("httpOnly")
    val httpOnly: Boolean,
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("path")
    val path: String,
    @SerialName("sameSite")
    val sameSite: String,
    @SerialName("secure")
    val secure: Boolean,
    @SerialName("session")
    val session: Boolean,
    @SerialName("storeId")
    val storeId: String,
    @SerialName("value")
    val value: String
)

fun EditThisCookie.toCookie() = Cookie(
    name = name,
    value = value,
    encoding = CookieEncoding.DQUOTES,
    expires = GMTDate(expirationDate?.times(1000)?.toLong()),
    domain = domain,
    path = path,
    secure = secure,
    httpOnly = httpOnly
)

fun readCookies(file: File): List<Cookie> {
    return SteamJson.decodeFromString<List<EditThisCookie>>(file.readText()).map {
        it.toCookie()
    }
}