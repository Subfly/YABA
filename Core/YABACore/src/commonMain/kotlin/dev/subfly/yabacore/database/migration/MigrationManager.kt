@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.migration

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.domain.BookmarkMetadataDomainModel
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
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
        snapshot.folders.sortedBy { it.order }.forEach { legacy ->
            migrateFolderToFilesystem(legacy.toFolder(), deviceId)
        }

        // Migrate tags
        snapshot.tags.sortedBy { it.order }.forEach { legacy ->
            migrateTagToFilesystem(legacy.toTag(), deviceId)
        }

        // Migrate bookmarks
        snapshot.bookmarks.sortedBy { it.createdAt }.forEach { legacy ->
            val localImagePath = legacy.previewImageData?.let {
                CoreConstants.FileSystem.Linkmark.linkImagePath(legacy.id, "jpeg")
            }
            val localIconPath = legacy.previewIconData?.let {
                CoreConstants.FileSystem.Linkmark.domainIconPath(legacy.id, "png")
            }

            val metadata = legacy.toBookmarkMetadata(
                localImagePath = localImagePath,
                localIconPath = localIconPath,
            )
            migrateBookmarkToFilesystem(
                metadata = metadata,
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
                    tagId = link.tagId.toString(),
                    bookmarkId = link.bookmarkId.toString(),
                )
            )
        }
    }

    private suspend fun migrateFolderToFilesystem(folder: FolderDomainModel, deviceId: String) {
        val initialClock = VectorClock.of(deviceId, 1)
        val folderJson = FolderMetaJson(
            id = folder.id.toString(),
            parentId = folder.parentId?.toString(),
            label = folder.label,
            description = folder.description,
            icon = folder.icon,
            colorCode = folder.color.code,
            order = folder.order,
            createdAt = folder.createdAt.toEpochMilliseconds(),
            editedAt = folder.editedAt.toEpochMilliseconds(),
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem
        entityFileManager.writeFolderMeta(folderJson)

        // 2. Update SQLite cache
        folderDao.upsert(
            FolderEntity(
                id = folder.id.toString(),
                parentId = folder.parentId?.toString(),
                label = folder.label,
                description = folder.description,
                icon = folder.icon,
                color = folder.color,
                order = folder.order,
                createdAt = folder.createdAt.toEpochMilliseconds(),
                editedAt = folder.editedAt.toEpochMilliseconds(),
            )
        )
    }

    private suspend fun migrateTagToFilesystem(tag: TagDomainModel, deviceId: String) {
        val initialClock = VectorClock.of(deviceId, 1)
        val tagJson = TagMetaJson(
            id = tag.id.toString(),
            label = tag.label,
            icon = tag.icon,
            colorCode = tag.color.code,
            order = tag.order,
            createdAt = tag.createdAt.toEpochMilliseconds(),
            editedAt = tag.editedAt.toEpochMilliseconds(),
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem
        entityFileManager.writeTagMeta(tagJson)

        // 2. Update SQLite cache
        tagDao.upsert(
            TagEntity(
                id = tag.id.toString(),
                label = tag.label,
                icon = tag.icon,
                color = tag.color,
                order = tag.order,
                createdAt = tag.createdAt.toEpochMilliseconds(),
                editedAt = tag.editedAt.toEpochMilliseconds(),
            )
        )
    }

    private suspend fun migrateBookmarkToFilesystem(
        metadata: BookmarkMetadataDomainModel,
        url: String,
        domain: String,
        linkTypeCode: Int,
        videoUrl: String?,
        deviceId: String,
    ) {
        val initialClock = VectorClock.of(deviceId, 1)

        // Create bookmark meta.json
        val bookmarkJson = BookmarkMetaJson(
            id = metadata.id.toString(),
            folderId = metadata.folderId.toString(),
            kind = metadata.kind.code,
            label = metadata.label,
            description = metadata.description,
            createdAt = metadata.createdAt.toEpochMilliseconds(),
            editedAt = metadata.editedAt.toEpochMilliseconds(),
            viewCount = 0,
            isPrivate = metadata.isPrivate,
            isPinned = metadata.isPinned,
            localImagePath = metadata.localImagePath,
            localIconPath = metadata.localIconPath,
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
        entityFileManager.writeLinkJson(metadata.id, linkJson)

        // 3. Update SQLite cache
        bookmarkDao.upsert(
            BookmarkEntity(
                id = metadata.id.toString(),
                folderId = metadata.folderId.toString(),
                kind = metadata.kind,
                label = metadata.label,
                description = metadata.description,
                createdAt = metadata.createdAt.toEpochMilliseconds(),
                editedAt = metadata.editedAt.toEpochMilliseconds(),
                viewCount = 0,
                isPrivate = metadata.isPrivate,
                isPinned = metadata.isPinned,
                localImagePath = metadata.localImagePath,
                localIconPath = metadata.localIconPath,
            )
        )

        linkBookmarkDao.upsert(
            LinkBookmarkEntity(
                bookmarkId = metadata.id.toString(),
                url = url,
                domain = domain,
                linkType = LinkType.fromCode(linkTypeCode),
                videoUrl = videoUrl,
            )
        )
    }

    /**
     * Defensive normalization: if a folder has an invalid order (<0) or references a missing
     * parent, move it to root and append order. For tags with invalid order, append to the end.
     */
    private fun sanitizeSnapshot(snapshot: LegacySnapshot): LegacySnapshot {
        val folderIds = snapshot.folders.map { it.id }.toSet()

        var nextRootOrder = (snapshot.folders.filter {
            it.parentId == null
        }.maxOfOrNull { it.order } ?: -1) + 1

        val fixedFolders = snapshot.folders.map { folder ->
            val parentExists = folder.parentId?.let { folderIds.contains(it) } ?: true
            val hasValidOrder = folder.order >= 0
            if (!parentExists || !hasValidOrder) {
                folder.copy(parentId = null, order = nextRootOrder++)
            } else {
                folder
            }
        }

        var nextTagOrder = (snapshot.tags.maxOfOrNull { it.order } ?: -1) + 1

        val fixedTags = snapshot.tags.map { tag ->
            if (tag.order < 0) {
                tag.copy(order = nextTagOrder++)
            } else tag
        }

        return snapshot.copy(
            folders = fixedFolders,
            tags = fixedTags,
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

    private fun LegacyFolder.toFolder(): FolderDomainModel =
        FolderDomainModel(
            id = id,
            parentId = parentId,
            label = label,
            description = description,
            icon = icon,
            color = YabaColor.fromCode(colorCode),
            createdAt = createdAt,
            editedAt = editedAt,
            order = order,
        )

    private fun LegacyTag.toTag(): TagDomainModel =
        TagDomainModel(
            id = id,
            label = label,
            icon = icon,
            color = YabaColor.fromCode(colorCode),
            createdAt = createdAt,
            editedAt = editedAt,
            order = order,
        )

    private fun LegacyBookmark.toBookmarkMetadata(
        localImagePath: String?,
        localIconPath: String?,
    ): BookmarkMetadataDomainModel =
        BookmarkMetadataDomainModel(
            id = id,
            folderId = folderId,
            kind = BookmarkKind.LINK,
            label = label,
            description = description,
            createdAt = createdAt,
            editedAt = editedAt,
            localImagePath = localImagePath,
            localIconPath = localIconPath,
        )
}
