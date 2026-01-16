@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.filesystem.access.FileAccessProvider
import dev.subfly.yabacore.filesystem.json.BookmarkMetaJson
import dev.subfly.yabacore.filesystem.json.DeletedJson
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.filesystem.json.LinkJson
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import dev.subfly.yabacore.sync.VectorClock
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Unified manager for reading/writing entity JSON files.
 *
 * This is the primary interface for filesystem operations on entity data.
 * Each entity type (folder, tag, bookmark) has its own directory containing
 * JSON metadata files.
 *
 * File layout:
 * - /folders/<uuid>/meta.json, deleted.json
 * - /tags/<uuid>/meta.json, deleted.json
 * - /bookmarks/<uuid>/meta.json, link.json, deleted.json, /content/
 */
object EntityFileManager {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val accessProvider = FileAccessProvider

    // ==================== Folder Operations ====================

    suspend fun readFolderMeta(folderId: Uuid): FolderMetaJson? {
        val path = CoreConstants.FileSystem.folderMetaPath(folderId)
        return readJson(path)
    }

    suspend fun writeFolderMeta(folder: FolderMetaJson) {
        val path = CoreConstants.FileSystem.folderMetaPath(Uuid.parse(folder.id))
        writeJson(path, folder)
    }

    suspend fun isFolderDeleted(folderId: Uuid): Boolean {
        val path = CoreConstants.FileSystem.folderDeletedPath(folderId)
        return fileExists(path)
    }

    suspend fun writeFolderDeleted(folderId: Uuid, clock: VectorClock) {
        val path = CoreConstants.FileSystem.folderDeletedPath(folderId)
        val deleted = DeletedJson(
            id = folderId.toString(),
            deleted = true,
            clock = clock.toMap(),
        )
        writeJson(path, deleted)
    }

    suspend fun deleteFolder(folderId: Uuid, clock: VectorClock) {
        // System folders cannot be deleted
        if (CoreConstants.Folder.isSystemFolder(folderId.toString())) {
            return
        }
        // Write the tombstone first
        writeFolderDeleted(folderId, clock)
        // Remove meta.json if it exists
        val metaPath = CoreConstants.FileSystem.folderMetaPath(folderId)
        deleteFile(metaPath)
    }

    /**
     * Removes any deleted.json tombstone for a folder.
     * Used for system folder self-healing.
     */
    suspend fun removeFolderTombstone(folderId: Uuid) {
        val path = CoreConstants.FileSystem.folderDeletedPath(folderId)
        deleteFile(path)
    }

    // ==================== Tag Operations ====================

    suspend fun readTagMeta(tagId: Uuid): TagMetaJson? {
        val path = CoreConstants.FileSystem.tagMetaPath(tagId)
        return readJson(path)
    }

    suspend fun writeTagMeta(tag: TagMetaJson) {
        val path = CoreConstants.FileSystem.tagMetaPath(Uuid.parse(tag.id))
        writeJson(path, tag)
    }

    suspend fun isTagDeleted(tagId: Uuid): Boolean {
        val path = CoreConstants.FileSystem.tagDeletedPath(tagId)
        return fileExists(path)
    }

    suspend fun writeTagDeleted(tagId: Uuid, clock: VectorClock) {
        val path = CoreConstants.FileSystem.tagDeletedPath(tagId)
        val deleted = DeletedJson(
            id = tagId.toString(),
            deleted = true,
            clock = clock.toMap(),
        )
        writeJson(path, deleted)
    }

    suspend fun deleteTag(tagId: Uuid, clock: VectorClock) {
        // System tags cannot be deleted
        if (CoreConstants.Tag.isSystemTag(tagId.toString())) {
            return
        }
        // Write the tombstone first
        writeTagDeleted(tagId, clock)
        // Remove meta.json if it exists
        val metaPath = CoreConstants.FileSystem.tagMetaPath(tagId)
        deleteFile(metaPath)
    }

    /**
     * Removes any deleted.json tombstone for a tag.
     * Used for system tag self-healing.
     */
    suspend fun removeTagTombstone(tagId: Uuid) {
        val path = CoreConstants.FileSystem.tagDeletedPath(tagId)
        deleteFile(path)
    }

    // ==================== Bookmark Operations ====================

    suspend fun readBookmarkMeta(bookmarkId: Uuid): BookmarkMetaJson? {
        val path = CoreConstants.FileSystem.bookmarkMetaPath(bookmarkId)
        return readJson(path)
    }

    suspend fun writeBookmarkMeta(bookmark: BookmarkMetaJson) {
        val path = CoreConstants.FileSystem.bookmarkMetaPath(Uuid.parse(bookmark.id))
        writeJson(path, bookmark)
    }

    suspend fun readLinkJson(bookmarkId: Uuid): LinkJson? {
        val path = CoreConstants.FileSystem.bookmarkLinkPath(bookmarkId)
        return readJson(path)
    }

    suspend fun writeLinkJson(bookmarkId: Uuid, link: LinkJson) {
        val path = CoreConstants.FileSystem.bookmarkLinkPath(bookmarkId)
        writeJson(path, link)
    }

    suspend fun isBookmarkDeleted(bookmarkId: Uuid): Boolean {
        val path = CoreConstants.FileSystem.bookmarkDeletedPath(bookmarkId)
        return fileExists(path)
    }

    suspend fun writeBookmarkDeleted(bookmarkId: Uuid, clock: VectorClock) {
        val path = CoreConstants.FileSystem.bookmarkDeletedPath(bookmarkId)
        val deleted = DeletedJson(
            id = bookmarkId.toString(),
            deleted = true,
            clock = clock.toMap(),
        )
        writeJson(path, deleted)
    }

    suspend fun getBookmarkContentDir(bookmarkId: Uuid): PlatformFile {
        val path = CoreConstants.FileSystem.bookmarkContentPath(bookmarkId)
        val dir = accessProvider.resolveRelativePath(path, ensureParentExists = true)
        dir.createDirectories()
        return dir
    }

    suspend fun deleteBookmark(bookmarkId: Uuid, clock: VectorClock) {
        // Write the tombstone first
        writeBookmarkDeleted(bookmarkId, clock)
        // Remove meta.json, link.json, and content directory
        val metaPath = CoreConstants.FileSystem.bookmarkMetaPath(bookmarkId)
        val linkPath = CoreConstants.FileSystem.bookmarkLinkPath(bookmarkId)
        val contentPath = CoreConstants.FileSystem.bookmarkContentPath(bookmarkId)
        deleteFile(metaPath)
        deleteFile(linkPath)
        deleteDirectory(contentPath)
    }

    // ==================== Scanning Operations ====================

    suspend fun scanAllFolders(): List<Uuid> {
        return scanEntityDirectory(CoreConstants.FileSystem.FOLDERS_DIR)
    }

    suspend fun scanAllTags(): List<Uuid> {
        return scanEntityDirectory(CoreConstants.FileSystem.TAGS_DIR)
    }

    suspend fun scanAllBookmarks(): List<Uuid> {
        return scanEntityDirectory(CoreConstants.FileSystem.BOOKMARKS_DIR)
    }

    /**
     * Reads the deleted.json file for any entity type.
     */
    suspend fun readDeleted(entityPath: String): DeletedJson? {
        val deletedPath = CoreConstants.FileSystem.join(entityPath, CoreConstants.FileSystem.DELETED_JSON)
        return readJson(deletedPath)
    }

    // ==================== Internal Helpers ====================

    private suspend inline fun <reified T> readJson(relativePath: String): T? {
        val file = accessProvider.resolveRelativePath(relativePath, ensureParentExists = false)
        if (!file.exists()) return null
        return try {
            val content = withContext(Dispatchers.IO) { file.readString() }
            json.decodeFromString<T>(content)
        } catch (e: Exception) {
            null
        }
    }

    private suspend inline fun <reified T> writeJson(relativePath: String, data: T) {
        val file = accessProvider.resolveRelativePath(relativePath, ensureParentExists = true)
        val content = json.encodeToString(data)
        withContext(Dispatchers.IO) {
            file.write(content.encodeToByteArray())
        }
    }

    private suspend fun fileExists(relativePath: String): Boolean {
        val file = accessProvider.resolveRelativePath(relativePath, ensureParentExists = false)
        return file.exists()
    }

    private suspend fun deleteFile(relativePath: String) {
        val file = accessProvider.resolveRelativePath(relativePath, ensureParentExists = false)
        if (file.exists() && !file.isDirectory()) {
            withContext(Dispatchers.IO) { file.delete() }
        }
    }

    private suspend fun deleteDirectory(relativePath: String) {
        val dir = accessProvider.resolveRelativePath(relativePath, ensureParentExists = false)
        if (dir.exists() && dir.isDirectory()) {
            withContext(Dispatchers.IO) { dir.delete() }
        }
    }

    private suspend fun scanEntityDirectory(entityDir: String): List<Uuid> {
        val dir = accessProvider.resolveRelativePath(entityDir, ensureParentExists = false)
        if (!dir.exists() || !dir.isDirectory()) return emptyList()

        return withContext(Dispatchers.IO) {
            dir.list()
                .filter { it.isDirectory() }
                .mapNotNull { folder ->
                    runCatching { Uuid.parse(folder.name) }.getOrNull()
                }
        }
    }
}
