package org.example.umineko

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

suspend fun main() {
    initDB()
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(KtorCachePlugin)

    install(ContentNegotiation){
        json(
            Json{
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }

    install(CORS) {
        anyHost() // 允许所有主机

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)

        // 允许的请求头
        allowHeader("Content-Type")
        allowHeader("Authorization")
        allowHeader("Accept")
        allowHeader("Origin")
        allowHeader("X-Requested-With")

        // 是否允许凭据（cookies、认证头等）
        allowCredentials = true
    }

    routing {
        get("/"){
            call.respondText("Hello World!", ContentType.Text.Plain)
        }
        get("/message") {
            val responses = listOf("哈基米", "曼波", "欧耶", "哦马琪里，曼波", "哇夏，曼波", "傻了吧唧")
            val randomResponse = responses.random()
            call.respondText(randomResponse)
        }
        get("/umineko/{id}") {
            val idString = call.parameters["id"]
            val id = idString?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id parameter")
                return@get
            }
            val result = demoTableDao.findMessage(id)
            call.respondText(result, ContentType.Application.Json)
        }
    }
}