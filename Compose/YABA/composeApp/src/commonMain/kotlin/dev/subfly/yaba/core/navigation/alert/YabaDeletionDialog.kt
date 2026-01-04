package dev.subfly.yaba.core.navigation.alert

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.announcements_delete_title
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.delete
import yaba.composeapp.generated.resources.delete_bookmark_title
import yaba.composeapp.generated.resources.delete_content_message
import yaba.composeapp.generated.resources.delete_tag_title

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
                ) { Text(text = stringResource(Res.string.delete)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        state.onCancel()
                        appStateManager.onHideDeletionDialog()
                        deletionManager.clear()
                    },
                    shapes = ButtonDefaults.shapes(),
                ) { Text(text = stringResource(Res.string.cancel)) }
            },
            icon = { YabaIcon(name = "delete-02") },
            title = {
                Text(
                    text = stringResource(
                        resource = when (state.deletionType) {
                            DeletionType.TAG -> Res.string.delete_tag_title
                            DeletionType.FOLDER -> Res.string.delete_tag_title // TODO: ADD FOLDER TITLE :D
                            DeletionType.BOOKMARK -> Res.string.delete_bookmark_title
                            DeletionType.ANNOUNCEMENT -> Res.string.announcements_delete_title
                        },
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(
                        resource = Res.string.delete_content_message,
                        when (state.deletionType) {
                            DeletionType.TAG -> state.tagToBeDeleted?.label ?: "-"
                            DeletionType.FOLDER -> state.folderToBeDeleted?.label ?: "-"
                            DeletionType.BOOKMARK -> state.bookmarkToBeDeleted?.label ?: "-"
                            DeletionType.ANNOUNCEMENT -> ""
                        }
                    )
                )
            }
        )
    }
}
