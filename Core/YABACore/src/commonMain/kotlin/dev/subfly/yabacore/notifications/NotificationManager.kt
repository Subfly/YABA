package dev.subfly.yabacore.notifications

/**
 * Centralized, stateless notification manager for scheduling and canceling
 * bookmark reminder notifications across all platforms.
 *
 * Delegates to platform-specific [expect] functions declared in
 * [NotificationPlatformHandler.kt]. Android and iOS provide real implementations;
 * JVM is a no-op until a JVM UI layer provides notifications.
 */
object NotificationManager {

    /**
     * Schedules a one-time reminder notification for the given bookmark.
     *
     * @param bookmarkId  Used as the notification identifier (one reminder per bookmark).
     * @param bookmarkKindCode  [dev.subfly.yabacore.model.utils.BookmarkKind.code] so the
     *                          notification tap can deep-link to the correct detail view.
     * @param title       Platform-specific localized title (no format args needed).
     * @param message     Platform-specific localized message template (contains a
     *                    placeholder for [bookmarkLabel], e.g. `%s` / `%@`).
     * @param bookmarkLabel  Plain bookmark name substituted into [message].
     * @param triggerDateEpochMillis  Fire date as UTC epoch milliseconds.
     */
    suspend fun scheduleReminder(
        bookmarkId: String,
        bookmarkKindCode: Int,
        title: PlatformNotificationText,
        message: PlatformNotificationText,
        bookmarkLabel: String,
        triggerDateEpochMillis: Long,
    ) {
        platformScheduleReminder(
            bookmarkId = bookmarkId,
            bookmarkKindCode = bookmarkKindCode,
            title = title,
            message = message,
            bookmarkLabel = bookmarkLabel,
            triggerDateEpochMillis = triggerDateEpochMillis,
        )
    }

    /**
     * Cancels the pending reminder for a single bookmark.
     */
    suspend fun cancelReminder(bookmarkId: String) {
        platformCancelReminder(bookmarkId)
    }

    /**
     * Cancels pending reminders for multiple bookmarks at once.
     */
    suspend fun cancelReminders(bookmarkIds: List<String>) {
        if (bookmarkIds.isEmpty()) return
        platformCancelReminders(bookmarkIds)
    }

    /**
     * Cancels every pending reminder (used during data wipe).
     */
    suspend fun cancelAllReminders() {
        platformCancelAllReminders()
    }

    /**
     * Returns the scheduled fire date (epoch millis) of the pending reminder
     * for [bookmarkId], or `null` if no reminder is pending.
     */
    suspend fun getPendingReminderDate(bookmarkId: String): Long? =
        platformGetPendingReminderDate(bookmarkId)

    /**
     * Requests notification permission from the user.
     *
     * @return `true` if permission was granted, `false` otherwise.
     */
    suspend fun requestPermission(): Boolean =
        platformRequestPermission()
}
