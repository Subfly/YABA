package dev.subfly.yabacore.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString

private const val REMINDERS_FILE = "yaba_reminders.preferences_pb"
private const val KEY_PREFIX = "reminder_"

internal object NotificationContextHolder {
    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun requireContext(): Context =
        appContext ?: error(
            "NotificationContextHolder not initialized. " +
                    "Call CoreRuntime.initialize(context) first."
        )

    fun contextOrNull(): Context? = appContext
}

internal object ReminderStore {
    @Volatile
    private var _dataStore: DataStore<Preferences>? = null

    fun initialize(context: Context) {
        if (_dataStore != null) return
        _dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = {
                context.applicationContext.filesDir
                    .resolve(REMINDERS_FILE)
                    .absolutePath
                    .toPath()
            },
        )
    }

    private val dataStore: DataStore<Preferences>
        get() = _dataStore ?: error("ReminderStore not initialized")

    suspend fun setReminder(bookmarkId: String, triggerMillis: Long) {
        dataStore.edit { prefs ->
            prefs[longPreferencesKey("${KEY_PREFIX}${bookmarkId}")] = triggerMillis
        }
    }

    suspend fun removeReminder(bookmarkId: String) {
        dataStore.edit { prefs ->
            prefs.remove(longPreferencesKey("${KEY_PREFIX}${bookmarkId}"))
        }
    }

    suspend fun getReminder(bookmarkId: String): Long? =
        dataStore.data.first()[longPreferencesKey("${KEY_PREFIX}${bookmarkId}")]

    suspend fun getAllReminderBookmarkIds(): List<String> =
        dataStore.data.first()
            .asMap()
            .keys
            .map { it.name }
            .filter { it.startsWith(KEY_PREFIX) }
            .map { it.removePrefix(KEY_PREFIX) }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}

internal actual fun initializePlatformNotifications(platformContext: Any?) {
    val context = platformContext as? Context ?: return
    NotificationContextHolder.initialize(context)
    ReminderStore.initialize(context)
    ensureNotificationChannel(context)
}

internal actual suspend fun platformScheduleReminder(
    bookmarkId: String,
    bookmarkKindCode: Int,
    title: PlatformNotificationText,
    message: PlatformNotificationText,
    bookmarkLabel: String,
    triggerDateEpochMillis: Long,
) {
    val context = NotificationContextHolder.requireContext()
    val resolvedTitle = getString(title)
    val resolvedMessage = getString(message, bookmarkLabel)

    ReminderStore.setReminder(bookmarkId, triggerDateEpochMillis)

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildPendingIntent(
        context = context,
        bookmarkId = bookmarkId,
        bookmarkKindCode = bookmarkKindCode,
        title = resolvedTitle,
        message = resolvedMessage,
    )

    pendingIntent?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerDateEpochMillis,
                it,
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerDateEpochMillis,
                it,
            )
        }
    }
}

internal actual suspend fun platformCancelReminder(bookmarkId: String) {
    val context = NotificationContextHolder.contextOrNull() ?: return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildPendingIntent(
        context = context,
        bookmarkId = bookmarkId,
        title = "",
        message = "",
        flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )
    pendingIntent?.let { alarmManager.cancel(it) }

    ReminderStore.removeReminder(bookmarkId)
}

internal actual suspend fun platformCancelReminders(bookmarkIds: List<String>) {
    bookmarkIds.forEach { platformCancelReminder(it) }
}

internal actual suspend fun platformCancelAllReminders() {
    val context = NotificationContextHolder.contextOrNull() ?: return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    ReminderStore.getAllReminderBookmarkIds().forEach { bookmarkId ->
        val pendingIntent = buildPendingIntent(
            context = context,
            bookmarkId = bookmarkId,
            title = "",
            message = "",
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    ReminderStore.clearAll()
}

internal actual suspend fun platformGetPendingReminderDate(bookmarkId: String): Long? {
    val millis = ReminderStore.getReminder(bookmarkId) ?: return null
    if (millis <= System.currentTimeMillis()) {
        ReminderStore.removeReminder(bookmarkId)
        return null
    }
    return millis
}

internal actual suspend fun platformRequestPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    val context = NotificationContextHolder.contextOrNull() ?: return false
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun buildPendingIntent(
    context: Context,
    bookmarkId: String,
    bookmarkKindCode: Int = 0,
    title: String,
    message: String,
    flags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
): PendingIntent? {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        action = "dev.subfly.yabacore.REMINDER_$bookmarkId"
        putExtra(ReminderReceiver.EXTRA_BOOKMARK_ID, bookmarkId)
        putExtra(ReminderReceiver.EXTRA_BOOKMARK_KIND_CODE, bookmarkKindCode)
        putExtra(ReminderReceiver.EXTRA_TITLE, title)
        putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
    }
    return PendingIntent.getBroadcast(context, 0, intent, flags)
}

private fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (manager.getNotificationChannel(ReminderReceiver.CHANNEL_ID) != null) return
    val channel = NotificationChannel(
        ReminderReceiver.CHANNEL_ID,
        "Bookmark Reminders",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Reminder notifications for bookmarks"
    }
    manager.createNotificationChannel(channel)
}
