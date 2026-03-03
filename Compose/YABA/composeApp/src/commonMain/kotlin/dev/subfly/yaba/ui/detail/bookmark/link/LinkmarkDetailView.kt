package dev.subfly.yaba.ui.detail.bookmark.link

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkContentDetailLayout
import dev.subfly.yaba.ui.detail.bookmark.link.components.RemindMePickerDialog
import dev.subfly.yaba.ui.detail.bookmark.link.layout.LinkmarkContentLayout
import dev.subfly.yaba.ui.detail.bookmark.link.layout.LinkmarkDetailLayout
import dev.subfly.yaba.util.rememberNotificationPermissionRequester
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.toast.ToastManager
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.notifications_disabled_message

@Composable
fun LinkmarkDetailView(
    modifier: Modifier = Modifier,
    bookmarkId: String,
) {
    val vm = viewModel { LinkmarkDetailVM() }
    val state by vm.state.collectAsStateWithLifecycle()
    var showRemindMePicker by remember { mutableStateOf(false) }

    val notificationPermission = rememberNotificationPermissionRequester { granted ->
        if (granted) {
            showRemindMePicker = true
        } else {
            ToastManager.show(message = Res.string.notifications_disabled_message)
        }
    }

    LaunchedEffect(bookmarkId) {
        vm.onEvent(LinkmarkDetailEvent.OnInit(bookmarkId = bookmarkId))
    }

    BookmarkContentDetailLayout(
        modifier = modifier,
        contentLayout = { onExpand ->
            LinkmarkContentLayout(
                state = state,
                onShowDetail = onExpand,
                onEvent = vm::onEvent,
                onShowRemindMePicker = {
                    if (notificationPermission.hasPermission) {
                        showRemindMePicker = true
                    } else {
                        notificationPermission.requestPermission()
                    }
                },
            )
        },
        detailLayout = { onHide ->
            LinkmarkDetailLayout(
                state = state,
                onHide = onHide,
                onEvent = vm::onEvent,
            )
        }
    )

    if (showRemindMePicker) {
        RemindMePickerDialog(
            bookmarkKind = state.bookmark?.kind ?: BookmarkKind.LINK,
            onEvent = vm::onEvent,
            onDismiss = { showRemindMePicker = false },
        )
    }
}
