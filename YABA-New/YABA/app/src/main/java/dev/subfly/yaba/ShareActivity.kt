package dev.subfly.yaba

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import dev.subfly.yaba.core.app.AppVM
import dev.subfly.yaba.core.components.toast.YabaToastHost
import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.filesystem.access.FileAccessProvider
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.core.navigation.alert.DeletionVM
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.DocmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.EmptyCretionRoute
import dev.subfly.yaba.core.navigation.creation.ImagemarkCreationRoute
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
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.SharedDocumentData
import dev.subfly.yaba.util.SharedImageData
import dev.subfly.yaba.core.model.utils.DocmarkType
import dev.subfly.yaba.core.notifications.NotificationManager
import dev.subfly.yaba.core.preferences.SettingsStores
import dev.subfly.yaba.core.preferences.UserPreferences
import dev.subfly.yaba.core.queue.CoreOperationQueue
import dev.subfly.yaba.core.util.SvgImageLoader
import java.io.InputStream

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FileAccessProvider.initialize(this)
        DatabaseProvider.initialize(this)
        SvgImageLoader.initialize(this)
        SettingsStores.initialize(this)
        NotificationManager.initialize(this)
        YabaFileAccessor.register(this)
        CoreOperationQueue.start()

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
                            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                        }
                    if (uri != null) {
                        val resolvedType = resolveMimeType(uri = uri, declaredType = type)

                        if (isImageMimeType(resolvedType)) {
                            readImageBytesFromUri(uri, resolvedType)?.let { (bytes, ext) ->
                                return SharedContent.ImageBytes(bytes, ext)
                            }
                            return null
                        }

                        if (isPdfMimeType(resolvedType) || isEpubMimeType(resolvedType)) {
                            readDocumentBytesFromUri(
                                uri = uri,
                                declaredType = resolvedType,
                            )?.let { doc ->
                                return SharedContent.DocumentBytes(
                                    bytes = doc.bytes,
                                    sourceFileName = doc.sourceFileName,
                                    docmarkType = doc.docmarkType,
                                )
                            }
                            return null
                        }

                        return null
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
                val uris =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    }
                if (uris.isNullOrEmpty().not()) {
                    val docUri = uris.firstOrNull { uri ->
                        val mt = resolveMimeType(uri = uri, declaredType = type)
                        isPdfMimeType(mt) || isEpubMimeType(mt)
                    }

                    if (docUri == null) return null

                    readDocumentBytesFromUri(uri = docUri, declaredType = type)?.let { doc ->
                        return SharedContent.DocumentBytes(
                            bytes = doc.bytes,
                            sourceFileName = doc.sourceFileName,
                            docmarkType = doc.docmarkType,
                        )
                    }

                    return null
                }
            }
        }

        return null
    }

    private fun isImageMimeType(mimeType: String?): Boolean {
        return mimeType != null && mimeType.lowercase().startsWith("image/")
    }

    private fun isPdfMimeType(mimeType: String?): Boolean {
        return mimeType != null && mimeType.lowercase() == "application/pdf"
    }

    private fun isEpubMimeType(mimeType: String?): Boolean {
        if (mimeType == null) return false
        val m = mimeType.lowercase()
        return m == "application/epub+zip" || m == "application/epub"
    }

    private fun resolveMimeType(uri: Uri, declaredType: String?): String? {
        val trimmedType =
            declaredType
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotBlank() && it != "*/*" }
        if (trimmedType != null) return trimmedType

        return runCatching { contentResolver.getType(uri) }.getOrNull()
    }

    private fun readDocumentBytesFromUri(uri: Uri, declaredType: String?): SharedDocumentData? {
        val mimeType = resolveMimeType(uri = uri, declaredType = declaredType)
        val docmarkType = when {
            isPdfMimeType(mimeType) -> DocmarkType.PDF
            isEpubMimeType(mimeType) -> DocmarkType.EPUB
            else -> return null
        }

        val bytes = readBytesFromUri(uri) ?: return null
        val sourceFileName = getFileNameFromUri(uri)

        return SharedDocumentData(bytes = bytes, sourceFileName = sourceFileName, docmarkType = docmarkType)
    }

    private fun readBytesFromUri(uri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                stream.readBytes()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val projectedColumns = arrayOf(OpenableColumns.DISPLAY_NAME)
        return runCatching {
            contentResolver.query(uri, projectedColumns, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getString(index)
                } else {
                    null
                }
            }
        }.getOrNull()?.takeIf { it.isNullOrBlank().not() } ?: uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
    }

    private fun readImageBytesFromUri(uri: Uri, mimeType: String?): Pair<ByteArray, String>? {
        return readBytesFromUri(uri)?.let { bytes ->
            val ext =
                mimeType?.substringAfterLast("/")?.lowercase()?.takeIf { it.isNotBlank() }
                    ?: "jpeg"
            Pair(bytes, if (ext == "jpeg" || ext == "jpg") "jpeg" else ext)
        }
    }
}

sealed class SharedContent {
    data class Text(val text: String, val mimeType: String?) : SharedContent()
    data class Uri(val uri: android.net.Uri, val mimeType: String?) : SharedContent()
    data class ImageBytes(val bytes: ByteArray, val extension: String) : SharedContent() {
        override fun equals(other: Any?): Boolean =
            other is ImageBytes &&
                bytes.contentEquals(other.bytes) &&
                extension == other.extension

        override fun hashCode(): Int = bytes.contentHashCode() * 31 + extension.hashCode()
    }

    data class DocumentBytes(
        val bytes: ByteArray,
        val sourceFileName: String?,
        val docmarkType: DocmarkType,
    ) : SharedContent() {
        override fun equals(other: Any?): Boolean =
            other is DocumentBytes &&
                bytes.contentEquals(other.bytes) &&
                sourceFileName == other.sourceFileName &&
                docmarkType == other.docmarkType

        override fun hashCode(): Int =
            bytes.contentHashCode() * 31 + (sourceFileName?.hashCode() ?: 0) + docmarkType.hashCode()
    }
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
    onNavigateToImagemark: (SharedImageData) -> Unit,
    onNavigateToDocmark: (SharedDocumentData) -> Unit,
    onNavigateToBookmarkSelection: () -> Unit,
    onShowCreation: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (sharedContent == null) {
        onDismiss()
        return
    }

    // Image bytes from share - go directly to Imagemark creation
    if (sharedContent is SharedContent.ImageBytes) {
        onNavigateToImagemark(SharedImageData(sharedContent.bytes, sharedContent.extension))
        onShowCreation()
        return
    }

    if (sharedContent is SharedContent.DocumentBytes) {
        onNavigateToDocmark(
            SharedDocumentData(
                bytes = sharedContent.bytes,
                sourceFileName = sharedContent.sourceFileName,
                docmarkType = sharedContent.docmarkType,
            ),
        )
        onShowCreation()
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

            is SharedContent.ImageBytes -> null
            is SharedContent.DocumentBytes -> null
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
    val userPreferences by
        SettingsStores.userPreferences.preferencesFlow.collectAsState(UserPreferences())

    val appVM = viewModel { AppVM() }
    val deletionVM = viewModel { DeletionVM() }

    val navigationResultStore = rememberResultStore()
    val creationNavigator =
        rememberNavBackStack(configuration = creationNavigationConfig, EmptyCretionRoute)

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
                            ),
                        )
                    },
                    onNavigateToImagemark = { imageData ->
                        navigationResultStore.setResult(
                            ResultStoreKeys.SHARED_IMAGE_DATA,
                            imageData,
                        )
                        creationNavigator.add(ImagemarkCreationRoute(bookmarkId = null))
                    },
                    onNavigateToDocmark = { docData ->
                        navigationResultStore.setResult(
                            ResultStoreKeys.SHARED_DOCUMENT_DATA,
                            docData,
                        )
                        creationNavigator.add(DocmarkCreationRoute(bookmarkId = null))
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

            Box(modifier = Modifier.fillMaxSize()) {
                YabaCreationSheet()
                YabaToastHost()
            }
        }
    }
}
