package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable

/**
 * A single readable content version with its assets and highlights.
 *
 * Content is stored as raw markdown; the UI parses it to render
 * (single Text + inline content for images, tables, code, etc.).
 */
@Stable
data class ReadableVersionUiModel(
    val contentVersion: Int,
    val createdAt: Long,
    val title: String?,
    val author: String?,
    val markdown: String? = null,
    val assets: List<ReadableAssetUiModel> = emptyList(),
    val highlights: List<HighlightUiModel> = emptyList(),
)
