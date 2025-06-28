package dev.subfly.yabasync

import java.util.Base64

/**
 * JVM-specific implementation of ImageUtils
 */
actual object ImageUtils {
    
    /**
     * Encode byte array to Base64 string
     */
    actual fun encodeImageData(data: ByteArray?): String? {
        return data?.let { bytes ->
            Base64.getEncoder().encodeToString(bytes)
        }
    }
    
    /**
     * Decode Base64 string to byte array
     */
    actual fun decodeImageData(base64String: String?): ByteArray? {
        return base64String?.let { str ->
            try {
                Base64.getDecoder().decode(str)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Check if a string is valid Base64
     */
    actual fun isValidBase64(str: String): Boolean {
        return try {
            Base64.getDecoder().decode(str)
            true
        } catch (e: Exception) {
            false
        }
    }
} 