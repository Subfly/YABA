package dev.subfly.yaba.ui.detail.bookmark.doc.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.DocmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.HighlightCreationRoute
import dev.subfly.yaba.core.navigation.main.FolderDetailRoute
import dev.subfly.yaba.core.navigation.main.TagDetailRoute
import dev.subfly.yaba.ui.detail.bookmark.doc.models.DocmarkDetailPage
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailHighlightItemContent
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailFolderSectionContent
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailReminderSectionContent
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailTagSectionContent
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.formatDateTime
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.docmark.DocmarkDetailEvent
import dev.subfly.yabacore.state.detail.docmark.DocmarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_created_at_title
import yaba.composeapp.generated.resources.bookmark_detail_edited_at_title
import yaba.composeapp.generated.resources.bookmark_detail_no_description_provided
import yaba.composeapp.generated.resources.bookmark_detail_no_tags_added_description
import yaba.composeapp.generated.resources.bookmark_detail_no_tags_added_title
import yaba.composeapp.generated.resources.delete
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.edit
import yaba.composeapp.generated.resources.move
import yaba.composeapp.generated.resources.remind_me
import yaba.composeapp.generated.resources.share

private data class DocActionItem(
    val key: String,
    val label: String,
    val iconName: String,
    val color: YabaColor,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DocmarkDetailLayout(
    state: DocmarkDetailUIState,
    onHide: () -> Unit,
    onEvent: (DocmarkDetailEvent) -> Unit,
    onShowRemindMePicker: () -> Unit = {},
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val deletionDialogManager = LocalDeletionDialogManager.current
    val mainColor by remember(state.bookmark) {
        mutableStateOf(state.bookmark?.parentFolder?.color ?: YabaColor.RED)
    }
    var currentPage by remember { mutableStateOf(DocmarkDetailPage.INFO) }
    val editText = stringResource(Res.string.edit)
    val moveText = stringResource(Res.string.move)
    val shareText = stringResource(Res.string.share)
    val deleteText = stringResource(Res.string.delete)
    val remindMeText = stringResource(Res.string.remind_me)
    val bookmark = state.bookmark

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(0.9f),
    ) {
        stickyHeader {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
            ) {
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors().copy(
                        contentColor = Color(mainColor.iconTintArgb()),
                    ),
                    onClick = onHide,
                ) { Text(stringResource(Res.string.done)) }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DocmarkDetailPage.entries.fastForEachIndexed { index, page ->
                        SegmentedButton(
                            selected = currentPage == page,
                            onClick = { currentPage = page },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                            label = { Text(page.label) },
                            icon = { YabaIcon(name = page.iconName) },
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(18.dp)) }
        if (bookmark != null) {
            when (currentPage) {
                DocmarkDetailPage.ACTIONS -> {
                    val actions = listOf(
                        DocActionItem("edit", editText, "edit-02", YabaColor.ORANGE),
                        DocActionItem("move", moveText, "arrow-move-up-right", YabaColor.TEAL),
                        DocActionItem("export", "Save Copy", "download-01", YabaColor.BLUE),
                        DocActionItem("share", shareText, "share-03", YabaColor.INDIGO),
                        DocActionItem(
                            "reminder",
                            if (state.reminderDateEpochMillis == null) remindMeText else "Cancel Reminder",
                            if (state.reminderDateEpochMillis == null) "notification-01" else "notification-off-03",
                            YabaColor.YELLOW,
                        ),
                        DocActionItem("delete", deleteText, "delete-02", YabaColor.RED),
                    )
                    itemsIndexed(actions, key = { _, action -> action.key }) { index, action ->
                        SegmentedListItem(
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 12.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                when (action.key) {
                                    "edit" -> {
                                        creationNavigator.add(DocmarkCreationRoute(bookmarkId = bookmark.id))
                                        appStateManager.onShowCreationContent()
                                    }

                                    "move" -> {
                                        creationNavigator.add(
                                            FolderSelectionRoute(
                                                mode = FolderSelectionMode.BOOKMARKS_MOVE,
                                                contextFolderId = bookmark.folderId,
                                                contextBookmarkIds = listOf(bookmark.id),
                                            ),
                                        )
                                        appStateManager.onShowCreationContent()
                                    }

                                    "export" -> onEvent(DocmarkDetailEvent.OnExportPdf)
                                    "share" -> onEvent(DocmarkDetailEvent.OnSharePdf)
                                    "reminder" -> {
                                        if (state.reminderDateEpochMillis == null) {
                                            onShowRemindMePicker()
                                        } else {
                                            onEvent(DocmarkDetailEvent.OnCancelReminder)
                                        }
                                    }

                                    "delete" -> {
                                        deletionDialogManager.send(
                                            DeletionState(
                                                deletionType = DeletionType.BOOKMARK,
                                                bookmarkToBeDeleted = bookmark,
                                                onConfirm = {
                                                    onEvent(DocmarkDetailEvent.OnDeleteBookmark)
                                                    navigator.removeLastOrNull()
                                                },
                                            ),
                                        )
                                    }
                                }
                            },
                            shapes = ListItemDefaults.segmentedShapes(index = index, count = actions.size),
                            content = {
                                Text(
                                    text = action.label,
                                    color = if (action.key == "delete") Color(YabaColor.RED.iconTintArgb()) else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            leadingContent = {
                                YabaIcon(
                                    name = action.iconName,
                                    color = action.color,
                                )
                            },
                        )
                    }
                }

                DocmarkDetailPage.INFO -> {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            SegmentedListItem(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                onClick = {},
                                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 4),
                                content = { Text(bookmark.label) },
                                leadingContent = { YabaIcon(name = "text", color = mainColor) },
                            )
                            SegmentedListItem(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                onClick = {},
                                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 4),
                                content = {
                                    Text(
                                        text = bookmark.description
                                            ?: stringResource(Res.string.bookmark_detail_no_description_provided),
                                        fontStyle = if (bookmark.description == null) FontStyle.Italic else FontStyle.Normal,
                                    )
                                },
                                leadingContent = { YabaIcon(name = "paragraph", color = mainColor) },
                            )
                            SegmentedListItem(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                onClick = {},
                                shapes = ListItemDefaults.segmentedShapes(index = 2, count = 4),
                                content = { Text(stringResource(Res.string.bookmark_detail_created_at_title)) },
                                leadingContent = { YabaIcon(name = "clock-01", color = mainColor) },
                                trailingContent = {
                                    Text(
                                        text = formatDateTime(bookmark.createdAt),
                                        style = MaterialTheme.typography.bodySmallEmphasized,
                                    )
                                },
                            )
                            if (bookmark.createdAt != bookmark.editedAt) {
                                SegmentedListItem(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                    onClick = {},
                                    shapes = ListItemDefaults.segmentedShapes(index = 3, count = 4),
                                    content = { Text(stringResource(Res.string.bookmark_detail_edited_at_title)) },
                                    leadingContent = { YabaIcon(name = "edit-02", color = mainColor) },
                                    trailingContent = {
                                        Text(
                                            text = formatDateTime(bookmark.editedAt),
                                            style = MaterialTheme.typography.bodySmallEmphasized,
                                        )
                                    },
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    bookmark.parentFolder?.let { folder ->
                        item {
                            BookmarkDetailFolderSectionContent(
                                folder = folder,
                                mainColor = mainColor,
                                onClickFolder = { navigator.add(FolderDetailRoute(folderId = it.id)) },
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item {
                        BookmarkDetailTagSectionContent(
                            tags = bookmark.tags,
                            onClickTag = { tag -> navigator.add(TagDetailRoute(tagId = tag.id)) },
                        )
                    }
                    state.reminderDateEpochMillis?.let { reminderMillis ->
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                        item {
                            BookmarkDetailReminderSectionContent(
                                reminderDateEpochMillis = reminderMillis,
                                mainColor = mainColor,
                                onCancelReminder = { onEvent(DocmarkDetailEvent.OnCancelReminder) },
                            )
                        }
                    }
                }

                DocmarkDetailPage.HIGHLIGHTS -> {
                    if (state.highlights.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                NoContentView(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .padding(vertical = 24.dp),
                                    iconName = "displeased",
                                    labelRes = Res.string.bookmark_detail_no_tags_added_title,
                                    message = {
                                        Text(text = stringResource(Res.string.bookmark_detail_no_tags_added_description))
                                    },
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = state.highlights,
                            key = { _, highlight -> highlight.id },
                        ) { index, highlight ->
                            LinkmarkDetailHighlightItemContent(
                                modifier = Modifier
                                    .animateItem()
                                    .padding(vertical = 4.dp),
                                highlight = highlight,
                                index = index,
                                count = state.highlights.size,
                                onScrollToHighlight = {
                                    onHide()
                                    onEvent(DocmarkDetailEvent.OnScrollToHighlight(highlight.id))
                                },
                                onEdit = {
                                    creationNavigator.add(
                                        HighlightCreationRoute(
                                            bookmarkId = bookmark.id,
                                            selectionDraft = null,
                                            highlightId = highlight.id,
                                        ),
                                    )
                                    appStateManager.onShowCreationContent()
                                },
                                onDelete = { onEvent(DocmarkDetailEvent.OnDeleteHighlight(highlight.id)) },
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(56.dp)) }
        }
    }
}
