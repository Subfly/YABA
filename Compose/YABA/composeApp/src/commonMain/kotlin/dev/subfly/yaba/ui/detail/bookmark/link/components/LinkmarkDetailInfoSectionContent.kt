package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
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
    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LinkmarkDetailLabel(
            modifier = Modifier.padding(bottom = 8.dp),
            iconName = "information-circle",
            label = stringResource(Res.string.info)
        )
        ListItem(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            headlineContent = { Text(bookmarkDetails.label) },
            leadingContent = { YabaIcon(name = "text", color = mainColor) }
        )
        ListItem(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            headlineContent = {
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
            ListItem(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                headlineContent = {
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
        ListItem(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            headlineContent = {
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
            ListItem(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                headlineContent = {
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
