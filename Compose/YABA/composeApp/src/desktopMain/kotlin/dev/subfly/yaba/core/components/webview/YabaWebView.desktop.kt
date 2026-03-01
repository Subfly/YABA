package dev.subfly.yaba.core.components.webview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.subfly.yabacore.model.utils.ReaderPreferences

/**
 * Desktop stub for YabaWebView.
 * JavaFX WebView integration will be implemented later.
 */
@Composable
actual fun YabaWebViewViewerInternal(
    modifier: Modifier,
    baseUrl: String,
    markdown: String,
    assetsBaseUrl: String?,
    platform: YabaWebPlatform,
    appearance: YabaWebAppearance,
    readerPreferences: ReaderPreferences,
    onUrlClick: (String) -> Boolean,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onReady: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "WebView not available on desktop yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    onReady()
}

@Composable
actual fun YabaWebViewEditorInternal(
    modifier: Modifier,
    baseUrl: String,
    markdown: String,
    assetsBaseUrl: String?,
    onUrlClick: (String) -> Boolean,
    onReady: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "WebView not available on desktop yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    onReady()
}

@Composable
actual fun YabaWebViewConverterInternal(
    modifier: Modifier,
    baseUrl: String,
    input: ConverterInput?,
    onConverterResult: (ConverterResult) -> Unit,
    onConverterError: (Throwable) -> Unit,
    onReady: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "WebView not available on desktop yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    onReady()
}
