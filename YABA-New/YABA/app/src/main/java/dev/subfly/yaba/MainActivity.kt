package dev.subfly.yaba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.ui.theme.YABATheme

class MainActivity : ComponentActivity() {
    private var keepSplash: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { keepSplash }

        DatabaseProvider.initialize(this)
        keepSplash = false

        setContent {
            YABATheme {

            }
        }
    }
}