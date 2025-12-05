package dev.subfly.yabacore.filesystem.crypto

import java.security.MessageDigest

actual fun computeSha256Hex(data: ByteArray): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(data)
        .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
