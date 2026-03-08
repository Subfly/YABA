package dev.subfly.yabacore.state.detail.imagemark

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.mappers.toPreviewUiModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.BookmarkWithRelations
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.access.YabaFileAccessor
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.ImagemarkManager
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.notifications.NotificationManager
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class ImagemarkDetailStateMachine : BaseStateMachine<ImagemarkDetailUIState, ImagemarkDetailEvent>(
    initialState = ImagemarkDetailUIState()
) {
    private var isInitialized = false
    private val bookmarkIdFlow = MutableStateFlow<String?>(null)

    override fun onEvent(event: ImagemarkDetailEvent) {
        when (event) {
            is ImagemarkDetailEvent.OnInit -> onInit(event.bookmarkId)
            ImagemarkDetailEvent.OnDeleteBookmark -> onDeleteBookmark()
            ImagemarkDetailEvent.OnShareImage -> onShareImage()
            ImagemarkDetailEvent.OnExportImage -> onExportImage()
            ImagemarkDetailEvent.OnRequestNotificationPermission -> {}
            is ImagemarkDetailEvent.OnScheduleReminder -> onScheduleReminder(event)
            ImagemarkDetailEvent.OnCancelReminder -> onCancelReminder()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun onInit(bookmarkId: String) {
        if (isInitialized) return
        isInitialized = true
        bookmarkIdFlow.value = bookmarkId

        launch {
            val reminderDate = NotificationManager.getPendingReminderDate(bookmarkId)
            updateState { it.copy(reminderDateEpochMillis = reminderDate) }
        }

        launch {
            bookmarkIdFlow.flatMapLatest { id ->
                if (id == null) {
                    MutableStateFlow(ImagemarkDetailUIState())
                } else {
                    updateState { it.copy(isLoading = true) }
                    DatabaseProvider.bookmarkDao
                        .observeByIdWithRelations(id)
                        .flatMapLatest { withRelations ->
                            flow {
                                val bookmark = withRelations?.toBookmarkPreviewUiModel()
                                val imagePath = ImagemarkManager.resolveImageAbsolutePath(id)
                                emit(
                                    ImagemarkDetailUIState(
                                        bookmark = bookmark,
                                        imageAbsolutePath = imagePath,
                                        isLoading = false,
                                    )
                                )
                            }
                        }
                }
            }.collectLatest { newState ->
                updateState {
                    it.copy(
                        bookmark = newState.bookmark,
                        imageAbsolutePath = newState.imageAbsolutePath,
                        isLoading = newState.isLoading,
                    )
                }
            }
        }
    }

    private fun onDeleteBookmark() {
        val id = bookmarkIdFlow.value ?: return
        AllBookmarksManager.deleteBookmarks(listOf(id))
    }

    private fun onShareImage() {
        launch {
            val id = bookmarkIdFlow.value ?: return@launch
            YabaFileAccessor.shareImageBookmark(id)
        }
    }

    private fun onExportImage() {
        launch {
            val id = bookmarkIdFlow.value ?: return@launch
            val bookmark = currentState().bookmark ?: return@launch
            val name = bookmark.label.ifBlank { "image" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val ext = bookmark.localImagePath?.substringAfterLast('.') ?: "jpeg"
            YabaFileAccessor.exportImageBookmark(id, name, ext)
        }
    }

    private fun onScheduleReminder(event: ImagemarkDetailEvent.OnScheduleReminder) {
        val id = bookmarkIdFlow.value ?: return
        val bookmark = currentState().bookmark ?: return
        launch {
            NotificationManager.cancelReminder(id)
            NotificationManager.scheduleReminder(
                bookmarkId = id,
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
        val id = bookmarkIdFlow.value ?: return
        launch {
            NotificationManager.cancelReminder(id)
            updateState { it.copy(reminderDateEpochMillis = null) }
        }
    }

    override fun clear() {
        isInitialized = false
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
