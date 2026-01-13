package dev.subfly.yabacore.state.detail.tag

import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.BookmarkSearchFilters
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)
class TagDetailStateMachine :
    BaseStateMachine<TagDetailUIState, TagDetailEvent>(initialState = TagDetailUIState()) {
    private var isInitialized = false
    private var dataSubscriptionJob: Job? = null
    private val preferencesStore = SettingsStores.userPreferences

    private val queryFlow = MutableStateFlow("")
    private val tagIdFlow = MutableStateFlow<Uuid?>(null)

    override fun onEvent(event: TagDetailEvent) {
        when (event) {
            is TagDetailEvent.OnInit -> onInit(event.tagId)
            is TagDetailEvent.OnChangeQuery -> onChangeQuery(event.query)
            is TagDetailEvent.OnToggleSelectionMode -> onToggleSelectionMode()
            is TagDetailEvent.OnToggleBookmarkSelection -> onToggleBookmarkSelection(event.bookmarkId)
            TagDetailEvent.OnDeleteSelected -> onDeleteSelected()
            is TagDetailEvent.OnChangeSort -> onChangeSort(event.sortType, event.sortOrder)
            is TagDetailEvent.OnChangeAppearance -> onChangeAppearance(event.appearance)
        }
    }

    private fun onInit(tagId: String) {
        if (isInitialized) return
        isInitialized = true
        tagIdFlow.value = Uuid.parse(tagId)

        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = launch {
            combine(
                tagIdFlow,
                queryFlow,
                preferencesStore.preferencesFlow
            ) { tagId, query, prefs ->
                if (tagId == null) null
                else Triple(tagId, query, prefs)
            }.flatMapLatest { params ->
                if (params == null) {
                    MutableStateFlow(TagDetailUIState())
                } else {
                    val (tagId, query, prefs) = params
                    updateState { it.copy(isLoading = true, query = query) }

                    val tagFlow = TagManager.observeTag(tagId)
                    val bookmarksFlow = AllBookmarksManager.searchBookmarksFlow(
                        query = query,
                        filters = BookmarkSearchFilters(tagIds = setOf(tagId)),
                        sortType = prefs.preferredCollectionSorting,
                        sortOrder = prefs.preferredSortOrder
                    )

                    combine(
                        tagFlow,
                        bookmarksFlow,
                        preferencesStore.preferencesFlow
                    ) { tag, bookmarks, latestPrefs ->
                        currentState().copy(
                            tag = tag,
                            bookmarks = bookmarks,
                            query = query,
                            bookmarkAppearance = latestPrefs.preferredBookmarkAppearance,
                            sortType = latestPrefs.preferredCollectionSorting,
                            sortOrder = latestPrefs.preferredSortOrder,
                            isLoading = false
                        )
                    }
                }
            }.collectLatest { newState ->
                updateState { newState }
            }
        }
    }

    private fun onChangeQuery(query: String) {
        queryFlow.update { query }
        updateState { it.copy(query = query) }
    }

    private fun onToggleSelectionMode() {
        updateState { state ->
            val nextSelectionMode = !state.isSelectionMode
            state.copy(
                isSelectionMode = nextSelectionMode,
                selectedBookmarkIds = if (nextSelectionMode) state.selectedBookmarkIds else emptySet()
            )
        }
    }

    private fun onToggleBookmarkSelection(bookmarkId: Uuid) {
        if (currentState().isSelectionMode.not()) return

        updateState { state ->
            val newSelection = if (state.selectedBookmarkIds.contains(bookmarkId)) {
                state.selectedBookmarkIds - bookmarkId
            } else {
                state.selectedBookmarkIds + bookmarkId
            }
            state.copy(selectedBookmarkIds = newSelection)
        }
    }

    private fun onDeleteSelected() {
        val selectedIds = currentState().selectedBookmarkIds
        if (selectedIds.isEmpty()) return

        launch {
            val bookmarksToDelete = currentState().bookmarks.filter { it.id in selectedIds }
            AllBookmarksManager.deleteBookmarks(bookmarksToDelete)
            updateState { it.copy(isSelectionMode = false, selectedBookmarkIds = emptySet()) }
        }
    }

    private fun onChangeSort(sortType: SortType, sortOrder: SortOrderType) {
        launch {
            preferencesStore.setPreferredBookmarkSorting(sortType)
            preferencesStore.setPreferredSortOrder(sortOrder)
        }
    }

    private fun onChangeAppearance(appearance: BookmarkAppearance) {
        launch { preferencesStore.setPreferredBookmarkAppearance(appearance) }
    }

    override fun clear() {
        isInitialized = false
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        super.clear()
    }
}
