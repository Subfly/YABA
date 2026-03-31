package dev.subfly.yaba.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Instant

/**
 * Formats an Instant to a readable date and time string.
 * Format: "Jan 25, 2026 at 4:16 PM"
 */
fun formatDateTime(instant: Instant): String {
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    val date = Date(instant.toEpochMilliseconds())
    return dateFormat.format(date)
}

/**
 * Parses ISO-8601-style timestamps from scraped metadata (e.g. article dates) and formats them like
 * [formatDateTime]. If parsing fails, returns [raw] trimmed.
 */
fun formatExtractedMetadataDate(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return trimmed
    val instant = runCatching { Instant.parse(trimmed) }.getOrNull()
    return if (instant != null) formatDateTime(instant) else trimmed
}
