package dev.subfly.yaba.util

enum class YabaDesktopPlatform {
    MAC,
    WINDOWS,
    LINUX
}

val DesktopPlatform: YabaDesktopPlatform
    get() {
        val osName = System.getProperty("os.name").lowercase()

        return when {
            osName.contains("win") -> YabaDesktopPlatform.WINDOWS
            osName.contains("mac") -> YabaDesktopPlatform.MAC
            else -> YabaDesktopPlatform.LINUX
        }
    }
