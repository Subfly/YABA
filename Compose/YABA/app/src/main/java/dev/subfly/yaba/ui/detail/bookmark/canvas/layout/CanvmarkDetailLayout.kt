package dev.subfly.yaba.ui.detail.bookmark.canvas.layout

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.R
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.state.detail.canvmark.CanvmarkDetailUIState
import dev.subfly.yaba.ui.detail.composables.BookmarkDetailLabel
import dev.subfly.yaba.util.formatDateTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CanvmarkDetailLayout(
    state: CanvmarkDetailUIState,
    onHide: () -> Unit,
) {
    val mainColor = state.bookmark?.parentFolder?.color ?: YabaColor.BLUE
    val bookmark = state.bookmark

    LazyColumn(
        modifier =
            Modifier.fillMaxWidth()
                .fillMaxSize(0.9f),
    ) {
        stickyHeader {
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                TextButton(
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp),
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors().copy(contentColor = Color(mainColor.iconTintArgb())),
                    onClick = onHide,
                ) { Text(stringResource(R.string.done)) }
            }
        }
        item { Spacer(modifier = Modifier.height(18.dp)) }
        if (bookmark != null) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BookmarkDetailLabel(
                        modifier = Modifier.padding(bottom = 8.dp),
                        iconName = "information-circle",
                        label = stringResource(R.string.info),
                    )
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
                                    ?: stringResource(R.string.bookmark_detail_no_description_provided),
                                fontStyle = if (bookmark.description == null) FontStyle.Italic else FontStyle.Normal,
                            )
                        },
                        leadingContent = { YabaIcon(name = "paragraph", color = mainColor) },
                    )
                    SegmentedListItem(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        onClick = {},
                        shapes = ListItemDefaults.segmentedShapes(index = 2, count = 4),
                        content = { Text(stringResource(R.string.bookmark_detail_created_at_title)) },
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
                            content = { Text(stringResource(R.string.bookmark_detail_edited_at_title)) },
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
            item { Spacer(modifier = Modifier.height(56.dp)) }
        }
    }
}
