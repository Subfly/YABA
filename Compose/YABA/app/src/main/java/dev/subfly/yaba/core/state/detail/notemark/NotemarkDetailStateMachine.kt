package dev.subfly.yaba.core.state.detail.notemark

import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.database.mappers.toPreviewUiModel
import dev.subfly.yaba.core.database.mappers.toUiModel
import dev.subfly.yaba.core.database.models.BookmarkWithRelations
import dev.subfly.yaba.core.filesystem.BookmarkFileManager
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.managers.NotemarkManager
import dev.subfly.yaba.core.managers.ReadableContentManager
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.notifications.NotificationManager
import dev.subfly.yaba.core.state.base.BaseStateMachine
import dev.subfly.yaba.core.webview.Toc
import dev.subfly.yaba.core.webview.WebShellLoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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
            is NotemarkDetailEvent.OnWebInitialContentLoad -> onWebInitialContentLoad(event)
            is NotemarkDetailEvent.OnTocChanged -> onTocChanged(event.toc)
            is NotemarkDetailEvent.OnNavigateToTocItem -> onNavigateToTocItem(event)
            NotemarkDetailEvent.OnClearTocNavigation -> onClearTocNavigation()
            NotemarkDetailEvent.OnDeleteBookmark -> onDeleteBookmark()
            NotemarkDetailEvent.OnRequestNotificationPermission -> onRequestNotificationPermission()
            is NotemarkDetailEvent.OnScheduleReminder -> onScheduleReminder(event)
            NotemarkDetailEvent.OnCancelReminder -> onCancelReminder()
            NotemarkDetailEvent.OnPickImageFromGallery -> onPickImageFromGallery()
            NotemarkDetailEvent.OnCaptureImageFromCamera -> onCaptureImageFromCamera()
            NotemarkDetailEvent.OnConsumedInlineImageInsert -> onConsumedInlineImageInsert()
            is NotemarkDetailEvent.OnExportMarkdownReady -> onExportMarkdownReady(event)
            is NotemarkDetailEvent.OnExportPdfReady -> onExportPdfReady(event)
        }
    }

    private fun onInit(bookmarkId: String) {
        if (isInitialized) return
        isInitialized = true
        didBootstrapEditor = false
        bookmarkIdFlow.value = bookmarkId
        AllBookmarksManager.recordBookmarkView(bookmarkId)

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
                                val assetsBaseUrl = NotemarkManager.resolveNoteAssetsBaseUrl(id)

                                if (note != null && !didBootstrapEditor) {
                                    didBootstrapEditor = true
                                    val docJson =
                                        NotemarkManager.readNoteDocumentJson(id).orEmpty()
                                    lastPersistedJson = docJson
                                    val loadGeneration = currentState().editorContentLoadGeneration + 1
                                    emit(
                                        currentState()
                                            .copy(
                                                bookmark = bookmarkModel,
                                                readableVersionId = vid,
                                                assetsBaseUrl = assetsBaseUrl,
                                                initialDocumentJson = docJson,
                                                editorContentLoadGeneration = loadGeneration,
                                                isLoading = true,
                                                webContentLoadFailed = false,
                                            ),
                                    )
                                } else {
                                    emit(
                                        currentState()
                                            .copy(
                                                bookmark = bookmarkModel,
                                                readableVersionId = vid,
                                                assetsBaseUrl = assetsBaseUrl,
                                                isLoading = false,
                                                webContentLoadFailed = false,
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
        persistNoteDocumentJsonIfChanged(event.documentJson, event.usedInlineAssetSrcs)
    }

    private fun onWebInitialContentLoad(event: NotemarkDetailEvent.OnWebInitialContentLoad) {
        updateState {
            it.copy(
                isLoading = false,
                webContentLoadFailed = event.result == WebShellLoadResult.Error,
            )
        }
    }

    private fun onTocChanged(toc: Toc?) {
        updateState { it.copy(toc = toc) }
    }

    private fun onNavigateToTocItem(event: NotemarkDetailEvent.OnNavigateToTocItem) {
        updateState { it.copy(pendingTocNavigate = event.id to event.extrasJson) }
    }

    private fun onClearTocNavigation() {
        updateState { it.copy(pendingTocNavigate = null) }
    }

    private fun persistNoteDocumentJsonIfChanged(
        json: String,
        usedInlineAssetSrcs: List<String>,
    ) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        if (json == lastPersistedJson) return
        launch {
            val result = NotemarkManager.persistNoteDocumentJsonAwait(bookmarkId, json)
            if (result.isSuccess) {
                lastPersistedJson = json
                NotemarkManager.pruneUnusedInlineAssets(bookmarkId, usedInlineAssetSrcs.toSet())
                updateState { it.copy(initialDocumentJson = json) }
            }
        }
    }

    private fun onDeleteBookmark() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        AllBookmarksManager.deleteBookmarks(listOf(bookmarkId))
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
                /* Show Error Toast */
            }
        }
    }

    private fun onConsumedInlineImageInsert() {
        updateState { it.copy(inlineImageDocumentSrc = null) }
    }

    private fun onExportMarkdownReady(event: NotemarkDetailEvent.OnExportMarkdownReady) {
        launch {
            if (event.markdown.isBlank()) return@launch
            val bookmarkId = bookmarkIdFlow.value ?: return@launch

            val label = currentState().bookmark?.label.orEmpty()
            val base = sanitizeExportBaseName(label)

            try {
                YabaFileAccessor.exportNotemarkMarkdownBundle(
                    markdown = event.markdown,
                    bookmarkId = bookmarkId,
                    suggestedMarkdownBaseName = base,
                )
                /* Show Success Toast */
            } catch (_: Exception) {
                /* Show Error Toast */
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun onExportPdfReady(event: NotemarkDetailEvent.OnExportPdfReady) {
        launch {
            if (event.pdfBase64.isBlank()) return@launch
            val payload = sanitizePdfBase64Payload(event.pdfBase64) ?: return@launch
            val bytes = runCatching { Base64.decode(payload) }.getOrNull() ?: return@launch
            if (bytes.isEmpty()) return@launch

            val label = currentState().bookmark?.label.orEmpty()
            val base = sanitizeExportBaseName(label)

            try {
                YabaFileAccessor.saveFileCopy(
                    bytes = bytes,
                    suggestedName = base,
                    extension = "pdf",
                )
                /* Show Success Toast */
            } catch (_: Exception) {
                /* Show Error Toast */
            }
        }
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

    private fun sanitizeExportBaseName(label: String): String =
        label.ifBlank { "note" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    /**
     * Strips data-URL prefix; rejects JSON (e.g. markdown sent to the PDF path by mistake).
     */
    private fun sanitizePdfBase64Payload(pdfBase64: String): String? {
        var t = pdfBase64.trim()
        if (t.isEmpty()) return null
        if (t.startsWith('{')) return null
        if (t.startsWith("data:", ignoreCase = true)) {
            val idx = t.indexOf("base64,")
            if (idx >= 0) {
                t = t.substring(idx + "base64,".length)
            }
        }
        return t.ifBlank { null }
    }
}
