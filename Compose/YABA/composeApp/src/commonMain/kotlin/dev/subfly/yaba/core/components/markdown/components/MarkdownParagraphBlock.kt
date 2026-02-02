package dev.subfly.yaba.core.components.markdown.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.TextStyle
import dev.subfly.yaba.core.components.markdown.MarkdownSelectableText
import dev.subfly.yabacore.markdown.formatting.PreviewBlockUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel

@Composable
internal fun MarkdownParagraphBlock(
    block: PreviewBlockUiModel.Paragraph,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
    baseStyle: TextStyle,
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
    MarkdownSelectableText(
        text = annotated,
        style = baseStyle.copy(color = defaultColor),
        inlineContent = inlineContent,
    )
}
