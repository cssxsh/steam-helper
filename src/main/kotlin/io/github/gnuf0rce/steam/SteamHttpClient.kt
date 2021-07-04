package io.github.gnuf0rce.steam

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.compression.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*

open class SteamHttpClient {
    protected open val ignore: (Throwable) -> Boolean = { it is IOException || it is HttpRequestTimeoutException }

    protected open val timeout: Long = 30 * 1000L

    protected open val dns: Dns = DnsOverHttps("https://public.dns.iij.jp/dns-query")

    open val storage = AcceptAllCookiesStorage()

    open val locale: Locale = Locale.getDefault()

    open val version: String = "8"

    suspend fun session(): String = requireNotNull(storage.all()["sessionid"]) { "未找到会话" }.value

    protected open fun client() = HttpClient(OkHttp) {
        BrowserUserAgent()
        ContentEncoding {
            gzip()
            deflate()
            identity()
        }
        Json {
            serializer = KotlinxSerializer(SteamJson)
        }
        install(HttpCookies) {
            storage = this@SteamHttpClient.storage
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
        engine {
            config {
                sslSocketFactory(RubySSLSocketFactory, RubyX509TrustManager)
                hostnameVerifier { _, _ -> true }
                dns(dns)
            }
        }
    }

    suspend fun <R> useHttpClient(block: suspend (HttpClient) -> R): R = supervisorScope {
        while (isActive) {
            runCatching {
                client().use { block(it) }
            }.onSuccess {
                return@supervisorScope it
            }.onFailure {
                if (isActive && ignore(it)) {
                    // e.printStackTrace()
                } else {
                    throw it
                }
            }
        }
        throw CancellationException()
    }
}

fun DnsOverHttps(url: String): DnsOverHttps {
    return DnsOverHttps.Builder().apply {
        client(OkHttpClient())
        includeIPv6(false)
        url(url.toHttpUrl())
        post(true)
        resolvePrivateAddresses(false)
        resolvePublicAddresses(true)
    }.build()
}

object RubySSLSocketFactory : SSLSocketFactory() {

    private fun Socket.setServerNames(): Socket = when (this) {
        is SSLSocket -> apply {
            // logger.info { inetAddress.hostAddress }
            sslParameters = sslParameters.apply {
                // cipherSuites = supportedCipherSuites
                // protocols = supportedProtocols
                serverNames = serverNames.filterIsInstance<SNIHostName>().filterNot { "steampowered" in it.asciiName }
            }
        }
        else -> this
    }

    private val socketFactory: SSLSocketFactory = SSLContext.getDefault().socketFactory

    override fun createSocket(socket: Socket?, host: String?, port: Int, autoClose: Boolean): Socket? =
        socketFactory.createSocket(socket, host, port, autoClose)?.setServerNames()

    override fun createSocket(host: String?, port: Int): Socket? =
        socketFactory.createSocket(host, port)?.setServerNames()

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket? =
        socketFactory.createSocket(host, port, localHost, localPort)?.setServerNames()

    override fun createSocket(host: InetAddress?, port: Int): Socket? =
        socketFactory.createSocket(host, port)?.setServerNames()

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket? =
        socketFactory.createSocket(address, port, localAddress, localPort)?.setServerNames()

    override fun getDefaultCipherSuites(): Array<String> = emptyArray()

    override fun getSupportedCipherSuites(): Array<String> = emptyArray()
}

object RubyX509TrustManager : X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}