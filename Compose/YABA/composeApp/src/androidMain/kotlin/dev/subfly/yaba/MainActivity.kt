package dev.subfly.yaba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.subfly.yaba.core.app.App
import dev.subfly.yabacore.database.DatabaseProvider

class MainActivity : ComponentActivity() {

    /**
     * Set to true when you need to hold the splash screen while preparing app state.
     */
    private var keepSplashOnScreen: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        DatabaseProvider.initialize(
            platformContext = this,
        )

        keepSplashOnScreen = false

        enableEdgeToEdge()

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}