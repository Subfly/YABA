package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.tag.TagItemView
import dev.subfly.yabacore.model.ui.TagUiModel
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_no_tags_added_description
import yaba.composeapp.generated.resources.bookmark_detail_no_tags_added_title
import yaba.composeapp.generated.resources.tags_title

@Composable
internal fun LinkmarkDetailTagSectionContent(
    modifier: Modifier = Modifier,
    tags: List<TagUiModel>,
    onClickTag: (TagUiModel) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinkmarkDetailLabel(
            modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 12.dp),
            iconName = "tag-01",
            label = stringResource(Res.string.tags_title)
        )
        if (tags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                NoContentView(
                    modifier = Modifier.padding(12.dp).padding(vertical = 24.dp),
                    iconName = "tags",
                    labelRes = Res.string.bookmark_detail_no_tags_added_title,
                    message = { Text(text = stringResource(Res.string.bookmark_detail_no_tags_added_description)) },
                )
            }
        } else {
            tags.fastForEach { tag ->
                TagItemView(
                    model = tag,
                    allowsDeletion = false,
                    onClick = onClickTag,
                    onDeleteTag = {}
                )
            }
        }
    }
}