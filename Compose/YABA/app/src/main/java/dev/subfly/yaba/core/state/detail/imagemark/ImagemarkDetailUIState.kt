package dev.subfly.yaba.core.state.detail.imagemark

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel

@Immutable
data class ImagemarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    val imageAbsolutePath: String? = null,
    val isLoading: Boolean = false,
    val reminderDateEpochMillis: Long? = null,
)
