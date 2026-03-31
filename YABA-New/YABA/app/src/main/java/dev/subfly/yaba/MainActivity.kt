package dev.subfly.yaba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.filesystem.access.FileAccessProvider
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.ui.theme.YABATheme

class MainActivity : ComponentActivity() {
    private var keepSplash: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { keepSplash }

        FileAccessProvider.initialize(this)
        DatabaseProvider.initialize(this)
        YabaFileAccessor.register(this)

        keepSplash = false

        setContent {
            YABATheme {

            }
        }
    }
}