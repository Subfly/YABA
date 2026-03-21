package dev.subfly.yabacore.state.detail.notemark

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.mappers.toPreviewUiModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.BookmarkWithRelations
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.HighlightManager
import dev.subfly.yabacore.managers.NotemarkManager
import dev.subfly.yabacore.managers.ReadableContentManager
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.utils.NoteSaveMode
import dev.subfly.yabacore.notifications.NotificationManager
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

private const val AUTOSAVE_DEBOUNCE_MS = 3_000L

@OptIn(ExperimentalCoroutinesApi::class)
class NotemarkDetailStateMachine :
    BaseStateMachine<NotemarkDetailUIState, NotemarkDetailEvent>(
        initialState = NotemarkDetailUIState(),
    ) {
    private var isInitialized = false
    private var didBootstrapEditor = false
    private var dataSubscriptionJob: Job? = null
    private var autosaveJob: Job? = null
    private val bookmarkIdFlow = MutableStateFlow<String?>(null)
    private val preferencesStore
        get() = SettingsStores.userPreferences

    override fun onEvent(event: NotemarkDetailEvent) {
        when (event) {
            is NotemarkDetailEvent.OnInit -> onInit(event.bookmarkId)
            is NotemarkDetailEvent.OnNoteSaveModeChanged -> onNoteSaveModeChanged(event)
            is NotemarkDetailEvent.OnEditorDocumentJsonChanged -> onEditorDocumentJsonChanged(event)
            NotemarkDetailEvent.OnManualSave -> onManualSave()
            NotemarkDetailEvent.OnFlushPendingSave -> onFlushPendingSave()
            NotemarkDetailEvent.OnDeleteBookmark -> onDeleteBookmark()
            is NotemarkDetailEvent.OnCreateHighlight -> onCreateHighlight(event)
            is NotemarkDetailEvent.OnUpdateHighlight -> onUpdateHighlight(event)
            is NotemarkDetailEvent.OnDeleteHighlight -> onDeleteHighlight(event)
            is NotemarkDetailEvent.OnScrollToHighlight -> onScrollToHighlight(event)
            NotemarkDetailEvent.OnClearScrollToHighlight -> onClearScrollToHighlight()
            NotemarkDetailEvent.OnRequestNotificationPermission -> onRequestNotificationPermission()
            is NotemarkDetailEvent.OnScheduleReminder -> onScheduleReminder(event)
            NotemarkDetailEvent.OnCancelReminder -> onCancelReminder()
        }
    }

    private fun onNoteSaveModeChanged(event: NotemarkDetailEvent.OnNoteSaveModeChanged) {
        updateState { it.copy(saveMode = event.mode) }
    }

    private fun onInit(bookmarkId: String) {
        if (isInitialized) return
        isInitialized = true
        didBootstrapEditor = false
        bookmarkIdFlow.value = bookmarkId

        launch {
            val reminderDate = NotificationManager.getPendingReminderDate(bookmarkId)
            val prefs = preferencesStore.get()
            updateState {
                it.copy(
                    reminderDateEpochMillis = reminderDate,
                    saveMode = prefs.preferredNoteSaveMode,
                )
            }
        }

        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = launch {
            bookmarkIdFlow.flatMapLatest { id ->
                if (id == null) {
                    MutableStateFlow(NotemarkDetailUIState())
                } else {
                    updateState { it.copy(isLoading = true) }

                    val bookmarkFlow = DatabaseProvider.bookmarkDao.observeByIdWithRelations(id)
                    val noteFlow = DatabaseProvider.noteBookmarkDao.observeByBookmarkId(id)
                    val readableVersionsFlow = ReadableContentManager.observeReadableVersions(id)

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
                                versions.find { v -> v.versionId == vid } ?: versions.firstOrNull()
                            val highlights = selectedVersion?.highlights ?: emptyList()
                            val assetsBaseUrl = NotemarkManager.resolveNoteAssetsBaseUrl(id)

                            if (note != null && !didBootstrapEditor) {
                                didBootstrapEditor = true
                                val docJson = NotemarkManager.readNoteDocumentJson(id).orEmpty()
                                emit(
                                    currentState().copy(
                                        bookmark = bookmarkModel,
                                        readableVersionId = vid,
                                        highlights = highlights,
                                        assetsBaseUrl = assetsBaseUrl,
                                        editorDocumentJson = docJson,
                                        lastSavedDocumentJson = docJson,
                                        isDirty = false,
                                        isLoading = false,
                                    ),
                                )
                            } else {
                                emit(
                                    currentState().copy(
                                        bookmark = bookmarkModel,
                                        readableVersionId = vid,
                                        highlights = highlights,
                                        assetsBaseUrl = assetsBaseUrl,
                                        isLoading = false,
                                    ),
                                )
                            }
                        }
                    }.flatMapLatest { it }
                }
            }.collectLatest { newState ->
                updateState { newState }
            }
        }
    }

    private fun onEditorDocumentJsonChanged(event: NotemarkDetailEvent.OnEditorDocumentJsonChanged) {
        val json = event.documentJson
        var scheduleAutosave = false
        updateState {
            val dirty = json != it.lastSavedDocumentJson
            scheduleAutosave =
                dirty && it.saveMode == NoteSaveMode.AUTOSAVE_3S_INACTIVITY
            it.copy(editorDocumentJson = json, isDirty = dirty)
        }
        if (scheduleAutosave) {
            scheduleAutosaveDebounced()
        }
    }

    private fun scheduleAutosaveDebounced() {
        autosaveJob?.cancel()
        autosaveJob = launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            performSave()
        }
    }

    private fun onManualSave() {
        autosaveJob?.cancel()
        performSave()
    }

    private fun onFlushPendingSave() {
        autosaveJob?.cancel()
        if (currentState().isDirty) {
            performSave()
        }
    }

    private fun performSave() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        val snapshot = currentState()
        if (!snapshot.isDirty) {
            updateState { it.copy(isSaving = false) }
            return
        }
        val documentJson = snapshot.editorDocumentJson
        updateState { it.copy(isSaving = true) }
        launch {
            val result = NotemarkManager.persistNoteDocumentJsonAwait(bookmarkId, documentJson)
            updateState {
                if (result.isSuccess) {
                    it.copy(
                        isSaving = false,
                        lastSavedDocumentJson = documentJson,
                        isDirty = false,
                    )
                } else {
                    it.copy(isSaving = false)
                }
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
            bookmarkId = bookmarkId,
            readableVersionId = event.readableVersionId,
            startSectionKey = event.startSectionKey,
            startOffsetInSection = event.startOffsetInSection,
            endSectionKey = event.endSectionKey,
            endOffsetInSection = event.endOffsetInSection,
            colorRole = event.colorRole,
            note = event.note,
            quoteText = event.quoteText,
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

    private fun onScrollToHighlight(event: NotemarkDetailEvent.OnScrollToHighlight) {
        updateState { it.copy(scrollToHighlightId = event.highlightId) }
    }

    private fun onClearScrollToHighlight() {
        updateState { it.copy(scrollToHighlightId = null) }
    }

    private fun onRequestNotificationPermission() {
        launch {
            NotificationManager.requestPermission()
        }
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
        launch {
            NotificationManager.cancelReminder(bookmarkId)
        }
        updateState { it.copy(reminderDateEpochMillis = null) }
    }

    override fun clear() {
        isInitialized = false
        didBootstrapEditor = false
        autosaveJob?.cancel()
        autosaveJob = null
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        bookmarkIdFlow.value = null
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
