package dev.subfly.yabacore.notifications

/**
 * Called once during [dev.subfly.yabacore.common.CoreRuntime.initialize] to hand
 * the platform context (Android Context) to the notification subsystem.
 */
internal expect fun initializePlatformNotifications(platformContext: Any?)

internal expect suspend fun platformScheduleReminder(
    bookmarkId: String,
    title: PlatformNotificationText,
    message: PlatformNotificationText,
    bookmarkLabel: String,
    triggerDateEpochMillis: Long,
)

internal expect suspend fun platformCancelReminder(bookmarkId: String)

internal expect suspend fun platformCancelReminders(bookmarkIds: List<String>)

internal expect suspend fun platformCancelAllReminders()

internal expect suspend fun platformGetPendingReminderDate(bookmarkId: String): Long?

internal expect suspend fun platformRequestPermission(): Boolean
