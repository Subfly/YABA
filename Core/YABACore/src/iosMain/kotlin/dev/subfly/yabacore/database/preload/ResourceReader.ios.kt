package dev.subfly.yabacore.database.preload

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import platform.Foundation.NSBundle

internal actual fun readResourceText(resourcePath: String): String {
    val resourceName = resourcePath.substringBeforeLast(".")
    val resourceExtension = resourcePath.substringAfterLast(".")
    val path = NSBundle.mainBundle.pathForResource(resourceName, resourceExtension)
        ?: error("Resource $resourcePath not found in bundle")
    val source = FileSystem.SYSTEM.source(path.toPath())
    val buffered = source.buffer()
    try {
        return buffered.readUtf8()
    } finally {
        buffered.close()
    }
}
