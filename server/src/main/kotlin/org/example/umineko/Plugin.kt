package org.example.umineko

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import kotlinx.serialization.json.Json
import org.example.umineko.auth.JwtConfig


fun Application.installSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm

            verifier(
                JWT.require(Algorithm.HMAC256(JwtConfig.secret))
                    .withIssuer(JwtConfig.issuer)
                    .build()
            )

            validate { credential ->
                val uid = credential.payload.getClaim("uid").asLong()
                if (uid != null) JWTPrincipal(credential.payload) else null
            }

            authHeader { call ->
                call.request.cookies[JwtConfig.accessCookie]
                    ?.let { HttpAuthHeader.Single("Bearer", it) }
            }
        }
    }
}

fun Application.installCors() {
    install(CORS) {
        allowHost("localhost:8081", schemes = listOf("http"))
        allowCredentials = true
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
}

fun Application.installSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}
