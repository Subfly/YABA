package dev.subfly.yaba.ui.detail.bookmark.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.core.managers.LinkmarkManager
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.CanvmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.DocmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.ImagemarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.LinkmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkCreationRoute
import dev.subfly.yaba.core.navigation.main.CanvasDetailRoute
import dev.subfly.yaba.core.navigation.main.DocDetailRoute
import dev.subfly.yaba.core.navigation.main.ImageDetailRoute
import dev.subfly.yaba.core.navigation.main.LinkDetailRoute
import dev.subfly.yaba.core.navigation.main.NoteDetailRoute
import dev.subfly.yaba.core.state.detail.canvmark.CanvmarkDetailEvent
import dev.subfly.yaba.ui.detail.bookmark.canvas.layout.CanvmarkContentLayout
import dev.subfly.yaba.ui.detail.bookmark.canvas.layout.CanvmarkDetailLayout
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkContentDetailLayout
import dev.subfly.yaba.util.BookmarkPrivatePasswordEventEffect
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.rememberShareHandler
import kotlinx.coroutines.launch

@Composable
fun CanvmarkDetailView(bookmarkId: String) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val deletionDialogManager = LocalDeletionDialogManager.current

    val vm = viewModel { CanvmarkDetailVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    val shareUrl = rememberShareHandler()
    val shareScope = rememberCoroutineScope()

    BookmarkPrivatePasswordEventEffect(
        resolveBookmark = { id -> state.bookmark?.takeIf { it.id == id } },
        onOpenBookmark = { model ->
            navigator.add(
                when (model.kind) {
                    BookmarkKind.LINK -> LinkDetailRoute(bookmarkId = model.id)
                    BookmarkKind.NOTE -> NoteDetailRoute(bookmarkId = model.id)
                    BookmarkKind.IMAGE -> ImageDetailRoute(bookmarkId = model.id)
                    BookmarkKind.FILE -> DocDetailRoute(bookmarkId = model.id)
                    BookmarkKind.CANVAS -> CanvasDetailRoute(bookmarkId = model.id)
                },
            )
        },
        onEditBookmark = { model ->
            when (model.kind) {
                BookmarkKind.LINK -> creationNavigator.add(LinkmarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.NOTE -> creationNavigator.add(NotemarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.IMAGE -> creationNavigator.add(ImagemarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.FILE -> creationNavigator.add(DocmarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.CANVAS -> creationNavigator.add(CanvmarkCreationRoute(bookmarkId = model.id))
            }
            appStateManager.onShowCreationContent()
        },
        onShareBookmark = { bookmark ->
            when (bookmark.kind) {
                BookmarkKind.LINK -> shareScope.launch {
                    LinkmarkManager.getBookmarkUrl(bookmark.id)?.let(shareUrl)
                }
                BookmarkKind.IMAGE -> shareScope.launch {
                    YabaFileAccessor.shareImageBookmark(bookmark.id)
                }
                else -> {}
            }
        },
        onDeleteBookmark = { bookmark ->
            deletionDialogManager.send(
                DeletionState(
                    deletionType = DeletionType.BOOKMARK,
                    bookmarkToBeDeleted = bookmark,
                    onConfirm = { vm.onEvent(CanvmarkDetailEvent.OnDeleteBookmark) },
                ),
            )
        },
    )

    LaunchedEffect(bookmarkId) {
        vm.onEvent(CanvmarkDetailEvent.OnInit(bookmarkId))
    }

    BookmarkContentDetailLayout(
        contentLayout = { onExpand ->
            CanvmarkContentLayout(
                state = state,
                onShowDetail = onExpand,
                onEvent = vm::onEvent,
            )
        },
        detailLayout = { onHide ->
            CanvmarkDetailLayout(
                state = state,
                onHide = onHide,
            )
        },
    )
}
