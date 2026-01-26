@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.migration

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.LinkmarkFileManager
import dev.subfly.yabacore.filesystem.json.BookmarkMetaJson
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.filesystem.json.LinkJson
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.preferences.PreferencesMigration
import dev.subfly.yabacore.sync.VectorClock
import kotlin.uuid.ExperimentalUuidApi

/**
 * Migrates legacy Darwin storage into the new filesystem-first architecture.
 *
 * - Settings/AppStorage/UserDefaults -> DataStore (handled per-platform)
 * - SwiftData snapshot (if provided) -> Filesystem JSON + Room cache
 */
object MigrationManager {
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val entityFileManager get() = EntityFileManager

    /**
     * Migrate everything from legacy Darwin storage into the Kotlin stack.
     *
     * - Settings/AppStorage/UserDefaults -> DataStore (handled per-platform)
     * - SwiftData snapshot (if provided) -> Filesystem JSON + Room via filesystem-first
     */
    suspend fun migrate(snapshot: LegacySnapshot? = null) {
        PreferencesMigration.migrateIfNeeded()

        snapshot?.let {
            val sanitized = sanitizeSnapshot(it)
            migrateBookmarkAssets(sanitized)
            migrateDatabaseSnapshot(sanitized)
        }
    }

    private suspend fun migrateDatabaseSnapshot(snapshot: LegacySnapshot) {
        val deviceId = DeviceIdProvider.get()
        
        // Migrate folders
        snapshot.folders.forEach { legacy ->
            migrateFolderToFilesystem(legacy.toFolderEntity(), deviceId)
        }

        // Migrate tags
        snapshot.tags.forEach { legacy ->
            migrateTagToFilesystem(legacy.toTagEntity(), deviceId)
        }

        // Migrate bookmarks
        snapshot.bookmarks.sortedBy { it.createdAt }.forEach { legacy ->
            val localImagePath = legacy.previewImageData?.let {
                CoreConstants.FileSystem.Linkmark.linkImagePath(legacy.id, "jpeg")
            }
            val localIconPath = legacy.previewIconData?.let {
                CoreConstants.FileSystem.Linkmark.domainIconPath(legacy.id, "png")
            }

            val bookmark = legacy.toBookmarkEntity(
                localImagePath = localImagePath,
                localIconPath = localIconPath,
            )
            migrateBookmarkToFilesystem(
                bookmark = bookmark,
                url = legacy.url,
                domain = legacy.domain,
                linkTypeCode = legacy.linkTypeCode,
                videoUrl = legacy.videoUrl,
                deviceId = deviceId,
            )
        }

        // Migrate tag-bookmark relationships
        snapshot.tagLinks.forEach { link ->
            tagBookmarkDao.insert(
                TagBookmarkCrossRef(
                    tagId = link.tagId,
                    bookmarkId = link.bookmarkId,
                )
            )
        }
    }

    private suspend fun migrateFolderToFilesystem(folder: FolderEntity, deviceId: String) {
        val initialClock = VectorClock.of(deviceId, 1)
        val folderJson = FolderMetaJson(
            id = folder.id,
            parentId = folder.parentId,
            label = folder.label,
            description = folder.description,
            icon = folder.icon,
            colorCode = folder.color.code,
            createdAt = folder.createdAt,
            editedAt = folder.editedAt,
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem
        entityFileManager.writeFolderMeta(folderJson)

        // 2. Update SQLite cache
        folderDao.upsert(folder)
    }

    private suspend fun migrateTagToFilesystem(tag: TagEntity, deviceId: String) {
        val initialClock = VectorClock.of(deviceId, 1)
        val tagJson = TagMetaJson(
            id = tag.id,
            label = tag.label,
            icon = tag.icon,
            colorCode = tag.color.code,
            createdAt = tag.createdAt,
            editedAt = tag.editedAt,
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem
        entityFileManager.writeTagMeta(tagJson)

        // 2. Update SQLite cache
        tagDao.upsert(tag)
    }

    private suspend fun migrateBookmarkToFilesystem(
        bookmark: BookmarkEntity,
        url: String,
        domain: String,
        linkTypeCode: Int,
        videoUrl: String?,
        deviceId: String,
    ) {
        val initialClock = VectorClock.of(deviceId, 1)

        // Create bookmark meta.json
        val bookmarkJson = BookmarkMetaJson(
            id = bookmark.id,
            folderId = bookmark.folderId,
            kind = bookmark.kind.code,
            label = bookmark.label,
            description = bookmark.description,
            createdAt = bookmark.createdAt,
            editedAt = bookmark.editedAt,
            viewCount = bookmark.viewCount,
            isPrivate = bookmark.isPrivate,
            isPinned = bookmark.isPinned,
            localImagePath = bookmark.localImagePath,
            localIconPath = bookmark.localIconPath,
            tagIds = emptyList(), // Tags will be linked separately
            clock = initialClock.toMap(),
        )

        // 1. Write bookmark meta.json to filesystem
        entityFileManager.writeBookmarkMeta(bookmarkJson)

        // Create link.json
        val linkJson = LinkJson(
            url = url,
            domain = domain,
            linkTypeCode = linkTypeCode,
            videoUrl = videoUrl,
            clock = initialClock.toMap(),
        )

        // 2. Write link.json to filesystem
        entityFileManager.writeLinkJson(bookmark.id, linkJson)

        // 3. Update SQLite cache
        bookmarkDao.upsert(bookmark)

        linkBookmarkDao.upsert(
            LinkBookmarkEntity(
                bookmarkId = bookmark.id,
                url = url,
                domain = domain,
                linkType = LinkType.fromCode(linkTypeCode),
                videoUrl = videoUrl,
            )
        )
    }

    /**
     * Defensive normalization: if a folder references a missing parent, move it to root.
     */
    private fun sanitizeSnapshot(snapshot: LegacySnapshot): LegacySnapshot {
        val folderIds = snapshot.folders.map { it.id }.toSet()

        val fixedFolders = snapshot.folders.map { folder ->
            val parentExists = folder.parentId?.let { folderIds.contains(it) } ?: true
            if (!parentExists) {
                folder.copy(parentId = null)
            } else {
                folder
            }
        }

        return snapshot.copy(
            folders = fixedFolders,
            tags = snapshot.tags,
        )
    }

    private suspend fun migrateBookmarkAssets(snapshot: LegacySnapshot) {
        snapshot.bookmarks.forEach { legacy ->
            legacy.previewImageData?.let { data ->
                LinkmarkFileManager.saveLinkImageBytes(
                    bookmarkId = legacy.id,
                    bytes = data,
                )
            }
            legacy.previewIconData?.let { data ->
                LinkmarkFileManager.saveDomainIconBytes(
                    bookmarkId = legacy.id,
                    bytes = data,
                )
            }
        }
    }

    private fun LegacyFolder.toFolderEntity(): FolderEntity =
        FolderEntity(
            id = id,
            parentId = parentId,
            label = label,
            description = description,
            icon = icon,
            color = YabaColor.fromCode(colorCode),
            createdAt = createdAt.toEpochMilliseconds(),
            editedAt = editedAt.toEpochMilliseconds(),
        )

    private fun LegacyTag.toTagEntity(): TagEntity =
        TagEntity(
            id = id,
            label = label,
            icon = icon,
            color = YabaColor.fromCode(colorCode),
            createdAt = createdAt.toEpochMilliseconds(),
            editedAt = editedAt.toEpochMilliseconds(),
        )

    private fun LegacyBookmark.toBookmarkEntity(
        localImagePath: String?,
        localIconPath: String?,
    ): BookmarkEntity =
        BookmarkEntity(
            id = id,
            folderId = folderId,
            kind = BookmarkKind.LINK,
            label = label,
            description = description,
            createdAt = createdAt.toEpochMilliseconds(),
            editedAt = editedAt.toEpochMilliseconds(),
            viewCount = 0,
            isPrivate = false,
            isPinned = false,
            localImagePath = localImagePath,
            localIconPath = localIconPath,
        )
}
