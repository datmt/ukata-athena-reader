package io.ukata.athena

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
