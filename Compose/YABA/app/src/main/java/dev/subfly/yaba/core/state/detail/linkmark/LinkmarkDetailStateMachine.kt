package dev.subfly.yaba.core.state.detail.linkmark

import dev.subfly.yaba.core.common.CoreConstants
import dev.subfly.yaba.core.common.computeTriggerMillisFromDatePicker
import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.database.entities.LinkBookmarkEntity
import dev.subfly.yaba.core.database.mappers.toPreviewUiModel
import dev.subfly.yaba.core.database.mappers.toUiModel
import dev.subfly.yaba.core.database.models.BookmarkWithRelations
import dev.subfly.yaba.core.filesystem.BookmarkFileManager
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.managers.AnnotationManager
import dev.subfly.yaba.core.managers.ReadableContentManager
import dev.subfly.yaba.core.model.annotation.AnnotationType
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.model.utils.ReaderFontSize
import dev.subfly.yaba.core.model.utils.ReaderLineHeight
import dev.subfly.yaba.core.model.utils.ReaderTheme
import dev.subfly.yaba.core.notifications.NotificationManager
import dev.subfly.yaba.core.state.base.BaseStateMachine
import dev.subfly.yaba.core.webview.Toc
import dev.subfly.yaba.core.webview.WebShellLoadResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalCoroutinesApi::class)
class LinkmarkDetailStateMachine :
    BaseStateMachine<LinkmarkDetailUIState, LinkmarkDetailEvent>(
        initialState = LinkmarkDetailUIState()
    ) {
    private var isInitialized = false
    private var initialReaderLoadSettled = false
    private var dataSubscriptionJob: Job? = null
    private val bookmarkIdFlow = MutableStateFlow<String?>(null)

    override fun onEvent(event: LinkmarkDetailEvent) {
        when (event) {
            is LinkmarkDetailEvent.OnInit -> onInit(event.bookmarkId)
            is LinkmarkDetailEvent.OnReaderWebInitialContentLoad -> onReaderWebInitialContentLoad(event)
            LinkmarkDetailEvent.OnDeleteBookmark -> onDeleteBookmark()
            LinkmarkDetailEvent.OnToggleReaderTheme -> onToggleReaderTheme()
            LinkmarkDetailEvent.OnToggleReaderFontSize -> onToggleReaderFontSize()
            LinkmarkDetailEvent.OnToggleReaderLineHeight -> onToggleReaderLineHeight()
            is LinkmarkDetailEvent.OnSetReaderTheme -> onSetReaderTheme(event.theme)
            is LinkmarkDetailEvent.OnSetReaderFontSize -> onSetReaderFontSize(event.fontSize)
            is LinkmarkDetailEvent.OnSetReaderLineHeight -> onSetReaderLineHeight(event.lineHeight)
            is LinkmarkDetailEvent.OnCreateAnnotation -> onCreateAnnotation(event)
            is LinkmarkDetailEvent.OnUpdateAnnotation -> onUpdateAnnotation(event)
            is LinkmarkDetailEvent.OnDeleteAnnotation -> onDeleteAnnotation(event)
            is LinkmarkDetailEvent.OnAnnotationReadableCreateCommitted -> onAnnotationReadableCreateCommitted(event)
            is LinkmarkDetailEvent.OnAnnotationReadableDeleteCommitted -> onAnnotationReadableDeleteCommitted(event)
            is LinkmarkDetailEvent.OnScrollToAnnotation -> onScrollToAnnotation(event)
            LinkmarkDetailEvent.OnClearScrollToAnnotation -> onClearScrollToAnnotation()
            is LinkmarkDetailEvent.OnTocChanged -> onTocChanged(event.toc)
            is LinkmarkDetailEvent.OnNavigateToTocItem -> onNavigateToTocItem(event)
            LinkmarkDetailEvent.OnClearTocNavigation -> onClearTocNavigation()
            LinkmarkDetailEvent.OnRequestNotificationPermission -> onRequestNotificationPermission()
            is LinkmarkDetailEvent.OnScheduleReminder -> onScheduleReminder(event)
            LinkmarkDetailEvent.OnCancelReminder -> onCancelReminder()
            is LinkmarkDetailEvent.OnExportMarkdownReady -> onExportMarkdownReady(event)
            is LinkmarkDetailEvent.OnExportPdfReady -> onExportPdfReady(event)
        }
    }

    private fun onInit(bookmarkId: String) {
        if (isInitialized) return
        isInitialized = true
        initialReaderLoadSettled = false
        bookmarkIdFlow.value = bookmarkId
        AllBookmarksManager.recordBookmarkView(bookmarkId)

        launch {
            val reminderDate = NotificationManager.getPendingReminderDate(bookmarkId)
            updateState {
                it.copy(reminderDateEpochMillis = reminderDate)
            }
        }

        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = launch {
            bookmarkIdFlow.flatMapLatest { id ->
                if (id == null) {
                    MutableStateFlow(LinkmarkDetailUIState())
                } else {
                    updateState { it.copy(isLoading = true) }

                    val bookmarkFlow = DatabaseProvider.bookmarkDao.observeByIdWithRelations(id)
                        .map { bwr -> bwr?.toBookmarkPreviewUiModel() }
                    val linkDetailsFlow = DatabaseProvider.linkBookmarkDao.observeByBookmarkId(id)
                    val readableFlow = ReadableContentManager.observeLinkReadable(id)

                    combine(
                        bookmarkFlow,
                        linkDetailsFlow,
                        readableFlow,
                    ) { bookmark, linkDetails, readableView ->
                        flow {
                            val documentJson = readableView?.body
                            val assetsBaseUrl = if (documentJson != null) {
                                val folderPath =
                                    BookmarkFileManager.getAbsolutePath(
                                        CoreConstants.FileSystem.bookmarkFolder(id),
                                    )
                                val base =
                                    if (folderPath.startsWith("file://")) folderPath else "file://$folderPath"
                                base.trimEnd('/') + "/"
                            } else {
                                null
                            }
                            emit(
                                currentState().copy(
                                    bookmark = bookmark,
                                    linkDetails = linkDetails?.toUiModel(),
                                    readableDocumentJson = documentJson,
                                    assetsBaseUrl = assetsBaseUrl,
                                    annotations = readableView?.annotations ?: emptyList(),
                                    isLoading =
                                        documentJson.isNullOrBlank().not() &&
                                        initialReaderLoadSettled.not(),
                                    readerWebContentLoadFailed = false,
                                ),
                            )
                        }
                    }.flatMapLatest { it }
                }
            }.collectLatest { newState ->
                updateState { current ->
                    val sameReadableBody =
                        current.readableDocumentJson == newState.readableDocumentJson
                    if (sameReadableBody && !current.isLoading) {
                        newState.copy(
                            isLoading = false,
                            readerWebContentLoadFailed = current.readerWebContentLoadFailed,
                        )
                    } else {
                        newState
                    }
                }
            }
        }
    }

    private fun onReaderWebInitialContentLoad(event: LinkmarkDetailEvent.OnReaderWebInitialContentLoad) {
        initialReaderLoadSettled = true
        updateState {
            it.copy(
                isLoading = false,
                readerWebContentLoadFailed = event.result == WebShellLoadResult.Error,
            )
        }
    }

    private fun onDeleteBookmark() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch { AllBookmarksManager.deleteBookmarks(listOf(bookmarkId)) }
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

    private fun onCreateAnnotation(event: LinkmarkDetailEvent.OnCreateAnnotation) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        AnnotationManager.createAnnotation(
            annotationId = event.annotationId,
            bookmarkId = bookmarkId,
            type = AnnotationType.READABLE,
            colorRole = event.colorRole,
            note = event.note,
            quoteText = event.quoteText,
            extrasJson = null,
        )
    }

    private fun onUpdateAnnotation(event: LinkmarkDetailEvent.OnUpdateAnnotation) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        AnnotationManager.updateAnnotation(
            bookmarkId = bookmarkId,
            annotationId = event.annotationId,
            colorRole = event.colorRole,
            note = event.note,
        )
    }

    private fun onDeleteAnnotation(event: LinkmarkDetailEvent.OnDeleteAnnotation) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        AnnotationManager.deleteAnnotation(bookmarkId, event.annotationId)
    }

    private fun onAnnotationReadableCreateCommitted(
        event: LinkmarkDetailEvent.OnAnnotationReadableCreateCommitted,
    ) {
        val activeBookmarkId = bookmarkIdFlow.value ?: return
        val req = event.request
        if (req.selectionDraft.bookmarkId != activeBookmarkId) return
        launch {
            ReadableContentManager.syncReadableDocumentMirror(
                bookmarkId = req.selectionDraft.bookmarkId,
                documentJson = event.documentJson,
            )
            AnnotationManager.createAnnotation(
                annotationId = event.annotationId,
                bookmarkId = req.selectionDraft.bookmarkId,
                type = AnnotationType.READABLE,
                colorRole = req.colorRole,
                note = req.note,
                quoteText = req.selectionDraft.quote.displayText.ifBlank { null },
                extrasJson = null,
            )
        }
    }

    private fun onAnnotationReadableDeleteCommitted(
        event: LinkmarkDetailEvent.OnAnnotationReadableDeleteCommitted,
    ) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch {
            ReadableContentManager.syncReadableDocumentMirror(
                bookmarkId = bookmarkId,
                documentJson = event.documentJson,
            )
            AnnotationManager.deleteAnnotation(bookmarkId, event.annotationId)
        }
    }

    private fun onScrollToAnnotation(event: LinkmarkDetailEvent.OnScrollToAnnotation) {
        updateState { it.copy(scrollToAnnotationId = event.annotationId) }
    }

    private fun onClearScrollToAnnotation() {
        updateState { it.copy(scrollToAnnotationId = null) }
    }

    private fun onTocChanged(toc: Toc?) {
        updateState { it.copy(toc = toc) }
    }

    private fun onNavigateToTocItem(event: LinkmarkDetailEvent.OnNavigateToTocItem) {
        updateState { it.copy(pendingTocNavigate = event.id to event.extrasJson) }
    }

    private fun onClearTocNavigation() {
        updateState { it.copy(pendingTocNavigate = null) }
    }

    private fun onRequestNotificationPermission() {
        launch {
            val granted = NotificationManager.requestPermission()
            updateState { it.copy(hasNotificationPermission = granted) }
        }
    }

    private fun onScheduleReminder(event: LinkmarkDetailEvent.OnScheduleReminder) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        val bookmark = currentState().bookmark ?: return
        val triggerMillis = computeTriggerMillisFromDatePicker(
            selectedDateMillis = event.selectedDateMillis,
            hour = event.hour,
            minute = event.minute,
        )
        launch {
            NotificationManager.cancelReminder(bookmarkId)
            NotificationManager.scheduleReminder(
                bookmarkId = bookmarkId,
                bookmarkKindCode = bookmark.kind.code,
                title = event.title,
                message = event.message,
                bookmarkLabel = bookmark.label,
                triggerDateEpochMillis = triggerMillis,
            )
            updateState { it.copy(reminderDateEpochMillis = triggerMillis) }
        }
    }

    private fun onCancelReminder() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch { NotificationManager.cancelReminder(bookmarkId) }
        updateState { it.copy(reminderDateEpochMillis = null) }
    }

    private fun onExportMarkdownReady(event: LinkmarkDetailEvent.OnExportMarkdownReady) {
        launch {
            if (event.markdown.isBlank()) return@launch
            val bookmarkId = bookmarkIdFlow.value ?: return@launch
            val label = currentState().bookmark?.label.orEmpty()
            val base = sanitizeExportBaseName(label)
            runCatching {
                YabaFileAccessor.exportNotemarkMarkdownBundle(
                    markdown = event.markdown,
                    bookmarkId = bookmarkId,
                    suggestedMarkdownBaseName = base,
                )
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun onExportPdfReady(event: LinkmarkDetailEvent.OnExportPdfReady) {
        launch {
            if (event.pdfBase64.isBlank()) return@launch
            val payload = sanitizePdfBase64Payload(event.pdfBase64) ?: return@launch
            val bytes = runCatching { Base64.decode(payload) }.getOrNull() ?: return@launch
            if (bytes.isEmpty()) return@launch
            val label = currentState().bookmark?.label.orEmpty()
            val base = sanitizeExportBaseName(label)
            runCatching {
                YabaFileAccessor.saveFileCopy(
                    bytes = bytes,
                    suggestedName = base,
                    extension = "pdf",
                )
            }
        }
    }

    private fun sanitizeExportBaseName(label: String): String =
        label.ifBlank { "note" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")

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

    override fun clear() {
        isInitialized = false
        initialReaderLoadSettled = false
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        bookmarkIdFlow.value = null
        super.clear()
    }

    private fun BookmarkWithRelations.toBookmarkPreviewUiModel(): BookmarkPreviewUiModel {
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

    private fun LinkBookmarkEntity.toUiModel(): LinkmarkLinkDetailsUiModel =
        LinkmarkLinkDetailsUiModel(
            url = url,
            domain = domain,
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            metadataTitle = metadataTitle,
            metadataDescription = metadataDescription,
            metadataAuthor = metadataAuthor,
            metadataDate = metadataDate,
        )
}
