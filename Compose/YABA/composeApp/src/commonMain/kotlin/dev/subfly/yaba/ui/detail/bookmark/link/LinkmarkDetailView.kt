package dev.subfly.yaba.ui.detail.bookmark.link

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkContentDetailLayout
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkContentLayout
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailLayout
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent

@Composable
fun LinkmarkDetailView(
    modifier: Modifier = Modifier,
    bookmarkId: String,
) {
    val vm = viewModel { LinkmarkDetailVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(bookmarkId) {
        vm.onEvent(LinkmarkDetailEvent.OnInit(bookmarkId = bookmarkId))
    }

    BookmarkContentDetailLayout(
        modifier = modifier,
        contentLayout = {
            LinkmarkContentLayout(
                state = state,
                onEvent = vm::onEvent,
            )
        },
        detailLayout = {
            LinkmarkDetailLayout(
                state = state,
                onEvent = vm::onEvent,
            )
        }
    )
}
