package dev.subfly.yaba.core.components.markdown.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.markdown.MarkdownSelectableText
import dev.subfly.yabacore.markdown.formatting.PreviewBlockUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel

@Composable
internal fun MarkdownDefinitionListBlock(
    block: PreviewBlockUiModel.DefinitionList,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        block.items.forEach { item ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val (termAnnotated, termInlineContent) = buildBlockAnnotatedString(
                    runs = item.term,
                    blockRange = block.range,
                    blockSectionKey = block.sectionKey,
                    highlights = highlights,
                    defaultColor = defaultColor,
                    linkColor = linkColor,
                    linkInteractionListener = linkInteractionListener,
                )
                MarkdownSelectableText(
                    text = termAnnotated,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = defaultColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    ),
                    inlineContent = termInlineContent,
                )
                item.definitions.forEach { defRuns ->
                    val (defAnnotated, defInlineContent) = buildBlockAnnotatedString(
                        runs = defRuns,
                        blockRange = block.range,
                        blockSectionKey = block.sectionKey,
                        highlights = highlights,
                        defaultColor = defaultColor,
                        linkColor = linkColor,
                        linkInteractionListener = linkInteractionListener,
                    )
                    MarkdownSelectableText(
                        text = defAnnotated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(color = defaultColor),
                        inlineContent = defInlineContent,
                    )
                }
            }
        }
    }
}
