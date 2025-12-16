package org.example.umineko

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


class ApiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendMessage(): String {
        return try {
            client.get("http://$SERVER_IP:$SERVER_PORT/umineko") {
                contentType(ContentType.Text.Plain)
            }.body()
        } catch (e: Exception) {
            e.printStackTrace()
            "ServerBoom了喵"
        }
    }
}