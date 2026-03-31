package dev.subfly.yaba.core.navigation.alert

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalDeletionDialogManager

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YabaDeletionDialog(
    modifier: Modifier = Modifier,
) {
    val appStateManager = LocalAppStateManager.current
    val deletionManager = LocalDeletionDialogManager.current
    val deletionState by deletionManager.state.collectAsState()

    deletionState?.let { state ->
        AlertDialog(
            modifier = modifier,
            onDismissRequest = {
                appStateManager.onHideDeletionDialog()
                deletionManager.clear()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.onConfirm()
                        appStateManager.onHideDeletionDialog()
                        deletionManager.clear()
                    },
                    shapes = ButtonDefaults.shapes(),
                ) { Text(text = stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        state.onCancel()
                        appStateManager.onHideDeletionDialog()
                        deletionManager.clear()
                    },
                    shapes = ButtonDefaults.shapes(),
                ) { Text(text = stringResource(R.string.cancel)) }
            },
            icon = { YabaIcon(name = "delete-02") },
            title = {
                Text(
                    text = stringResource(
                        id = when (state.deletionType) {
                            DeletionType.TAG -> R.string.delete_tag_title
                            DeletionType.FOLDER -> R.string.delete_tag_title // TODO: ADD FOLDER TITLE :D
                            DeletionType.BOOKMARK -> R.string.delete_bookmark_title
                            DeletionType.BOOKMARKS -> R.string.delete_bookmark_title // TODO: ADD PLURAL :D
                            DeletionType.ANNOUNCEMENT -> R.string.announcements_delete_title
                        },
                    )
                )
            },
            text = {
                Text(
                    text = if (state.deletionType == DeletionType.BOOKMARKS) {
                        stringResource(R.string.bookmark_selection_delete_all_message)
                    } else {
                        stringResource(
                            id = R.string.delete_content_message,
                            when (state.deletionType) {
                                DeletionType.TAG -> state.tagToBeDeleted?.label ?: "-"
                                DeletionType.FOLDER -> state.folderToBeDeleted?.label ?: "-"
                                DeletionType.BOOKMARK -> state.bookmarkToBeDeleted?.label ?: "-"
                                DeletionType.ANNOUNCEMENT -> ""
                            }
                        )
                    }
                )
            }
        )
    }
}
