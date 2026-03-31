package dev.subfly.yaba.core.state.creation.folder

import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.utils.YabaColor

data class FolderCreationUIState(
    val label: String = "",
    val description: String = "",
    val selectedColor: YabaColor = YabaColor.BLUE,
    val selectedIcon: String = "folder-01",
    val selectedParent: FolderUiModel? = null,
    val editingFolder: FolderUiModel? = null,
    val isSaving: Boolean = false,
)
