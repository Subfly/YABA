package dev.subfly.yabacore.state.search

import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.model.utils.BookmarkSearchFilters
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.preferences.UserPreferences
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private data class FilterParams(
    val prefs: UserPreferences,
    val query: String,
    val folderIds: Set<String>,
    val tagIds: Set<String>,
)

class SearchStateMachine :
    BaseStateMachine<SearchUIState, SearchEvent>(initialState = SearchUIState()) {
    private var isInitialized = false
    private var dataSubscriptionJob: Job? = null
    private val preferencesStore = SettingsStores.userPreferences

    private val queryFlow = MutableStateFlow("")
    private val selectedFolderIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    private val selectedTagIdsFlow = MutableStateFlow<Set<String>>(emptySet())

    override fun onEvent(event: SearchEvent) {
        when (event) {
            SearchEvent.OnInit -> onInit()
            is SearchEvent.OnChangeQuery -> onChangeQuery(event.query)
            is SearchEvent.OnToggleFolderFilter -> onToggleFolderFilter(event.folderId)
            is SearchEvent.OnToggleTagFilter -> onToggleTagFilter(event.tagId)
            is SearchEvent.OnChangeSort -> onChangeSort(event.sortType, event.sortOrder)
            is SearchEvent.OnChangeAppearance -> onChangeAppearance(event)
            is SearchEvent.OnDeleteBookmark -> onDeleteBookmark(event.bookmark)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun onInit() {
        if (isInitialized) return
        isInitialized = true

        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = launch {
            combine(
                preferencesStore.preferencesFlow,
                queryFlow,
                selectedFolderIdsFlow,
                selectedTagIdsFlow
            ) { prefs, query, folderIds, tagIds ->
                FilterParams(prefs, query, folderIds, tagIds)
            }.flatMapLatest { params ->
                updateState { it.copy(isLoading = true, query = params.query) }
                AllBookmarksManager.searchBookmarksFlow(
                    query = params.query,
                    filters = BookmarkSearchFilters(
                        folderIds = params.folderIds.takeIf { it.isNotEmpty() },
                        tagIds = params.tagIds.takeIf { it.isNotEmpty() }
                    ),
                    sortType = params.prefs.preferredBookmarkSorting,
                    sortOrder = params.prefs.preferredBookmarkSortOrder
                ).map { bookmarks ->
                    SearchUIState(
                        query = params.query,
                        bookmarks = bookmarks,
                        selectedFolderIds = params.folderIds,
                        selectedTagIds = params.tagIds,
                        bookmarkAppearance = params.prefs.preferredBookmarkAppearance,
                        sortType = params.prefs.preferredBookmarkSorting,
                        sortOrder = params.prefs.preferredBookmarkSortOrder,
                        isLoading = false
                    )
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

    private fun onToggleFolderFilter(folderId: String) {
        selectedFolderIdsFlow.update { current ->
            if (current.contains(folderId)) current - folderId else current + folderId
        }
    }

    private fun onToggleTagFilter(tagId: String) {
        selectedTagIdsFlow.update { current ->
            if (current.contains(tagId)) current - tagId else current + tagId
        }
    }

    private fun onChangeSort(sortType: SortType, sortOrder: SortOrderType) {
        launch {
            preferencesStore.setPreferredBookmarkSorting(sortType)
            preferencesStore.setPreferredBookmarkSortOrder(sortOrder)
        }
    }

    private fun onChangeAppearance(event: SearchEvent.OnChangeAppearance) {
        launch {
            preferencesStore.setPreferredBookmarkAppearance(event.appearance)
            event.cardImageSizing?.let { sizing ->
                preferencesStore.setPreferredCardImageSizing(sizing)
            }
        }
    }

    private fun onDeleteBookmark(bookmark: dev.subfly.yabacore.model.ui.BookmarkUiModel) {
        launch { AllBookmarksManager.deleteBookmarks(listOf(bookmark.id)) }
    }

    override fun clear() {
        isInitialized = false
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        super.clear()
    }
}
