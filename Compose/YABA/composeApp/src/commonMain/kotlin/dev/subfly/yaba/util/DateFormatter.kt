package dev.subfly.yaba.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Instant

/**
 * Formats an Instant to a readable date and time string.
 * Format: "Jan 25, 2026 at 4:16"
 */
fun formatDateTime(instant: Instant): String {
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    val date = Date(instant.toEpochMilliseconds())
    return dateFormat.format(date)
}
