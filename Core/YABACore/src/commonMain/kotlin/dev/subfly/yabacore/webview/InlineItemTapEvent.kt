package dev.subfly.yabacore.webview

import androidx.compose.runtime.Stable

/**
 * User tapped an inline rich item in the note editor.
 */
@Stable
data class InlineItemTapEvent(
    val documentPos: Int,
    val text: String,
)

/**
 * User tapped an inline link item in the note editor.
 */
@Stable
data class InlineLinkTapEvent(
    val documentPos: Int,
    val text: String,
    val url: String,
)

/**
 * User tapped an inline mention item in the note editor.
 */
@Stable
data class InlineMentionTapEvent(
    val documentPos: Int,
    val text: String,
    val bookmarkId: String,
    val bookmarkKindCode: Int,
    val bookmarkLabel: String,
)
