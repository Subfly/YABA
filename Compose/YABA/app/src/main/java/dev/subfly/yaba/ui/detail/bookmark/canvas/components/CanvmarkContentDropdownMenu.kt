package dev.subfly.yaba.ui.detail.bookmark.canvas.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.R
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.CanvmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.model.utils.FolderSelectionMode
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.state.detail.canvmark.CanvmarkDetailEvent
import dev.subfly.yaba.core.state.detail.canvmark.CanvmarkDetailUIState
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.PrivateBookmarkPasswordReason
import dev.subfly.yaba.util.rememberPrivateBookmarkProtectedAction
import dev.subfly.yaba.util.rememberPrivateBookmarkToggleAction

private data class CanvmarkDetailMenuAction(
    val key: String,
    val icon: String,
    val text: String,
    val color: YabaColor,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CanvmarkContentDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    state: CanvmarkDetailUIState,
    onEvent: (CanvmarkDetailEvent) -> Unit,
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val deletionDialogManager = LocalDeletionDialogManager.current

    val bookmark = state.bookmark
    val runEdit = rememberPrivateBookmarkProtectedAction(
        model = bookmark,
        reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
    ) {
        val b = state.bookmark ?: return@rememberPrivateBookmarkProtectedAction
        creationNavigator.add(CanvmarkCreationRoute(bookmarkId = b.id))
        appStateManager.onShowCreationContent()
    }
    val runMove = rememberPrivateBookmarkProtectedAction(
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
    val runPin = rememberPrivateBookmarkProtectedAction(
        model = bookmark,
        reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
    ) {
        val b = state.bookmark ?: return@rememberPrivateBookmarkProtectedAction
        AllBookmarksManager.toggleBookmarkPinned(b.id)
    }
    val runDelete = rememberPrivateBookmarkProtectedAction(
        model = bookmark,
        reason = PrivateBookmarkPasswordReason.DELETE_BOOKMARK,
    ) {
        val b = state.bookmark ?: return@rememberPrivateBookmarkProtectedAction
        deletionDialogManager.send(
            DeletionState(
                deletionType = DeletionType.BOOKMARK,
                bookmarkToBeDeleted = b,
                onConfirm = {
                    onEvent(CanvmarkDetailEvent.OnDeleteBookmark)
                    navigator.removeLastOrNull()
                },
            ),
        )
    }
    val onPrivateToggle = rememberPrivateBookmarkToggleAction(bookmark)

    val editText = stringResource(R.string.edit)
    val moveText = stringResource(R.string.move)
    val deleteText = stringResource(R.string.delete)
    val privateActionText = if (bookmark?.isPrivate == true) "Private" else "Not Private"

    val isPinned = state.bookmark?.isPinned == true
    val pinActionText = if (isPinned) "Pin" else "Unpin"

    val primaryActions = remember(editText, moveText, pinActionText, isPinned) {
        listOf(
            CanvmarkDetailMenuAction(
                key = "edit",
                icon = "edit-02",
                text = editText,
                color = YabaColor.ORANGE,
            ),
            CanvmarkDetailMenuAction(
                key = "move",
                icon = "arrow-move-up-right",
                text = moveText,
                color = YabaColor.TEAL,
            ),
            CanvmarkDetailMenuAction(
                key = "pin",
                icon = if (isPinned) "pin" else "pin-off",
                text = pinActionText,
                color = YabaColor.YELLOW,
            ),
        )
    }

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = 2),
        ) {
            primaryActions.forEachIndexed { index, action ->
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(index, primaryActions.size),
                    checked = false,
                    onCheckedChange = {
                        onDismissRequest()
                        when (action.key) {
                            "edit" -> runEdit()
                            "move" -> runMove()
                            "pin" -> runPin()
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
            shapes = MenuDefaults.groupShape(index = 1, count = 2),
        ) {
            if (bookmark != null) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 2),
                    checked = false,
                    onCheckedChange = {
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
                    },
                )
            }
            DropdownMenuItem(
                shapes = MenuDefaults.itemShape(
                    if (bookmark != null) 1 else 0,
                    if (bookmark != null) 2 else 1
                ),
                checked = false,
                onCheckedChange = {
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
                },
            )
        }
    }
}
