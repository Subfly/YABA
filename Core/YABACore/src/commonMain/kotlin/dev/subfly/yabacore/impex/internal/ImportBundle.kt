@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.impex.internal

import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.operations.OperationDraft
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.tagLinkOperationDraft
import dev.subfly.yabacore.database.operations.toOperationDraft
import dev.subfly.yabacore.impex.model.TagLink
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

internal data class ImportBundle(
    val folders: List<FolderDomainModel> = emptyList(),
    val tags: List<TagDomainModel> = emptyList(),
    val bookmarks: List<LinkBookmarkDomainModel> = emptyList(),
    val tagLinks: List<TagLink> = emptyList(),
)

internal fun ImportBundle.toOperationDrafts(): List<OperationDraft> {
    val drafts = mutableListOf<OperationDraft>()
    drafts += folders.sortedBy { it.order }.map { it.toOperationDraft(OperationKind.CREATE) }
    drafts += tags.sortedBy { it.order }.map { it.toOperationDraft(OperationKind.CREATE) }
    drafts += bookmarks.map { it.toOperationDraft(OperationKind.CREATE) }
    drafts += tagLinks.map { link ->
        tagLinkOperationDraft(
            tagId = link.tagId,
            bookmarkId = link.bookmarkId,
            kind = OperationKind.TAG_ADD,
            happenedAt = latestEditedAt(bookmarkId = link.bookmarkId)
                ?: Instant.fromEpochMilliseconds(0)
        )
    }
    return drafts
}

private fun ImportBundle.latestEditedAt(bookmarkId: kotlin.uuid.Uuid): Instant? =
    bookmarks.firstOrNull { it.id == bookmarkId }?.editedAt
