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
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import io.github.vinceglb.filekit.coil.securelyAccessFile

/**
 * Cross-platform image renderer that loads [PlatformFile] objects using Coil + FileKit.
 *
 * The loader is configured with `addPlatformFileSupport()` so that FileKit-backed files can be
 * fetched across Android, iOS, macOS, and desktop targets. On Apple targets, `securelyAccessFile`
 * keeps security-scoped access alive while Coil reads the file.
 */
@Composable
fun rememberYabaImageLoader(): ImageLoader {
    val context = LocalPlatformContext.current
    return remember(context) {
        ImageLoader.Builder(context).components { addPlatformFileSupport() }.build()
    }
}

@Composable
fun YabaImage(
    file: PlatformFile?,
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
        model = file,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
        onLoading = { state ->
            if (file != null) state.securelyAccessFile(file)
            onState?.invoke(state)
        },
        onSuccess = { state ->
            if (file != null) state.securelyAccessFile(file)
            onState?.invoke(state)
        },
        onError = { state ->
            if (file != null) state.securelyAccessFile(file)
            onState?.invoke(state)
        },
    )
}
