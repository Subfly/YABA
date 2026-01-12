@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.migration

import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationDraft
import dev.subfly.yabacore.database.operations.OperationEntityType
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.TagLinkPayload
import dev.subfly.yabacore.database.operations.toOperationDraft
import dev.subfly.yabacore.filesystem.LinkmarkFileManager
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.preferences.PreferencesMigration
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

object MigrationManager {
    private val opApplier
        get() = OpApplier

    /**
     * Migrate everything from legacy Darwin storage into the Kotlin stack.
     *
     * - Settings/AppStorage/UserDefaults -> DataStore (handled per-platform)
     * - SwiftData snapshot (if provided) -> Room via op drafts
     */
    suspend fun migrate(snapshot: LegacySnapshot? = null) {
        PreferencesMigration.migrateIfNeeded()

        snapshot?.let {
            val sanitized = sanitizeSnapshot(it)
            migrateDatabaseSnapshot(sanitized)
            migrateBookmarkAssets(sanitized)
        }
    }

    private suspend fun migrateDatabaseSnapshot(snapshot: LegacySnapshot) {
        applyDrafts(buildFolderDrafts(snapshot))
        applyDrafts(buildTagDrafts(snapshot))
        applyDrafts(buildBookmarkDrafts(snapshot))
        applyDrafts(buildTagLinkDrafts(snapshot))
    }

    private suspend fun applyDrafts(drafts: List<OperationDraft>) {
        if (drafts.isNotEmpty()) {
            opApplier.applyLocal(drafts)
        }
    }

    private fun buildFolderDrafts(snapshot: LegacySnapshot): List<OperationDraft> =
        snapshot.folders.sortedBy { it.order }.map { legacy ->
            legacy.toFolder().toOperationDraft(OperationKind.CREATE)
        }

    private fun buildTagDrafts(snapshot: LegacySnapshot): List<OperationDraft> =
        snapshot.tags.sortedBy { it.order }.map { legacy ->
            legacy.toTag().toOperationDraft(OperationKind.CREATE)
        }

    private fun buildBookmarkDrafts(snapshot: LegacySnapshot): List<OperationDraft> =
        snapshot.bookmarks.sortedBy { it.createdAt }.map { legacy ->
            legacy.toBookmark().toOperationDraft(OperationKind.CREATE)
        }

    private fun buildTagLinkDrafts(snapshot: LegacySnapshot): List<OperationDraft> {
        val now = Clock.System.now()
        return snapshot.tagLinks.map { link ->
            OperationDraft(
                entityType = OperationEntityType.TAG_LINK,
                entityId = "${link.tagId}|${link.bookmarkId}",
                kind = OperationKind.TAG_ADD,
                happenedAt = now,
                payload = TagLinkPayload(
                    tagId = link.tagId.toString(),
                    bookmarkId = link.bookmarkId.toString(),
                ),
            )
        }
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

    private fun LegacyBookmark.toBookmark(): LinkBookmarkDomainModel =
        LinkBookmarkDomainModel(
            id = id,
            folderId = folderId,
            kind = BookmarkKind.LINK,
            label = label,
            description = description,
            createdAt = createdAt,
            editedAt = editedAt,
            localImagePath = null,
            localIconPath = null,
            url = url,
            domain = domain,
            linkType = LinkType.fromCode(linkTypeCode),
            videoUrl = videoUrl,
        )
}
