package dev.subfly.yaba.ui.detail.bookmark.link.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.webview.YabaWebAppearance
import dev.subfly.yaba.core.components.webview.YabaWebPlatform
import dev.subfly.yaba.core.components.webview.YabaWebScrollDirection
import dev.subfly.yaba.core.components.webview.YabaWebViewViewer
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.LinkmarkCreationRoute
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkReaderFloatingToolbar
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yaba.util.rememberShareHandler
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.webview.WebComponentUris
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_title
import yaba.composeapp.generated.resources.delete
import yaba.composeapp.generated.resources.edit
import yaba.composeapp.generated.resources.move
import yaba.composeapp.generated.resources.refresh
import yaba.composeapp.generated.resources.remind_me
import yaba.composeapp.generated.resources.reader_not_available_description
import yaba.composeapp.generated.resources.reader_not_available_title
import yaba.composeapp.generated.resources.share

private data class DetailMenuAction(
    val key: String,
    val icon: String,
    val text: String,
    val color: YabaColor,
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
internal fun LinkmarkContentLayout(
    modifier: Modifier = Modifier,
    state: LinkmarkDetailUIState,
    onShowDetail: () -> Unit,
    onEvent: (LinkmarkDetailEvent) -> Unit,
    onShowRemindMePicker: () -> Unit = {},
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val openUrl = rememberUrlLauncher()
    val shareUrl = rememberShareHandler()
    val appearance = if (isSystemInDarkTheme()) YabaWebAppearance.Dark else YabaWebAppearance.Light
    val hasReaderContent = !state.isLoading && !state.readableMarkdown.isNullOrBlank()
    val readerFabColor = state.bookmark?.parentFolder?.color ?: YabaColor.BLUE
    var isReaderToolbarVisible by remember(
        state.readableMarkdown,
        state.isLoading
    ) { mutableStateOf(true) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    val editText = stringResource(Res.string.edit)
    val moveText = stringResource(Res.string.move)
    val remindMeText = stringResource(Res.string.remind_me)
    val cancelReminderText = "Cancel Reminder" // TODO: LOCALIZATION
    val refreshText = stringResource(Res.string.refresh)
    val shareText = stringResource(Res.string.share)
    val deleteText = stringResource(Res.string.delete)

    val isAndroid = Platform == YabaPlatform.ANDROID
    val hasActiveReminder = state.reminderDateEpochMillis != null

    val regularActions = remember(isAndroid, hasActiveReminder) {
        buildList {
            add(
                DetailMenuAction(
                    key = "edit",
                    icon = "edit-02",
                    text = editText,
                    color = YabaColor.ORANGE
                )
            )
            add(
                DetailMenuAction(
                    key = "move",
                    icon = "arrow-move-up-right",
                    text = moveText,
                    color = YabaColor.TEAL
                )
            )
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
                    key = "refresh",
                    icon = "refresh",
                    text = refreshText,
                    color = YabaColor.BLUE
                )
            )
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    title = { Text(text = stringResource(Res.string.bookmark_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = navigator::removeLastOrNull) {
                            YabaIcon(name = "arrow-left-01")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onShowDetail,
                            shapes = IconButtonDefaults.shapes(),
                            content = { YabaIcon(name = "information-circle") }
                        )
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                            IconButton(
                                onClick = { isMenuExpanded = !isMenuExpanded },
                                shapes = IconButtonDefaults.shapes(),
                            ) { YabaIcon(name = "more-horizontal-circle-02") }

                            DropdownMenuPopup(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                            ) {
                                DropdownMenuGroup(
                                    shapes = MenuDefaults.groupShape(index = 0, count = 2)
                                ) {
                                    regularActions.forEachIndexed { index, action ->
                                        DropdownMenuItem(
                                            shapes = MenuDefaults.itemShape(
                                                index,
                                                regularActions.size
                                            ),
                                            checked = false,
                                            onCheckedChange = { _ ->
                                                isMenuExpanded = false
                                                when (action.key) {
                                                    "edit" -> {
                                                        val bookmarkId = state.bookmark?.id
                                                            ?: return@DropdownMenuItem
                                                        creationNavigator.add(
                                                            LinkmarkCreationRoute(
                                                                bookmarkId = bookmarkId
                                                            )
                                                        )
                                                        appStateManager.onShowCreationContent()
                                                    }

                                                    "move" -> {
                                                        val bookmark = state.bookmark
                                                            ?: return@DropdownMenuItem
                                                        creationNavigator.add(
                                                            FolderSelectionRoute(
                                                                mode = FolderSelectionMode.BOOKMARKS_MOVE,
                                                                contextFolderId = bookmark.folderId,
                                                                contextBookmarkIds = listOf(bookmark.id),
                                                            )
                                                        )
                                                        appStateManager.onShowCreationContent()
                                                    }

                                                    "remind_me" -> onShowRemindMePicker()
                                                    "cancel_reminder" -> onEvent(LinkmarkDetailEvent.OnCancelReminder)
                                                    "refresh" -> {
                                                        // TODO: Implement refresh functionality
                                                    }

                                                    "share" -> {
                                                        val url = state.linkDetails?.url
                                                            ?: return@DropdownMenuItem
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
                                    shapes = MenuDefaults.groupShape(index = 1, count = 2)
                                ) {
                                    DropdownMenuItem(
                                        shapes = MenuDefaults.itemShape(0, 1),
                                        checked = false,
                                        onCheckedChange = { _ ->
                                            isMenuExpanded = false
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
                    }
                )
                AnimatedContent(state.isLoading) { loading ->
                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .background(color = MaterialTheme.colorScheme.surface)
                        ) { LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    ) { paddings ->
        Box(modifier = Modifier.fillMaxSize().padding(paddings)) {
            if (hasReaderContent) {
                LinkmarkReaderFloatingToolbar(
                    isVisible = isReaderToolbarVisible,
                    fabColor = readerFabColor,
                    readerPreferences = state.readerPreferences,
                    onEvent = onEvent,
                    onFabClick = {
                        // TODO: Wire FAB action for reader toolbar.
                    },
                )

                YabaWebViewViewer(
                    modifier = Modifier.fillMaxSize(),
                    baseUrl = WebComponentUris.getViewerUri(),
                    markdown = state.readableMarkdown ?: "",
                    assetsBaseUrl = state.assetsBaseUrl,
                    platform = YabaWebPlatform.Compose,
                    appearance = appearance,
                    readerPreferences = state.readerPreferences,
                    onUrlClick = openUrl,
                    onScrollDirectionChanged = { direction ->
                        if (direction == YabaWebScrollDirection.Down) isReaderToolbarVisible = false
                        if (direction == YabaWebScrollDirection.Up) isReaderToolbarVisible = true
                    },
                )
            } else if (!state.isLoading) {
                NoContentView(
                    modifier = Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    iconName = "cancel-square",
                    labelRes = Res.string.reader_not_available_title,
                ) {
                    Text(text = stringResource(Res.string.reader_not_available_description))
                }
            }
        }
    }
}
