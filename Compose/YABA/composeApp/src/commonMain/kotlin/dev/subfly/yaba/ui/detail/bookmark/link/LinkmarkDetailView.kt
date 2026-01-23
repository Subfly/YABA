package dev.subfly.yaba.ui.detail.bookmark.link

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkContentDetailLayout
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkContentLayout
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkDetailLayout

@Composable
fun LinkmarkDetailView(
    modifier: Modifier = Modifier,
    bookmarkId: String,
) {
    BookmarkContentDetailLayout(
        modifier = modifier,
        contentLayout = {
            LinkmarkContentLayout()
        },
        detailLayout = {
            LinkmarkDetailLayout()
        }
    )
}
