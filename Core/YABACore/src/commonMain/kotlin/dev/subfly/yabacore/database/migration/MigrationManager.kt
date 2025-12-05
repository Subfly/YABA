@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

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
import kotlin.time.ExperimentalTime
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
            migrateDatabaseSnapshot(it)
            migrateBookmarkAssets(it)
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
            url = url,
            domain = domain,
            linkType = LinkType.fromCode(linkTypeCode),
            previewImageUrl = previewImageUrl,
            previewIconUrl = previewIconUrl,
            videoUrl = videoUrl,
        )
}
