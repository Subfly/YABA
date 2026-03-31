package dev.subfly.yaba.core.navigation.alert

import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel

data class DeletionState(
    val deletionType: DeletionType,
    val tagToBeDeleted: TagUiModel? = null,
    val folderToBeDeleted: FolderUiModel? = null,
    val bookmarkToBeDeleted: BookmarkUiModel? = null,
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {},
)
