package dev.subfly.yaba.core.components.markdown.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.markdown.MarkdownSelectableText
import dev.subfly.yabacore.markdown.formatting.PreviewBlockUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel

@Composable
internal fun MarkdownListBlock(
    block: PreviewBlockUiModel.ListBlock,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        block.items.forEachIndexed { index: Int, item: PreviewBlockUiModel.ListItem ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                when (val checked = item.checked) {
                    null -> Text(
                        text = if (block.ordered) "${index + 1}." else "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = defaultColor,
                    )
                    else -> Checkbox(
                        checked = checked,
                        onCheckedChange = {},
                        modifier = Modifier.size(24.dp),
                        enabled = false,
                    )
                }
                val (annotated, inlineContent) = buildBlockAnnotatedString(
                    runs = item.runs,
                    blockRange = block.range,
                    blockSectionKey = block.sectionKey,
                    highlights = highlights,
                    defaultColor = defaultColor,
                    linkColor = linkColor,
                    linkInteractionListener = linkInteractionListener,
                )
                MarkdownSelectableText(
                    text = annotated,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge.copy(color = defaultColor),
                    inlineContent = inlineContent,
                )
            }
        }
    }
}
