package dev.subfly.yabacore.toast

/**
 * Platform-specific toast text payload.
 *
 * - Android/JVM actual: Compose [org.jetbrains.compose.resources.StringResource]
 * - iOS actual: localized key [String]
 */
expect class PlatformToastText
