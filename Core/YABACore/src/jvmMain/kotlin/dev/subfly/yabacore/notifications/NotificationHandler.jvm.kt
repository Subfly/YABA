package dev.subfly.yabacore.notifications

internal actual fun initializePlatformNotifications(platformContext: Any?) {
    // No-op on JVM; notifications not supported until native desktop UI is implemented.
}

internal actual suspend fun platformScheduleReminder(
    bookmarkId: String,
    bookmarkKindCode: Int,
    title: PlatformNotificationText,
    message: PlatformNotificationText,
    bookmarkLabel: String,
    triggerDateEpochMillis: Long,
) {
    // No-op on JVM
}

internal actual suspend fun platformCancelReminder(bookmarkId: String) {
    // No-op on JVM
}

internal actual suspend fun platformCancelReminders(bookmarkIds: List<String>) {
    // No-op on JVM
}

internal actual suspend fun platformCancelAllReminders() {
    // No-op on JVM
}

internal actual suspend fun platformGetPendingReminderDate(bookmarkId: String): Long? = null

internal actual suspend fun platformRequestPermission(): Boolean = false
