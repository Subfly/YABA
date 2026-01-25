package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.runtime.Composable
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailUIState

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun LinkmarkDetailLayout(
    state: LinkmarkDetailUIState,
    onEvent: (LinkmarkDetailEvent) -> Unit,
) {
    if (state.readableVersions.isEmpty() && state.highlights.isEmpty()) return
}
