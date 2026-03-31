package dev.subfly.yaba.core.model.ui

import androidx.compose.runtime.Stable

/**
 * A single readable content version with its assets and annotations.
 *
 * [body] is the raw file text: sanitized HTML for link readables, or document JSON for notemark mirrors.
 */
@Stable
data class ReadableVersionUiModel(
    val versionId: String,
    val createdAt: Long,
    val body: String? = null,
    val assets: List<ReadableAssetUiModel> = emptyList(),
    val annotations: List<AnnotationUiModel> = emptyList(),
)
