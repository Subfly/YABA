package dev.subfly.yaba.core.state.detail.canvmark

import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.database.mappers.toPreviewUiModel
import dev.subfly.yaba.core.database.mappers.toUiModel
import dev.subfly.yaba.core.database.models.BookmarkWithRelations
import dev.subfly.yaba.core.filesystem.BookmarkFileManager
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.managers.CanvmarkManager
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.state.base.BaseStateMachine
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
class CanvmarkDetailStateMachine :
    BaseStateMachine<CanvmarkDetailUIState, CanvmarkDetailEvent>(
        initialState = CanvmarkDetailUIState(),
    ) {
    private var isInitialized = false
    private var didBootstrap = false
    /** Web shell reported [WebShellLoadResult.Loaded] (bridge ready + canvas host loaded). */
    private var webShellLoadedOk = false
    private var dataSubscriptionJob: Job? = null
    private val bookmarkIdFlow = MutableStateFlow<String?>(null)
    private var lastPersistedSceneJson: String? = null

    override fun onEvent(event: CanvmarkDetailEvent) {
        when (event) {
            is CanvmarkDetailEvent.OnInit -> onInit(event.bookmarkId)
            is CanvmarkDetailEvent.OnSave -> onSave(event.sceneJson)
            is CanvmarkDetailEvent.OnWebInitialContentLoad -> onWebInitialContentLoad(event)
            is CanvmarkDetailEvent.OnCanvasMetricsChanged -> onCanvasMetricsChanged(event)
            CanvmarkDetailEvent.OnPickImageFromGallery -> onPickImageFromGallery()
            CanvmarkDetailEvent.OnCaptureImageFromCamera -> onCaptureImageFromCamera()
            CanvmarkDetailEvent.OnConsumedPendingImageInsert -> onConsumedPendingImageInsert()
            CanvmarkDetailEvent.OnDeleteBookmark -> onDeleteBookmark()
        }
    }

    private fun onInit(bookmarkId: String) {
        if (isInitialized) return
        isInitialized = true
        didBootstrap = false
        webShellLoadedOk = false
        bookmarkIdFlow.value = bookmarkId
        AllBookmarksManager.recordBookmarkView(bookmarkId)

        dataSubscriptionJob?.cancel()
        dataSubscriptionJob = launch {
            bookmarkIdFlow
                .flatMapLatest { id ->
                    if (id == null) {
                        MutableStateFlow(CanvmarkDetailUIState())
                    } else {
                        updateState { it.copy(isLoading = true) }
                        val bookmarkFlow = DatabaseProvider.bookmarkDao.observeByIdWithRelations(id)
                        val canvasFlow = DatabaseProvider.canvasBookmarkDao.observeByBookmarkId(id)

                        combine(bookmarkFlow, canvasFlow) { bookmark, canvas ->
                            flow {
                                val bookmarkModel = bookmark?.toBookmarkPreviewUiModel()
                                when {
                                    canvas != null && !didBootstrap -> {
                                        didBootstrap = true
                                        val sceneJson =
                                            CanvmarkManager.readCanvasSceneJson(id).orEmpty()
                                        lastPersistedSceneJson = sceneJson
                                        emit(
                                            currentState().copy(
                                                bookmark = bookmarkModel,
                                                initialSceneJson = sceneJson,
                                                isLoading = !webShellLoadedOk,
                                                webContentLoadFailed = false,
                                            ),
                                        )
                                    }
                                    !didBootstrap -> {
                                        emit(
                                            currentState().copy(
                                                bookmark = bookmarkModel,
                                                isLoading = true,
                                            ),
                                        )
                                    }
                                    else -> {
                                        emit(
                                            currentState().copy(
                                                bookmark = bookmarkModel,
                                                isLoading = !webShellLoadedOk,
                                            ),
                                        )
                                    }
                                }
                            }
                        }.flatMapLatest { it }
                    }
                }
                .collectLatest { emitted ->
                    updateState { current ->
                        emitted.copy(
                            pendingImageDataUrl = current.pendingImageDataUrl,
                            metrics = current.metrics,
                        )
                    }
                }
        }
    }

    private fun onSave(sceneJson: String) {
        val bookmarkId = bookmarkIdFlow.value ?: return
        if (sceneJson == lastPersistedSceneJson) return
        launch {
            val result = CanvmarkManager.persistCanvasSceneJsonAwait(bookmarkId, sceneJson)
            if (result.isSuccess) {
                lastPersistedSceneJson = sceneJson
            }
        }
    }

    private fun onWebInitialContentLoad(event: CanvmarkDetailEvent.OnWebInitialContentLoad) {
        webShellLoadedOk = event.result == WebShellLoadResult.Loaded
        updateState {
            val failed = event.result == WebShellLoadResult.Error
            it.copy(
                isLoading = !failed && !(didBootstrap && webShellLoadedOk),
                webContentLoadFailed = failed,
            )
        }
    }

    private fun onCanvasMetricsChanged(event: CanvmarkDetailEvent.OnCanvasMetricsChanged) {
        updateState { it.copy(metrics = event.metrics) }
    }

    private fun onPickImageFromGallery() {
        launch {
            try {
                val file = YabaFileAccessor.pickSingleImage() ?: return@launch
                val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                val ext = file.name.substringAfterLast('.', "").ifBlank { "png" }
                updateState {
                    it.copy(
                        pendingImageDataUrl =
                            toDataUrl(bytes = bytes, extension = ext),
                    )
                }
            } catch (_: Exception) {
                // Picker dismissed or read failed.
            }
        }
    }

    private fun onCaptureImageFromCamera() {
        launch {
            try {
                val file = YabaFileAccessor.capturePhoto() ?: return@launch
                val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                val ext = file.name.substringAfterLast('.', "").ifBlank { "jpeg" }
                updateState {
                    it.copy(
                        pendingImageDataUrl =
                            toDataUrl(bytes = bytes, extension = ext),
                    )
                }
            } catch (_: Exception) {
                // Camera flow failed or cancelled.
            }
        }
    }

    private fun onConsumedPendingImageInsert() {
        updateState { it.copy(pendingImageDataUrl = null) }
    }

    private fun onDeleteBookmark() {
        val bookmarkId = bookmarkIdFlow.value ?: return
        AllBookmarksManager.deleteBookmarks(listOf(bookmarkId))
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun toDataUrl(bytes: ByteArray, extension: String): String {
        val normalized = extension.lowercase().removePrefix(".")
        val mime = when (normalized) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/png"
        }
        val base64 = Base64.encode(bytes)
        return "data:$mime;base64,$base64"
    }

    override fun clear() {
        isInitialized = false
        didBootstrap = false
        webShellLoadedOk = false
        lastPersistedSceneJson = null
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
