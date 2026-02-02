package dev.subfly.yabacore.state.detail.linkmark

import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.unfurl.ReadableUnfurl

sealed interface LinkmarkDetailEvent {
    data class OnInit(val bookmarkId: String) : LinkmarkDetailEvent
    data class OnSaveReadableContent(val readable: ReadableUnfurl) : LinkmarkDetailEvent
    data object OnFetchReadableContent : LinkmarkDetailEvent
    data object OnDeleteBookmark : LinkmarkDetailEvent
    data class OnCreateHighlight(
        val contentVersion: Int,
        val startSectionKey: String,
        val startOffsetInSection: Int,
        val endSectionKey: String,
        val endOffsetInSection: Int,
        val colorRole: YabaColor = YabaColor.NONE,
        val note: String? = null,
    ) : LinkmarkDetailEvent
    data class OnUpdateHighlight(
        val highlightId: String,
        val colorRole: YabaColor,
        val note: String?,
    ) : LinkmarkDetailEvent
    data class OnDeleteHighlight(val highlightId: String) : LinkmarkDetailEvent
}
