@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.mappers.toDomain
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.tagLinkOperationDraft
import dev.subfly.yabacore.database.operations.toOperationDraft
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

object AllBookmarksManager {
    private val opApplier
        get() = OpApplier
    private val bookmarkFileManager
        get() = BookmarkFileManager
    private val clock = Clock.System

    suspend fun moveBookmarksToFolder(
        bookmarks: List<BookmarkUiModel>,
        targetFolder: FolderUiModel,
    ) {
        if (bookmarks.isEmpty()) return
        val now = clock.now()
        val drafts = bookmarks.mapNotNull { bookmark ->
            bookmark.toDomainBookmark()
                ?.copy(folderId = targetFolder.id, editedAt = now)
                ?.toOperationDraft(OperationKind.MOVE)
        }
        if (drafts.isEmpty()) return
        opApplier.applyLocal(drafts)
    }

    suspend fun deleteBookmarks(bookmarks: List<BookmarkUiModel>) {
        if (bookmarks.isEmpty()) return
        val now = clock.now()
        val drafts = bookmarks.mapNotNull { bookmark ->
            bookmark.toDomainBookmark()
                ?.copy(editedAt = now)
                ?.toOperationDraft(OperationKind.DELETE)
        }
        if (drafts.isEmpty()) return
        opApplier.applyLocal(drafts)
        bookmarks.forEach { bookmark ->
            bookmarkFileManager.deleteBookmarkTree(bookmark.id)
        }
    }

    suspend fun addTagToBookmark(tag: TagUiModel, bookmark: BookmarkUiModel) {
        val draft = tagLinkOperationDraft(
            tag.id,
            bookmark.id,
            OperationKind.TAG_ADD,
            clock.now()
        )
        opApplier.applyLocal(listOf(draft))
    }

    suspend fun removeTagFromBookmark(tag: TagUiModel, bookmark: BookmarkUiModel) {
        val draft = tagLinkOperationDraft(
            tag.id,
            bookmark.id,
            OperationKind.TAG_REMOVE,
            clock.now()
        )
        opApplier.applyLocal(listOf(draft))
    }

    private fun BookmarkUiModel.toDomainBookmark(): LinkBookmarkDomainModel? =
        when (this) {
            is LinkmarkUiModel -> this.toDomain()
        }
}
