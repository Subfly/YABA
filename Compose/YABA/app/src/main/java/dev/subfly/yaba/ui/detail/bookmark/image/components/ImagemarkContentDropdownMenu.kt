package dev.subfly.yaba.ui.detail.bookmark.image.components

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

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
import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.ImagemarkCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.PrivateBookmarkPasswordReason
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yaba.util.rememberPrivateBookmarkProtectedAction
import dev.subfly.yaba.util.rememberPrivateBookmarkToggleAction
import dev.subfly.yaba.core.model.utils.FolderSelectionMode
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.state.detail.imagemark.ImagemarkDetailEvent
import dev.subfly.yaba.core.state.detail.imagemark.ImagemarkDetailUIState

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

    val bookmark = state.bookmark
    val noop = remember { { } }
    val runEdit = if (bookmark != null) {
        rememberPrivateBookmarkProtectedAction(
            model = bookmark,
            reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
        ) {
            val bookmarkId = state.bookmark?.id ?: return@rememberPrivateBookmarkProtectedAction
            creationNavigator.add(ImagemarkCreationRoute(bookmarkId = bookmarkId))
            appStateManager.onShowCreationContent()
        }
    } else {
        noop
    }
    val runMove = if (bookmark != null) {
        rememberPrivateBookmarkProtectedAction(
            model = bookmark,
            reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
        ) {
            val b = state.bookmark ?: return@rememberPrivateBookmarkProtectedAction
            creationNavigator.add(
                FolderSelectionRoute(
                    mode = FolderSelectionMode.BOOKMARKS_MOVE,
                    contextFolderId = b.folderId,
                    contextBookmarkIds = listOf(b.id),
                ),
            )
            appStateManager.onShowCreationContent()
        }
    } else {
        noop
    }
    val runPin = if (bookmark != null) {
        rememberPrivateBookmarkProtectedAction(
            model = bookmark,
            reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
        ) {
            val id = state.bookmark?.id ?: return@rememberPrivateBookmarkProtectedAction
            AllBookmarksManager.toggleBookmarkPinned(id)
        }
    } else {
        noop
    }
    val runExport = if (bookmark != null) {
        rememberPrivateBookmarkProtectedAction(
            model = bookmark,
            reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
        ) {
            onEvent(ImagemarkDetailEvent.OnExportImage)
        }
    } else {
        noop
    }
    val runRemindMe = if (bookmark != null) {
        rememberPrivateBookmarkProtectedAction(
            model = bookmark,
            reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
        ) {
            onShowRemindMePicker()
        }
    } else {
        noop
    }
    val runCancelReminder = if (bookmark != null) {
        rememberPrivateBookmarkProtectedAction(
            model = bookmark,
            reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
        ) {
            onEvent(ImagemarkDetailEvent.OnCancelReminder)
        }
    } else {
        noop
    }
    val runShare = if (bookmark != null) {
        rememberPrivateBookmarkProtectedAction(
            model = bookmark,
            reason = PrivateBookmarkPasswordReason.SHARE_BOOKMARK,
        ) {
            onEvent(ImagemarkDetailEvent.OnShareImage)
        }
    } else {
        noop
    }
    val runDelete = if (bookmark != null) {
        rememberPrivateBookmarkProtectedAction(
            model = bookmark,
            reason = PrivateBookmarkPasswordReason.DELETE_BOOKMARK,
        ) {
            val b = state.bookmark ?: return@rememberPrivateBookmarkProtectedAction
            deletionDialogManager.send(
                DeletionState(
                    deletionType = DeletionType.BOOKMARK,
                    bookmarkToBeDeleted = b,
                    onConfirm = {
                        onEvent(ImagemarkDetailEvent.OnDeleteBookmark)
                        navigator.removeLastOrNull()
                    },
                ),
            )
        }
    } else {
        noop
    }
    val onPrivateToggle = if (bookmark != null) {
        rememberPrivateBookmarkToggleAction(bookmark)
    } else {
        noop
    }

    val editText = stringResource(R.string.edit)
    val moveText = stringResource(R.string.move)
    val remindMeText = stringResource(R.string.remind_me)
    val cancelReminderText = "Cancel Reminder"
    val shareText = stringResource(R.string.share)
    val exportText = "Save Copy"
    val deleteText = stringResource(R.string.delete)
    // TODO: LOCALIZATION (match BookmarkItemView)
    val privateActionText = if (bookmark?.isPrivate == true) "Private" else "Not Private"

    val isAndroid = Platform == YabaPlatform.ANDROID
    val hasActiveReminder = state.reminderDateEpochMillis != null

    val isPinned = state.bookmark?.isPinned == true
    // TODO: LOCALIZATION
    val pinActionText = if (isPinned) "Pin" else "Unpin"

    val primaryActions = remember(editText, moveText, exportText, pinActionText, isPinned) {
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
                key = "pin",
                icon = if (isPinned) "pin" else "pin-off",
                text = pinActionText,
                color = YabaColor.YELLOW
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
                            "edit" -> runEdit()
                            "move" -> runMove()
                            "pin" -> runPin()
                            "export" -> runExport()
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
                            "remind_me" -> runRemindMe()
                            "cancel_reminder" -> runCancelReminder()
                            "share" -> runShare()
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
            if (bookmark != null) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 2),
                    checked = false,
                    onCheckedChange = { _ ->
                        onDismissRequest()
                        onPrivateToggle()
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = if (bookmark.isPrivate) "circle-lock-02" else "circle-unlock-02",
                            color = Color(YabaColor.RED.iconTintArgb()),
                        )
                    },
                    text = {
                        Text(
                            text = privateActionText,
                            color = Color(YabaColor.RED.iconTintArgb()),
                        )
                    }
                )
            }
            DropdownMenuItem(
                shapes = MenuDefaults.itemShape(if (bookmark != null) 1 else 0, if (bookmark != null) 2 else 1),
                checked = false,
                onCheckedChange = { _ ->
                    onDismissRequest()
                    runDelete()
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
