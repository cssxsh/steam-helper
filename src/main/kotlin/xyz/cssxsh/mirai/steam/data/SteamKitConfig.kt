package xyz.cssxsh.mirai.steam.data

import `in`.dragonbra.javasteam.networking.steam3.ProtocolTypes
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import okhttp3.OkHttpClient
import xyz.cssxsh.mirai.steam.*
import java.io.File

public object SteamKitConfig : ReadOnlyPluginConfig("SteamKit") {

    private var folder = File("./servers.bin")

    private val http: OkHttpClient = OkHttpClient()

    @ValueName("web_api_key")
    public val key: String by value()

    @ValueName("connection_timeout")
    public val timeout: Long by value(30_000L)

    @ValueName("protocol_type")
    public val protocol: ProtocolTypes by value(ProtocolTypes.WEB_SOCKET)

    @ValueName("dns_over_https")
    public val doh: String by value("https://public.dns.iij.jp/dns-query")

    @OptIn(ConsoleExperimentalApi::class)
    override fun onInit(owner: PluginDataHolder, storage: PluginDataStorage) {
        if (owner is JvmPlugin) {
            folder = owner.dataFolder
        }
    }

    public fun toSteamConfiguration(): SteamConfiguration {
        return SteamConfiguration.create { builder ->
            builder.withConnectionTimeout(timeout)
            builder.withWebAPIKey(key)
            builder.withProtocolTypes(protocol)
            builder.withServerListProvider(FileServerListProvider(folder.resolve("servers.bin")))
            builder.withHttpClient(http.newBuilder().dns(DnsOverHttps(doh)).build())
        }
    }
}