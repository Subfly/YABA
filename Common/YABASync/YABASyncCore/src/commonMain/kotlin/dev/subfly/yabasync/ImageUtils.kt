package dev.subfly.yabasync

/**
 * Utility functions for handling image data in sync operations
 */
expect object ImageUtils {
    
    /**
     * Encode byte array to Base64 string
     */
    fun encodeImageData(data: ByteArray?): String?
    
    /**
     * Decode Base64 string to byte array
     */
    fun decodeImageData(base64String: String?): ByteArray?
    
    /**
     * Check if a string is valid Base64
     */
    fun isValidBase64(str: String): Boolean
} 