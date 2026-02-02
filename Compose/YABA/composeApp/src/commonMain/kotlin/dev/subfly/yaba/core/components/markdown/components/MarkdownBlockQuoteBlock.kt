package dev.subfly.yaba.core.components.markdown.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.markdown.MarkdownSelectableText
import dev.subfly.yabacore.markdown.formatting.PreviewBlockUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel

@Composable
internal fun MarkdownBlockQuoteBlock(
    block: PreviewBlockUiModel.BlockQuote,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
) {
    val (annotated, inlineContent) = buildBlockAnnotatedString(
        runs = block.inlineRuns,
        blockRange = block.range,
        blockSectionKey = block.sectionKey,
        highlights = highlights,
        defaultColor = defaultColor,
        linkColor = linkColor,
        linkInteractionListener = linkInteractionListener,
    )
    val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .padding(start = 12.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 4.dp.toPx(),
                )
            },
    ) {
        MarkdownSelectableText(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(color = defaultColor),
            inlineContent = inlineContent,
        )
    }
}
