package dev.subfly.yaba

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform