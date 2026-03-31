package dev.subfly.yaba.core.util

import android.content.Context
import coil3.ImageLoader
import coil3.svg.SvgDecoder
import kotlin.concurrent.Volatile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Singleton Coil [ImageLoader] with [SvgDecoder] for bundled HugeIcons SVGs.
 *
 * Call [initialize] once with any [Context] (typically from [android.app.Application] or the
 * launcher [android.app.Activity]), then use [imageLoader] from anywhere (e.g. [dev.subfly.yaba.ui.components.YabaIcon]).
 */
object SvgImageLoader {
    private val mutex = Mutex()

    @Volatile
    private var loader: ImageLoader? = null

    fun initialize(context: Context) {
        if (loader != null) return
        runBlocking {
            mutex.withLock {
                if (loader != null) return@withLock
                val app = context.applicationContext
                loader = ImageLoader
                    .Builder(app)
                    .components { add(SvgDecoder.Factory()) }
                    .build()
            }
        }
    }

    val imageLoader: ImageLoader
        get() = loader ?: error("SvgImageLoader.initialize() must be called before use")
}
