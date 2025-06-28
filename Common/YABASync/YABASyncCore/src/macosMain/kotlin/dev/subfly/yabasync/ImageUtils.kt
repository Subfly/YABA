package dev.subfly.yabasync

import platform.Foundation.NSData
import platform.Foundation.dataUsingEncoding
import platform.Foundation.initWithBase64EncodedString
import platform.Foundation.NSUTF8StringEncoding

/**
 * macOS-specific implementation of ImageUtils
 */
actual object ImageUtils {
    
    /**
     * Encode byte array to Base64 string
     */
    actual fun encodeImageData(data: ByteArray?): String? {
        return data?.let { bytes ->
            val nsData = NSData.dataWithBytes(bytes, bytes.size.toULong())
            nsData.base64EncodedStringWithOptions(0u)
        }
    }
    
    /**
     * Decode Base64 string to byte array
     */
    actual fun decodeImageData(base64String: String?): ByteArray? {
        return base64String?.let { str ->
            try {
                val nsData = NSData.initWithBase64EncodedString(str, null)
                nsData?.let { data ->
                    data.bytes?.let { ptr ->
                        ByteArray(data.length.toInt()) { i ->
                            ptr[i]
                        }
                    }
                }
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
            val nsData = NSData.initWithBase64EncodedString(str, null)
            nsData != null
        } catch (e: Exception) {
            false
        }
    }
} 