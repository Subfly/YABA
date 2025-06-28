package dev.subfly.yabasync

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val ipAddress: String,
    val port: Int,
    val fullAddress: String
) 