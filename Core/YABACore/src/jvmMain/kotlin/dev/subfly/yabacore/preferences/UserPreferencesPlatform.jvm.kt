package dev.subfly.yabacore.preferences

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir

internal actual fun resolveUserSettingsDataStoreFile(
    subdir: String,
    fileName: String,
): PlatformFile {
    val settingsDir = (FileKit.filesDir / subdir).also { it.createDirectories() }
    return settingsDir / fileName
}

internal actual suspend fun migratePlatformPreferencesIfNeeded(store: UserPreferencesStore) {
    // No legacy AppStorage on JVM targets.
}
