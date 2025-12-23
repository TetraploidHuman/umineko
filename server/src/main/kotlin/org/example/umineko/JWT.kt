package org.example.umineko.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtUtil {

    private val algo = Algorithm.HMAC256(JwtConfig.secret)

    fun access(uid: Long, role: String, ver: Int): String =
        JWT.create()
            .withIssuer(JwtConfig.issuer)
            .withClaim("uid", uid)
            .withClaim("role", role)
            .withClaim("ver", ver)
            .withExpiresAt(Date(System.currentTimeMillis() + 15 * 60_000))
            .sign(algo)

    fun refresh(uid: Long, ver: Int): String =
        JWT.create()
            .withIssuer(JwtConfig.issuer)
            .withClaim("uid", uid)
            .withClaim("ver", ver)
            .withExpiresAt(Date(System.currentTimeMillis() + 7 * 24 * 3600_000))
            .sign(algo)
}

object JwtConfig {
    const val issuer = "umineko"
    const val audience = "umineko-client"
    const val realm = "umineko"
    const val secret = "CHANGE_ME"

    const val accessCookie = "ACCESS_TOKEN"
    const val refreshCookie = "REFRESH_TOKEN"
}