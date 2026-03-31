package dev.subfly.yaba.core.webview

import androidx.compose.runtime.Stable

/**
 * Table of contents tree emitted from WebView shells (TipTap, PDF.js, epub.js).
 * [extrasJson] is opaque to Kotlin; pass it back to the WebView bridge for navigation.
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
