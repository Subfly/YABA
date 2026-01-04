package dev.subfly.yaba.util

enum class YabaPlatform {
    JVM,
    ANDROID,
}

expect val Platform: YabaPlatform
