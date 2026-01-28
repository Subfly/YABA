package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.formatDateTime
import dev.subfly.yaba.util.uiTitle
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.model.utils.uiIconName
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkLinkDetailsUiModel
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_created_at_title
import yaba.composeapp.generated.resources.bookmark_detail_edited_at_title
import yaba.composeapp.generated.resources.bookmark_detail_no_description_provided
import yaba.composeapp.generated.resources.create_bookmark_type_placeholder
import yaba.composeapp.generated.resources.info

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkDetailInfoSectionContent(
    modifier: Modifier = Modifier,
    bookmarkDetails: BookmarkPreviewUiModel,
    linkDetails: LinkmarkLinkDetailsUiModel?,
    mainColor: YabaColor,
) {
    val itemCount = remember(linkDetails, bookmarkDetails.createdAt, bookmarkDetails.editedAt) {
        3 + (if (linkDetails != null) 1 else 0) + (if (bookmarkDetails.createdAt != bookmarkDetails.editedAt) 1 else 0)
    }
    var currentIndex = 0
    
    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LinkmarkDetailLabel(
            modifier = Modifier.padding(bottom = 8.dp),
            iconName = "information-circle",
            label = stringResource(Res.string.info)
        )
        SegmentedListItem(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            onClick = {},
            shapes = ListItemDefaults.segmentedShapes(index = currentIndex++, count = itemCount),
            content = { Text(bookmarkDetails.label) },
            leadingContent = { YabaIcon(name = "text", color = mainColor) }
        )
        SegmentedListItem(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            onClick = {},
            shapes = ListItemDefaults.segmentedShapes(index = currentIndex++, count = itemCount),
            content = {
                Text(
                    text = bookmarkDetails.description
                        ?: stringResource(Res.string.bookmark_detail_no_description_provided),
                    fontStyle = if (bookmarkDetails.description == null)
                        FontStyle.Italic else FontStyle.Normal,
                )
            },
            leadingContent = { YabaIcon(name = "paragraph", color = mainColor) }
        )
        linkDetails?.let { linkMetadata ->
            SegmentedListItem(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                onClick = {},
                shapes = ListItemDefaults.segmentedShapes(index = currentIndex++, count = itemCount),
                content = {
                    Text(stringResource(Res.string.create_bookmark_type_placeholder))
                },
                leadingContent = {
                    YabaIcon(
                        name = linkMetadata.linkType.uiIconName(),
                        color = mainColor
                    )
                },
                trailingContent = {
                    Text(
                        text = linkMetadata.linkType.uiTitle(),
                        style = MaterialTheme.typography.bodySmallEmphasized
                    )
                }
            )
        }
        SegmentedListItem(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            onClick = {},
            shapes = ListItemDefaults.segmentedShapes(index = currentIndex++, count = itemCount),
            content = {
                Text(stringResource(Res.string.bookmark_detail_created_at_title))
            },
            leadingContent = { YabaIcon(name = "clock-01", color = mainColor) },
            trailingContent = {
                Text(
                    text = formatDateTime(bookmarkDetails.createdAt),
                    style = MaterialTheme.typography.bodySmallEmphasized
                )
            }
        )
        if (bookmarkDetails.createdAt != bookmarkDetails.editedAt) {
            SegmentedListItem(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                onClick = {},
                shapes = ListItemDefaults.segmentedShapes(index = currentIndex++, count = itemCount),
                content = {
                    Text(stringResource(Res.string.bookmark_detail_edited_at_title))
                },
                leadingContent = { YabaIcon(name = "edit-02", color = mainColor) },
                trailingContent = {
                    Text(
                        text = formatDateTime(bookmarkDetails.editedAt),
                        style = MaterialTheme.typography.bodySmallEmphasized
                    )
                }
            )
        }
    }
}
