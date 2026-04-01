package dev.subfly.yaba.core.filesystem

import dev.subfly.yaba.core.common.CoreConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Default empty Excalidraw scene JSON. */
const val EMPTY_CANVAS_SCENE_JSON = """{"type":"excalidraw","version":2,"source":"https://excalidraw.com","elements":[],"appState":{"viewBackgroundColor":"#ffffff"},"files":{}}"""

object CanvmarkFileManager {
    fun sceneRelativePath(bookmarkId: String): String =
        CoreConstants.FileSystem.Canvmark.scenePath(bookmarkId)

    suspend fun ensureEmptyScene(bookmarkId: String) {
        val relativePath = sceneRelativePath(bookmarkId)
        val file = BookmarkFileManager.resolve(relativePath)
        if (!file.exists()) {
            BookmarkFileManager.writeBytes(relativePath, EMPTY_CANVAS_SCENE_JSON.encodeToByteArray())
        }
    }

    suspend fun writeScene(
        bookmarkId: String,
        sceneJson: String,
    ) {
        val relativePath = sceneRelativePath(bookmarkId)
        BookmarkFileManager.writeBytes(relativePath, sceneJson.encodeToByteArray())
    }

    suspend fun readScene(bookmarkId: String): String? {
        val file = BookmarkFileManager.find(sceneRelativePath(bookmarkId)) ?: return null
        return withContext(Dispatchers.IO) { file.readBytes().decodeToString() }
    }

    suspend fun readSceneByRelativePath(relativePath: String): String? {
        val file = BookmarkFileManager.find(relativePath) ?: return null
        return withContext(Dispatchers.IO) { file.readBytes().decodeToString() }
    }
}
