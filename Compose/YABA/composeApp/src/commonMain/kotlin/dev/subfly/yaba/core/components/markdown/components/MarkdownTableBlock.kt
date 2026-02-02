package dev.subfly.yaba.core.components.markdown.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.markdown.MarkdownSelectableText
import dev.subfly.yabacore.markdown.formatting.PreviewBlockUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel

@Composable
internal fun MarkdownTableBlock(
    block: PreviewBlockUiModel.TableBlock,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
            )
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            block.header.forEach { cellRuns ->
                val (annotated, inlineContent) = buildBlockAnnotatedString(
                    runs = cellRuns,
                    blockRange = block.range,
                    blockSectionKey = block.sectionKey,
                    highlights = highlights,
                    defaultColor = defaultColor,
                    linkColor = linkColor,
                    linkInteractionListener = linkInteractionListener,
                )
                MarkdownSelectableText(
                    text = annotated,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    inlineContent = inlineContent,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        block.rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { cellRuns ->
                    val (annotated, inlineContent) = buildBlockAnnotatedString(
                        runs = cellRuns,
                        blockRange = block.range,
                        blockSectionKey = block.sectionKey,
                        highlights = highlights,
                        defaultColor = defaultColor,
                        linkColor = linkColor,
                        linkInteractionListener = linkInteractionListener,
                    )
                    MarkdownSelectableText(
                        text = annotated,
                        style = MaterialTheme.typography.bodySmall,
                        inlineContent = inlineContent,
                    )
                }
            }
        }
    }
}
