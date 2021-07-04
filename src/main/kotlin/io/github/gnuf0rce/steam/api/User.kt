package io.github.gnuf0rce.steam.api

import io.github.gnuf0rce.steam.SteamHttpClient
import io.github.gnuf0rce.steam.data.UserData
import io.ktor.client.request.*


const val STORE_USERDATA_URL = "https://store.steampowered.com/dynamicstore/userdata/?id=232190062&cc=CN&v=8"

suspend fun SteamHttpClient.userdata(id: Long? = null): UserData = useHttpClient {
    it.get(STORE_USERDATA_URL) {
        parameter("id", id)
        parameter("cc", locale.country)
        parameter("v", version)
    }
}