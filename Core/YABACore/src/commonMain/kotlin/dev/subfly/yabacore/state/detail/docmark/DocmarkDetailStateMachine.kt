package dev.subfly.yabacore.state.detail.docmark

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.mappers.toPreviewUiModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.BookmarkWithRelations
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.access.YabaFileAccessor
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.DocmarkManager
import dev.subfly.yabacore.managers.HighlightManager
import dev.subfly.yabacore.managers.ReadableContentManager
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.notifications.NotificationManager
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class DocmarkDetailStateMachine : BaseStateMachine<DocmarkDetailUIState, DocmarkDetailEvent>(
    initialState = DocmarkDetailUIState(),
) {
    private var isInitialized = false
    private val bookmarkIdFlow = MutableStateFlow<String?>(null)
    private val selectedReadableVersionIdFlow = MutableStateFlow<String?>(null)

    override fun onEvent(event: DocmarkDetailEvent) {
        when (event) {
            is DocmarkDetailEvent.OnInit -> onInit(event.bookmarkId)
            DocmarkDetailEvent.OnDeleteBookmark -> onDeleteBookmark()
            DocmarkDetailEvent.OnSharePdf -> onSharePdf()
            DocmarkDetailEvent.OnExportPdf -> onExportPdf()
            is DocmarkDetailEvent.OnDeleteHighlight -> onDeleteHighlight(event.highlightId)
            is DocmarkDetailEvent.OnScrollToHighlight -> onScrollToHighlight(event.highlightId)
            DocmarkDetailEvent.OnClearScrollToHighlight -> onClearScrollToHighlight()
            DocmarkDetailEvent.OnRequestNotificationPermission -> {}
            is DocmarkDetailEvent.OnScheduleReminder -> onScheduleReminder(event)
            DocmarkDetailEvent.OnCancelReminder -> onCancelReminder()
        }
    }

    private fun onInit(bookmarkId: String) {
        if (isInitialized) return
        isInitialized = true
        bookmarkIdFlow.value = bookmarkId

        launch {
            val reminderDate = NotificationManager.getPendingReminderDate(bookmarkId)
            updateState { it.copy(reminderDateEpochMillis = reminderDate) }
        }

        /**
         * New docmarks may have no readable rows after creation-time readable save was removed.
         * Highlight creation still needs a [ReadableVersionEntity] id; ensure a minimal placeholder once.
         */
        launch {
            bookmarkIdFlow.flatMapLatest { id ->
                if (id == null) {
                    emptyFlow()
                } else {
                    ReadableContentManager.observeReadableVersions(id)
                        .map { it.size }
                        .distinctUntilChanged()
                        .map { size -> Pair(id, size) }
                }
            }.collect { (id, size) ->
                if (size == 0) {
                    val pdfPath = DocmarkManager.resolvePdfAbsolutePath(id)
                    if (pdfPath.isNullOrBlank().not()) {
                        ReadableContentManager.ensurePdfDocmarkHighlightReadableVersionIfNeeded(id)
                    }
                }
            }
        }

        launch {
            bookmarkIdFlow.flatMapLatest { id ->
                if (id == null) {
                    MutableStateFlow(DocmarkDetailUIState())
                } else {
                    updateState { it.copy(isLoading = true) }
                    val bookmarkFlow = DatabaseProvider.bookmarkDao.observeByIdWithRelations(id)
                    val docFlow = DatabaseProvider.docBookmarkDao.observeByBookmarkId(id)
                    val readableVersionsFlow = ReadableContentManager.observeReadableVersions(id)
                    combine(
                        bookmarkFlow,
                        docFlow,
                        readableVersionsFlow,
                        selectedReadableVersionIdFlow,
                    ) { bookmark, doc, readableVersions, selectedReadableVersionId ->
                        flow {
                            val selectedVersion = readableVersions.find { it.versionId == selectedReadableVersionId }
                                ?: readableVersions.firstOrNull()
                            val pdfPath = DocmarkManager.resolvePdfAbsolutePath(id)
                            emit(
                                currentState().copy(
                                    bookmark = bookmark?.toBookmarkPreviewUiModel(),
                                    summary = doc?.summary,
                                    pdfAbsolutePath = pdfPath,
                                    selectedReadableVersionId = selectedVersion?.versionId,
                                    highlights = selectedVersion?.highlights ?: emptyList(),
                                    isLoading = false,
                                ),
                            )
                        }
                    }.flatMapLatest { it }
                }
            }.collectLatest { newState ->
                updateState {
                    it.copy(
                        bookmark = newState.bookmark,
                        summary = newState.summary,
                        pdfAbsolutePath = newState.pdfAbsolutePath,
                        selectedReadableVersionId = newState.selectedReadableVersionId,
                        highlights = newState.highlights,
                        isLoading = newState.isLoading,
                    )
                }
            }
        }
    }

    private fun onDeleteBookmark() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        AllBookmarksManager.deleteBookmarks(listOf(bookmarkId))
    }

    private fun onSharePdf() {
        launch {
            val bookmarkId = bookmarkIdFlow.value ?: return@launch
            YabaFileAccessor.shareDocmark(bookmarkId)
        }
    }

    private fun onExportPdf() {
        launch {
            val bookmarkId = bookmarkIdFlow.value ?: return@launch
            val bookmark = currentState().bookmark ?: return@launch
            val name = bookmark.label.ifBlank { "document" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            YabaFileAccessor.exportDocmark(
                bookmarkId = bookmarkId,
                suggestedName = name,
                extension = "pdf",
            )
        }
    }

    private fun onDeleteHighlight(highlightId: String) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        HighlightManager.deleteHighlight(
            bookmarkId = bookmarkId,
            highlightId = highlightId,
        )
    }

    private fun onScrollToHighlight(highlightId: String) {
        updateState { it.copy(scrollToHighlightId = highlightId) }
    }

    private fun onClearScrollToHighlight() {
        updateState { it.copy(scrollToHighlightId = null) }
    }

    private fun onScheduleReminder(event: DocmarkDetailEvent.OnScheduleReminder) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        val bookmark = currentState().bookmark ?: return
        launch {
            NotificationManager.cancelReminder(bookmarkId)
            NotificationManager.scheduleReminder(
                bookmarkId = bookmarkId,
                bookmarkKindCode = bookmark.kind.code,
                title = event.title,
                message = event.message,
                bookmarkLabel = bookmark.label,
                triggerDateEpochMillis = event.triggerAtEpochMillis,
            )
            updateState { it.copy(reminderDateEpochMillis = event.triggerAtEpochMillis) }
        }
    }

    private fun onCancelReminder() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch {
            NotificationManager.cancelReminder(bookmarkId)
            updateState { it.copy(reminderDateEpochMillis = null) }
        }
    }

    override fun clear() {
        isInitialized = false
        bookmarkIdFlow.value = null
        selectedReadableVersionIdFlow.value = null
        super.clear()
    }

    private suspend fun BookmarkWithRelations.toBookmarkPreviewUiModel(): BookmarkPreviewUiModel {
        val folderUi = folder.toUiModel()
        val tagsUi = tags.map { it.toUiModel() }
        val localImageAbsolutePath = bookmark.localImagePath?.let { path ->
            BookmarkFileManager.getAbsolutePath(path)
        }
        val localIconAbsolutePath = bookmark.localIconPath?.let { path ->
            BookmarkFileManager.getAbsolutePath(path)
        }
        return bookmark.toPreviewUiModel(
            folder = folderUi,
            tags = tagsUi,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
        )
    }
}
