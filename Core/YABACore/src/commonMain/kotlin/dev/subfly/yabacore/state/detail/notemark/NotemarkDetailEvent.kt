package dev.subfly.yabacore.state.detail.notemark

import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.notifications.PlatformNotificationText

sealed interface NotemarkDetailEvent {
    data class OnInit(val bookmarkId: String) : NotemarkDetailEvent

    /**
     * Persist a snapshot of the editor document (JSON). The UI obtains this from
     * [dev.subfly.yabacore.webview.WebViewEditorBridge.getDocumentJson] on lifecycle pause / disposal.
     */
    data class OnSave(val documentJson: String) : NotemarkDetailEvent

    data object OnDeleteBookmark : NotemarkDetailEvent

    data class OnCreateHighlight(
        val readableVersionId: String,
        val startSectionKey: String,
        val startOffsetInSection: Int,
        val endSectionKey: String,
        val endOffsetInSection: Int,
        val colorRole: YabaColor,
        val note: String?,
        val quoteText: String?,
    ) : NotemarkDetailEvent

    data class OnUpdateHighlight(
        val highlightId: String,
        val colorRole: YabaColor,
        val note: String?,
    ) : NotemarkDetailEvent

    data class OnDeleteHighlight(val highlightId: String) : NotemarkDetailEvent

    data class OnScrollToHighlight(val highlightId: String) : NotemarkDetailEvent

    data object OnClearScrollToHighlight : NotemarkDetailEvent

    data object OnRequestNotificationPermission : NotemarkDetailEvent

    data class OnScheduleReminder(
        val title: PlatformNotificationText,
        val message: PlatformNotificationText,
        val triggerAtEpochMillis: Long,
    ) : NotemarkDetailEvent

    data object OnCancelReminder : NotemarkDetailEvent

    /** Opens the gallery picker and, on success, saves bytes under the note bookmark folder and sets [NotemarkDetailUIState.pendingInsertedImageSrc]. */
    data object OnPickImageFromGallery : NotemarkDetailEvent

    /** Opens the camera capture flow and, on success, saves bytes under the note bookmark folder and sets [NotemarkDetailUIState.pendingInsertedImageSrc]. */
    data object OnCaptureImageFromCamera : NotemarkDetailEvent

    /** Clears [NotemarkDetailUIState.pendingInsertedImageSrc] after the UI has inserted the image into the editor. */
    data object OnConsumedPendingInsertedImage : NotemarkDetailEvent
}
