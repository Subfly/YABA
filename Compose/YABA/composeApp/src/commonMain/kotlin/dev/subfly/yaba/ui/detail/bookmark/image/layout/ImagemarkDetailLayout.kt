package dev.subfly.yaba.ui.detail.bookmark.image.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.navigation.main.FolderDetailRoute
import dev.subfly.yaba.core.navigation.main.TagDetailRoute
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailFolderSectionContent
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailLabel
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailReminderSectionContent
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailTagSectionContent
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.formatDateTime
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.imagemark.ImagemarkDetailEvent
import dev.subfly.yabacore.state.detail.imagemark.ImagemarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_created_at_title
import yaba.composeapp.generated.resources.bookmark_detail_edited_at_title
import yaba.composeapp.generated.resources.bookmark_detail_no_description_provided
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.info

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ImagemarkDetailLayout(
    state: ImagemarkDetailUIState,
    onHide: () -> Unit,
    onEvent: (ImagemarkDetailEvent) -> Unit,
) {
    val navigator = LocalContentNavigator.current

    val mainColor by remember(state.bookmark) {
        mutableStateOf(state.bookmark?.parentFolder?.color ?: YabaColor.GREEN)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(0.9f),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors().copy(
                        contentColor = Color(mainColor.iconTintArgb())
                    ),
                    onClick = onHide,
                ) { Text(stringResource(Res.string.done)) }
            }
        }
        item { Spacer(modifier = Modifier.height(18.dp)) }
        state.bookmark?.let { bookmark ->
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BookmarkDetailLabel(
                        modifier = Modifier.padding(bottom = 8.dp),
                        iconName = "information-circle",
                        label = stringResource(Res.string.info)
                    )
                    SegmentedListItem(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        onClick = {},
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 4),
                        content = { Text(bookmark.label) },
                        leadingContent = { YabaIcon(name = "text", color = mainColor) }
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
                        leadingContent = { YabaIcon(name = "paragraph", color = mainColor) }
                    )
                    SegmentedListItem(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        onClick = {},
                        shapes = ListItemDefaults.segmentedShapes(index = 2, count = 4),
                        content = {
                            Text(stringResource(Res.string.bookmark_detail_created_at_title))
                        },
                        leadingContent = { YabaIcon(name = "clock-01", color = mainColor) },
                        trailingContent = {
                            Text(
                                text = formatDateTime(bookmark.createdAt),
                                style = MaterialTheme.typography.bodySmallEmphasized
                            )
                        }
                    )
                    if (bookmark.createdAt != bookmark.editedAt) {
                        SegmentedListItem(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                            onClick = {},
                            shapes = ListItemDefaults.segmentedShapes(index = 3, count = 4),
                            content = {
                                Text(stringResource(Res.string.bookmark_detail_edited_at_title))
                            },
                            leadingContent = { YabaIcon(name = "edit-02", color = mainColor) },
                            trailingContent = {
                                Text(
                                    text = formatDateTime(bookmark.editedAt),
                                    style = MaterialTheme.typography.bodySmallEmphasized
                                )
                            }
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
                        onClickFolder = { navigator.add(FolderDetailRoute(folderId = it.id)) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                BookmarkDetailTagSectionContent(
                    tags = bookmark.tags,
                    onClickTag = { tag -> navigator.add(TagDetailRoute(tagId = tag.id)) }
                )
            }
            state.reminderDateEpochMillis?.let { reminderMillis ->
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    BookmarkDetailReminderSectionContent(
                        reminderDateEpochMillis = reminderMillis,
                        mainColor = mainColor,
                        onCancelReminder = { onEvent(ImagemarkDetailEvent.OnCancelReminder) },
                    )
                }
            }
        }
    }
}
