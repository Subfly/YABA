package dev.subfly.yaba.core.model.ui

import androidx.compose.runtime.Stable

/**
 * A single readable content version with its annotations.
 *
 * Embedded images and other assets are referenced from [body] (document JSON); paths resolve via the
 * bookmark folder base URL in the reader.
 *
 * [body] is the raw file text: sanitized HTML for link readables, or document JSON for notemark mirrors.
 */
@Stable
data class ReadableVersionUiModel(
    val versionId: String,
    val createdAt: Long,
    val body: String? = null,
    val annotations: List<AnnotationUiModel> = emptyList(),
)
