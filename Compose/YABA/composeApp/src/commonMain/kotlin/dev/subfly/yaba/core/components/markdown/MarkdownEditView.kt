package dev.subfly.yaba.core.components.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.markdown.formatting.EditorSpanStyle
import dev.subfly.yabacore.markdown.formatting.EditorStyleSpan

/**
 * Full-screen basic text field with syntax-aware styling via spans.
 * No parsing logic inside the component: receives [text], [spans], and [onTextChange].
 * Spans are applied as visual styling only (no layout changes).
 */
@Composable
fun MarkdownEditView(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    spans: List<EditorStyleSpan> = emptyList(),
    singleLine: Boolean = false,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    cursorColor: Color = MaterialTheme.colorScheme.primary,
) {
    val defaultColor = textStyle.color
    val transformation = EditorVisualTransformation(spans, defaultColor)
    SelectionContainer {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface),
            textStyle = textStyle,
            cursorBrush = SolidColor(cursorColor),
            singleLine = singleLine,
            readOnly = readOnly,
            visualTransformation = transformation,
            decorationBox = { innerTextField ->
                innerTextField()
            },
        )
    }
}

/**
 * Visual transformation that displays the text with [spans] applied as styles.
 * Builds AnnotatedString from current text and spans inside filter() so it stays in sync.
 */
private class EditorVisualTransformation(
    private val spans: List<EditorStyleSpan>,
    private val defaultColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotated = buildEditorAnnotatedString(text.text, spans, defaultColor)
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

private fun buildEditorAnnotatedString(
    text: String,
    spans: List<EditorStyleSpan>,
    defaultColor: Color,
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        append(text)
        val len = text.length
        for (span in spans) {
            val start = span.range.start.coerceIn(0, len)
            val end = span.range.end.coerceIn(0, len)
            if (start < end) {
                addStyle(spanStyleFor(span.style, defaultColor), start, end)
            }
        }
    }
}

private fun spanStyleFor(style: EditorSpanStyle, defaultColor: Color): SpanStyle = when (style) {
    EditorSpanStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)
    EditorSpanStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor)
    EditorSpanStyle.STRIKETHROUGH -> SpanStyle(
        textDecoration = TextDecoration.LineThrough,
        color = defaultColor
    )

    EditorSpanStyle.CODE -> SpanStyle(fontFamily = FontFamily.Monospace, color = defaultColor)
    EditorSpanStyle.LINK -> SpanStyle(
        color = defaultColor,
        textDecoration = TextDecoration.Underline
    )

    EditorSpanStyle.HEADING -> SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)
}
