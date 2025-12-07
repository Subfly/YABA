package dev.subfly.yabacore.database.preload

/** Lightweight resource reader for bundled assets shipped with the library. */
internal expect fun readResourceText(resourcePath: String): String

/** Binary resource reader (returns null if not found). */
internal expect fun readResourceBytesOrNull(resourcePath: String): ByteArray?
