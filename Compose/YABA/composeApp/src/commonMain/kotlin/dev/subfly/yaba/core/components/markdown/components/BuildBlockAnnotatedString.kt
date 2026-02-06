package dev.subfly.yaba.core.components.markdown.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.subfly.yaba.core.components.markdown.util.highlightRangeInBlock
import dev.subfly.yabacore.markdown.core.Range
import dev.subfly.yabacore.markdown.formatting.InlineRunUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.image.YabaImage

@Composable
internal fun buildBlockAnnotatedString(
    runs: List<InlineRunUiModel>,
    blockRange: Range,
    blockSectionKey: String,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    var imageIndex = 0
    val annotated = buildAnnotatedString {
        for (run in runs) {
            when (run) {
                is InlineRunUiModel.Text -> {
                    var style = SpanStyle(
                        color = defaultColor,
                        fontWeight = if (run.bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (run.italic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (run.strikethrough) TextDecoration.LineThrough else null,
                        fontFamily = if (run.code) FontFamily.Monospace else FontFamily.Default,
                        baselineShift = when {
                            run.superscript -> BaselineShift.Superscript
                            run.subscript -> BaselineShift.Subscript
                            else -> null
                        },
                    )
                    if (run.linkUrl != null) {
                        style = style.copy(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                    val start = length
                    val url = run.linkUrl
                    if (url != null) {
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = style.fontWeight,
                                        fontStyle = style.fontStyle,
                                    ),
                                ),
                                linkInteractionListener = linkInteractionListener,
                            ),
                        ) {
                            withStyle(style) { append(run.text) }
                        }
                    } else {
                        withStyle(style) { append(run.text) }
                    }
                    val blockStart = blockRange.start
                    for (h in highlights) {
                        val rangeInBlock =
                            highlightRangeInBlock(h, blockSectionKey, blockRange) ?: continue
                        val (startInBlock, endInBlock) = rangeInBlock
                        val runStartInBlock = run.range.start - blockStart
                        val runEndInBlock = run.range.end - blockStart
                        val overlapStart = maxOf(startInBlock, runStartInBlock)
                        val overlapEnd = minOf(endInBlock, runEndInBlock)
                        if (overlapStart < overlapEnd) {
                            val runStart = overlapStart - runStartInBlock
                            val runEnd = overlapEnd - runStartInBlock
                            val charStart = start + runStart.coerceIn(0, run.text.length)
                            val charEnd = start + runEnd.coerceIn(0, run.text.length)
                            if (charStart < charEnd) {
                                addStyle(
                                    SpanStyle(background = Color(h.colorRole.iconTintArgb())),
                                    charStart,
                                    charEnd,
                                )
                            }
                        }
                    }
                }

                is InlineRunUiModel.Image -> {
                    val key = "img:$blockSectionKey:$imageIndex"
                    imageIndex++
                    appendInlineContent(key, "\uFFFC")
                    inlineContent[key] = InlineTextContent(
                        placeholder = Placeholder(
                            width = 20.em,
                            height = 12.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
                        ),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            YabaImage(
                                filePath = run.path,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp, max = 420.dp),
                                contentDescription = run.alt,
                            )
                            run.caption?.let { cap ->
                                Text(
                                    text = cap,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    return Pair(annotated, inlineContent)
}
