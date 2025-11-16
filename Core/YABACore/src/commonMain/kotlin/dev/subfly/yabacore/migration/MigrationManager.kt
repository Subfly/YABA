@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.migration

import dev.subfly.yabacore.model.BookmarkKind
import dev.subfly.yabacore.model.Folder
import dev.subfly.yabacore.model.LinkBookmark
import dev.subfly.yabacore.model.LinkType
import dev.subfly.yabacore.model.Tag
import dev.subfly.yabacore.model.YabaColor
import dev.subfly.yabacore.operations.OpApplier
import dev.subfly.yabacore.operations.OperationDraft
import dev.subfly.yabacore.operations.OperationEntityType
import dev.subfly.yabacore.operations.OperationKind
import dev.subfly.yabacore.operations.TagLinkPayload
import dev.subfly.yabacore.operations.toOperationDraft
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

class MigrationManager(
    private val opApplier: OpApplier,
) {
    suspend fun migrate(snapshot: LegacySnapshot) {
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
        snapshot.folders
            .sortedBy { it.order }
            .map { legacy ->
                legacy.toFolder().toOperationDraft(OperationKind.CREATE)
            }

    private fun buildTagDrafts(snapshot: LegacySnapshot): List<OperationDraft> =
        snapshot.tags
            .sortedBy { it.order }
            .map { legacy ->
                legacy.toTag().toOperationDraft(OperationKind.CREATE)
            }

    private fun buildBookmarkDrafts(snapshot: LegacySnapshot): List<OperationDraft> =
        snapshot.bookmarks
            .sortedBy { it.createdAt }
            .map { legacy ->
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
                payload =
                    TagLinkPayload(
                        tagId = link.tagId.toString(),
                        bookmarkId = link.bookmarkId.toString(),
                    ),
            )
        }
    }

    private fun LegacyFolder.toFolder(): Folder =
        Folder(
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

    private fun LegacyTag.toTag(): Tag =
        Tag(
            id = id,
            label = label,
            icon = icon,
            color = YabaColor.fromCode(colorCode),
            createdAt = createdAt,
            editedAt = editedAt,
            order = order,
        )

    private fun LegacyBookmark.toBookmark(): LinkBookmark =
        LinkBookmark(
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

