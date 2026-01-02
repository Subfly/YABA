package dev.subfly.yabacore.state.folder

import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.YabaColor

data class FolderCreationUIState(
    val label: String = "",
    val description: String = "",
    val selectedColor: YabaColor = YabaColor.BLUE,
    val selectedIcon: String = "folder-01",
    val selectedParent: FolderUiModel? = null,
    val editingFolder: FolderUiModel? = null,
)
