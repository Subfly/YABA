package dev.subfly.yaba.ui.detail.bookmark.note.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailEvent
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.delete
import yaba.composeapp.generated.resources.edit
import yaba.composeapp.generated.resources.move
import yaba.composeapp.generated.resources.remind_me

private data class DetailMenuAction(
    val key: String,
    val icon: String,
    val text: String,
    val color: YabaColor,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NotemarkContentDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    state: NotemarkDetailUIState,
    onEvent: (NotemarkDetailEvent) -> Unit,
    onShowRemindMePicker: () -> Unit,
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val deletionDialogManager = LocalDeletionDialogManager.current

    val editText = stringResource(Res.string.edit)
    val moveText = stringResource(Res.string.move)
    val remindMeText = stringResource(Res.string.remind_me)
    // TODO: LOCALIZATION
    val cancelReminderText = "Cancel Reminder"
    val deleteText = stringResource(Res.string.delete)

    val isAndroid = Platform == YabaPlatform.ANDROID
    val hasActiveReminder = state.reminderDateEpochMillis != null

    val primaryActions = remember(editText, moveText) {
        listOf(
            DetailMenuAction(
                key = "edit",
                icon = "edit-02",
                text = editText,
                color = YabaColor.ORANGE,
            ),
            DetailMenuAction(
                key = "move",
                icon = "arrow-move-up-right",
                text = moveText,
                color = YabaColor.BLUE,
            ),
        )
    }

    val secondaryActions = remember(remindMeText, cancelReminderText, hasActiveReminder) {
        buildList {
            add(
                DetailMenuAction(
                    key = "remind_me",
                    icon = "notification-02",
                    text = remindMeText,
                    color = YabaColor.PURPLE,
                ),
            )
            if (hasActiveReminder) {
                add(
                    DetailMenuAction(
                        key = "cancel_reminder",
                        icon = "notification-off-02",
                        text = cancelReminderText,
                        color = YabaColor.RED,
                    ),
                )
            }
        }
    }

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = 3),
        ) {
            primaryActions.fastForEachIndexed { index, action ->
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(index, primaryActions.size),
                    checked = false,
                    onCheckedChange = {
                        onDismissRequest()
                        when (action.key) {
                            "edit" -> {
                                val bookmark = state.bookmark ?: return@DropdownMenuItem
                                creationNavigator.add(NotemarkCreationRoute(bookmarkId = bookmark.id))
                                appStateManager.onShowCreationContent()
                            }
                            "move" -> {
                                val bookmark = state.bookmark ?: return@DropdownMenuItem
                                creationNavigator.add(
                                    FolderSelectionRoute(
                                        mode = FolderSelectionMode.BOOKMARKS_MOVE,
                                        contextFolderId = bookmark.folderId,
                                        contextBookmarkIds = listOf(bookmark.id),
                                    ),
                                )
                                appStateManager.onShowCreationContent()
                            }
                        }
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = action.icon,
                            color = Color(action.color.iconTintArgb()),
                        )
                    },
                    text = { Text(text = action.text) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 1, count = 3),
        ) {
            secondaryActions.fastForEachIndexed { index, action ->
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(index, secondaryActions.size),
                    checked = false,
                    onCheckedChange = {
                        onDismissRequest()
                        when (action.key) {
                            "remind_me" -> onShowRemindMePicker()
                            "cancel_reminder" -> onEvent(NotemarkDetailEvent.OnCancelReminder)
                        }
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = action.icon,
                            color = Color(action.color.iconTintArgb()),
                        )
                    },
                    text = { Text(text = action.text) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 2, count = 3),
        ) {
            DropdownMenuItem(
                shapes = MenuDefaults.itemShape(0, 1),
                checked = false,
                onCheckedChange = {
                    onDismissRequest()
                    val bookmark = state.bookmark ?: return@DropdownMenuItem
                    deletionDialogManager.send(
                        DeletionState(
                            deletionType = DeletionType.BOOKMARK,
                            bookmarkToBeDeleted = bookmark,
                            onConfirm = {
                                onEvent(NotemarkDetailEvent.OnDeleteBookmark)
                                navigator.removeLastOrNull()
                            },
                        ),
                    )
                },
                leadingIcon = {
                    YabaIcon(
                        name = "delete-02",
                        color = Color(YabaColor.RED.iconTintArgb()),
                    )
                },
                text = {
                    Text(
                        text = deleteText,
                        color = Color(YabaColor.RED.iconTintArgb()),
                    )
                },
            )
        }
    }
}
