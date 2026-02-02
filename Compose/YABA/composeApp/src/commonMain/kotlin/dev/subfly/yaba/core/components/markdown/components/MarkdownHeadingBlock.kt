package dev.subfly.yaba.core.components.markdown.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.LinkInteractionListener
import dev.subfly.yaba.core.components.markdown.MarkdownSelectableText
import dev.subfly.yabacore.markdown.formatting.PreviewBlockUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel

@Composable
internal fun MarkdownHeadingBlock(
    block: PreviewBlockUiModel.Heading,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
) {
    val typography = MaterialTheme.typography
    val baseStyle = when (block.level) {
        1 -> typography.headlineMedium
        2 -> typography.headlineSmall
        else -> typography.titleMedium
    }.copy(fontWeight = FontWeight.Bold)
    val (annotated, inlineContent) = buildBlockAnnotatedString(
        runs = block.inlineRuns,
        blockRange = block.range,
        blockSectionKey = block.sectionKey,
        highlights = highlights,
        defaultColor = defaultColor,
        linkColor = linkColor,
        linkInteractionListener = linkInteractionListener,
    )
    MarkdownSelectableText(
        text = annotated,
        style = baseStyle.copy(color = defaultColor),
        inlineContent = inlineContent,
    )
}
