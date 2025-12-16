package org.example.umineko


interface Platform {
    val name: String
}

expect fun getPlatform(): Platform