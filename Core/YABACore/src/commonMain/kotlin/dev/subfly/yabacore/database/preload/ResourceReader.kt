package dev.subfly.yabacore.database.preload

import dev.subfly.yabacore.yabacore.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Lightweight resource reader for bundled assets shipped with the library.
 *
 * Resources live under `src/commonMain/composeResources/...` and are loaded via the generated
 * Compose Multiplatform `Res` accessor.
 */
@OptIn(ExperimentalResourceApi::class)
internal suspend fun readResourceText(resourcePath: String): String =
    readResourceBytesOrNull(resourcePath)?.decodeToString()
        ?: error("Resource $resourcePath not found")

/** Binary resource reader (returns null if not found). */
@OptIn(ExperimentalResourceApi::class)
internal suspend fun readResourceBytesOrNull(resourcePath: String): ByteArray? =
    try {
        Res.readBytes(resourcePath)
    } catch (_: Exception) {
        null
    }
