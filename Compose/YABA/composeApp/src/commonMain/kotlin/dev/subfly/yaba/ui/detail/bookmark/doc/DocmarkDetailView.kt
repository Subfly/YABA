package dev.subfly.yaba.ui.detail.bookmark.doc

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
import dev.subfly.yaba.ui.detail.bookmark.doc.layout.DocmarkContentLayout
import dev.subfly.yaba.ui.detail.bookmark.doc.layout.DocmarkDetailLayout
import dev.subfly.yaba.ui.detail.bookmark.link.components.RemindMePickerDialog
import dev.subfly.yaba.util.rememberNotificationPermissionRequester
import dev.subfly.yabacore.common.computeTriggerMillisFromDatePicker
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.state.detail.docmark.DocmarkDetailEvent
import dev.subfly.yabacore.toast.ToastIconType
import dev.subfly.yabacore.toast.ToastManager
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.notifications_disabled_message

@Composable
fun DocmarkDetailView(
    modifier: Modifier = Modifier,
    bookmarkId: String,
) {
    val vm = viewModel { DocmarkDetailVM() }
    val state by vm.state.collectAsStateWithLifecycle()
    var showRemindMePicker by remember { mutableStateOf(false) }

    val notificationPermission = rememberNotificationPermissionRequester { granted ->
        if (granted) {
            showRemindMePicker = true
        } else {
            ToastManager.show(
                message = Res.string.notifications_disabled_message,
                iconType = ToastIconType.ERROR,
            )
        }
    }

    LaunchedEffect(bookmarkId) {
        vm.onEvent(DocmarkDetailEvent.OnInit(bookmarkId = bookmarkId))
    }

    BookmarkContentDetailLayout(
        modifier = modifier,
        contentLayout = { onExpand ->
            DocmarkContentLayout(
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
            DocmarkDetailLayout(
                state = state,
                onHide = onHide,
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
    )

    if (showRemindMePicker) {
        RemindMePickerDialog(
            bookmarkKind = state.bookmark?.kind ?: BookmarkKind.FILE,
            onScheduleReminder = { selectedDateMillis, hour, minute, title, message ->
                vm.onEvent(
                    DocmarkDetailEvent.OnScheduleReminder(
                        title = title,
                        message = message,
                        triggerAtEpochMillis = computeTriggerMillisFromDatePicker(
                            selectedDateMillis = selectedDateMillis,
                            hour = hour,
                            minute = minute,
                        ),
                    ),
                )
            },
            onDismiss = { showRemindMePicker = false },
        )
    }
}
