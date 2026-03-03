package dev.subfly.yabacore.notifications

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSBundle
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

internal actual fun initializePlatformNotifications(platformContext: Any?) {
    // No-op on iOS; UNUserNotificationCenter is always available.
}

internal actual suspend fun platformScheduleReminder(
    bookmarkId: String,
    bookmarkKindCode: Int,
    title: PlatformNotificationText,
    message: PlatformNotificationText,
    bookmarkLabel: String,
    triggerDateEpochMillis: Long,
) {
    val center = UNUserNotificationCenter.currentNotificationCenter()

    val resolvedTitle = NSBundle.mainBundle.localizedStringForKey(title, null, null)
    val resolvedMessageFormat = NSBundle.mainBundle.localizedStringForKey(message, null, null)
    val resolvedMessage = resolvedMessageFormat.replace("%@", bookmarkLabel)

    val content = UNMutableNotificationContent().apply {
        setTitle(resolvedTitle)
        setBody(resolvedMessage)
        setSound(UNNotificationSound.defaultSound())
        setUserInfo(mapOf("id" to bookmarkId, "kindCode" to bookmarkKindCode))
    }

    val date = Instant.fromEpochMilliseconds(triggerDateEpochMillis).toNSDate()
    val calendar = NSCalendar.currentCalendar
    val units = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
        NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond
    val components = calendar.components(units, fromDate = date)
    val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
        dateComponents = components,
        repeats = false,
    )

    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = bookmarkId,
        content = content,
        trigger = trigger,
    )

    suspendCancellableCoroutine { continuation ->
        center.addNotificationRequest(request) { error ->
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
    }
}

internal actual suspend fun platformCancelReminder(bookmarkId: String) {
    UNUserNotificationCenter.currentNotificationCenter()
        .removePendingNotificationRequestsWithIdentifiers(listOf(bookmarkId))
}

internal actual suspend fun platformCancelReminders(bookmarkIds: List<String>) {
    UNUserNotificationCenter.currentNotificationCenter()
        .removePendingNotificationRequestsWithIdentifiers(bookmarkIds)
}

internal actual suspend fun platformCancelAllReminders() {
    UNUserNotificationCenter.currentNotificationCenter()
        .removeAllPendingNotificationRequests()
}

internal actual suspend fun platformGetPendingReminderDate(
    bookmarkId: String,
): Long? = suspendCancellableCoroutine { continuation ->
    UNUserNotificationCenter.currentNotificationCenter()
        .getPendingNotificationRequestsWithCompletionHandler { requests ->
            @Suppress("UNCHECKED_CAST")
            val typedRequests = requests as? List<UNNotificationRequest>
            val match = typedRequests?.firstOrNull { it.identifier == bookmarkId }
            val calendarTrigger = match?.trigger as? UNCalendarNotificationTrigger
            val dateComponents = calendarTrigger?.dateComponents
            val fireDate = dateComponents?.let {
                NSCalendar.currentCalendar.dateFromComponents(it)
            }
            val epochMillis = fireDate
                ?.toKotlinInstant()
                ?.toEpochMilliseconds()
            if (continuation.isActive) {
                continuation.resume(epochMillis)
            }
        }
}

internal actual suspend fun platformRequestPermission(): Boolean =
    suspendCancellableCoroutine { continuation ->
        val options = UNAuthorizationOptionAlert or
            UNAuthorizationOptionBadge or
            UNAuthorizationOptionSound
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(options) { granted, _ ->
                if (continuation.isActive) {
                    continuation.resume(granted)
                }
            }
    }
