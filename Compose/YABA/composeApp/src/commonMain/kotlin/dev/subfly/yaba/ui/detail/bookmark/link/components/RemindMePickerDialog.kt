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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.subfly.yaba.util.formatDateTime
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.toast.ToastIconType
import dev.subfly.yabacore.toast.ToastManager
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.notification_message_1
import yaba.composeapp.generated.resources.notification_message_2
import yaba.composeapp.generated.resources.notification_message_3
import yaba.composeapp.generated.resources.notification_message_4
import yaba.composeapp.generated.resources.notification_message_5
import yaba.composeapp.generated.resources.notification_title_1
import yaba.composeapp.generated.resources.notification_title_2
import yaba.composeapp.generated.resources.notification_title_3
import yaba.composeapp.generated.resources.notification_title_4
import yaba.composeapp.generated.resources.notification_title_5
import yaba.composeapp.generated.resources.setup_reminder_success_message
import yaba.composeapp.generated.resources.setup_reminder_title
import java.util.Calendar
import java.util.TimeZone
import kotlin.time.Clock

private val notificationTitlesByKind: Map<BookmarkKind, List<StringResource>> = mapOf(
    BookmarkKind.LINK to listOf(
        Res.string.notification_title_1,
        Res.string.notification_title_2,
        Res.string.notification_title_3,
        Res.string.notification_title_4,
        Res.string.notification_title_5,
    ),
    BookmarkKind.NOTE to listOf(
        Res.string.notification_title_1,
        Res.string.notification_title_2,
        Res.string.notification_title_3,
        Res.string.notification_title_4,
        Res.string.notification_title_5,
    ),
    BookmarkKind.IMAGE to listOf(
        Res.string.notification_title_1,
        Res.string.notification_title_2,
        Res.string.notification_title_3,
        Res.string.notification_title_4,
        Res.string.notification_title_5,
    ),
    BookmarkKind.FILE to listOf(
        Res.string.notification_title_1,
        Res.string.notification_title_2,
        Res.string.notification_title_3,
        Res.string.notification_title_4,
        Res.string.notification_title_5,
    ),
)

private val notificationMessagesByKind: Map<BookmarkKind, List<StringResource>> = mapOf(
    BookmarkKind.LINK to listOf(
        Res.string.notification_message_1,
        Res.string.notification_message_2,
        Res.string.notification_message_3,
        Res.string.notification_message_4,
        Res.string.notification_message_5,
    ),
    BookmarkKind.NOTE to listOf(
        Res.string.notification_message_1,
        Res.string.notification_message_2,
        Res.string.notification_message_3,
        Res.string.notification_message_4,
        Res.string.notification_message_5,
    ),
    BookmarkKind.IMAGE to listOf(
        Res.string.notification_message_1,
        Res.string.notification_message_2,
        Res.string.notification_message_3,
        Res.string.notification_message_4,
        Res.string.notification_message_5,
    ),
    BookmarkKind.FILE to listOf(
        Res.string.notification_message_1,
        Res.string.notification_message_2,
        Res.string.notification_message_3,
        Res.string.notification_message_4,
        Res.string.notification_message_5,
    ),
)

private const val STEP_DATE = 0
private const val STEP_TIME = 1

/**
 * Two-step reminder picker: first selects a date, then selects a time.
 * Permission is verified by the caller before this dialog is shown.
 * Dispatches [LinkmarkDetailEvent.OnScheduleReminder] with a randomly chosen title/message
 * selected from the pool matching [bookmarkKind].
 */
@Composable
internal fun RemindMePickerDialog(
    bookmarkKind: BookmarkKind,
    onEvent: (LinkmarkDetailEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableIntStateOf(STEP_DATE) }

    val now = remember { Clock.System.now().toEpochMilliseconds() }
    val todayStartUtc = remember { (now / 86_400_000L) * 86_400_000L }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = now,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= todayStartUtc
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
                    ) { Text(stringResource(Res.string.done)) }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.cancel))
                    }
                },
            ) {
                DatePicker(
                    state = datePickerState,
                    title = {
                        Text(
                            text = stringResource(Res.string.setup_reminder_title),
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
                            text = stringResource(Res.string.setup_reminder_title),
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
                                Text(stringResource(Res.string.cancel))
                            }
                            TextButton(onClick = {
                                val selectedDateMillis = datePickerState.selectedDateMillis ?: return@TextButton

                                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                utcCal.timeInMillis = selectedDateMillis
                                val year = utcCal.get(Calendar.YEAR)
                                val month = utcCal.get(Calendar.MONTH)
                                val day = utcCal.get(Calendar.DAY_OF_MONTH)

                                val localCal = Calendar.getInstance()
                                localCal.set(year, month, day, timePickerState.hour, timePickerState.minute, 0)
                                localCal.set(Calendar.MILLISECOND, 0)
                                val triggerMillis = localCal.timeInMillis

                                val titles = notificationTitlesByKind[bookmarkKind]
                                    ?: notificationTitlesByKind.getValue(BookmarkKind.LINK)
                                val messages = notificationMessagesByKind[bookmarkKind]
                                    ?: notificationMessagesByKind.getValue(BookmarkKind.LINK)

                                onEvent(
                                    LinkmarkDetailEvent.OnScheduleReminder(
                                        title = titles.random(),
                                        message = messages.random(),
                                        triggerDateEpochMillis = triggerMillis,
                                    )
                                )

                                val formattedDate = formatDateTime(
                                    Instant.fromEpochMilliseconds(triggerMillis)
                                )
                                ToastManager.show(
                                    message = Res.string.setup_reminder_success_message,
                                    messageFormatArgs = listOf(formattedDate),
                                    iconType = ToastIconType.SUCCESS,
                                )
                                onDismiss()
                            }) {
                                Text(stringResource(Res.string.done))
                            }
                        }
                    }
                }
            }
        }
    }
}
