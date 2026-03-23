package dev.subfly.yabacore.state.detail.notemark

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.mappers.toPreviewUiModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.BookmarkWithRelations
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.access.YabaFileAccessor
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.HighlightManager
import dev.subfly.yabacore.model.highlight.HighlightType
import dev.subfly.yabacore.managers.NotemarkManager
import dev.subfly.yabacore.managers.ReadableContentManager
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.notifications.NotificationManager
import dev.subfly.yabacore.state.base.BaseStateMachine
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class NotemarkDetailStateMachine :
    BaseStateMachine<NotemarkDetailUIState, NotemarkDetailEvent>(
        initialState = NotemarkDetailUIState(),
    ) {
    private var isInitialized = false
    private var didBootstrapEditor = false
    private var dataSubscriptionJob: Job? = null
    private val bookmarkIdFlow = MutableStateFlow<String?>(null)

    /** Matches last known persisted document JSON (disk bootstrap or successful [OnSave]). */
    private var lastPersistedJson: String? = null

    override fun onEvent(event: NotemarkDetailEvent) {
        when (event) {
            is NotemarkDetailEvent.OnInit -> onInit(event.bookmarkId)
            is NotemarkDetailEvent.OnSave -> onSave(event)
            NotemarkDetailEvent.OnDeleteBookmark -> onDeleteBookmark()
            is NotemarkDetailEvent.OnCreateHighlight -> onCreateHighlight(event)
            is NotemarkDetailEvent.OnUpdateHighlight -> onUpdateHighlight(event)
            is NotemarkDetailEvent.OnDeleteHighlight -> onDeleteHighlight(event)
            is NotemarkDetailEvent.OnHighlightReadableCreateCommitted -> onHighlightReadableCreateCommitted(event)
            is NotemarkDetailEvent.OnHighlightReadableDeleteCommitted -> onHighlightReadableDeleteCommitted(event)
            is NotemarkDetailEvent.OnScrollToHighlight -> onScrollToHighlight(event)
            NotemarkDetailEvent.OnClearScrollToHighlight -> onClearScrollToHighlight()
            NotemarkDetailEvent.OnRequestNotificationPermission -> onRequestNotificationPermission()
            is NotemarkDetailEvent.OnScheduleReminder -> onScheduleReminder(event)
            NotemarkDetailEvent.OnCancelReminder -> onCancelReminder()
            NotemarkDetailEvent.OnPickImageFromGallery -> onPickImageFromGallery()
            NotemarkDetailEvent.OnCaptureImageFromCamera -> onCaptureImageFromCamera()
            NotemarkDetailEvent.OnConsumedInlineImageInsert -> onConsumedInlineImageInsert()
        }
    }

    private fun onInit(bookmarkId: String) {
        if (isInitialized) return
        isInitialized = true
        didBootstrapEditor = false
        bookmarkIdFlow.value = bookmarkId

        launch {
            val reminderDate = NotificationManager.getPendingReminderDate(bookmarkId)
            updateState { it.copy(reminderDateEpochMillis = reminderDate) }
        }

        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = launch {
            bookmarkIdFlow
                .flatMapLatest { id ->
                    if (id == null) {
                        MutableStateFlow(NotemarkDetailUIState())
                    } else {
                        updateState { it.copy(isLoading = true) }

                        val bookmarkFlow =
                            DatabaseProvider.bookmarkDao.observeByIdWithRelations(id)
                        val noteFlow = DatabaseProvider.noteBookmarkDao.observeByBookmarkId(id)
                        val readableVersionsFlow =
                            ReadableContentManager.observeReadableVersions(id)

                        combine(
                            bookmarkFlow,
                            noteFlow,
                            readableVersionsFlow,
                        ) { bookmark, note, versions ->
                            flow {
                                val bookmarkModel =
                                    if (bookmark != null) {
                                        bookmark.toBookmarkPreviewUiModel()
                                    } else {
                                        null
                                    }
                                val vid = note?.readableVersionId
                                val selectedVersion =
                                    versions.find { v -> v.versionId == vid }
                                        ?: versions.firstOrNull()
                                val highlights = selectedVersion?.highlights ?: emptyList()
                                val assetsBaseUrl = NotemarkManager.resolveNoteAssetsBaseUrl(id)

                                if (note != null && !didBootstrapEditor) {
                                    didBootstrapEditor = true
                                    val docJson =
                                        NotemarkManager.readNoteDocumentJson(id).orEmpty()
                                    lastPersistedJson = docJson
                                    emit(
                                        currentState()
                                            .copy(
                                                bookmark = bookmarkModel,
                                                readableVersionId = vid,
                                                highlights = highlights,
                                                assetsBaseUrl = assetsBaseUrl,
                                                initialDocumentJson = docJson,
                                                isLoading = false,
                                            ),
                                    )
                                } else {
                                    emit(
                                        currentState()
                                            .copy(
                                                bookmark = bookmarkModel,
                                                readableVersionId = vid,
                                                highlights = highlights,
                                                assetsBaseUrl = assetsBaseUrl,
                                                isLoading = false,
                                            ),
                                    )
                                }
                            }
                        }
                            .flatMapLatest { it }
                    }
                }
                .collectLatest { emitted ->
                    updateState { current ->
                        emitted.copy(
                            inlineImageDocumentSrc = current.inlineImageDocumentSrc,
                        )
                    }
                }
        }
    }

    private fun onSave(event: NotemarkDetailEvent.OnSave) {
        persistNoteDocumentJsonIfChanged(event.documentJson)
    }

    private fun persistNoteDocumentJsonIfChanged(json: String) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        if (json == lastPersistedJson) return
        launch {
            val result = NotemarkManager.persistNoteDocumentJsonAwait(bookmarkId, json)
            if (result.isSuccess) {
                lastPersistedJson = json
            }
        }
    }

    private fun onDeleteBookmark() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        AllBookmarksManager.deleteBookmarks(listOf(bookmarkId))
    }

    private fun onCreateHighlight(event: NotemarkDetailEvent.OnCreateHighlight) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        HighlightManager.createHighlight(
            highlightId = event.highlightId,
            bookmarkId = bookmarkId,
            readableVersionId = event.readableVersionId,
            type = HighlightType.NOTE,
            colorRole = event.colorRole,
            note = event.note,
            quoteText = event.quoteText,
            extrasJson = null,
        )
    }

    private fun onUpdateHighlight(event: NotemarkDetailEvent.OnUpdateHighlight) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        HighlightManager.updateHighlight(
            bookmarkId = bookmarkId,
            highlightId = event.highlightId,
            colorRole = event.colorRole,
            note = event.note,
        )
    }

    private fun onDeleteHighlight(event: NotemarkDetailEvent.OnDeleteHighlight) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        HighlightManager.deleteHighlight(bookmarkId, event.highlightId)
    }

    private fun onHighlightReadableCreateCommitted(
        event: NotemarkDetailEvent.OnHighlightReadableCreateCommitted,
    ) {
        val activeBookmarkId = bookmarkIdFlow.value ?: return
        val req = event.request
        if (req.selectionDraft.bookmarkId != activeBookmarkId) return
        launch {
            ReadableContentManager.syncNotemarkReadableMirror(
                bookmarkId = req.selectionDraft.bookmarkId,
                versionId = req.selectionDraft.readableVersionId,
                documentJson = event.documentJson,
            )
            HighlightManager.createHighlight(
                highlightId = event.highlightId,
                bookmarkId = req.selectionDraft.bookmarkId,
                readableVersionId = req.selectionDraft.readableVersionId,
                type = HighlightType.NOTE,
                colorRole = req.colorRole,
                note = req.note,
                quoteText = req.selectionDraft.quote.displayText.ifBlank { null },
                extrasJson = null,
            )
            persistNoteDocumentJsonIfChanged(event.documentJson)
        }
    }

    private fun onHighlightReadableDeleteCommitted(
        event: NotemarkDetailEvent.OnHighlightReadableDeleteCommitted,
    ) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch {
            val versionId = currentState().readableVersionId ?: return@launch
            ReadableContentManager.syncNotemarkReadableMirror(
                bookmarkId = bookmarkId,
                versionId = versionId,
                documentJson = event.documentJson,
            )
            HighlightManager.deleteHighlight(bookmarkId, event.highlightId)
            persistNoteDocumentJsonIfChanged(event.documentJson)
        }
    }

    private fun onScrollToHighlight(event: NotemarkDetailEvent.OnScrollToHighlight) {
        updateState { it.copy(scrollToHighlightId = event.highlightId) }
    }

    private fun onClearScrollToHighlight() {
        updateState { it.copy(scrollToHighlightId = null) }
    }

    private fun onRequestNotificationPermission() {
        launch { NotificationManager.requestPermission() }
    }

    private fun onScheduleReminder(event: NotemarkDetailEvent.OnScheduleReminder) {
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
        launch { NotificationManager.cancelReminder(bookmarkId) }
        updateState { it.copy(reminderDateEpochMillis = null) }
    }

    private fun onPickImageFromGallery() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch {
            try {
                val file = YabaFileAccessor.pickSingleImage() ?: return@launch
                val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                val ext =
                    NotemarkManager.sanitizeInlineImageExtension(
                        file.name.substringAfterLast('.', ""),
                    )
                val docSrc = NotemarkManager.saveInlineImageBytes(bookmarkId, bytes, ext)
                updateState { it.copy(inlineImageDocumentSrc = docSrc) }
            } catch (_: Exception) {
                // Picker cancelled or read failed
            }
        }
    }

    private fun onCaptureImageFromCamera() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch {
            try {
                val file = YabaFileAccessor.capturePhoto() ?: return@launch
                val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                val ext =
                    NotemarkManager.sanitizeInlineImageExtension(
                        file.name.substringAfterLast('.', ""),
                    )
                val docSrc = NotemarkManager.saveInlineImageBytes(bookmarkId, bytes, ext)
                updateState { it.copy(inlineImageDocumentSrc = docSrc) }
            } catch (_: Exception) {
                // Capture cancelled or read failed
            }
        }
    }

    private fun onConsumedInlineImageInsert() {
        updateState { it.copy(inlineImageDocumentSrc = null) }
    }

    override fun clear() {
        isInitialized = false
        didBootstrapEditor = false
        lastPersistedJson = null
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        bookmarkIdFlow.value = null
        super.clear()
    }

    private suspend fun BookmarkWithRelations.toBookmarkPreviewUiModel(): BookmarkPreviewUiModel {
        val folderUi = folder.toUiModel()
        val tagsUi = tags.map { it.toUiModel() }
        val localImageAbsolutePath =
            bookmark.localImagePath?.let { path -> BookmarkFileManager.getAbsolutePath(path) }
        val localIconAbsolutePath =
            bookmark.localIconPath?.let { path -> BookmarkFileManager.getAbsolutePath(path) }
        return bookmark.toPreviewUiModel(
            folder = folderUi,
            tags = tagsUi,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
        )
    }
}
