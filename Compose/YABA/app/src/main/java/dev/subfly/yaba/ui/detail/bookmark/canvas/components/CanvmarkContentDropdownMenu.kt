package dev.subfly.yaba.ui.detail.bookmark.canvas.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
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

private data class CanvmarkDetailMenuAction(
    val key: String,
    val icon: String,
    val text: String,
    val color: YabaColor,
)

// TODO: LOCALIZATIONS
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CanvmarkContentDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    state: CanvmarkDetailUIState,
    onEvent: (CanvmarkDetailEvent) -> Unit,
    onShowRemindMePicker: () -> Unit,
    onExportPng: (exportBackground: Boolean) -> Unit,
    onExportSvg: (exportBackground: Boolean) -> Unit,
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val deletionDialogManager = LocalDeletionDialogManager.current

    val runEdit = remember(state.bookmark?.id) {
        {
            state.bookmark?.let { b ->
                creationNavigator.add(CanvmarkCreationRoute(bookmarkId = b.id))
                appStateManager.onShowCreationContent()
            }
        }
    }
    val runMove = remember(state.bookmark?.id, state.bookmark?.folderId) {
        {
            state.bookmark?.let { b ->
                creationNavigator.add(
                    FolderSelectionRoute(
                        mode = FolderSelectionMode.BOOKMARKS_MOVE,
                        contextFolderId = b.folderId,
                        contextBookmarkIds = listOf(b.id),
                    ),
                )
                appStateManager.onShowCreationContent()
            }
        }
    }
    val runPin = remember(state.bookmark?.id) {
        {
            state.bookmark?.let { b -> AllBookmarksManager.toggleBookmarkPinned(b.id) }
        }
    }
    val runRemindMe = remember(onShowRemindMePicker) {
        { onShowRemindMePicker() }
    }
    val runCancelReminder = remember(onEvent) {
        { onEvent(CanvmarkDetailEvent.OnCancelReminder) }
    }
    val runDelete = remember(state.bookmark?.id) {
        {
            state.bookmark?.let { b ->
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
        }
    }

    val editText = stringResource(R.string.edit)
    val moveText = stringResource(R.string.move)
    val remindMeText = stringResource(R.string.remind_me)
    val cancelReminderText = "Cancel Reminder"
    val exportText = "Export"
    val pngLabel = "PNG"
    val svgLabel = "SVG"
    val backgroundLabel = "Background"
    val deleteText = stringResource(R.string.delete)

    val hasActiveReminder = state.reminderDateEpochMillis != null

    val isPinned = state.bookmark?.isPinned == true
    val pinActionText = if (isPinned) "Pin" else "Unpin"

    var isExportExpanded by remember { mutableStateOf(false) }
    var exportBackground by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (!expanded) {
            isExportExpanded = false
            exportBackground = false
        }
    }

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

    val primaryGroupSiblingCount = primaryActions.size + 1

    val secondaryActions = remember(
        remindMeText,
        cancelReminderText,
        hasActiveReminder,
    ) {
        buildList {
            if (hasActiveReminder) {
                add(
                    CanvmarkDetailMenuAction(
                        key = "cancel_reminder",
                        icon = "notification-off-03",
                        text = cancelReminderText,
                        color = YabaColor.YELLOW,
                    ),
                )
            } else {
                add(
                    CanvmarkDetailMenuAction(
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
            CanvmarkExportSubmenuSection(
                itemIndex = primaryActions.size,
                siblingCount = primaryGroupSiblingCount,
                exportText = exportText,
                pngLabel = pngLabel,
                svgLabel = svgLabel,
                backgroundLabel = backgroundLabel,
                exportBackground = exportBackground,
                onExportBackgroundChange = { exportBackground = it },
                isExpanded = isExportExpanded,
                onToggleExpand = { isExportExpanded = !isExportExpanded },
                onDismissSubmenu = { isExportExpanded = false },
                onDismissRootMenu = onDismissRequest,
                onExportPng = onExportPng,
                onExportSvg = onExportSvg,
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
            DropdownMenuItem(
                shapes = MenuDefaults.itemShape(0, 1),
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
private fun CanvmarkExportSubmenuSection(
    itemIndex: Int,
    siblingCount: Int,
    exportText: String,
    pngLabel: String,
    svgLabel: String,
    backgroundLabel: String,
    exportBackground: Boolean,
    onExportBackgroundChange: (Boolean) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismissSubmenu: () -> Unit,
    onDismissRootMenu: () -> Unit,
    onExportPng: (exportBackground: Boolean) -> Unit,
    onExportSvg: (exportBackground: Boolean) -> Unit,
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
            text = { Text(text = exportText) },
        )
        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = onDismissSubmenu,
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 2),
            ) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 2),
                    checked = false,
                    onCheckedChange = {
                        val bg = exportBackground
                        onDismissSubmenu()
                        onDismissRootMenu()
                        onExportPng(bg)
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = "png-02",
                            color = Color(YabaColor.GRAY.iconTintArgb()),
                        )
                    },
                    text = { Text(text = pngLabel) },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(1, 2),
                    checked = false,
                    onCheckedChange = {
                        val bg = exportBackground
                        onDismissSubmenu()
                        onDismissRootMenu()
                        onExportSvg(bg)
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = "svg-02",
                            color = Color(YabaColor.YELLOW.iconTintArgb()),
                        )
                    },
                    text = { Text(text = svgLabel) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 1, count = 2),
            ) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 1),
                    checked = false,
                    onCheckedChange = { },
                    text = { Text(text = backgroundLabel) },
                    trailingIcon = {
                        Switch(
                            checked = exportBackground,
                            onCheckedChange = onExportBackgroundChange,
                        )
                    },
                )
            }
        }
    }
}
