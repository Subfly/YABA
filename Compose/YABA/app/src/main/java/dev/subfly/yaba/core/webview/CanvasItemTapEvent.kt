package dev.subfly.yaba.core.webview

import androidx.compose.runtime.Stable

@Stable
data class CanvasLinkTapEvent(
    val elementId: String,
    val text: String,
    val url: String,
)

@Stable
data class CanvasMentionTapEvent(
    val elementId: String,
    val text: String,
    val bookmarkId: String,
    val bookmarkKindCode: Int,
    val bookmarkLabel: String,
)
