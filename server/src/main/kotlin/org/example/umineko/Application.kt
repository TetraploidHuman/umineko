package org.example.umineko

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.example.umineko.auth.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.request.receive
import org.example.umineko.DB.Role
import org.example.umineko.DB.demoTableDao
import org.example.umineko.DB.findById
import org.example.umineko.DB.findByUsername
import org.example.umineko.DB.increaseTokenVersion
import org.example.umineko.DB.verifyPassword
import org.example.umineko.DSL.KtorCachePlugin

suspend fun main() {
    initDB()
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(KtorCachePlugin) //自定义的缓存插件
    installSerialization() //JSON的序列化插件
    installCors()
    installSecurity()

    routing {
        testRoutes()
        authRoutes()
        logoutRoute()
        protectedRoutes()
    }
}


