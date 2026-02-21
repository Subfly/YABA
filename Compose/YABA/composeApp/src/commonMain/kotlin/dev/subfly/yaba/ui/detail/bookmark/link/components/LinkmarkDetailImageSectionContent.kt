package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkLinkDetailsUiModel
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.image.YabaImage
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_image_error_description
import yaba.composeapp.generated.resources.bookmark_detail_image_error_title
import yaba.composeapp.generated.resources.bookmark_detail_image_header_title

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkDetailImageSectionContent(
    modifier: Modifier = Modifier,
    bookmarkDetails: BookmarkPreviewUiModel,
    linkDetails: LinkmarkLinkDetailsUiModel?,
    mainColor: YabaColor,
) {
    val openUrl = rememberUrlLauncher()

    Column(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = linkDetails != null) {
                linkDetails?.url?.let { url ->
                    openUrl(url)
                }
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LinkmarkDetailLabel(
                iconName = "image-03",
                label = stringResource(Res.string.bookmark_detail_image_header_title)
            )
            linkDetails?.let { linkMetadata ->
                if (linkMetadata.domain.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = linkMetadata.domain)
                        YabaImage(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            filePath = bookmarkDetails.localIconPath,
                        )
                    }
                }
            }
        }
        if (bookmarkDetails.localImagePath != null) {
            YabaImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(12.dp)),
                filePath = bookmarkDetails.localImagePath
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                NoContentView(
                    modifier = Modifier.padding(vertical = 20.dp, horizontal = 4.dp),
                    iconName = "image-not-found-01",
                    labelRes = Res.string.bookmark_detail_image_error_title,
                    message = { Text(text = stringResource(Res.string.bookmark_detail_image_error_description)) },
                )
            }
        }
        linkDetails?.let { linkMetadata ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                YabaIcon(
                    name = "link-02",
                    color = mainColor,
                )
                Text(
                    text = linkMetadata.url,
                    style = MaterialTheme.typography.bodySmallEmphasized
                )
            }
        }
    }
}