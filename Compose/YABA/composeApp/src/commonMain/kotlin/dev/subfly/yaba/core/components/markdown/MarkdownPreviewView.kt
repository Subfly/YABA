package dev.subfly.yaba.core.components.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.subfly.yabacore.markdown.core.Range
import dev.subfly.yabacore.markdown.core.SectionKey
import dev.subfly.yabacore.markdown.formatting.InlineRunUiModel
import dev.subfly.yabacore.markdown.formatting.PreviewBlockUiModel
import dev.subfly.yabacore.markdown.formatting.PreviewDocumentUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.image.YabaImage

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

@Composable
private fun MarkdownBlockContent(
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
            modifier = Modifier.padding(
                vertical = 8.dp
            )
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

@Composable
private fun MarkdownParagraphBlock(
    block: PreviewBlockUiModel.Paragraph,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
    baseStyle: androidx.compose.ui.text.TextStyle,
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

@Composable
private fun MarkdownHeadingBlock(
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

@Composable
private fun MarkdownBlockQuoteBlock(
    block: PreviewBlockUiModel.BlockQuote,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
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
    val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .padding(start = 12.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 4.dp.toPx(),
                )
            },
    ) {
        MarkdownSelectableText(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(color = defaultColor),
            inlineContent = inlineContent,
        )
    }
}

@Composable
private fun MarkdownCodeFenceBlock(block: PreviewBlockUiModel.CodeFence) {
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
            text = block.text,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun MarkdownListBlock(
    block: PreviewBlockUiModel.ListBlock,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        block.items.forEachIndexed { index, runs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (block.ordered) "${index + 1}." else "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = defaultColor,
                )
                val (annotated, inlineContent) = buildBlockAnnotatedString(
                    runs = runs,
                    blockRange = block.range,
                    blockSectionKey = block.sectionKey,
                    highlights = highlights,
                    defaultColor = defaultColor,
                    linkColor = linkColor,
                    linkInteractionListener = linkInteractionListener,
                )
                MarkdownSelectableText(
                    text = annotated,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge.copy(color = defaultColor),
                    inlineContent = inlineContent,
                )
            }
        }
    }
}

@Composable
private fun MarkdownTableBlock(
    block: PreviewBlockUiModel.TableBlock,
    highlights: List<HighlightUiModel>,
    defaultColor: Color,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
            )
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            block.header.forEach { cellRuns ->
                val (annotated, inlineContent) = buildBlockAnnotatedString(
                    runs = cellRuns,
                    blockRange = block.range,
                    blockSectionKey = block.sectionKey,
                    highlights = highlights,
                    defaultColor = defaultColor,
                    linkColor = linkColor,
                    linkInteractionListener = linkInteractionListener,
                )
                MarkdownSelectableText(
                    text = annotated,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    inlineContent = inlineContent,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        block.rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { cellRuns ->
                    val (annotated, inlineContent) = buildBlockAnnotatedString(
                        runs = cellRuns,
                        blockRange = block.range,
                        blockSectionKey = block.sectionKey,
                        highlights = highlights,
                        defaultColor = defaultColor,
                        linkColor = linkColor,
                        linkInteractionListener = linkInteractionListener,
                    )
                    MarkdownSelectableText(
                        text = annotated,
                        style = MaterialTheme.typography.bodySmall,
                        inlineContent = inlineContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownImageBlock(block: PreviewBlockUiModel.Image) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        YabaImage(
            filePath = block.path,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 420.dp),
            contentDescription = block.alt,
        )
        block.caption?.let { cap ->
            Text(
                text = cap,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun buildBlockAnnotatedString(
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

/**
 * Returns (startInBlock, endInBlock) for a highlight that touches this block, or null if it doesn't.
 */
private fun highlightRangeInBlock(
    h: HighlightUiModel,
    blockSectionKey: String,
    blockRange: Range,
): Pair<Int, Int>? {
    val blockIndex = SectionKey.parseBlockIndex(blockSectionKey) ?: return null
    val startBlockIndex = SectionKey.parseBlockIndex(h.startSectionKey) ?: return null
    val endBlockIndex = SectionKey.parseBlockIndex(h.endSectionKey) ?: return null
    if (blockIndex !in startBlockIndex..endBlockIndex) return null
    val startInBlock = if (blockIndex == startBlockIndex) h.startOffsetInSection else 0
    val endInBlock = if (blockIndex == endBlockIndex) h.endOffsetInSection else blockRange.length
    return startInBlock to endInBlock
}
