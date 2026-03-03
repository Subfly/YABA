package dev.subfly.yaba

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.subfly.yaba.core.app.App
import dev.subfly.yaba.core.deeplink.DeepLinkManager
import dev.subfly.yaba.core.deeplink.DeepLinkTarget
import dev.subfly.yabacore.common.CoreRuntime
import dev.subfly.yabacore.notifications.ReminderReceiver

class MainActivity : ComponentActivity() {

    /**
     * Set to true when you need to hold the splash screen while preparing app state.
     */
    private var keepSplashOnScreen: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        CoreRuntime.initialize(platformContext = this)

        keepSplashOnScreen = false

        enableEdgeToEdge()

        handleDeepLink(intent)

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

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}