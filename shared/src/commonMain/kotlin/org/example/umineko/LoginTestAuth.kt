package org.example.umineko

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ApiClient {

    val client = HttpClient(CIO) {

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        // 关键：Cookie 自动保存 & 自动发送
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }

        expectSuccess = false
    }

    const val BASE_URL = "http://localhost:8080"
}

@Serializable
data class LoginReq(
    val username: String,
    val password: String
)
class AuthViewModel {

    private val _result = MutableStateFlow("Ready")
    val result: StateFlow<String> = _result.asStateFlow()

    suspend fun login(username: String, password: String) =
        request("POST /login") {
            ApiClient.client.post("${ApiClient.BASE_URL}/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginReq(username, password))
            }
        }

    suspend fun profile() =
        request("GET /profile") {
            ApiClient.client.get("${ApiClient.BASE_URL}/profile")
        }

    suspend fun admin() =
        request("GET /admin") {
            ApiClient.client.get("${ApiClient.BASE_URL}/admin")
        }

    suspend fun logout() =
        request("POST /logout") {
            ApiClient.client.post("${ApiClient.BASE_URL}/logout")
        }

    private suspend fun request(
        title: String,
        block: suspend () -> HttpResponse
    ) {
        _result.value = "⏳ $title ..."
        try {
            val res = block()
            val text = res.bodyAsText()
            _result.value = """
                ✅ $title
                Status: ${res.status}
                Response:
                $text
            """.trimIndent()
        } catch (e: Exception) {
            _result.value = """
                ❌ $title
                Exception:
                ${e::class.simpleName}
                ${e.message}
            """.trimIndent()
        }
    }
}