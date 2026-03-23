package dev.subfly.yabacore.state.detail.notemark

import dev.subfly.yabacore.notifications.PlatformNotificationText

sealed interface NotemarkDetailEvent {
    data class OnInit(val bookmarkId: String) : NotemarkDetailEvent

    /**
     * Persist a snapshot of the editor document (JSON). The UI obtains this from
     * [dev.subfly.yabacore.webview.WebViewEditorBridge.getDocumentJson] on lifecycle pause /
     * disposal.
     */
    data class OnSave(val documentJson: String) : NotemarkDetailEvent

    data object OnDeleteBookmark : NotemarkDetailEvent

    data object OnRequestNotificationPermission : NotemarkDetailEvent

    data class OnScheduleReminder(
        val title: PlatformNotificationText,
        val message: PlatformNotificationText,
        val triggerAtEpochMillis: Long,
    ) : NotemarkDetailEvent

    data object OnCancelReminder : NotemarkDetailEvent

    /**
     * Opens the gallery picker; Core saves bytes and sets
     * [NotemarkDetailUIState.inlineImageDocumentSrc].
     */
    data object OnPickImageFromGallery : NotemarkDetailEvent

    /**
     * Opens the camera; Core saves bytes and sets [NotemarkDetailUIState.inlineImageDocumentSrc].
     */
    data object OnCaptureImageFromCamera : NotemarkDetailEvent

    /**
     * Clears [NotemarkDetailUIState.inlineImageDocumentSrc] after the bridge has inserted the
     * image.
     */
    data object OnConsumedInlineImageInsert : NotemarkDetailEvent
}
