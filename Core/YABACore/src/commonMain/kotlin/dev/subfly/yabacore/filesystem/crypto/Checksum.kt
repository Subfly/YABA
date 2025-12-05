package dev.subfly.yabacore.filesystem.crypto

/**
 * Computes the lowercase hexadecimal SHA-256 digest for the provided bytes.
 *
 * Implemented per-platform to avoid bringing in extra hashing libraries.
 */
expect fun computeSha256Hex(data: ByteArray): String
