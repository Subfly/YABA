package dev.subfly.yaba.core.security

import java.security.MessageDigest

/**
 * Verifies the 6-digit private bookmark PIN against the value stored in the Keystore-encrypted
 * preferences blob.
 */
object PrivateBookmarkPasswordVerifier {
    fun verify(pinDigits: String, stored: String): Boolean {
        if (stored.isBlank()) return false
        val normalized = pinDigits.trim()
        if (normalized.length != 6 || !normalized.all { it.isDigit() }) return false
        return MessageDigest.isEqual(
                normalized.toByteArray(Charsets.UTF_8),
                stored.toByteArray(Charsets.UTF_8),
        )
    }
}
