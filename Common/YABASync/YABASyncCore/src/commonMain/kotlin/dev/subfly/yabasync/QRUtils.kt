package dev.subfly.yabasync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Utility functions for QR code generation and parsing
 * Note: This library doesn't generate QR codes directly, but provides the data structure
 * that apps can use to generate QR codes
 */
object QRUtils {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Create QR code data from server information
     * @param serverInfo The server information to encode
     * @return JSON string that can be used to generate a QR code
     */
    fun createQRData(serverInfo: ServerInfo): String {
        val qrData = QRData(
            type = "yaba-sync",
            version = "1.0",
            serverAddress = serverInfo.fullAddress,
            ipAddress = serverInfo.ipAddress,
            port = serverInfo.port,
            timestamp = System.currentTimeMillis()
        )
        return json.encodeToString(qrData)
    }
    
    /**
     * Parse QR code data to extract server information
     * @param qrDataString The QR code data string
     * @return ServerInfo extracted from the QR code
     * @throws IllegalArgumentException if the QR data is invalid
     */
    fun parseQRData(qrDataString: String): ServerInfo {
        try {
            val qrData = json.decodeFromString<QRData>(qrDataString)
            
            // Validate the QR data
            if (qrData.type != "yaba-sync") {
                throw IllegalArgumentException("Invalid QR code type: ${qrData.type}")
            }
            
            if (qrData.serverAddress.isBlank()) {
                throw IllegalArgumentException("Server address is missing")
            }
            
            return ServerInfo(
                ipAddress = qrData.ipAddress,
                port = qrData.port,
                fullAddress = qrData.serverAddress
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse QR code data: ${e.message}")
        }
    }
    
    /**
     * Validate if a string looks like valid YABA sync QR data
     * @param data The string to validate
     * @return true if it appears to be valid YABA sync QR data
     */
    fun isValidQRData(data: String): Boolean {
        return try {
            val qrData = json.decodeFromString<QRData>(data)
            qrData.type == "yaba-sync" && qrData.serverAddress.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}

@Serializable
private data class QRData(
    val type: String,
    val version: String,
    val serverAddress: String,
    val ipAddress: String,
    val port: Int,
    val timestamp: Long
) 