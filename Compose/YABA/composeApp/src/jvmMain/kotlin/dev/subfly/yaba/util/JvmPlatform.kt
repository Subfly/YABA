package dev.subfly.yaba.util

enum class YabaJvmOs {
    MAC,
    WINDOWS,
    LINUX
}

val JvmOs: YabaJvmOs
    get() {
        val osName = System.getProperty("os.name").lowercase()

        return when {
            osName.contains("win") -> YabaJvmOs.WINDOWS
            osName.contains("mac") -> YabaJvmOs.MAC
            else -> YabaJvmOs.LINUX
        }
    }
