package dev.subfly.yabacore.state.detail.linkmark

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.mappers.toPreviewUiModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.BookmarkWithRelations
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.HighlightManager
import dev.subfly.yabacore.managers.LinkmarkManager
import dev.subfly.yabacore.managers.ReadableContentManager
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.state.base.BaseStateMachine
import dev.subfly.yabacore.unfurl.Unfurler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class LinkmarkDetailStateMachine :
    BaseStateMachine<LinkmarkDetailUIState, LinkmarkDetailEvent>(
        initialState = LinkmarkDetailUIState()
    ) {
    private var isInitialized = false
    private var dataSubscriptionJob: Job? = null
    private val bookmarkIdFlow = MutableStateFlow<String?>(null)

    override fun onEvent(event: LinkmarkDetailEvent) {
        when (event) {
            is LinkmarkDetailEvent.OnInit -> onInit(event.bookmarkId)
            is LinkmarkDetailEvent.OnSaveReadableContent -> onSaveReadableContent(event)
            LinkmarkDetailEvent.OnFetchReadableContent -> onFetchReadableContent()
            LinkmarkDetailEvent.OnDeleteBookmark -> onDeleteBookmark()
            is LinkmarkDetailEvent.OnCreateHighlight -> onCreateHighlight(event)
            is LinkmarkDetailEvent.OnUpdateHighlight -> onUpdateHighlight(event)
            is LinkmarkDetailEvent.OnDeleteHighlight -> onDeleteHighlight(event)
        }
    }

    private fun onInit(bookmarkId: String) {
        if (isInitialized) return
        isInitialized = true
        bookmarkIdFlow.value = bookmarkId

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
                    val highlightsFlow = LinkmarkManager.observeHighlights(id)

                    combine(
                        bookmarkFlow,
                        linkDetailsFlow,
                        readableVersionsFlow,
                        highlightsFlow,
                    ) { bookmark, linkDetails, readableVersions, highlights ->
                        currentState().copy(
                            bookmark = bookmark,
                            linkDetails = linkDetails?.toUiModel(),
                            readableVersions = readableVersions,
                            highlights = highlights,
                            isLoading = false,
                        )
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

    private fun onDeleteBookmark() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        launch { AllBookmarksManager.deleteBookmarks(listOf(bookmarkId)) }
    }

    private fun onCreateHighlight(event: LinkmarkDetailEvent.OnCreateHighlight) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        HighlightManager.createHighlight(
            bookmarkId = bookmarkId,
            contentVersion = event.contentVersion,
            startOffset = event.startOffset,
            endOffset = event.endOffset,
            colorRole = event.colorRole,
            note = event.note,
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

    override fun clear() {
        isInitialized = false
        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = null
        bookmarkIdFlow.value = null
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
            linkType = linkType,
            videoUrl = videoUrl,
        )
}
