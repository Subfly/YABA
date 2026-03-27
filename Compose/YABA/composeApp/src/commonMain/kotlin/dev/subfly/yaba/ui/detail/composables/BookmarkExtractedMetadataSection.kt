@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.subfly.yaba.ui.detail.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon

/**
 * Read-only extracted metadata (link scrape or PDF/EPUB), shown as a segmented list.
 * Renders only rows whose values are non-blank. Pass [identifier] only for doc metadata (ISBN, etc.).
 */
@Composable
fun BookmarkExtractedMetadataSection(
    modifier: Modifier = Modifier,
    mainColor: YabaColor,
    metadataTitle: String?,
    metadataDescription: String?,
    metadataAuthor: String?,
    metadataDate: String?,
    audioUrl: String?,
    videoUrl: String?,
    identifier: String?,
) {
    val rows = buildList {
        metadataTitle?.takeIf { it.isNotBlank() }?.let {
            add(MetadataRow(icon = "text", value = it))
        }
        metadataDescription?.takeIf { it.isNotBlank() }?.let {
            add(MetadataRow(icon = "paragraph", value = it))
        }
        metadataAuthor?.takeIf { it.isNotBlank() }?.let {
            add(MetadataRow(icon = "user-edit-01", value = it))
        }
        metadataDate?.takeIf { it.isNotBlank() }?.let {
            add(MetadataRow(icon = "calendar-03", value = it))
        }
        audioUrl?.takeIf { it.isNotBlank() }?.let {
            add(MetadataRow(icon = "audio-wave-01", value = it))
        }
        videoUrl?.takeIf { it.isNotBlank() }?.let {
            add(MetadataRow(icon = "computer-video", value = it))
        }
        identifier?.takeIf { it.isNotBlank() }?.let {
            add(MetadataRow(icon = "bar-code-01", value = it))
        }
    }
    if (rows.isEmpty()) return

    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BookmarkDetailLabel(
            modifier = Modifier.padding(bottom = 8.dp),
            iconName = "database-01",
            label = "Metadata", // TODO: LOCALIZATIONS
        )
        rows.forEachIndexed { index, row ->
            SegmentedListItem(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                onClick = {},
                shapes = ListItemDefaults.segmentedShapes(index = index, count = rows.size),
                content = {
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingContent = { YabaIcon(name = row.icon, color = mainColor) },
            )
        }
    }
}

private data class MetadataRow(
    val icon: String,
    val value: String,
)
