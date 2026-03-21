package dev.subfly.yaba.ui.detail.bookmark.note

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
import dev.subfly.yaba.ui.detail.bookmark.note.layout.NotemarkContentLayout
import dev.subfly.yaba.ui.detail.bookmark.note.layout.NotemarkDetailLayout
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yaba.util.rememberNotificationPermissionRequester
import dev.subfly.yabacore.common.computeTriggerMillisFromDatePicker
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailEvent
import dev.subfly.yabacore.toast.ToastIconType
import dev.subfly.yabacore.toast.ToastManager
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.notifications_disabled_message

@Composable
fun NotemarkDetailView(
    modifier: Modifier = Modifier,
    bookmarkId: String,
) {
    val vm = viewModel { NotemarkDetailVM() }
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

    val userPreferences = LocalUserPreferences.current

    LaunchedEffect(bookmarkId) {
        vm.onEvent(NotemarkDetailEvent.OnInit(bookmarkId = bookmarkId))
    }

    LaunchedEffect(userPreferences.preferredNoteSaveMode) {
        vm.onEvent(NotemarkDetailEvent.OnNoteSaveModeChanged(userPreferences.preferredNoteSaveMode))
    }

    BookmarkContentDetailLayout(
        modifier = modifier,
        contentLayout = { onExpand ->
            NotemarkContentLayout(
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
            NotemarkDetailLayout(
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
            bookmarkKind = state.bookmark?.kind ?: BookmarkKind.NOTE,
            onScheduleReminder = { selectedDateMillis, hour, minute, title, message ->
                vm.onEvent(
                    NotemarkDetailEvent.OnScheduleReminder(
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
