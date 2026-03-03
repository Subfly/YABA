package dev.subfly.yabacore.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.subfly.yabacore.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bookmarkId = intent.getStringExtra(EXTRA_BOOKMARK_ID) ?: return
        val bookmarkKindCode = intent.getIntExtra(EXTRA_BOOKMARK_KIND_CODE, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return

        val contentPendingIntent = buildContentPendingIntent(
            context = context,
            bookmarkId = bookmarkId,
            bookmarkKindCode = bookmarkKindCode,
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(R.drawable.ic_notification_bookmark)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(bookmarkId.hashCode(), notification)
    }

    private fun buildContentPendingIntent(
        context: Context,
        bookmarkId: String,
        bookmarkKindCode: Int,
    ): PendingIntent? {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
                putExtra(EXTRA_BOOKMARK_KIND_CODE, bookmarkKindCode)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            } ?: return null

        return PendingIntent.getActivity(
            context,
            bookmarkId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "yaba_bookmark_reminders"
        const val EXTRA_BOOKMARK_ID = "bookmarkId"
        const val EXTRA_BOOKMARK_KIND_CODE = "bookmarkKindCode"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
    }
}
