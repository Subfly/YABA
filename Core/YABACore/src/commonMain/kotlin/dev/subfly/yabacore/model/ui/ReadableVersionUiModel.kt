package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable

/**
 * A single readable content version with its assets and highlights.
 */
@Stable
data class ReadableVersionUiModel(
    val contentVersion: Int,
    val createdAt: Long,
    val title: String?,
    val author: String?,
    val document: ReadableDocumentUiModel?,
    val assets: List<ReadableAssetUiModel> = emptyList(),
    val highlights: List<HighlightUiModel> = emptyList(),
)
