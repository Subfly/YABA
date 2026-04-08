package dev.subfly.yaba.ui.detail.composables

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.tag.TagItemView
import dev.subfly.yaba.core.model.ui.TagUiModel

@Composable
internal fun BookmarkDetailTagSectionContent(
    modifier: Modifier = Modifier,
    tags: List<TagUiModel>,
    onClickTag: (TagUiModel) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BookmarkDetailLabel(
            modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 12.dp),
            iconName = "tag-01",
            label = stringResource(R.string.tags_title)
        )
        if (tags.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                NoContentView(
                    modifier = Modifier.padding(12.dp).padding(vertical = 24.dp),
                    iconName = "tags",
                    labelRes = R.string.bookmark_detail_no_tags_added_title,
                    message = { Text(text = stringResource(R.string.bookmark_detail_no_tags_added_description)) },
                )
            }
        } else {
            tags.fastForEach { tag ->
                TagItemView(
                    model = tag,
                    allowsDeletion = false,
                    onClick = onClickTag,
                    containerColor = MaterialTheme.colorScheme.surface,
                    showBookmarkCounts = false,
                    onDeleteTag = {}
                )
            }
        }
    }
}
