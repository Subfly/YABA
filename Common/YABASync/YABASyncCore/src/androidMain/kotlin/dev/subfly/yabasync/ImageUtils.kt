package dev.subfly.yabasync

/**
 * Android-specific implementation of ImageUtils
 */
actual object ImageUtils {
    
    /**
     * Encode byte array to Base64 string
     */
    actual fun encodeImageData(data: ByteArray?): String? {
        return data?.let { bytes ->
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        }
    }
    
    /**
     * Decode Base64 string to byte array
     */
    actual fun decodeImageData(base64String: String?): ByteArray? {
        return base64String?.let { str ->
            try {
                android.util.Base64.decode(str, android.util.Base64.DEFAULT)
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
            android.util.Base64.decode(str, android.util.Base64.DEFAULT)
            true
        } catch (e: Exception) {
            false
        }
    }
} 