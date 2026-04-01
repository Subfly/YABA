package dev.subfly.yaba

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.subfly.yaba.core.app.App
import dev.subfly.yaba.core.security.PrivateBookmarkSessionGuard
import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.deeplink.DeepLinkManager
import dev.subfly.yaba.core.deeplink.DeepLinkTarget
import dev.subfly.yaba.core.filesystem.access.FileAccessProvider
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.core.notifications.NotificationManager
import dev.subfly.yaba.core.notifications.ReminderReceiver
import dev.subfly.yaba.core.preferences.SettingsStores
import dev.subfly.yaba.core.queue.CoreOperationQueue
import dev.subfly.yaba.core.util.SvgImageLoader

class MainActivity : ComponentActivity() {
    private var keepSplash: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { keepSplash }

        FileAccessProvider.initialize(this)
        DatabaseProvider.initialize(this)
        SvgImageLoader.initialize(this)
        SettingsStores.initialize(this)
        NotificationManager.initialize(this)
        YabaFileAccessor.register(this)
        CoreOperationQueue.start()

        keepSplash = false

        handleDeepLink(intent)

        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    PrivateBookmarkSessionGuard.lock()
                }
            },
        )

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val bookmarkId = intent.getStringExtra(ReminderReceiver.EXTRA_BOOKMARK_ID) ?: return
        val bookmarkKindCode = intent.getIntExtra(ReminderReceiver.EXTRA_BOOKMARK_KIND_CODE, 0)
        DeepLinkManager.handle(
            DeepLinkTarget.BookmarkDetail(
                bookmarkId = bookmarkId,
                bookmarkKindCode = bookmarkKindCode,
            )
        )
    }
}
