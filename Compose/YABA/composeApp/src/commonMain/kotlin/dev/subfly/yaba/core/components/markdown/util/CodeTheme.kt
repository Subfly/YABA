package dev.subfly.yaba.core.components.markdown.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import dev.subfly.yabacore.markdown.codehighlight.CodeSpan
import dev.subfly.yabacore.markdown.codehighlight.CodeTokenType

/**
 * Maps [CodeTokenType] to [Color] using Material theme.
 * Used for syntax-highlighted code fence rendering.
 */
data class CodeTheme(
    val comment: Color,
    val string: Color,
    val keyword: Color,
    val number: Color,
    val identifier: Color,
    val operator: Color,
    val whitespace: Color,
)

@Composable
internal fun codeTheme(): CodeTheme {
    val cs = MaterialTheme.colorScheme
    return CodeTheme(
        comment = cs.onSurfaceVariant.copy(alpha = 0.7f),
        string = cs.tertiary,
        keyword = cs.primary,
        number = cs.secondary,
        identifier = cs.onSurfaceVariant,
        operator = cs.onSurfaceVariant.copy(alpha = 0.9f),
        whitespace = cs.onSurfaceVariant,
    )
}

internal fun CodeTheme.colorFor(type: CodeTokenType): Color = when (type) {
    CodeTokenType.Comment -> comment
    CodeTokenType.String -> string
    CodeTokenType.Keyword -> keyword
    CodeTokenType.Number -> number
    CodeTokenType.Identifier -> identifier
    CodeTokenType.Operator -> operator
    CodeTokenType.Whitespace -> whitespace
}

/**
 * Builds [AnnotatedString] from raw code and syntax [CodeSpan]s with [CodeTheme] colors.
 * Uses monospace font for the whole span; only color varies by token type.
 */
internal fun buildCodeAnnotatedString(
    code: String,
    spans: List<CodeSpan>,
    theme: CodeTheme,
    fontFamily: FontFamily = FontFamily.Monospace,
): AnnotatedString {
    if (code.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        for (span in spans) {
            val slice = code.substring(span.start, span.end)
            val color = theme.colorFor(span.type)
            withStyle(SpanStyle(color = color, fontFamily = fontFamily)) {
                append(slice)
            }
        }
    }
}
