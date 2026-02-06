package dev.subfly.yaba.core.components.markdown.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.markdown.util.buildCodeAnnotatedString
import dev.subfly.yaba.core.components.markdown.util.codeTheme
import dev.subfly.yabacore.markdown.codehighlight.DefaultCodeHighlighter
import dev.subfly.yabacore.markdown.formatting.PreviewBlockUiModel

@Composable
internal fun MarkdownCodeFenceBlock(block: PreviewBlockUiModel.CodeFence) {
    val spans = remember(block.language, block.text) {
        DefaultCodeHighlighter.highlight(block.language, block.text)
    }
    val theme = codeTheme()
    val annotated = buildCodeAnnotatedString(block.text, spans, theme)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(12.dp),
    ) {
        block.language?.let { lang ->
            Text(
                text = lang,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = annotated,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
