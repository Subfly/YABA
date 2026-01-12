package dev.subfly.yabacore.state.selection

import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalUuidApi::class)
class FolderSelectionStateMachine :
    BaseStateMachine<FolderSelectionUIState, FolderSelectionEvent>(
        initialState = FolderSelectionUIState()
    ) {

    private var isInitialized = false
    private var folderSubscriptionJob: Job? = null

    // Stored context for filtering and move operations
    private var mode: FolderSelectionMode = FolderSelectionMode.FOLDER_SELECTION
    private var contextFolderId: Uuid? = null
    private var contextBookmarkId: Uuid? = null
    private var excludedIds: Set<Uuid> = emptySet()
    private var currentParentId: Uuid? = null

    // Store unfiltered folders for re-filtering on search changes
    private var allAvailableFolders: List<FolderUiModel> = emptyList()

    override fun onEvent(event: FolderSelectionEvent) {
        when (event) {
            is FolderSelectionEvent.OnInit -> onInit(event)
            is FolderSelectionEvent.OnSearchQueryChanged -> onSearchQueryChanged(event)
            is FolderSelectionEvent.OnMoveFolderToSelected -> onMoveFolderToSelected(event)
            is FolderSelectionEvent.OnMoveBookmarkToSelected -> onMoveBookmarkToSelected(event)
        }
    }

    private fun onInit(event: FolderSelectionEvent.OnInit) {
        if (isInitialized) return
        isInitialized = true

        mode = event.mode
        contextFolderId = event.contextFolderId?.let { Uuid.parse(it) }
        contextBookmarkId = event.contextBookmarkId?.let { Uuid.parse(it) }

        launch {
            updateState { it.copy(isLoading = true) }

            // Pre-compute excluded IDs based on mode
            when (mode) {
                FolderSelectionMode.FOLDER_SELECTION -> {
                    // No exclusions for simple folder selection
                    excludedIds = emptySet()
                    updateState { it.copy(canMoveToRoot = false) }
                }

                FolderSelectionMode.PARENT_SELECTION -> {
                    // Exclude the folder itself, current parent, and all its descendants
                    contextFolderId?.let { folderId ->
                        val folder = FolderManager.getFolder(folderId)
                        if (folder != null) {
                            val descendants = collectDescendantIds(folderId)
                            excludedIds = setOf(folderId) + descendants
                            currentParentId = folder.parentId

                            // Can move to root if folder currently has a parent
                            updateState { it.copy(canMoveToRoot = folder.parentId != null) }
                        }
                    } ?: run {
                        excludedIds = emptySet()
                        updateState { it.copy(canMoveToRoot = false) }
                    }
                }

                FolderSelectionMode.FOLDER_MOVE -> {
                    // Same as PARENT_SELECTION: exclude self, descendants, and current parent
                    contextFolderId?.let { folderId ->
                        val folder = FolderManager.getFolder(folderId)
                        if (folder != null) {
                            val descendants = collectDescendantIds(folderId)
                            excludedIds = setOf(folderId) + descendants
                            currentParentId = folder.parentId

                            // Can move to root if folder currently has a parent
                            updateState { it.copy(canMoveToRoot = folder.parentId != null) }
                        }
                    } ?: run {
                        excludedIds = emptySet()
                        updateState { it.copy(canMoveToRoot = false) }
                    }
                }

                FolderSelectionMode.BOOKMARK_MOVE -> {
                    // Exclude only the bookmark's current containing folder
                    excludedIds = contextFolderId?.let { setOf(it) } ?: emptySet()
                    updateState { it.copy(canMoveToRoot = false) }
                }
            }

            // Start observing folders
            startFolderObservation()
        }
    }

    private fun startFolderObservation() {
        folderSubscriptionJob?.cancel()
        folderSubscriptionJob = launch {
            FolderManager.observeAllFoldersSorted(
                sortType = SortType.LABEL,
                sortOrder = SortOrderType.ASCENDING
            ).collectLatest { allFolders ->
                // Apply mode-based exclusions (these don't change during the session)
                allAvailableFolders = applyModeExclusions(allFolders)

                // Apply search filter
                val filtered = applySearchFilter(allAvailableFolders, currentState().searchQuery)

                updateState {
                    it.copy(
                        folders = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun onSearchQueryChanged(event: FolderSelectionEvent.OnSearchQueryChanged) {
        val query = event.query
        val filtered = applySearchFilter(allAvailableFolders, query)
        updateState { state ->
            state.copy(
                searchQuery = query,
                folders = filtered
            )
        }
    }

    private fun onMoveFolderToSelected(event: FolderSelectionEvent.OnMoveFolderToSelected) {
        val folderToMove = contextFolderId ?: return

        launch {
            val folder = FolderManager.getFolder(folderToMove) ?: return@launch
            val targetFolder = event.targetFolderId?.let { FolderManager.getFolder(Uuid.parse(it)) }
            FolderManager.moveFolder(folder, targetFolder)
        }
    }

    private fun onMoveBookmarkToSelected(event: FolderSelectionEvent.OnMoveBookmarkToSelected) {
        val bookmarkToMove = contextBookmarkId ?: return

        launch {
            val bookmark = AllBookmarksManager.getBookmarkDetail(bookmarkToMove) ?: return@launch
            val targetFolder = FolderManager.getFolder(Uuid.parse(event.targetFolderId)) ?: return@launch
            AllBookmarksManager.moveBookmarksToFolder(listOf(bookmark), targetFolder)
        }
    }

    /**
     * Applies mode-based exclusions (folder itself, descendants, current parent).
     * These exclusions are determined at init and don't change.
     */
    private fun applyModeExclusions(allFolders: List<FolderUiModel>): List<FolderUiModel> {
        var result = allFolders

        // Apply exclusions based on mode
        if (excludedIds.isNotEmpty()) {
            result = result.filter { folder -> folder.id !in excludedIds }
        }

        // For parent/folder selection, also exclude the current parent (since it's already the parent)
        val excludeCurrentParent = mode == FolderSelectionMode.PARENT_SELECTION ||
            mode == FolderSelectionMode.FOLDER_MOVE
        if (excludeCurrentParent && currentParentId != null) {
            result = result.filter { folder -> folder.id != currentParentId }
        }

        return result
    }

    /**
     * Applies search filter to folders.
     */
    private fun applySearchFilter(
        folders: List<FolderUiModel>,
        searchQuery: String,
    ): List<FolderUiModel> {
        if (searchQuery.isBlank()) return folders

        val lowerQuery = searchQuery.lowercase()
        return folders.filter { folder ->
            folder.label.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Collects all descendant folder IDs for a given folder.
     */
    private suspend fun collectDescendantIds(rootId: Uuid): Set<Uuid> {
        val allFolders = FolderManager.getMovableFolders(
            currentFolderId = null,
            sortType = SortType.CUSTOM,
            sortOrder = SortOrderType.ASCENDING
        )
        val grouped = allFolders.groupBy { it.parentId }
        val visited = mutableSetOf<Uuid>()

        fun traverse(parentId: Uuid) {
            grouped[parentId]?.forEach { child ->
                if (visited.add(child.id)) {
                    traverse(child.id)
                }
            }
        }

        traverse(rootId)
        return visited
    }

    override fun clear() {
        isInitialized = false
        folderSubscriptionJob?.cancel()
        folderSubscriptionJob = null
        super.clear()
    }
}
