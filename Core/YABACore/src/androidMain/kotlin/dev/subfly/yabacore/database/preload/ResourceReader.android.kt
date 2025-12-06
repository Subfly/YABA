package dev.subfly.yabacore.database.preload

import okio.buffer
import okio.source

internal actual fun readResourceText(resourcePath: String): String {
    val stream =
        PreloadDataGenerator::class.java.classLoader?.getResourceAsStream(resourcePath)
            ?: error("Resource $resourcePath not found")
    return stream.source().buffer().use { it.readUtf8() }
}
