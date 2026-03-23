package dev.subfly.yabacore.state.detail.linkmark

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.mappers.toPreviewUiModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.BookmarkWithRelations
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.common.computeTriggerMillisFromDatePicker
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.HighlightManager
import dev.subfly.yabacore.model.highlight.HighlightType
import dev.subfly.yabacore.managers.ReadableContentManager
import dev.subfly.yabacore.notifications.NotificationManager
import dev.subfly.yabacore.unfurl.ConverterResultProcessor
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.utils.ReaderFontSize
import dev.subfly.yabacore.model.utils.ReaderLineHeight
import dev.subfly.yabacore.model.utils.ReaderTheme
import dev.subfly.yabacore.state.base.BaseStateMachine
import dev.subfly.yabacore.unfurl.Unfurler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private data class LinkmarkDetailCombine(
    val bookmark: BookmarkPreviewUiModel?,
    val linkDetails: LinkBookmarkEntity?,
    val readableVersions: List<dev.subfly.yabacore.model.ui.ReadableVersionUiModel>,
    val selectedReadableVersionId: String?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LinkmarkDetailStateMachine :
    BaseStateMachine<LinkmarkDetailUIState, LinkmarkDetailEvent>(
        initialState = LinkmarkDetailUIState()
    ) {
    private var isInitialized = false
    private var dataSubscriptionJob: Job? = null
    private val bookmarkIdFlow = MutableStateFlow<String?>(null)
    private val selectedReadableVersionIdFlow = MutableStateFlow<String?>(null)

    override fun onEvent(event: LinkmarkDetailEvent) {
        when (event) {
            is LinkmarkDetailEvent.OnInit -> onInit(event.bookmarkId)
            is LinkmarkDetailEvent.OnSaveReadableContent -> onSaveReadableContent(event)
            LinkmarkDetailEvent.OnFetchReadableContent -> onFetchReadableContent()
            LinkmarkDetailEvent.OnUpdateReadableRequested -> onUpdateReadableRequested()
            is LinkmarkDetailEvent.OnConverterSucceeded -> onConverterSucceeded(event)
            is LinkmarkDetailEvent.OnConverterFailed -> onConverterFailed(event)
            is LinkmarkDetailEvent.OnSelectReadableVersion -> onSelectReadableVersion(event.versionId)
            is LinkmarkDetailEvent.OnDeleteReadableVersion -> onDeleteReadableVersion(event.versionId)
            LinkmarkDetailEvent.OnDeleteBookmark -> onDeleteBookmark()
            LinkmarkDetailEvent.OnToggleReaderTheme -> onToggleReaderTheme()
            LinkmarkDetailEvent.OnToggleReaderFontSize -> onToggleReaderFontSize()
            LinkmarkDetailEvent.OnToggleReaderLineHeight -> onToggleReaderLineHeight()
            is LinkmarkDetailEvent.OnSetReaderTheme -> onSetReaderTheme(event.theme)
            is LinkmarkDetailEvent.OnSetReaderFontSize -> onSetReaderFontSize(event.fontSize)
            is LinkmarkDetailEvent.OnSetReaderLineHeight -> onSetReaderLineHeight(event.lineHeight)
            is LinkmarkDetailEvent.OnCreateHighlight -> onCreateHighlight(event)
            is LinkmarkDetailEvent.OnUpdateHighlight -> onUpdateHighlight(event)
            is LinkmarkDetailEvent.OnDeleteHighlight -> onDeleteHighlight(event)
            is LinkmarkDetailEvent.OnHighlightReadableCreateCommitted -> onHighlightReadableCreateCommitted(event)
            is LinkmarkDetailEvent.OnHighlightReadableDeleteCommitted -> onHighlightReadableDeleteCommitted(event)
            is LinkmarkDetailEvent.OnScrollToHighlight -> onScrollToHighlight(event)
            LinkmarkDetailEvent.OnClearScrollToHighlight -> onClearScrollToHighlight()
            LinkmarkDetailEvent.OnRequestNotificationPermission -> onRequestNotificationPermission()
            is LinkmarkDetailEvent.OnScheduleReminder -> onScheduleReminder(event)
            LinkmarkDetailEvent.OnCancelReminder -> onCancelReminder()
        }
    }

    private fun onInit(bookmarkId: String) {
        if (isInitialized) return
        isInitialized = true
        bookmarkIdFlow.value = bookmarkId

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
                        .map { bookmarkWithRelations ->
                            bookmarkWithRelations?.toBookmarkPreviewUiModel()
                        }
                    val linkDetailsFlow = DatabaseProvider.linkBookmarkDao.observeByBookmarkId(id)
                    val readableVersionsFlow = ReadableContentManager.observeReadableVersions(id)

                    combine(
                        bookmarkFlow,
                        linkDetailsFlow,
                        readableVersionsFlow,
                        selectedReadableVersionIdFlow,
                    ) { bookmark, linkDetails, readableVersions, selectedId ->
                        LinkmarkDetailCombine(
                            bookmark = bookmark,
                            linkDetails = linkDetails,
                            readableVersions = readableVersions,
                            selectedReadableVersionId = selectedId,
                        )
                    }.flatMapLatest { combined ->
                        flow {
                            val selectedVersion = combined.readableVersions.find {
                                it.versionId == combined.selectedReadableVersionId
                            } ?: combined.readableVersions.firstOrNull()
                            val documentJson = selectedVersion?.body
                            val assetsBaseUrl = if (documentJson != null) {
                                val folderPath =
                                    BookmarkFileManager.getAbsolutePath(
                                        CoreConstants.FileSystem.bookmarkFolder(id),
                                    )
                                val base =
                                    if (folderPath.startsWith("file://")) folderPath else "file://$folderPath"
                                base.trimEnd('/') + "/"
                            } else null
                            emit(
                                currentState().copy(
                                    bookmark = combined.bookmark,
                                    linkDetails = combined.linkDetails?.toUiModel(),
                                    readableVersions = combined.readableVersions,
                                    selectedReadableVersionId = combined.selectedReadableVersionId,
                                    readableDocumentJson = documentJson,
                                    assetsBaseUrl = assetsBaseUrl,
                                    highlights = selectedVersion?.highlights ?: emptyList(),
                                    isLoading = false,
                                ),
                            )
                        }
                    }
                }
            }.collectLatest { newState ->
                updateState { newState }
            }
        }
    }

    private fun onSaveReadableContent(event: LinkmarkDetailEvent.OnSaveReadableContent) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch { ReadableContentManager.saveReadableContent(bookmarkId, event.readable) }
    }

    private fun onFetchReadableContent() {
        val linkUrl = currentState().linkDetails?.url ?: return
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch {
            runCatching { Unfurler.unfurlReadable(linkUrl) }
                .getOrNull()
                ?.let { readable -> ReadableContentManager.saveReadableContent(bookmarkId, readable) }
        }
    }

    private fun onUpdateReadableRequested() {
        val linkUrl = currentState().linkDetails?.url ?: return
        launch {
            updateState { it.copy(isUpdatingReadable = true, converterError = null) }
            runCatching { Unfurler.unfurl(linkUrl) }
                .getOrNull()
                ?.let { preview ->
                    updateState {
                        it.copy(
                            converterHtml = preview.rawHtml,
                            converterBaseUrl = preview.url,
                            isUpdatingReadable = false,
                        )
                    }
                }
                ?: run {
                    updateState {
                        it.copy(
                            isUpdatingReadable = false,
                            converterError = "Unable to fetch link content",
                        )
                    }
                }
        }
    }

    private fun onConverterSucceeded(event: LinkmarkDetailEvent.OnConverterSucceeded) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch {
            val readable = ConverterResultProcessor.process(
                documentJson = event.documentJson,
                assets = event.assets,
            )
            val versionId = ReadableContentManager.saveReadableContentInternal(bookmarkId, readable)
            selectedReadableVersionIdFlow.value = versionId
            updateState {
                it.copy(
                    converterHtml = null,
                    converterBaseUrl = null,
                    converterError = null,
                    isUpdatingReadable = false,
                )
            }
        }
    }

    private fun onConverterFailed(event: LinkmarkDetailEvent.OnConverterFailed) {
        updateState {
            it.copy(
                converterHtml = null,
                converterBaseUrl = null,
                converterError = event.error.message ?: "Conversion failed",
                isUpdatingReadable = false,
            )
        }
    }

    private fun onSelectReadableVersion(versionId: String) {
        selectedReadableVersionIdFlow.value = versionId
    }

    private fun onDeleteReadableVersion(versionId: String) {
        ReadableContentManager.deleteVersion(versionId)
        val currentSelected = selectedReadableVersionIdFlow.value
        if (currentSelected == versionId) {
            selectedReadableVersionIdFlow.value = null
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

    private fun onCreateHighlight(event: LinkmarkDetailEvent.OnCreateHighlight) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        HighlightManager.createHighlight(
            highlightId = event.highlightId,
            bookmarkId = bookmarkId,
            readableVersionId = event.readableVersionId,
            type = HighlightType.READABLE,
            colorRole = event.colorRole,
            note = event.note,
            quoteText = event.quoteText,
            extrasJson = null,
        )
    }

    private fun onUpdateHighlight(event: LinkmarkDetailEvent.OnUpdateHighlight) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        HighlightManager.updateHighlight(
            bookmarkId = bookmarkId,
            highlightId = event.highlightId,
            colorRole = event.colorRole,
            note = event.note,
        )
    }

    private fun onDeleteHighlight(event: LinkmarkDetailEvent.OnDeleteHighlight) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        HighlightManager.deleteHighlight(bookmarkId, event.highlightId)
    }

    private fun onHighlightReadableCreateCommitted(
        event: LinkmarkDetailEvent.OnHighlightReadableCreateCommitted,
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
                type = HighlightType.READABLE,
                colorRole = req.colorRole,
                note = req.note,
                quoteText = req.selectionDraft.quote.displayText.ifBlank { null },
                extrasJson = null,
            )
        }
    }

    private fun onHighlightReadableDeleteCommitted(
        event: LinkmarkDetailEvent.OnHighlightReadableDeleteCommitted,
    ) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch {
            val state = currentState()
            val versionId = state.selectedReadableVersionId
                ?: state.readableVersions.firstOrNull()?.versionId
                ?: return@launch
            ReadableContentManager.syncNotemarkReadableMirror(
                bookmarkId = bookmarkId,
                versionId = versionId,
                documentJson = event.documentJson,
            )
            HighlightManager.deleteHighlight(bookmarkId, event.highlightId)
        }
    }

    private fun onScrollToHighlight(event: LinkmarkDetailEvent.OnScrollToHighlight) {
        updateState { it.copy(scrollToHighlightId = event.highlightId) }
    }

    private fun onClearScrollToHighlight() {
        updateState { it.copy(scrollToHighlightId = null) }
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
        launch {
            NotificationManager.cancelReminder(bookmarkId)
        }
        updateState { it.copy(reminderDateEpochMillis = null) }
    }

    override fun clear() {
        isInitialized = false
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        bookmarkIdFlow.value = null
        selectedReadableVersionIdFlow.value = null
        super.clear()
    }

    private suspend fun BookmarkWithRelations.toBookmarkPreviewUiModel(): BookmarkPreviewUiModel {
        val folderUi = folder.toUiModel()
        val tagsUi = tags.map { it.toUiModel() }

        val localImageAbsolutePath = bookmark.localImagePath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }

        val localIconAbsolutePath = bookmark.localIconPath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
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
        )
}
