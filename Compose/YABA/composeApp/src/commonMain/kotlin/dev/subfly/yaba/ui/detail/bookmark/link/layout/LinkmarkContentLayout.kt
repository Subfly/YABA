package dev.subfly.yaba.ui.detail.bookmark.link.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yabacore.markdown.InlinePart
import dev.subfly.yabacore.markdown.MarkdownParser
import dev.subfly.yabacore.markdown.MarkdownSegment
import dev.subfly.yabacore.markdown.parseInlineMarkdown
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.image.YabaImage
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_title

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
internal fun LinkmarkContentLayout(
    modifier: Modifier = Modifier,
    state: LinkmarkDetailUIState,
    onEvent: (LinkmarkDetailEvent) -> Unit,
) {
    val navigator = LocalContentNavigator.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    title = { Text(text = stringResource(Res.string.bookmark_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = navigator::removeLastOrNull) {
                            YabaIcon(name = "arrow-left-01")
                        }
                    },
                    actions = {}
                )
                AnimatedContent(state.isLoading) { loading ->
                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .background(color = MaterialTheme.colorScheme.surface)
                        ) {
                            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    } else {
                        Box(modifier = Modifier)
                    }
                }
            }
        }
    ) { paddings ->
        SelectionContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                state.readableVersions.firstOrNull()?.let { readable ->
                    readable.title?.let { title ->
                        item {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLargeEmphasized
                            )
                        }
                    }
                    state.linkDetails?.let { linkmarkMetadata ->
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(linkmarkMetadata.domain)
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(
                                            color = Color.Gray,
                                            shape = CircleShape,
                                        )
                                )
                                readable.author?.let { author ->
                                    Text(author)
                                }
                            }
                        }
                    }
                    readable.markdown?.let { markdown ->
                        item {
                            val segments by remember(markdown) {
                                derivedStateOf { MarkdownParser.parse(markdown) }
                            }
                            val assetPathByAssetId = readable.assets.associate { it.assetId to it.absolutePath }
                            val defaultColor = MaterialTheme.colorScheme.onSurface
                            val linkColor = MaterialTheme.colorScheme.primary
                            val openUrl = rememberUrlLauncher()
                            val (annotatedString, inlineContent) = buildReadableAnnotatedString(
                                segments = segments,
                                assetPathByAssetId = assetPathByAssetId,
                                defaultColor = defaultColor,
                                linkColor = linkColor,
                                openUrl = openUrl,
                            )
                            Text(
                                text = annotatedString,
                                style = MaterialTheme.typography.bodyLarge.copy(color = defaultColor),
                                inlineContent = inlineContent,
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }
    }
}

private fun spanStyleFor(
    part: InlinePart,
    defaultColor: Color,
    linkColor: Color,
    baseStyle: SpanStyle? = null,
): SpanStyle {
    var style = baseStyle ?: SpanStyle(color = defaultColor)
    if (part.bold) style = style.copy(fontWeight = FontWeight.Bold)
    if (part.italic) style = style.copy(fontStyle = FontStyle.Italic)
    if (part.code) style = style.copy(
        fontFamily = FontFamily.Monospace,
        background = Color.Transparent,
    )
    if (part.linkUrl != null) style = style.copy(
        color = linkColor,
        textDecoration = TextDecoration.Underline,
        fontWeight = if (part.bold) FontWeight.Bold else style.fontWeight,
        fontStyle = if (part.italic) FontStyle.Italic else style.fontStyle,
    )
    return style
}

@Composable
private fun RichText(
    content: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    defaultColor: Color,
    linkColor: Color,
    openUrl: (String) -> Boolean,
) {
    val linkInteractionListener = LinkInteractionListener { link ->
        (link as? LinkAnnotation.Clickable)?.tag?.let { url -> openUrl(url) }
    }
    val annotated = remember(content, defaultColor, linkColor, openUrl) {
        buildAnnotatedString {
            for (part in parseInlineMarkdown(content)) {
                val partStyle = spanStyleFor(part, defaultColor, linkColor, null)
                part.linkUrl?.let { url ->
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = partStyle.fontWeight,
                                    fontStyle = partStyle.fontStyle,
                                ),
                            ),
                            linkInteractionListener = linkInteractionListener,
                        ),
                    ) {
                        withStyle(partStyle) { append(part.text) }
                    }
                } ?: withStyle(partStyle) { append(part.text) }
            }
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style.copy(color = defaultColor),
    )
}

@Composable
private fun buildReadableAnnotatedString(
    segments: List<MarkdownSegment>,
    assetPathByAssetId: Map<String, String?>,
    defaultColor: Color,
    linkColor: Color,
    openUrl: (String) -> Boolean,
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val typography = MaterialTheme.typography
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    var tableIndex = 0
    var quoteIndex = 0
    var listIndex = 0
    var codeIndex = 0

    val linkInteractionListener = LinkInteractionListener { link ->
        (link as? LinkAnnotation.Clickable)?.tag?.let { url -> openUrl(url) }
    }

    val annotatedString = buildAnnotatedString {
        fun appendInlineParsed(content: String, baseStyle: SpanStyle? = null) {
            for (part in parseInlineMarkdown(content)) {
                val partStyle = spanStyleFor(part, defaultColor, linkColor, baseStyle)
                part.linkUrl?.let { url ->
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = partStyle.fontWeight,
                                    fontStyle = partStyle.fontStyle,
                                ),
                            ),
                            linkInteractionListener = linkInteractionListener,
                        ),
                    ) {
                        withStyle(partStyle) { append(part.text) }
                    }
                } ?: withStyle(partStyle) { append(part.text) }
            }
        }

        val headingStyleH1 = SpanStyle(fontWeight = FontWeight.Bold, fontSize = typography.headlineMedium.fontSize)
        val headingStyleH2 = SpanStyle(fontWeight = FontWeight.Bold, fontSize = typography.headlineSmall.fontSize)
        val headingStyleOther = SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = typography.titleMedium.fontSize)

        segments.forEachIndexed { index, segment ->
            if (index > 0) append("\n\n")
            when (segment) {
                is MarkdownSegment.Text -> {
                    appendInlineParsed(segment.content)
                }
                is MarkdownSegment.Heading -> {
                    val headingStyle = when (segment.level) {
                        1 -> headingStyleH1
                        2 -> headingStyleH2
                        else -> headingStyleOther
                    }
                    appendInlineParsed(segment.content, baseStyle = headingStyle)
                }
                is MarkdownSegment.Image -> {
                    val key = "img:${segment.assetId}"
                    appendInlineContent(key, "\uFFFC")
                    val path = assetPathByAssetId[segment.assetId]
                    val alt = segment.alt
                    val caption = segment.caption
                    inlineContent[key] = InlineTextContent(
                        placeholder = Placeholder(
                            width = 20.em,
                            height = 12.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
                        )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            YabaImage(
                                filePath = path,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp, max = 420.dp),
                                contentDescription = alt,
                            )
                            caption?.let { cap ->
                                Text(
                                    text = cap,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                is MarkdownSegment.CodeBlock -> {
                    val key = "code:$codeIndex"
                    codeIndex++
                    appendInlineContent(key, "\uFFFC")
                    val codeLang = segment.language
                    val codeText = segment.text
                    val codeLineCount = codeText.lines().size.coerceAtLeast(1)
                    inlineContent[key] = InlineTextContent(
                        placeholder = Placeholder(
                            width = 40.em,
                            height = (codeLineCount * 1.8).em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .padding(12.dp),
                        ) {
                            if (codeLang != null) {
                                Text(
                                    text = codeLang,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = codeText,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                softWrap = true,
                            )
                        }
                    }
                }
                is MarkdownSegment.Table -> {
                    val key = "tbl:$tableIndex"
                    tableIndex++
                    appendInlineContent(key, "\uFFFC")
                    val tableHeader = segment.header
                    val tableRows = segment.rows
                    inlineContent[key] = InlineTextContent(
                        placeholder = Placeholder(
                            width = 30.em,
                            height = 4.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
                        )
                    ) {
                        Column(
                            modifier = Modifier
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
                                tableHeader.forEach { cell ->
                                    Text(
                                        text = cell,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            tableRows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    row.forEach { cell ->
                                        Text(
                                            text = cell,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is MarkdownSegment.Quote -> {
                    val key = "quote:$quoteIndex"
                    quoteIndex++
                    appendInlineContent(key, "\uFFFC")
                    val quoteContent = segment.content
                    inlineContent[key] = InlineTextContent(
                        placeholder = Placeholder(
                            width = 30.em,
                            height = 3.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
                        )
                    ) {
                        val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        Box(
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
                            RichText(
                                content = quoteContent,
                                style = MaterialTheme.typography.bodyMedium,
                                defaultColor = defaultColor,
                                linkColor = linkColor,
                                openUrl = openUrl,
                            )
                        }
                    }
                }
                is MarkdownSegment.ListBlock -> {
                    val key = "list:$listIndex"
                    listIndex++
                    appendInlineContent(key, "\uFFFC")
                    val listOrdered = segment.ordered
                    val listItems = segment.items
                    val listPlaceholderHeight = (listItems.size * 5).em
                    inlineContent[key] = InlineTextContent(
                        placeholder = Placeholder(
                            width = 30.em,
                            height = listPlaceholderHeight,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = if (listOrdered) "${index + 1}." else "•",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = defaultColor,
                                    )
                                    RichText(
                                        content = item,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge,
                                        defaultColor = defaultColor,
                                        linkColor = linkColor,
                                        openUrl = openUrl,
                                    )
                                }
                            }
                        }
                    }
                }
                is MarkdownSegment.Divider -> {
                    append("\n")
                }
            }
        }
    }

    return annotatedString to inlineContent
}
