package dev.subfly.yabacore.common

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Converts the DatePicker's selected UTC date and TimePicker's local hour/minute
 * into a single epoch millis for the user's local timezone.
 *
 * DatePicker returns [selectedDateMillis] in UTC (midnight of the selected day).
 * TimePicker provides [hour] and [minute] in the user's local timezone.
 * This combines them to produce the final trigger timestamp.
 */
fun computeTriggerMillisFromDatePicker(
    selectedDateMillis: Long,
    hour: Int,
    minute: Int,
): Long {
    val utcInstant = Instant.fromEpochMilliseconds(selectedDateMillis)
    val utcDate = utcInstant.toLocalDateTime(TimeZone.UTC).date

    val localDateTime = LocalDateTime(
        year = utcDate.year,
        month = utcDate.month,
        day = utcDate.day,
        hour = hour,
        minute = minute,
        second = 0,
        nanosecond = 0
    )
    return localDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}
