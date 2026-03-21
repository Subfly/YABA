package dev.subfly.yabacore.state.detail.notemark

import dev.subfly.yabacore.model.utils.NoteSaveMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.notifications.PlatformNotificationText

sealed interface NotemarkDetailEvent {
    data class OnInit(val bookmarkId: String) : NotemarkDetailEvent

    data class OnNoteSaveModeChanged(val mode: NoteSaveMode) : NotemarkDetailEvent

    /** Editor content changed (markdown). */
    data class OnEditorMarkdownChanged(val markdown: String) : NotemarkDetailEvent

    /** User explicitly saves (manual mode or toolbar). */
    data object OnManualSave : NotemarkDetailEvent

    /** Persist immediately if there are unsaved changes (e.g. leaving the screen). */
    data object OnFlushPendingSave : NotemarkDetailEvent

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
}
