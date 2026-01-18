package dev.subfly.yabacore.state.home

import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

private const val RECENT_BOOKMARKS_LIMIT = 5

class HomeStateMachine : BaseStateMachine<HomeUIState, HomeEvent>(initialState = HomeUIState()) {
    private var isInitialized = false
    private var dataSubscriptionJob: Job? = null
    private val preferencesStore
        get() = SettingsStores.userPreferences

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.OnInit -> onInit()
            is HomeEvent.OnChangeBookmarkAppearance -> onChangeBookmarkAppearance(event)
            is HomeEvent.OnChangeCardImageSizing -> onChangeCardImageSizing(event)
            is HomeEvent.OnChangeCollectionSorting -> onChangeCollectionSorting(event)
            is HomeEvent.OnChangeSortOrder -> onChangeSortOrder(event)
            is HomeEvent.OnDeleteFolder -> onDeleteFolder(event)
            is HomeEvent.OnMoveFolder -> onMoveFolder(event)
            is HomeEvent.OnReorderFolder -> onReorderFolder(event)
            is HomeEvent.OnDeleteTag -> onDeleteTag(event)
            is HomeEvent.OnReorderTag -> onReorderTag(event)
            is HomeEvent.OnDeleteBookmark -> onDeleteBookmark(event)
            is HomeEvent.OnMoveBookmarkToFolder -> onMoveBookmarkToFolder(event)
            is HomeEvent.OnMoveBookmarkToTag -> onMoveBookmarkToTag(event)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun onInit() {
        if (isInitialized) return
        isInitialized = true

        // Cancel any existing subscription
        dataSubscriptionJob?.cancel()

        dataSubscriptionJob = launch {
            // First, observe preferences to get sorting parameters
            // When sorting changes, flatMapLatest will cancel old subscriptions and create new ones
            preferencesStore
                .preferencesFlow
                .map { prefs ->
                    SortingParams(
                        prefs.preferredCollectionSorting,
                        prefs.preferredCollectionSortOrder
                    )
                }
                .distinctUntilChanged()
                .flatMapLatest { sortingParams ->
                    val recentBookmarksFlow = AllBookmarksManager
                        .observeAllBookmarks(
                            sortType = SortType.EDITED_AT,
                            sortOrder = SortOrderType.DESCENDING,
                        )
                        .map { it.take(RECENT_BOOKMARKS_LIMIT) }

                    // Combine all data sources with the current sorting parameters
                    combine(
                        preferencesStore.preferencesFlow,
                        FolderManager.observeFolderTree(
                            sortType = sortingParams.sortType,
                            sortOrder = sortingParams.sortOrder,
                        ),
                        TagManager.observeTags(
                            sortType = sortingParams.sortType,
                            sortOrder = sortingParams.sortOrder,
                        ),
                        recentBookmarksFlow,
                    ) { preferences, folders, tags, recentBookmarks ->
                        HomeUIState(
                            folders = folders,
                            tags = tags,
                            recentBookmarks = recentBookmarks,
                            bookmarkAppearance = preferences.preferredBookmarkAppearance,
                            cardImageSizing = preferences.preferredCardImageSizing,
                            collectionSorting = preferences.preferredCollectionSorting,
                            sortOrder = preferences.preferredCollectionSortOrder,
                            isLoading = false,
                        )
                    }
                }
                .collectLatest { newState -> updateState { newState } }
        }
    }

    private fun onChangeBookmarkAppearance(event: HomeEvent.OnChangeBookmarkAppearance) {
        launch { preferencesStore.setPreferredBookmarkAppearance(event.appearance) }
    }

    private fun onChangeCardImageSizing(event: HomeEvent.OnChangeCardImageSizing) {
        launch { preferencesStore.setPreferredCardImageSizing(event.sizing) }
    }

    private fun onChangeCollectionSorting(event: HomeEvent.OnChangeCollectionSorting) {
        launch { preferencesStore.setPreferredCollectionSorting(event.sortType) }
    }

    private fun onChangeSortOrder(event: HomeEvent.OnChangeSortOrder) {
        launch { preferencesStore.setPreferredCollectionSortOrder(event.sortOrder) }
    }

    private data class SortingParams(
        val sortType: SortType,
        val sortOrder: SortOrderType,
    )

    private fun onDeleteFolder(event: HomeEvent.OnDeleteFolder) {
        launch { FolderManager.deleteFolderCascade(event.folder.id) }
    }

    private fun onMoveFolder(event: HomeEvent.OnMoveFolder) {
        launch { FolderManager.moveFolder(event.folder, event.targetParent) }
    }

    private fun onReorderFolder(event: HomeEvent.OnReorderFolder) {
        launch { FolderManager.reorderFolder(event.dragged, event.target, event.zone) }
    }

    // Tag operations - delegated to TagManager
    private fun onDeleteTag(event: HomeEvent.OnDeleteTag) {
        launch { TagManager.deleteTag(event.tag) }
    }

    private fun onReorderTag(event: HomeEvent.OnReorderTag) {
        launch { TagManager.reorderTag(event.dragged, event.target, event.zone) }
    }

    // Bookmark operations - delegated to AllBookmarksManager
    private fun onDeleteBookmark(event: HomeEvent.OnDeleteBookmark) {
        launch { AllBookmarksManager.deleteBookmarks(listOf(event.bookmark.id)) }
    }

    private fun onMoveBookmarkToFolder(event: HomeEvent.OnMoveBookmarkToFolder) {
        launch {
            AllBookmarksManager.moveBookmarksToFolder(
                bookmarkIds = listOf(event.bookmark.id),
                targetFolderId = event.targetFolder.id,
            )
        }
    }

    private fun onMoveBookmarkToTag(event: HomeEvent.OnMoveBookmarkToTag) {
        launch { AllBookmarksManager.addTagToBookmark(tagId = event.targetTag.id, bookmarkId = event.bookmark.id) }
    }

    override fun clear() {
        isInitialized = false
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        super.clear()
    }
}
