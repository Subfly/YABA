package dev.subfly.yaba.core.components.markdown

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle

/**
 * Reusable selectable text. Wraps [Text] with [SelectionContainer] so that
 * selection works correctly when used inside LazyColumn (wrap leaf text, not the whole list).
 */
@Composable
fun MarkdownSelectableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    SelectionContainer {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            inlineContent = inlineContent,
        )
    }
}
