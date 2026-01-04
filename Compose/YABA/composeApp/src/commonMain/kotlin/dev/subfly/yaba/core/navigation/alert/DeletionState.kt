package dev.subfly.yaba.core.navigation.alert

import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel

data class DeletionState(
    val deletionType: DeletionType,
    val tagToBeDeleted: TagUiModel? = null,
    val folderToBeDeleted: FolderUiModel? = null,
    val bookmarkToBeDeleted: BookmarkUiModel? = null,
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {},
)
