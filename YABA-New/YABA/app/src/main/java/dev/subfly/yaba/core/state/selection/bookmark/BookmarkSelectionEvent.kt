package dev.subfly.yaba.core.state.selection.bookmark

sealed interface BookmarkSelectionEvent {
    data class OnInit(
        val selectedBookmarkId: String? = null,
    ) : BookmarkSelectionEvent

    data class OnChangeQuery(
        val query: String,
    ) : BookmarkSelectionEvent

    data class OnSelectBookmark(
        val bookmarkId: String,
    ) : BookmarkSelectionEvent
}
