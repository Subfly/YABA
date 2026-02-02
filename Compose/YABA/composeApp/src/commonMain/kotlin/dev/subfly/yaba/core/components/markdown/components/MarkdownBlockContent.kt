package dev.subfly.yaba.core.components.markdown.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.markdown.formatting.PreviewBlockUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel

@Composable
internal fun MarkdownBlockContent(
    block: PreviewBlockUiModel,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
) {
    when (block) {
        is PreviewBlockUiModel.Paragraph -> MarkdownParagraphBlock(
            block = block,
            highlights = highlights,
            defaultColor = defaultColor,
            linkColor = linkColor,
            linkInteractionListener = linkInteractionListener,
            baseStyle = MaterialTheme.typography.bodyLarge,
        )

        is PreviewBlockUiModel.Heading -> MarkdownHeadingBlock(
            block = block,
            highlights = highlights,
            defaultColor = defaultColor,
            linkColor = linkColor,
            linkInteractionListener = linkInteractionListener,
        )

        is PreviewBlockUiModel.BlockQuote -> MarkdownBlockQuoteBlock(
            block = block,
            highlights = highlights,
            defaultColor = defaultColor,
            linkColor = linkColor,
            linkInteractionListener = linkInteractionListener,
        )

        is PreviewBlockUiModel.CodeFence -> MarkdownCodeFenceBlock(block)
        is PreviewBlockUiModel.ListBlock -> MarkdownListBlock(
            block = block,
            highlights = highlights,
            defaultColor = defaultColor,
            linkColor = linkColor,
            linkInteractionListener = linkInteractionListener,
        )

        is PreviewBlockUiModel.HorizontalRule -> HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )

        is PreviewBlockUiModel.TableBlock -> MarkdownTableBlock(
            block = block,
            highlights = highlights,
            defaultColor = defaultColor,
            linkColor = linkColor,
            linkInteractionListener = linkInteractionListener,
        )

        is PreviewBlockUiModel.Image -> MarkdownImageBlock(block)
    }
}
