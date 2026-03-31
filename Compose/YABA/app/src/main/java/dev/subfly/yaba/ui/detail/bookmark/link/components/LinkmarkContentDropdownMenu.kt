package dev.subfly.yaba.ui.detail.bookmark.link.components

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
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.LinkmarkCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yaba.util.rememberShareHandler
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yaba.core.model.utils.FolderSelectionMode
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yaba.core.state.detail.linkmark.LinkmarkDetailUIState

private data class DetailMenuAction(
    val key: String,
    val icon: String,
    val text: String,
    val color: YabaColor,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkContentDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    state: LinkmarkDetailUIState,
    onEvent: (LinkmarkDetailEvent) -> Unit,
    onShowRemindMePicker: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportPdf: () -> Unit,
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val shareUrl = rememberShareHandler()
    val openUrl = rememberUrlLauncher()

    val openText = stringResource(R.string.open)
    val editText = stringResource(R.string.edit)
    val moveText = stringResource(R.string.move)
    val remindMeText = stringResource(R.string.remind_me)
    val cancelReminderText = "Cancel Reminder" // TODO: LOCALIZATION
    val updateSectionTitle = "Update" // TODO: LOCALIZATION
    val metadataLabel = "Metadata" // TODO: LOCALIZATION
    val versionLabel = "Version" // TODO: LOCALIZATION
    val shareText = stringResource(R.string.share)
    val deleteText = stringResource(R.string.delete)
    // TODO: LOCALIZATION
    val saveCopyText = "Export"
    val mdLabel = "MD"
    val pdfLabel = "PDF"

    val isAndroid = Platform == YabaPlatform.ANDROID
    val hasActiveReminder = state.reminderDateEpochMillis != null

    val primaryActions = remember(openText, editText, moveText) {
        listOf(
            DetailMenuAction(key = "open", icon = "link-04", text = openText, color = YabaColor.GREEN),
            DetailMenuAction(key = "edit", icon = "edit-02", text = editText, color = YabaColor.ORANGE),
            DetailMenuAction(key = "move", icon = "arrow-move-up-right", text = moveText, color = YabaColor.TEAL),
        )
    }

    var isUpdateSubmenuExpanded by remember { mutableStateOf(false) }
    var isSaveCopyExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (!expanded) {
            isUpdateSubmenuExpanded = false
            isSaveCopyExpanded = false
        }
    }

    val primaryRowCount = primaryActions.size + 2

    val secondaryActions = remember(isAndroid, hasActiveReminder, remindMeText, cancelReminderText, shareText) {
        buildList {
            if (isAndroid) {
                if (hasActiveReminder) {
                    add(DetailMenuAction(key = "cancel_reminder", icon = "notification-off-03", text = cancelReminderText, color = YabaColor.YELLOW))
                } else {
                    add(DetailMenuAction(key = "remind_me", icon = "notification-01", text = remindMeText, color = YabaColor.YELLOW))
                }
            }
            add(DetailMenuAction(key = "share", icon = "share-03", text = shareText, color = YabaColor.INDIGO))
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
                    shapes = MenuDefaults.itemShape(index, primaryRowCount),
                    checked = false,
                    onCheckedChange = { _ ->
                        onDismissRequest()
                        when (action.key) {
                            "open" -> {
                                val url = state.linkDetails?.url ?: return@DropdownMenuItem
                                openUrl(url)
                            }
                            "edit" -> {
                                val bookmarkId = state.bookmark?.id ?: return@DropdownMenuItem
                                creationNavigator.add(LinkmarkCreationRoute(bookmarkId = bookmarkId))
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

            LinkmarkSaveCopySubmenuSection(
                itemIndex = primaryActions.size,
                siblingCount = primaryRowCount,
                saveCopyText = saveCopyText,
                mdLabel = mdLabel,
                pdfLabel = pdfLabel,
                isExpanded = isSaveCopyExpanded,
                onToggleExpand = { isSaveCopyExpanded = !isSaveCopyExpanded },
                onDismissSubmenu = { isSaveCopyExpanded = false },
                onDismissRootMenu = onDismissRequest,
                onExportMarkdown = onExportMarkdown,
                onExportPdf = onExportPdf,
            )

            LinkmarkUpdateSubmenuSection(
                itemIndex = primaryActions.size + 1,
                siblingCount = primaryRowCount,
                title = updateSectionTitle,
                metadataLabel = metadataLabel,
                versionLabel = versionLabel,
                accentColor = Color(YabaColor.MINT.iconTintArgb()),
                isExpanded = isUpdateSubmenuExpanded,
                onToggleExpand = { isUpdateSubmenuExpanded = !isUpdateSubmenuExpanded },
                onDismissSubmenu = { isUpdateSubmenuExpanded = false },
                onDismissRootMenu = onDismissRequest,
                onEvent = onEvent,
            )
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
                            "cancel_reminder" -> onEvent(LinkmarkDetailEvent.OnCancelReminder)
                            "share" -> {
                                val url = state.linkDetails?.url ?: return@DropdownMenuItem
                                shareUrl(url)
                            }
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
                                onEvent(LinkmarkDetailEvent.OnDeleteBookmark)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkmarkSaveCopySubmenuSection(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkmarkUpdateSubmenuSection(
    itemIndex: Int,
    siblingCount: Int,
    title: String,
    metadataLabel: String,
    versionLabel: String,
    accentColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismissSubmenu: () -> Unit,
    onDismissRootMenu: () -> Unit,
    onEvent: (LinkmarkDetailEvent) -> Unit,
) {
    Box {
        DropdownMenuItem(
            shapes = MenuDefaults.itemShape(itemIndex, siblingCount),
            checked = false,
            onCheckedChange = { _ -> onToggleExpand() },
            leadingIcon = {
                YabaIcon(
                    name = "arrow-reload-horizontal",
                    color = accentColor,
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
            text = { Text(text = title) },
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
                    onCheckedChange = { _ ->
                        onDismissSubmenu()
                        onDismissRootMenu()
                        onEvent(LinkmarkDetailEvent.OnUpdateLinkMetadataRequested)
                    },
                    leadingIcon = {
                        YabaIcon(name = "database-01", color = accentColor)
                    },
                    text = { Text(text = metadataLabel) },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(1, 2),
                    checked = false,
                    onCheckedChange = { _ ->
                        onDismissSubmenu()
                        onDismissRootMenu()
                        onEvent(LinkmarkDetailEvent.OnUpdateReadableRequested)
                    },
                    leadingIcon = {
                        YabaIcon(name = "book-open-01", color = accentColor)
                    },
                    text = { Text(text = versionLabel) },
                )
            }
        }
    }
}
