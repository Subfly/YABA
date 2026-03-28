package dev.subfly.yabacore.webview

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Table of contents tree emitted from WebView shells (TipTap, PDF.js, epub.js).
 * [extrasJson] is opaque to Kotlin; pass it back to [WebViewReaderBridge.navigateToTocItem] for navigation.
 */
@Stable
data class Toc(
    val items: List<TocItem> = emptyList(),
)

@Stable
data class TocItem(
    val id: String,
    val title: String,
    val level: Int,
    val children: List<TocItem> = emptyList(),
    val extrasJson: String? = null,
)
