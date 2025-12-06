package dev.subfly.yabacore.filesystem.crypto

import okio.ByteString.Companion.toByteString

/** Computes the lowercase hexadecimal SHA-256 digest for the provided bytes using Okio. */
fun computeSha256Hex(data: ByteArray): String = data.toByteString().sha256().hex()
