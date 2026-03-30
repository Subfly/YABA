package dev.subfly.yaba.ui.detail.bookmark.image.components

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
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.ImagemarkCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.imagemark.ImagemarkDetailEvent
import dev.subfly.yabacore.state.detail.imagemark.ImagemarkDetailUIState
import dev.subfly.yabacore.util.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.delete
import yaba.composeapp.generated.resources.edit
import yaba.composeapp.generated.resources.move
import yaba.composeapp.generated.resources.remind_me
import yaba.composeapp.generated.resources.share

private data class DetailMenuAction(
    val key: String,
    val icon: String,
    val text: String,
    val color: YabaColor,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ImagemarkContentDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    state: ImagemarkDetailUIState,
    onEvent: (ImagemarkDetailEvent) -> Unit,
    onShowRemindMePicker: () -> Unit,
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val deletionDialogManager = LocalDeletionDialogManager.current

    val editText = stringResource(Res.string.edit)
    val moveText = stringResource(Res.string.move)
    val remindMeText = stringResource(Res.string.remind_me)
    val cancelReminderText = "Cancel Reminder"
    val shareText = stringResource(Res.string.share)
    val exportText = "Save Copy"
    val deleteText = stringResource(Res.string.delete)

    val isAndroid = Platform == YabaPlatform.ANDROID
    val hasActiveReminder = state.reminderDateEpochMillis != null

    val primaryActions = remember(editText, moveText, exportText) {
        listOf(
            DetailMenuAction(
                key = "edit",
                icon = "edit-02",
                text = editText,
                color = YabaColor.ORANGE
            ),
            DetailMenuAction(
                key = "move",
                icon = "arrow-move-up-right",
                text = moveText,
                color = YabaColor.TEAL
            ),
            DetailMenuAction(
                key = "export",
                icon = "download-01",
                text = exportText,
                color = YabaColor.BLUE
            ),
        )
    }

    val secondaryActions =
        remember(isAndroid, hasActiveReminder, remindMeText, cancelReminderText, shareText) {
            buildList {
                if (isAndroid) {
                    if (hasActiveReminder) {
                        add(
                            DetailMenuAction(
                                key = "cancel_reminder",
                                icon = "notification-off-03",
                                text = cancelReminderText,
                                color = YabaColor.YELLOW
                            )
                        )
                    } else {
                        add(
                            DetailMenuAction(
                                key = "remind_me",
                                icon = "notification-01",
                                text = remindMeText,
                                color = YabaColor.YELLOW
                            )
                        )
                    }
                }
                add(
                    DetailMenuAction(
                        key = "share",
                        icon = "share-03",
                        text = shareText,
                        color = YabaColor.INDIGO
                    )
                )
            }
        }

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = 3)
        ) {
            primaryActions.fastForEachIndexed { index, action ->
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(index, primaryActions.size),
                    checked = false,
                    onCheckedChange = { _ ->
                        onDismissRequest()
                        when (action.key) {
                            "edit" -> {
                                val bookmarkId = state.bookmark?.id ?: return@DropdownMenuItem
                                creationNavigator.add(ImagemarkCreationRoute(bookmarkId = bookmarkId))
                                appStateManager.onShowCreationContent()
                            }

                            "move" -> {
                                val bookmark = state.bookmark ?: return@DropdownMenuItem
                                creationNavigator.add(
                                    FolderSelectionRoute(
                                        mode = FolderSelectionMode.BOOKMARKS_MOVE,
                                        contextFolderId = bookmark.folderId,
                                        contextBookmarkIds = listOf(bookmark.id),
                                    )
                                )
                                appStateManager.onShowCreationContent()
                            }

                            "export" -> onEvent(ImagemarkDetailEvent.OnExportImage)
                        }
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = action.icon,
                            color = Color(action.color.iconTintArgb()),
                        )
                    },
                    text = { Text(text = action.text) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 1, count = 3)
        ) {
            secondaryActions.fastForEachIndexed { index, action ->
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(index, secondaryActions.size),
                    checked = false,
                    onCheckedChange = { _ ->
                        onDismissRequest()
                        when (action.key) {
                            "remind_me" -> onShowRemindMePicker()
                            "cancel_reminder" -> onEvent(ImagemarkDetailEvent.OnCancelReminder)
                            "share" -> onEvent(ImagemarkDetailEvent.OnShareImage)
                        }
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = action.icon,
                            color = Color(action.color.iconTintArgb()),
                        )
                    },
                    text = { Text(text = action.text) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 2, count = 3)
        ) {
            DropdownMenuItem(
                shapes = MenuDefaults.itemShape(0, 1),
                checked = false,
                onCheckedChange = { _ ->
                    onDismissRequest()
                    val bookmark = state.bookmark ?: return@DropdownMenuItem
                    deletionDialogManager.send(
                        DeletionState(
                            deletionType = DeletionType.BOOKMARK,
                            bookmarkToBeDeleted = bookmark,
                            onConfirm = {
                                onEvent(ImagemarkDetailEvent.OnDeleteBookmark)
                                navigator.removeLastOrNull()
                            },
                        )
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
                }
            )
        }
    }
}
