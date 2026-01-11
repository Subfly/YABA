package dev.subfly.yabacore.state.home

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.LinkmarkFileManager
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.state.base.BaseStateMachine
import io.github.vinceglb.filekit.path
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
    private val bookmarkDao
        get() = DatabaseProvider.bookmarkDao
    private val tagDao
        get() = DatabaseProvider.tagDao
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
                    SortingParams(prefs.preferredCollectionSorting, prefs.preferredSortOrder)
                }
                .distinctUntilChanged()
                .flatMapLatest { sortingParams ->
                    // Transform bookmarks flow to include tags before combining
                    val bookmarksWithTagsFlow = bookmarkDao.observeAllLinkBookmarks(
                        sortType = SortType.EDITED_AT.name,
                        sortOrder = SortOrderType.DESCENDING.name,
                    ).map { bookmarks ->
                        // Load tags for each bookmark
                        bookmarks.map { linkBookmark ->
                            val domainModel = linkBookmark.toModel()
                            val bookmarkTags = tagDao.getTagsForBookmarkWithCounts(domainModel.id.toString())
                                .map { it.toUiModel() }
                            linkBookmark to bookmarkTags
                        }
                    }

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
                        bookmarksWithTagsFlow,
                    ) { preferences, folders, tags, bookmarksWithTags ->
                        // Build recent bookmarks with local file paths and tags
                        val recentBookmarks = mutableListOf<BookmarkUiModel>()
                        for ((linkBookmark, bookmarkTags) in bookmarksWithTags.take(RECENT_BOOKMARKS_LIMIT)) {
                            val domainModel = linkBookmark.toModel()
                            val localImagePath = LinkmarkFileManager
                                .getLinkImageFile(domainModel.id)
                                ?.path
                            val localIconPath = LinkmarkFileManager
                                .getDomainIconFile(domainModel.id)
                                ?.path
                            recentBookmarks.add(
                                domainModel.toUiModel(
                                    tags = bookmarkTags,
                                    localImagePath = localImagePath,
                                    localIconPath = localIconPath,
                                )
                            )
                        }

                        HomeUIState(
                            folders = folders,
                            tags = tags,
                            recentBookmarks = recentBookmarks,
                            bookmarkAppearance = preferences.preferredBookmarkAppearance,
                            cardImageSizing = preferences.preferredCardImageSizing,
                            collectionSorting = preferences.preferredCollectionSorting,
                            sortOrder = preferences.preferredSortOrder,
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
        launch { preferencesStore.setPreferredSortOrder(event.sortOrder) }
    }

    private data class SortingParams(
        val sortType: SortType,
        val sortOrder: SortOrderType,
    )

    // Folder operations - delegated to FolderManager
    private fun onDeleteFolder(event: HomeEvent.OnDeleteFolder) {
        launch { FolderManager.deleteFolder(event.folder) }
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
        launch { AllBookmarksManager.deleteBookmarks(listOf(event.bookmark)) }
    }

    private fun onMoveBookmarkToFolder(event: HomeEvent.OnMoveBookmarkToFolder) {
        launch {
            AllBookmarksManager.moveBookmarksToFolder(
                listOf(event.bookmark),
                event.targetFolder,
            )
        }
    }

    private fun onMoveBookmarkToTag(event: HomeEvent.OnMoveBookmarkToTag) {
        launch { AllBookmarksManager.addTagToBookmark(event.targetTag, event.bookmark) }
    }

    override fun clear() {
        isInitialized = false
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        super.clear()
    }
}
