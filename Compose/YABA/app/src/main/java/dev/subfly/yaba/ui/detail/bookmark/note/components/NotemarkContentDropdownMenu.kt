package dev.subfly.yaba.ui.detail.bookmark.note.components

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.PrivateBookmarkPasswordReason
import dev.subfly.yaba.util.rememberPrivateBookmarkProtectedAction
import dev.subfly.yaba.util.rememberPrivateBookmarkToggleAction
import dev.subfly.yaba.core.model.utils.FolderSelectionMode
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.state.detail.notemark.NotemarkDetailEvent
import dev.subfly.yaba.core.state.detail.notemark.NotemarkDetailUIState

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
    onExportMarkdown: () -> Unit,
    onExportPdf: () -> Unit,
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
        creationNavigator.add(NotemarkCreationRoute(bookmarkId = b.id))
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
        val id = state.bookmark?.id ?: return@rememberPrivateBookmarkProtectedAction
        AllBookmarksManager.toggleBookmarkPinned(id)
    }
    val runExportMarkdown = rememberPrivateBookmarkProtectedAction(
        model = bookmark,
        reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
    ) {
        onExportMarkdown()
    }
    val runExportPdf = rememberPrivateBookmarkProtectedAction(
        model = bookmark,
        reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
    ) {
        onExportPdf()
    }
    val runRemindMe = rememberPrivateBookmarkProtectedAction(
        model = bookmark,
        reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
    ) {
        onShowRemindMePicker()
    }
    val runCancelReminder = rememberPrivateBookmarkProtectedAction(
        model = bookmark,
        reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
    ) {
        onEvent(NotemarkDetailEvent.OnCancelReminder)
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
                    onEvent(NotemarkDetailEvent.OnDeleteBookmark)
                    navigator.removeLastOrNull()
                },
            ),
        )
    }
    val onPrivateToggle = rememberPrivateBookmarkToggleAction(bookmark)

    val editText = stringResource(R.string.edit)
    val moveText = stringResource(R.string.move)
    val remindMeText = stringResource(R.string.remind_me)
    // TODO: LOCALIZATION
    val cancelReminderText = "Cancel Reminder"
    val saveCopyText = "Export"
    val mdLabel = "MD"
    val pdfLabel = "PDF"
    val deleteText = stringResource(R.string.delete)
    // TODO: LOCALIZATION (match BookmarkItemView)
    val privateActionText = if (bookmark?.isPrivate == true) "Private" else "Not Private"

    val hasActiveReminder = state.reminderDateEpochMillis != null

    val isPinned = state.bookmark?.isPinned == true
    // TODO: LOCALIZATION
    val pinActionText = if (isPinned) "Pin" else "Unpin"

    var isSaveCopyExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (!expanded) isSaveCopyExpanded = false
    }

    val primaryActions = remember(editText, moveText, pinActionText, isPinned) {
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
                color = YabaColor.TEAL,
            ),
            DetailMenuAction(
                key = "pin",
                icon = if (isPinned) "pin" else "pin-off",
                text = pinActionText,
                color = YabaColor.YELLOW,
            ),
        )
    }

    val primaryGroupSiblingCount = primaryActions.size + 1

    val secondaryActions = remember(
        remindMeText,
        cancelReminderText,
        hasActiveReminder,
    ) {
        buildList {
            if (hasActiveReminder) {
                add(
                    DetailMenuAction(
                        key = "cancel_reminder",
                        icon = "notification-off-03",
                        text = cancelReminderText,
                        color = YabaColor.YELLOW,
                    ),
                )
            } else {
                add(
                    DetailMenuAction(
                        key = "remind_me",
                        icon = "notification-01",
                        text = remindMeText,
                        color = YabaColor.YELLOW,
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
                    shapes = MenuDefaults.itemShape(index, primaryGroupSiblingCount),
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
            NotemarkSaveCopySubmenuSection(
                itemIndex = primaryActions.size,
                siblingCount = primaryGroupSiblingCount,
                saveCopyText = saveCopyText,
                mdLabel = mdLabel,
                pdfLabel = pdfLabel,
                isExpanded = isSaveCopyExpanded,
                onToggleExpand = { isSaveCopyExpanded = !isSaveCopyExpanded },
                onDismissSubmenu = { isSaveCopyExpanded = false },
                onDismissRootMenu = onDismissRequest,
                onExportMarkdown = { runExportMarkdown() },
                onExportPdf = { runExportPdf() },
            )
        }

        if (secondaryActions.isNotEmpty()) {
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
                                "remind_me" -> runRemindMe()
                                "cancel_reminder" -> runCancelReminder()
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 2, count = 3),
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
                shapes = MenuDefaults.itemShape(if (bookmark != null) 1 else 0, if (bookmark != null) 2 else 1),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotemarkSaveCopySubmenuSection(
    itemIndex: Int,
    siblingCount: Int,
    saveCopyText: String,
    mdLabel: String,
    pdfLabel: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismissSubmenu: () -> Unit,
    onDismissRootMenu: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportPdf: () -> Unit,
) {
    val saveCopyAccent = Color(YabaColor.BLUE.iconTintArgb())
    Box {
        DropdownMenuItem(
            shapes = MenuDefaults.itemShape(itemIndex, siblingCount),
            checked = false,
            onCheckedChange = { onToggleExpand() },
            leadingIcon = {
                YabaIcon(
                    name = "download-01",
                    color = saveCopyAccent,
                )
            },
            trailingIcon = {
                val expandedRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 90F else 0F,
                )
                YabaIcon(
                    modifier = Modifier.rotate(expandedRotation),
                    name = "arrow-right-01",
                )
            },
            text = { Text(text = saveCopyText) },
        )
        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = onDismissSubmenu,
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(
                    index = 0,
                    count = 1,
                ),
            ) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 2),
                    checked = false,
                    onCheckedChange = {
                        onDismissSubmenu()
                        onDismissRootMenu()
                        onExportMarkdown()
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = "document-attachment",
                            color = Color(YabaColor.GRAY.iconTintArgb()),
                        )
                    },
                    text = { Text(text = mdLabel) },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(1, 2),
                    checked = false,
                    onCheckedChange = {
                        onDismissSubmenu()
                        onDismissRootMenu()
                        onExportPdf()
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = "pdf-02",
                            color = Color(YabaColor.RED.iconTintArgb()),
                        )
                    },
                    text = { Text(text = pdfLabel) },
                )
            }
        }
    }
}
