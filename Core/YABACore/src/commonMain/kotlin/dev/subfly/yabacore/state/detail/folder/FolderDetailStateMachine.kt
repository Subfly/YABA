package dev.subfly.yabacore.state.detail.folder

import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.FolderManager
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

@OptIn(ExperimentalUuidApi::class)
class FolderDetailStateMachine :
    BaseStateMachine<FolderDetailUIState, FolderDetailEvent>(initialState = FolderDetailUIState()) {
    private var isInitialized = false
    private var dataSubscriptionJob: Job? = null
    private val preferencesStore = SettingsStores.userPreferences

    private val queryFlow = MutableStateFlow("")
    private val folderIdFlow = MutableStateFlow<Uuid?>(null)

    override fun onEvent(event: FolderDetailEvent) {
        when (event) {
            is FolderDetailEvent.OnInit -> onInit(event.folderId)
            is FolderDetailEvent.OnChangeQuery -> onChangeQuery(event.query)
            is FolderDetailEvent.OnToggleSelectionMode -> onToggleSelectionMode()
            is FolderDetailEvent.OnToggleBookmarkSelection -> onToggleBookmarkSelection(event.bookmarkId)
            is FolderDetailEvent.OnMoveSelectedToFolder -> onMoveSelectedToFolder(event.targetFolder)
            FolderDetailEvent.OnDeleteSelected -> onDeleteSelected()
            is FolderDetailEvent.OnDeleteBookmark -> onDeleteBookmark(event.bookmark)
            is FolderDetailEvent.OnChangeSort -> onChangeSort(event.sortType, event.sortOrder)
            is FolderDetailEvent.OnChangeAppearance -> onChangeAppearance(event.appearance)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun onInit(folderId: String) {
        if (isInitialized) return
        isInitialized = true
        folderIdFlow.value = Uuid.parse(folderId)

        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = launch {
            combine(
                folderIdFlow,
                queryFlow,
                preferencesStore.preferencesFlow
            ) { folderId, query, prefs ->
                if (folderId == null) null
                else Triple(folderId, query, prefs)
            }.flatMapLatest { params ->
                if (params == null) {
                    MutableStateFlow(FolderDetailUIState())
                } else {
                    val (folderId, query, prefs) = params
                    updateState { it.copy(isLoading = true, query = query) }

                    val folderFlow = FolderManager.observeFolder(folderId)
                    val bookmarksFlow = AllBookmarksManager.searchBookmarksFlow(
                        query = query,
                        filters = BookmarkSearchFilters(folderIds = setOf(folderId)),
                        sortType = prefs.preferredCollectionSorting,
                        sortOrder = prefs.preferredSortOrder
                    )

                    combine(
                        folderFlow,
                        bookmarksFlow,
                        preferencesStore.preferencesFlow
                    ) { folder, bookmarks, latestPrefs ->
                        currentState().copy(
                            folder = folder,
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
            val nextSelectionMode = state.isSelectionMode.not()
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

    private fun onMoveSelectedToFolder(targetFolder: dev.subfly.yabacore.model.ui.FolderUiModel) {
        val selectedIds = currentState().selectedBookmarkIds
        if (selectedIds.isEmpty()) return

        launch {
            val bookmarksToMove = currentState().bookmarks.filter { it.id in selectedIds }
            AllBookmarksManager.moveBookmarksToFolder(bookmarksToMove, targetFolder)
            updateState { it.copy(isSelectionMode = false, selectedBookmarkIds = emptySet()) }
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

    private fun onDeleteBookmark(bookmark: dev.subfly.yabacore.model.ui.BookmarkUiModel) {
        launch { AllBookmarksManager.deleteBookmarks(listOf(bookmark)) }
    }

    private fun onChangeSort(sortType: SortType, sortOrder: SortOrderType) {
        launch {
            preferencesStore.setPreferredCollectionSorting(sortType)
            preferencesStore.setPreferredSortOrder(sortOrder)
        }
    }

    private fun onChangeAppearance(appearance: BookmarkAppearance) {
        launch {
            preferencesStore.setPreferredBookmarkAppearance(appearance)
        }
    }

    override fun clear() {
        isInitialized = false
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        super.clear()
    }
}
