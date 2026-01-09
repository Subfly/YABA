package dev.subfly.yaba.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A reusable composable that displays tags in a row format.
 *
 * Shows different layouts based on the number of tags:
 * - Empty: Shows a placeholder icon and text
 * - Less than 6 tags: Shows all tags as connected surfaces
 * - 6 or more tags: Shows first 5 tags and a "+N" indicator
 *
 * @param tags The list of tags to display
 * @param modifier The modifier to be applied to the Row
 * @param emptyStateTextRes The string resource for the empty state text
 * @param emptyStateColor The color to use for the empty state icon
 */
@Composable
fun TagsRowContent(
    tags: List<TagUiModel>,
    modifier: Modifier = Modifier,
    emptyStateTextRes: StringResource,
    emptyStateColor: YabaColor,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        when {
            tags.isEmpty() -> {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(emptyStateColor.iconTintArgb()).copy(alpha = 0.3F),
                ) {
                    YabaIcon(
                        modifier = Modifier.padding(4.dp),
                        name = "tags",
                        color = emptyStateColor,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(emptyStateTextRes),
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            tags.size < 6 -> {
                tags.fastForEach { tag ->
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = if (tag == tags.first()) {
                            RoundedCornerShape(
                                topStart = 4.dp,
                                bottomStart = 4.dp
                            )
                        } else if (tag == tags.last()) {
                            RoundedCornerShape(
                                topEnd = 4.dp,
                                bottomEnd = 4.dp
                            )
                        } else {
                            RoundedCornerShape(0.dp)
                        },
                        color = Color(tag.color.iconTintArgb()).copy(alpha = 0.3F),
                    ) {
                        YabaIcon(
                            modifier = Modifier.padding(4.dp),
                            name = tag.icon,
                            color = tag.color,
                        )
                    }
                }
            }

            else -> {
                tags.subList(0, 5).fastForEach { tag ->
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = if (tag == tags.first()) {
                            RoundedCornerShape(
                                topStart = 4.dp,
                                bottomStart = 4.dp
                            )
                        } else if (tag == tags.subList(0, 5).last()) {
                            RoundedCornerShape(
                                topEnd = 4.dp,
                                bottomEnd = 4.dp
                            )
                        } else {
                            RoundedCornerShape(0.dp)
                        },
                        color = Color(tag.color.iconTintArgb()).copy(alpha = 0.3F),
                    ) {
                        YabaIcon(
                            modifier = Modifier.padding(4.dp),
                            name = tag.icon,
                            color = tag.color,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+${tags.size - 5}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

