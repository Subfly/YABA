package dev.subfly.yaba.core.webview

/**
 * Host UI platform passed to the web reader/editor for theming.
 */
enum class YabaWebPlatform {
    Compose,
}

/**
 * Light/dark appearance hint for web components.
 */
enum class YabaWebAppearance {
    Auto,
    Light,
    Dark,
}

/**
 * Vertical scroll direction from touch gestures (toolbar visibility).
 */
enum class YabaWebScrollDirection {
    Up,
    Down,
}
