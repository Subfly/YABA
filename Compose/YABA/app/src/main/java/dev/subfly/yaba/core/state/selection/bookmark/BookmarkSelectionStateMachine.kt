package dev.subfly.yaba.core.state.selection.bookmark

import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.security.PrivateBookmarkSessionGuard
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType
import dev.subfly.yaba.core.state.base.BaseStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update

class BookmarkSelectionStateMachine :
    BaseStateMachine<BookmarkSelectionUIState, BookmarkSelectionEvent>(
        initialState = BookmarkSelectionUIState(),
    ) {
    private var isInitialized = false
    private var bookmarksJob: Job? = null
    private val queryFlow = MutableStateFlow("")
    private val sessionUnlockedFlow = PrivateBookmarkSessionGuard.isUnlocked

    override fun onEvent(event: BookmarkSelectionEvent) {
        when (event) {
            is BookmarkSelectionEvent.OnInit -> onInit(event)
            is BookmarkSelectionEvent.OnChangeQuery -> onChangeQuery(event)
            is BookmarkSelectionEvent.OnSelectBookmark -> onSelectBookmark(event)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun onInit(event: BookmarkSelectionEvent.OnInit) {
        if (isInitialized) return
        isInitialized = true
        updateState {
            it.copy(
                selectedBookmarkId = event.selectedBookmarkId,
                isLoading = true,
            )
        }
        bookmarksJob?.cancel()
        bookmarksJob = launch {
            combine(queryFlow, sessionUnlockedFlow) { q, u -> q to u }
                .flatMapLatest { (query, unlocked) ->
                    updateState { state -> state.copy(query = query, isLoading = true) }
                    val excludePrivate = !unlocked && query.isNotEmpty()
                    AllBookmarksManager.searchBookmarksFlow(
                        query = query,
                        sortType = SortType.EDITED_AT,
                        sortOrder = SortOrderType.DESCENDING,
                        excludePrivate = excludePrivate,
                    )
                }
                .collectLatest { bookmarks ->
                    updateState { state ->
                        val refreshedSelection =
                            state.selectedBookmarkId?.let { id ->
                                bookmarks.firstOrNull { it.id == id }
                                    ?: state.selectedBookmark
                            }
                        state.copy(
                            bookmarks = bookmarks,
                            isLoading = false,
                            selectedBookmark = refreshedSelection,
                        )
                    }
                }
        }
    }

    private fun onChangeQuery(event: BookmarkSelectionEvent.OnChangeQuery) {
        queryFlow.update { event.query }
        updateState { it.copy(query = event.query) }
    }

    private fun onSelectBookmark(event: BookmarkSelectionEvent.OnSelectBookmark) {
        updateState { state ->
            val model =
                state.bookmarks.firstOrNull { it.id == event.bookmarkId }
                    ?: return@updateState state
            state.copy(
                selectedBookmarkId = event.bookmarkId,
                selectedBookmark = model,
            )
        }
    }

    override fun clear() {
        isInitialized = false
        bookmarksJob?.cancel()
        bookmarksJob = null
        super.clear()
    }
}
