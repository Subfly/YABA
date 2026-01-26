package dev.subfly.yabacore.state.home

import androidx.compose.ui.util.fastForEach
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.HomeFolderRowUiModel
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

private data class SortingParams(
    val sortType: SortType,
    val sortOrder: SortOrderType,
)

private data class HomeDataSnapshot(
    val folders: List<FolderUiModel>,
    val tags: List<dev.subfly.yabacore.model.ui.TagUiModel>,
    val recentBookmarks: List<dev.subfly.yabacore.model.ui.BookmarkUiModel>,
    val bookmarkAppearance: dev.subfly.yabacore.model.utils.BookmarkAppearance,
    val cardImageSizing: dev.subfly.yabacore.model.utils.CardImageSizing,
    val collectionSorting: SortType,
    val sortOrder: SortOrderType,
)

private const val RECENT_BOOKMARKS_LIMIT = 5

class HomeStateMachine : BaseStateMachine<HomeUIState, HomeEvent>(initialState = HomeUIState()) {
    private var isInitialized = false
    private var dataSubscriptionJob: Job? = null
    private var allFoldersSorted: List<FolderUiModel> = emptyList()
    private val preferencesStore
        get() = SettingsStores.userPreferences

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.OnInit -> onInit()
            is HomeEvent.OnChangeBookmarkAppearance -> onChangeBookmarkAppearance(event)
            is HomeEvent.OnChangeCardImageSizing -> onChangeCardImageSizing(event)
            is HomeEvent.OnChangeCollectionSorting -> onChangeCollectionSorting(event)
            is HomeEvent.OnChangeSortOrder -> onChangeSortOrder(event)
            is HomeEvent.OnToggleFolderExpanded -> onToggleFolderExpanded(event)
            is HomeEvent.OnDeleteFolder -> onDeleteFolder(event)
            is HomeEvent.OnMoveFolder -> onMoveFolder(event)
            is HomeEvent.OnDeleteTag -> onDeleteTag(event)
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
                        FolderManager.observeAllFoldersSorted(
                            sortType = sortingParams.sortType,
                            sortOrder = sortingParams.sortOrder,
                        ),
                        TagManager.observeTags(
                            sortType = sortingParams.sortType,
                            sortOrder = sortingParams.sortOrder,
                        ),
                        recentBookmarksFlow,
                    ) { preferences, folders, tags, recentBookmarks ->
                        HomeDataSnapshot(
                            folders = folders,
                            tags = tags,
                            recentBookmarks = recentBookmarks,
                            bookmarkAppearance = preferences.preferredBookmarkAppearance,
                            cardImageSizing = preferences.preferredCardImageSizing,
                            collectionSorting = preferences.preferredCollectionSorting,
                            sortOrder = preferences.preferredCollectionSortOrder,
                        )
                    }
                }
                .collectLatest { snapshot ->
                    val state = currentState()
                    val existingIds = snapshot.folders.asSequence().map { it.id }.toSet()
                    val prunedExpanded = state.expandedFolderIds.intersect(existingIds)
                    val rows = buildVisibleFolderRows(
                        allFolders = snapshot.folders,
                        expandedFolderIds = prunedExpanded,
                    )

                    allFoldersSorted = snapshot.folders
                    updateState { state ->
                        state.copy(
                            folderRows = rows,
                            expandedFolderIds = prunedExpanded,
                            tags = snapshot.tags,
                            recentBookmarks = snapshot.recentBookmarks,
                            bookmarkAppearance = snapshot.bookmarkAppearance,
                            cardImageSizing = snapshot.cardImageSizing,
                            collectionSorting = snapshot.collectionSorting,
                            sortOrder = snapshot.sortOrder,
                            isLoading = false,
                        )
                    }
                }
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

    private fun onToggleFolderExpanded(event: HomeEvent.OnToggleFolderExpanded) {
        val folderId = event.folderId
        if (folderId.isBlank()) return

        val state = currentState()
        val nextExpanded =
            if (folderId in state.expandedFolderIds) state.expandedFolderIds - folderId
            else state.expandedFolderIds + folderId

        val rows = buildVisibleFolderRows(
            allFolders = allFoldersSorted,
            expandedFolderIds = nextExpanded,
        )

        updateState { state ->
            state.copy(
                expandedFolderIds = nextExpanded,
                folderRows = rows,
            )
        }
    }

    private fun onDeleteFolder(event: HomeEvent.OnDeleteFolder) {
        launch { FolderManager.deleteFolder(event.folder.id) }
    }

    private fun onMoveFolder(event: HomeEvent.OnMoveFolder) {
        launch { FolderManager.moveFolder(event.folder, event.targetParent) }
    }

    // Tag operations - delegated to TagManager
    private fun onDeleteTag(event: HomeEvent.OnDeleteTag) {
        launch { TagManager.deleteTag(event.tag) }
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
        launch {
            AllBookmarksManager.addTagToBookmark(
                tagId = event.targetTag.id,
                bookmarkId = event.bookmark.id
            )
        }
    }

    override fun clear() {
        isInitialized = false
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        allFoldersSorted = emptyList()
        super.clear()
    }
}

private fun buildVisibleFolderRows(
    allFolders: List<FolderUiModel>,
    expandedFolderIds: Set<String>,
): List<HomeFolderRowUiModel> {
    if (allFolders.isEmpty()) return emptyList()

    val childrenByParentId = mutableMapOf<String?, MutableList<FolderUiModel>>()
    allFolders.fastForEach { folder ->
        childrenByParentId.getOrPut(folder.parentId) { mutableListOf() }.add(folder)
    }

    val result = mutableListOf<HomeFolderRowUiModel>()
    val colorStack = mutableListOf<dev.subfly.yabacore.model.utils.YabaColor>()
    val visited = mutableSetOf<String>()

    fun visit(folder: FolderUiModel) {
        if (!visited.add(folder.id)) return

        val children = childrenByParentId[folder.id].orEmpty()
        val hasChildren = children.isNotEmpty()
        val isExpanded = folder.id in expandedFolderIds

        result.add(
            HomeFolderRowUiModel(
                folder = folder,
                parentColors = colorStack.toList(),
                isExpanded = isExpanded,
                hasChildren = hasChildren,
            )
        )

        if (!hasChildren || !isExpanded) return

        colorStack.add(folder.color)
        children.fastForEach(::visit)
        if (colorStack.isNotEmpty()) colorStack.removeAt(colorStack.lastIndex)
    }

    childrenByParentId[null].orEmpty().fastForEach(::visit)
    return result
}
