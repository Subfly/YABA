@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.migration

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.filesystem.LinkmarkFileManager
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.preferences.PreferencesMigration
import kotlin.uuid.ExperimentalUuidApi

/**
 * Migrates legacy Darwin storage into the DB-first architecture.
 *
 * - Settings/AppStorage/UserDefaults -> DataStore (handled per-platform)
 * - SwiftData snapshot (if provided) -> Room only
 */
object MigrationManager {
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao

    suspend fun migrate(snapshot: LegacySnapshot? = null) {
        PreferencesMigration.migrateIfNeeded()

        snapshot?.let {
            val sanitized = sanitizeSnapshot(it)
            migrateBookmarkAssets(sanitized)
            migrateDatabaseSnapshot(sanitized)
        }
    }

    private suspend fun migrateDatabaseSnapshot(snapshot: LegacySnapshot) {
        snapshot.folders.forEach { legacy ->
            folderDao.upsert(legacy.toFolderEntity())
        }

        snapshot.tags.forEach { legacy ->
            tagDao.upsert(legacy.toTagEntity())
        }

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
            bookmarkDao.upsert(bookmark)

            linkBookmarkDao.upsert(
                LinkBookmarkEntity(
                    bookmarkId = bookmark.id,
                    url = legacy.url,
                    domain = legacy.domain,
                    linkType = LinkType.fromCode(legacy.linkTypeCode),
                    videoUrl = legacy.videoUrl,
                )
            )
        }

        snapshot.tagLinks.forEach { link ->
            tagBookmarkDao.insert(
                TagBookmarkCrossRef(
                    tagId = link.tagId,
                    bookmarkId = link.bookmarkId,
                )
            )
        }
    }

    private fun sanitizeSnapshot(snapshot: LegacySnapshot): LegacySnapshot {
        val folderIds = snapshot.folders.map { it.id }.toSet()
        val fixedFolders = snapshot.folders.map { folder ->
            val parentExists = folder.parentId?.let { folderIds.contains(it) } ?: true
            if (!parentExists) folder.copy(parentId = null) else folder
        }
        return snapshot.copy(folders = fixedFolders)
    }

    private suspend fun migrateBookmarkAssets(snapshot: LegacySnapshot) {
        snapshot.bookmarks.forEach { legacy ->
            legacy.previewImageData?.let { data ->
                LinkmarkFileManager.saveLinkImageBytes(bookmarkId = legacy.id, bytes = data)
            }
            legacy.previewIconData?.let { data ->
                LinkmarkFileManager.saveDomainIconBytes(bookmarkId = legacy.id, bytes = data)
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
