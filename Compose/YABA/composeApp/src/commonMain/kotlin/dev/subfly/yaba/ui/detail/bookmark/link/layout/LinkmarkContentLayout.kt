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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.markdown.MarkdownPreviewView
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
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
                    state.previewDocument?.let { doc ->
                        item {
                            val openUrl = rememberUrlLauncher()
                            MarkdownPreviewView(
                                model = doc,
                                highlights = state.highlights,
                                contentPadding = PaddingValues(0.dp),
                                defaultColor = MaterialTheme.colorScheme.onSurface,
                                linkColor = MaterialTheme.colorScheme.primary,
                                openUrl = openUrl,
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }
    }
}

