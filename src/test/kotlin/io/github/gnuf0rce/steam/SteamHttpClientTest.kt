package io.github.gnuf0rce.steam

import io.github.gnuf0rce.steam.api.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

internal class SteamHttpClientTest {
    private val client: SteamHttpClient by lazy {
        object : SteamHttpClient() {
            init {
                runBlocking {
                    storage.save(cookies)
                }
            }

            override val ignore: (Throwable) -> Boolean = { throwable ->
                super.ignore(throwable).also {
                    if (it) println("Ignore $throwable")
                }
            }
        }
    }

    private val test: File = File("./test")

    private val cookies: List<Cookie> by lazy { readCookies(test.resolve("cookie.json")) }

    @Test
    fun index(): Unit = runBlocking {
        test.resolve("store.html").writeText(client.useHttpClient { it.get(STORE_INDEX_URL) })

        test.resolve("community.html").writeText(client.useHttpClient { it.get(COMMUNITY_INDEX_URL) })
    }
}