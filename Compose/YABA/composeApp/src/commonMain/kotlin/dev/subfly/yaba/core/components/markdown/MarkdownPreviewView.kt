package dev.subfly.yaba.core.components.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.markdown.components.MarkdownBlockContent
import dev.subfly.yabacore.markdown.formatting.PreviewDocumentUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel

/**
 * Renders parsed markdown as a LazyColumn of blocks keyed by sectionKey.
 * Accepts [highlights] from outside and applies highlight styling per block.
 * Leaf text is wrapped with SelectionContainer for text selection.
 */
@Composable
fun MarkdownPreviewView(
    model: PreviewDocumentUiModel,
    modifier: Modifier = Modifier,
    highlights: List<HighlightUiModel> = emptyList(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    defaultColor: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    openUrl: (String) -> Boolean = { false },
) {
    val linkInteractionListener = LinkInteractionListener { link ->
        (link as? LinkAnnotation.Clickable)?.tag?.let { url -> openUrl(url) }
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        model.blocks.forEach { block ->
            item(key = block.sectionKey) {
                MarkdownBlockContent(
                    block = block,
                    highlights = highlights,
                    defaultColor = defaultColor,
                    linkColor = linkColor,
                    linkInteractionListener = linkInteractionListener,
                )
            }
        }
    }
}
