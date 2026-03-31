@file:OptIn(ExperimentalMaterial3Api::class)

package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.subfly.yaba.R
import dev.subfly.yaba.util.formatDateTime
import dev.subfly.yaba.core.common.computeTriggerMillisFromDatePicker
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.toast.ToastIconType
import dev.subfly.yaba.core.toast.ToastManager
import kotlin.time.Clock
import kotlin.time.Instant

private val notificationTitlesByKind: Map<BookmarkKind, List<Int>> = mapOf(
    BookmarkKind.LINK to listOf(
        R.string.notification_title_1,
        R.string.notification_title_2,
        R.string.notification_title_3,
        R.string.notification_title_4,
        R.string.notification_title_5,
    ),
    BookmarkKind.NOTE to listOf(
        R.string.notification_title_1,
        R.string.notification_title_2,
        R.string.notification_title_3,
        R.string.notification_title_4,
        R.string.notification_title_5,
    ),
    BookmarkKind.IMAGE to listOf(
        R.string.notification_title_1,
        R.string.notification_title_2,
        R.string.notification_title_3,
        R.string.notification_title_4,
        R.string.notification_title_5,
    ),
    BookmarkKind.FILE to listOf(
        R.string.notification_title_1,
        R.string.notification_title_2,
        R.string.notification_title_3,
        R.string.notification_title_4,
        R.string.notification_title_5,
    ),
)

private val notificationMessagesByKind: Map<BookmarkKind, List<Int>> = mapOf(
    BookmarkKind.LINK to listOf(
        R.string.notification_message_1,
        R.string.notification_message_2,
        R.string.notification_message_3,
        R.string.notification_message_4,
        R.string.notification_message_5,
    ),
    BookmarkKind.NOTE to listOf(
        R.string.notification_message_1,
        R.string.notification_message_2,
        R.string.notification_message_3,
        R.string.notification_message_4,
        R.string.notification_message_5,
    ),
    BookmarkKind.IMAGE to listOf(
        R.string.notification_message_1,
        R.string.notification_message_2,
        R.string.notification_message_3,
        R.string.notification_message_4,
        R.string.notification_message_5,
    ),
    BookmarkKind.FILE to listOf(
        R.string.notification_message_1,
        R.string.notification_message_2,
        R.string.notification_message_3,
        R.string.notification_message_4,
        R.string.notification_message_5,
    ),
)

private const val STEP_DATE = 0
private const val STEP_TIME = 1

/**
 * Two-step reminder picker: first selects a date, then selects a time.
 * Permission is verified by the caller before this dialog is shown.
 * Invokes [onScheduleReminder] with a randomly chosen title/message from the pool matching [bookmarkKind].
 */
@Composable
internal fun RemindMePickerDialog(
    bookmarkKind: BookmarkKind,
    onScheduleReminder: (
        selectedDateMillis: Long,
        hour: Int,
        minute: Int,
        title: String,
        message: String,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableIntStateOf(STEP_DATE) }
    val context = LocalContext.current

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(
                utcTimeMillis: Long,
            ): Boolean {
                val now = Clock.System.now().toEpochMilliseconds()
                val todayStartUtc = (now / 86_400_000L) * 86_400_000L
                return utcTimeMillis >= todayStartUtc
            }
        },
    )

    val timePickerState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
    )

    when (step) {
        STEP_DATE -> {
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        enabled = datePickerState.selectedDateMillis != null,
                        onClick = { step = STEP_TIME },
                    ) { Text(stringResource(R.string.done)) }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            ) {
                DatePicker(
                    state = datePickerState,
                    title = {
                        Text(
                            text = stringResource(R.string.setup_reminder_title),
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        )
                    },
                )
            }
        }

        STEP_TIME -> {
            Dialog(onDismissRequest = onDismiss) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.setup_reminder_title),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                        )

                        TimePicker(state = timePickerState)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.cancel))
                            }
                            TextButton(
                                onClick = {
                                    val selectedDateMillis =
                                        datePickerState.selectedDateMillis ?: return@TextButton

                                    val titles = notificationTitlesByKind[bookmarkKind]
                                        ?: notificationTitlesByKind.getValue(BookmarkKind.LINK)
                                    val messages = notificationMessagesByKind[bookmarkKind]
                                        ?: notificationMessagesByKind.getValue(BookmarkKind.LINK)
                                    val res = context.resources
                                    val title = res.getString(titles.random())
                                    val message = res.getString(messages.random())

                                    onScheduleReminder(
                                        selectedDateMillis,
                                        timePickerState.hour,
                                        timePickerState.minute,
                                        title,
                                        message,
                                    )

                                    val triggerMillis = computeTriggerMillisFromDatePicker(
                                        selectedDateMillis,
                                        timePickerState.hour,
                                        timePickerState.minute,
                                    )
                                    val formattedDate = formatDateTime(
                                        Instant.fromEpochMilliseconds(triggerMillis)
                                    )
                                    ToastManager.show(
                                        message = res.getString(
                                            R.string.setup_reminder_success_message,
                                            formattedDate,
                                        ),
                                        iconType = ToastIconType.SUCCESS,
                                    )
                                    onDismiss()
                                }
                            ) { Text(stringResource(R.string.done)) }
                        }
                    }
                }
            }
        }
    }
}
