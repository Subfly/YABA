package dev.subfly.yabacore.state.detail.docmark

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.mappers.toPreviewUiModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.BookmarkWithRelations
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.access.YabaFileAccessor
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.DocmarkManager
import dev.subfly.yabacore.managers.AnnotationManager
import dev.subfly.yabacore.managers.ReadableContentManager
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.utils.DocmarkType
import dev.subfly.yabacore.model.utils.ReaderFontSize
import dev.subfly.yabacore.model.utils.ReaderLineHeight
import dev.subfly.yabacore.model.utils.ReaderTheme
import dev.subfly.yabacore.notifications.NotificationManager
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
            DocmarkDetailEvent.OnShareDocument -> onShareDocument()
            DocmarkDetailEvent.OnExportDocument -> onExportDocument()
            DocmarkDetailEvent.OnToggleReaderTheme -> onToggleReaderTheme()
            DocmarkDetailEvent.OnToggleReaderFontSize -> onToggleReaderFontSize()
            DocmarkDetailEvent.OnToggleReaderLineHeight -> onToggleReaderLineHeight()
            is DocmarkDetailEvent.OnSetReaderTheme -> onSetReaderTheme(event.theme)
            is DocmarkDetailEvent.OnSetReaderFontSize -> onSetReaderFontSize(event.fontSize)
            is DocmarkDetailEvent.OnSetReaderLineHeight -> onSetReaderLineHeight(event.lineHeight)
            is DocmarkDetailEvent.OnDeleteAnnotation -> onDeleteAnnotation(event.annotationId)
            is DocmarkDetailEvent.OnScrollToAnnotation -> onScrollToAnnotation(event.annotationId)
            DocmarkDetailEvent.OnClearScrollToAnnotation -> onClearScrollToAnnotation()
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
         * Annotation creation still needs a [ReadableVersionEntity] id; ensure a minimal placeholder once.
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
                    val docType = DatabaseProvider.docBookmarkDao.getByBookmarkId(id)?.type ?: DocmarkType.PDF
                    val docPath = DocmarkManager.resolveDocumentAbsolutePath(id, docType)
                    if (docPath.isNullOrBlank().not()) {
                        ReadableContentManager.ensureDocmarkAnnotationReadableVersionIfNeeded(id)
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
                            val docmarkType = doc?.type ?: DocmarkType.PDF
                            val documentPath = DocmarkManager.resolveDocumentAbsolutePath(id, docmarkType)
                            emit(
                                currentState().copy(
                                    bookmark = bookmark?.toBookmarkPreviewUiModel(),
                                    summary = doc?.summary,
                                    docmarkType = docmarkType,
                                    documentAbsolutePath = documentPath,
                                    selectedReadableVersionId = selectedVersion?.versionId,
                                    annotations = selectedVersion?.annotations ?: emptyList(),
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
                        docmarkType = newState.docmarkType,
                        documentAbsolutePath = newState.documentAbsolutePath,
                        selectedReadableVersionId = newState.selectedReadableVersionId,
                        annotations = newState.annotations,
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

    private fun onShareDocument() {
        launch {
            val bookmarkId = bookmarkIdFlow.value ?: return@launch
            YabaFileAccessor.shareDocmark(bookmarkId)
        }
    }

    private fun onExportDocument() {
        launch {
            val bookmarkId = bookmarkIdFlow.value ?: return@launch
            val bookmark = currentState().bookmark ?: return@launch
            val name = bookmark.label.ifBlank { "document" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            YabaFileAccessor.exportDocmark(
                bookmarkId = bookmarkId,
                suggestedName = name,
                extension = null,
            )
        }
    }

    private fun onToggleReaderTheme() {
        updateState { state ->
            val currentPreferences = state.readerPreferences
            state.copy(
                readerPreferences = currentPreferences.copy(
                    theme = when (currentPreferences.theme) {
                        ReaderTheme.SYSTEM -> ReaderTheme.DARK
                        ReaderTheme.DARK -> ReaderTheme.LIGHT
                        ReaderTheme.LIGHT -> ReaderTheme.SEPIA
                        ReaderTheme.SEPIA -> ReaderTheme.SYSTEM
                    },
                ),
            )
        }
    }

    private fun onToggleReaderFontSize() {
        updateState { state ->
            val currentPreferences = state.readerPreferences
            state.copy(
                readerPreferences = currentPreferences.copy(
                    fontSize = when (currentPreferences.fontSize) {
                        ReaderFontSize.SMALL -> ReaderFontSize.MEDIUM
                        ReaderFontSize.MEDIUM -> ReaderFontSize.LARGE
                        ReaderFontSize.LARGE -> ReaderFontSize.SMALL
                    },
                ),
            )
        }
    }

    private fun onToggleReaderLineHeight() {
        updateState { state ->
            val currentPreferences = state.readerPreferences
            state.copy(
                readerPreferences = currentPreferences.copy(
                    lineHeight = when (currentPreferences.lineHeight) {
                        ReaderLineHeight.NORMAL -> ReaderLineHeight.RELAXED
                        ReaderLineHeight.RELAXED -> ReaderLineHeight.NORMAL
                    },
                ),
            )
        }
    }

    private fun onSetReaderTheme(theme: ReaderTheme) {
        updateState { state ->
            state.copy(
                readerPreferences = state.readerPreferences.copy(theme = theme),
            )
        }
    }

    private fun onSetReaderFontSize(fontSize: ReaderFontSize) {
        updateState { state ->
            state.copy(
                readerPreferences = state.readerPreferences.copy(fontSize = fontSize),
            )
        }
    }

    private fun onSetReaderLineHeight(lineHeight: ReaderLineHeight) {
        updateState { state ->
            state.copy(
                readerPreferences = state.readerPreferences.copy(lineHeight = lineHeight),
            )
        }
    }

    private fun onDeleteAnnotation(annotationId: String) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        AnnotationManager.deleteAnnotation(
            bookmarkId = bookmarkId,
            annotationId = annotationId,
        )
    }

    private fun onScrollToAnnotation(annotationId: String) {
        updateState { it.copy(scrollToAnnotationId = annotationId) }
    }

    private fun onClearScrollToAnnotation() {
        updateState { it.copy(scrollToAnnotationId = null) }
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
