package dev.subfly.yabacore.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext

/** Creates and remembers a Coil [ImageLoader] for the current platform context. */
@Composable
fun rememberYabaImageLoader(): ImageLoader {
    val context = LocalPlatformContext.current
    return remember(context) { ImageLoader.Builder(context).build() }
}

/**
 * Cross-platform image renderer that loads images from a file path.
 *
 * @param filePath The absolute path to the image file on disk.
 * @param modifier Modifier for the image composable.
 * @param contentDescription Accessibility description.
 * @param contentScale How the image should be scaled.
 * @param placeholder Painter to show while loading.
 * @param error Painter to show on error.
 * @param fallback Painter to show if [filePath] is null.
 * @param imageLoader The Coil ImageLoader to use.
 * @param onState Callback for image loading state changes.
 */
@Composable
fun YabaImage(
    filePath: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = null,
    error: Painter? = null,
    fallback: Painter? = placeholder,
    imageLoader: ImageLoader = rememberYabaImageLoader(),
    onState: ((AsyncImagePainter.State) -> Unit)? = null,
) {
    // Convert file path to file:// URI for Coil
    val model = filePath?.let { path -> if (path.startsWith("file://")) path else "file://$path" }

    AsyncImage(
        modifier = modifier,
        model = model,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
        onLoading = { state -> onState?.invoke(state) },
        onSuccess = { state -> onState?.invoke(state) },
        onError = { state -> onState?.invoke(state) },
    )
}

/**
 * Cross-platform image renderer that loads images from a [ByteArray].
 *
 * Useful for displaying in-memory image data (e.g., from network responses or unfurling).
 *
 * @param bytes The image data as a ByteArray.
 * @param modifier Modifier for the image composable.
 * @param contentDescription Accessibility description.
 * @param contentScale How the image should be scaled.
 * @param placeholder Painter to show while loading.
 * @param error Painter to show on error.
 * @param fallback Painter to show if [bytes] is null.
 * @param imageLoader The Coil ImageLoader to use.
 * @param onState Callback for image loading state changes.
 */
@Composable
fun YabaImage(
    bytes: ByteArray?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = null,
    error: Painter? = null,
    fallback: Painter? = placeholder,
    imageLoader: ImageLoader = rememberYabaImageLoader(),
    onState: ((AsyncImagePainter.State) -> Unit)? = null,
) {
    AsyncImage(
        modifier = modifier,
        model = bytes,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
        onLoading = { state -> onState?.invoke(state) },
        onSuccess = { state -> onState?.invoke(state) },
        onError = { state -> onState?.invoke(state) },
    )
}

/**
 * Cross-platform image renderer that loads images from a URL string.
 *
 * Supports both remote URLs (http/https) and local file URLs (file://).
 *
 * @param url The URL to load the image from.
 * @param modifier Modifier for the image composable.
 * @param contentDescription Accessibility description.
 * @param contentScale How the image should be scaled.
 * @param placeholder Painter to show while loading.
 * @param error Painter to show on error.
 * @param fallback Painter to show if [url] is null.
 * @param imageLoader The Coil ImageLoader to use.
 * @param onState Callback for image loading state changes.
 */
@Composable
fun YabaImageFromUrl(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = null,
    error: Painter? = null,
    fallback: Painter? = placeholder,
    imageLoader: ImageLoader = rememberYabaImageLoader(),
    onState: ((AsyncImagePainter.State) -> Unit)? = null,
) {
    AsyncImage(
        modifier = modifier,
        model = url,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
        onLoading = { state -> onState?.invoke(state) },
        onSuccess = { state -> onState?.invoke(state) },
        onError = { state -> onState?.invoke(state) },
    )
}
