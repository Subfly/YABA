package dev.subfly.yaba.ui.detail.bookmark.link.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.webview.YabaWebViewViewer
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.webview.WebComponentUris
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
    onShowDetail: () -> Unit,
    onEvent: (LinkmarkDetailEvent) -> Unit,
) {
    val navigator = LocalContentNavigator.current
    val openUrl = rememberUrlLauncher()

    var isMenuExpanded by remember { mutableStateOf(false) }

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
                        IconButton(
                            onClick = onShowDetail,
                            shapes = IconButtonDefaults.shapes(),
                            content = { YabaIcon(name = "information-circle") }
                        )
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                            IconButton(
                                onClick = { isMenuExpanded = !isMenuExpanded },
                                shapes = IconButtonDefaults.shapes(),
                            ) { YabaIcon(name = "more-horizontal-circle-02") }
                        }
                    }
                )
                AnimatedContent(state.isLoading) { loading ->
                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .background(color = MaterialTheme.colorScheme.surface)
                        ) { LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    ) { paddings ->
        YabaWebViewViewer(
            modifier = Modifier.fillMaxSize().padding(paddings),
            baseUrl = WebComponentUris.getViewerUri(),
            markdown = state.readableMarkdown ?: "",
            assetsBaseUrl = state.assetsBaseUrl,
            onUrlClick = openUrl,
        )
    }
}

