package dev.subfly.yabacore.database.preload

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

internal actual fun readResourceText(resourcePath: String): String {
    val source = FileSystem.RESOURCES.source(resourcePath.toPath())
    return source.buffer().use { it.readUtf8() }
}
