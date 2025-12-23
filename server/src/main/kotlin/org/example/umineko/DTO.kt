package org.example.umineko

import kotlinx.serialization.Serializable

@Serializable
data class LoginReq(
    val username: String,
    val password: String
)
