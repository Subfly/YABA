package dev.subfly.yaba

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import dev.subfly.yaba.core.app.AppVM
import dev.subfly.yaba.core.navigation.alert.DeletionVM
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.EmptyCretionRoute
import dev.subfly.yaba.core.navigation.creation.LinkmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.YabaCreationSheet
import dev.subfly.yaba.core.navigation.creation.creationNavigationConfig
import dev.subfly.yaba.core.navigation.creation.rememberResultStore
import dev.subfly.yaba.core.theme.YabaTheme
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.CoreRuntime
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.preferences.UserPreferences

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoreRuntime.initialize(platformContext = this)

        enableEdgeToEdge()

        val sharedContent = extractSharedContent()

        setContent {
            ShareActivityContent(
                sharedContent = sharedContent,
                onDismiss = ::finish,
            )
        }
    }

    private fun extractSharedContent(): SharedContent? {
        val intent = intent ?: return null
        val action = intent.action
        val type = intent.type

        when (action) {
            Intent.ACTION_SEND -> {
                // Single item share - accept for all types
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    val uri =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                Intent.EXTRA_STREAM,
                                Uri::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                        }
                    if (uri != null) {
                        return SharedContent.Uri(uri, type)
                    }
                }

                if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (text != null) {
                        return SharedContent.Text(text, type)
                    }
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                // Multiple items share - only accept if NOT image or PDF
                // For images and PDFs, reject multiple shares to avoid user confusion
                val isImageOrPdf = type != null &&
                        (type.startsWith("image/") || type == "application/pdf")
                if (isImageOrPdf) {
                    // Reject multiple images/PDFs - return null to close activity
                    return null
                }

                // For text/other types, take first item
                val uris =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(
                            Intent.EXTRA_STREAM,
                            Uri::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    }
                if (uris.isNullOrEmpty().not()) {
                    return SharedContent.Uri(uris[0], type)
                }
            }
        }

        return null
    }
}

sealed class SharedContent {
    data class Text(val text: String, val mimeType: String?) : SharedContent()
    data class Uri(val uri: android.net.Uri, val mimeType: String?) : SharedContent()
}

private fun extractUrlFromText(text: String): String? {
    // Simple URL extraction - look for http:// or https://
    val urlPattern = Regex("""https?://\S+""")
    val match = urlPattern.find(text)
    return match?.value
}

private fun isValidUrl(text: String): Boolean {
    return text.startsWith("http://", ignoreCase = true) ||
            text.startsWith("https://", ignoreCase = true)
}

private fun handleSharedContent(
    sharedContent: SharedContent?,
    onNavigateToLinkmark: (String) -> Unit,
    onNavigateToBookmarkSelection: () -> Unit,
    onShowCreation: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (sharedContent == null) {
        onDismiss()
        return
    }

    // Determine if content is a valid URL
    val url: String? =
        when (sharedContent) {
            is SharedContent.Text -> {
                val extractedUrl = extractUrlFromText(sharedContent.text)
                if (extractedUrl != null && isValidUrl(extractedUrl)) {
                    extractedUrl
                } else if (isValidUrl(sharedContent.text)) {
                    sharedContent.text
                } else {
                    null
                }
            }

            is SharedContent.Uri -> {
                // For URI, check if it's a web URL
                val uriString = sharedContent.uri.toString()
                if (isValidUrl(uriString)) {
                    uriString
                } else {
                    null
                }
            }
        }

    // Navigate to appropriate route
    if (url != null) {
        // Valid URL - navigate directly to LinkmarkCreationRoute with initialUrl
        onNavigateToLinkmark(url)
    } else {
        // Not a valid URL - show bookmark selection
        onNavigateToBookmarkSelection()
    }

    // Show creation content
    onShowCreation()
}

@Composable
private fun ShareActivityContent(
    sharedContent: SharedContent?,
    onDismiss: () -> Unit,
) {
    val userPreferences by SettingsStores
        .userPreferences
        .preferencesFlow
        .collectAsState(UserPreferences())

    val appVM = viewModel { AppVM() }
    val deletionVM = viewModel { DeletionVM() }

    val navigationResultStore = rememberResultStore()
    val creationNavigator = rememberNavBackStack(
        configuration = creationNavigationConfig,
        EmptyCretionRoute
    )

    CompositionLocalProvider(
        LocalUserPreferences provides userPreferences,
        LocalResultStore provides navigationResultStore,
        LocalCreationContentNavigator provides creationNavigator,
        LocalAppStateManager provides appVM,
        LocalDeletionDialogManager provides deletionVM,
    ) {
        YabaTheme {

            // Handle shared content and navigate appropriately
            LaunchedEffect(sharedContent) {
                handleSharedContent(
                    sharedContent = sharedContent,
                    onNavigateToLinkmark = { url ->
                        creationNavigator.add(
                            LinkmarkCreationRoute(
                                bookmarkId = null,
                                initialUrl = url,
                            )
                        )
                    },
                    onNavigateToBookmarkSelection = {
                        creationNavigator.add(BookmarkCreationRoute())
                    },
                    onShowCreation = appVM::onShowCreationContent,
                    onDismiss = onDismiss,
                )
            }

            // Listen for sheet dismissal
            val appState by appVM.state.collectAsState()
            var didOpenCreationSheet by remember { mutableStateOf(false) }
            LaunchedEffect(appState.showCreationContent) {
                if (appState.showCreationContent) {
                    didOpenCreationSheet = true
                    return@LaunchedEffect
                }

                if (didOpenCreationSheet) onDismiss()
            }

            YabaCreationSheet()
        }
    }
}
