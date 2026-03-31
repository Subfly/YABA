package dev.subfly.yaba.ui.detail.bookmark.link

import dev.subfly.yaba.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkContentDetailLayout
import dev.subfly.yaba.ui.detail.bookmark.link.components.RemindMePickerDialog
import dev.subfly.yaba.ui.detail.bookmark.link.layout.LinkmarkContentLayout
import dev.subfly.yaba.ui.detail.bookmark.link.layout.LinkmarkDetailLayout
import dev.subfly.yaba.util.rememberNotificationPermissionRequester
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yaba.core.toast.ToastIconType
import dev.subfly.yaba.core.toast.ToastManager

@Composable
fun LinkmarkDetailView(
    modifier: Modifier = Modifier,
    bookmarkId: String,
) {
    val vm = viewModel { LinkmarkDetailVM() }
    val state by vm.state.collectAsStateWithLifecycle()
    var showRemindMePicker by remember { mutableStateOf(false) }

    val toastDisabledMessage = stringResource(R.string.notifications_disabled_message)

    val notificationPermission = rememberNotificationPermissionRequester { granted ->
        if (granted) {
            showRemindMePicker = true
        } else {
            ToastManager.show(
                message = toastDisabledMessage,
                iconType = ToastIconType.ERROR,
            )
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
            onScheduleReminder = { selectedDateMillis, hour, minute, title, message ->
                vm.onEvent(
                    LinkmarkDetailEvent.OnScheduleReminder(
                        selectedDateMillis = selectedDateMillis,
                        hour = hour,
                        minute = minute,
                        title = title,
                        message = message,
                    ),
                )
            },
            onDismiss = { showRemindMePicker = false },
        )
    }
}
