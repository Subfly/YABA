package dev.subfly.yaba.core.model.ui

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.model.utils.YabaColor

@Immutable
data class HomeFolderRowUiModel(
    val folder: FolderUiModel,
    val parentColors: List<YabaColor> = emptyList(),
    val isExpanded: Boolean = false,
    val hasChildren: Boolean = false,
)
