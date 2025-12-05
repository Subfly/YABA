package dev.subfly.yabacore.filesystem.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

private const val FILE_SETTINGS_SUBDIR = "settings"
private const val FILE_SETTINGS_FILENAME = "file_settings.preferences_pb"
private val FILE_ROOT_PATH_KEY = stringPreferencesKey("file_root_path")

class FileSystemSettingsStore
internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val rootPathFlow: Flow<String?> = dataStore.data.map { it[FILE_ROOT_PATH_KEY] }

    suspend fun getRootPath(): String? = dataStore.data.first()[FILE_ROOT_PATH_KEY]

    suspend fun setRootPath(absolutePath: String) {
        dataStore.edit { prefs -> prefs[FILE_ROOT_PATH_KEY] = absolutePath }
    }

    suspend fun setRootPath(file: PlatformFile) {
        setRootPath(file.absolutePath())
    }

    suspend fun clearRootPath() {
        dataStore.edit { prefs -> prefs.remove(FILE_ROOT_PATH_KEY) }
    }
}

object FileSystemSettingsStoreFactory {
    fun create(
        coroutineContext: CoroutineContext = Dispatchers.Default,
    ): FileSystemSettingsStore = FileSystemSettingsStore(
        createFileSettingsDataStore(coroutineContext),
    )
}

object FileSystemSettings {
    val store: FileSystemSettingsStore by lazy { FileSystemSettingsStoreFactory.create() }
}

private fun createFileSettingsDataStore(
    coroutineContext: CoroutineContext,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        scope = CoroutineScope(SupervisorJob() + coroutineContext),
        produceFile = { resolveDataStoreFile().absolutePath().toPath() },
    )

private fun resolveDataStoreFile(): PlatformFile {
    val settingsDir = (FileKit.filesDir / FILE_SETTINGS_SUBDIR).also { it.createDirectories() }
    return settingsDir / FILE_SETTINGS_FILENAME
}
