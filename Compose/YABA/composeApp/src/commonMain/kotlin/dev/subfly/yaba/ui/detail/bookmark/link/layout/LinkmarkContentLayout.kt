package dev.subfly.yaba.ui.detail.bookmark.link.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yabacore.model.ui.ReadableBlockUiModel
import dev.subfly.yabacore.model.ui.ReadableListItemUiModel
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.image.YabaImage
import dev.subfly.yabacore.unfurl.ReadableHeadingLevel
import dev.subfly.yabacore.unfurl.ReadableInline
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
                    actions = {

                    }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings),
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
                readable.document?.let { document ->
                    items(
                        items = document.blocks,
                        key = { it.id },
                    ) { block ->
                        ReadableBlockContent(
                            block = block,
                            level = 0,
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }
}

@Composable
private fun ReadableBlockContent(
    block: ReadableBlockUiModel,
    level: Int,
) {
    when (block) {
        is ReadableBlockUiModel.Paragraph -> {
            Text(
                text = buildInlineAnnotatedString(block.inlines),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        is ReadableBlockUiModel.Heading -> {
            Text(
                text = buildInlineAnnotatedString(block.inlines),
                style = headingTextStyle(block.level),
            )
        }
        is ReadableBlockUiModel.Code -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(12.dp),
            )
        }
        is ReadableBlockUiModel.Divider -> {
            HorizontalDivider()
        }
        is ReadableBlockUiModel.Image -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                YabaImage(
                    filePath = block.assetPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 420.dp),
                    contentDescription = block.caption,
                )
                block.caption?.let { caption ->
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        is ReadableBlockUiModel.ListBlock -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.items.forEachIndexed { index, item ->
                    ReadableListItemContent(
                        item = item,
                        ordered = block.ordered,
                        index = index,
                        level = level,
                    )
                }
            }
        }
        is ReadableBlockUiModel.Quote -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                block.children.forEach { child ->
                    ReadableBlockContent(block = child, level = level + 1)
                }
            }
        }
    }
}

@Composable
private fun ReadableListItemContent(
    item: ReadableListItemUiModel,
    ordered: Boolean,
    index: Int,
    level: Int,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (ordered) "${index + 1}." else "•",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = if (level == 0) 0.dp else 2.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item.blocks.forEach { block ->
                ReadableBlockContent(block = block, level = level + 1)
            }
        }
    }
}

@Composable
private fun headingTextStyle(level: ReadableHeadingLevel) = when (level) {
    ReadableHeadingLevel.H1 -> MaterialTheme.typography.headlineLarge
    ReadableHeadingLevel.H2 -> MaterialTheme.typography.headlineMedium
    ReadableHeadingLevel.H3 -> MaterialTheme.typography.headlineSmall
    ReadableHeadingLevel.H4 -> MaterialTheme.typography.titleLarge
    ReadableHeadingLevel.H5 -> MaterialTheme.typography.titleMedium
    ReadableHeadingLevel.H6 -> MaterialTheme.typography.titleSmall
}

@Composable
private fun buildInlineAnnotatedString(inlines: List<ReadableInline>): AnnotatedString {
    val colors = MaterialTheme.colorScheme
    return buildAnnotatedString {
        inlines.forEach { inline ->
            appendInline(
                inline = inline,
                colors = colors,
            )
        }
    }
}

private fun AnnotatedString.Builder.appendInline(
    inline: ReadableInline,
    colors: ColorScheme,
) {
    when (inline) {
        is ReadableInline.Text -> append(inline.content)
        is ReadableInline.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            inline.children.forEach { child -> appendInline(child, colors) }
        }
        is ReadableInline.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            inline.children.forEach { child -> appendInline(child, colors) }
        }
        is ReadableInline.Underline -> withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
            inline.children.forEach { child -> appendInline(child, colors) }
        }
        is ReadableInline.Strikethrough -> withStyle(
            SpanStyle(textDecoration = TextDecoration.LineThrough)
        ) {
            inline.children.forEach { child -> appendInline(child, colors) }
        }
        is ReadableInline.Code -> withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = colors.surfaceVariant,
            )
        ) {
            append(inline.content)
        }
        is ReadableInline.Link -> {
            pushStringAnnotation(tag = "url", annotation = inline.href)
            withStyle(
                SpanStyle(
                    color = colors.primary,
                    textDecoration = TextDecoration.Underline,
                )
            ) {
                inline.children.forEach { child -> appendInline(child, colors) }
            }
            pop()
        }
        is ReadableInline.Color -> {
            val color = when (inline.role.lowercase()) {
                "muted" -> colors.onSurfaceVariant
                "accent" -> colors.secondary
                "info" -> colors.tertiary
                "warning" -> colors.error.copy(alpha = 0.85f)
                "success" -> colors.primary.copy(alpha = 0.85f)
                "error" -> colors.error
                else -> colors.onSurface
            }
            withStyle(SpanStyle(color = color)) {
                inline.children.forEach { child -> appendInline(child, colors) }
            }
        }
    }
}
