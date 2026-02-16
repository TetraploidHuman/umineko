package org.example.umineko

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
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

    suspend fun sendMessage(
        maxRetries: Int = 3,
        onRetry: (String) -> Unit // 用于实时回传重试次数
    ): String {
        var currentRetry = 0

        while (currentRetry <= maxRetries) {
            try {
                // 如果是重试，通知 UI 更新次数
                if (currentRetry > 0) {
                    onRetry("$currentRetry/$maxRetries")
                }

                return client.get("http://$SERVER_IP:$SERVER_PORT/message") {
                    contentType(ContentType.Text.Plain)
                }.body()

            } catch (e: Exception) {
                currentRetry++
                if (currentRetry > maxRetries) {
                    e.printStackTrace()
                    return "ServerBoom了喵"
                }
                // 可以在这里加一个微小的延迟，避免瞬间完成3次重试
                kotlinx.coroutines.delay(3000)
            }
        }
        return "ServerBoom了喵"
    }
}