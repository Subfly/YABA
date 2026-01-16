package dev.subfly.yabacore.filesystem.json

import kotlinx.serialization.Serializable

/**
 * JSON schema for link-specific bookmark data stored at `/bookmarks/<uuid>/link.json`.
 *
 * This file is only present for bookmarks with kind = LINK.
 * It participates equally with `meta.json` in CRDT merging.
 *
 * This is the authoritative representation of link bookmark details in the filesystem.
 */
@Serializable
data class LinkJson(
    val url: String,
    val domain: String,
    /** Link type code (see LinkType enum) */
    val linkTypeCode: Int,
    val videoUrl: String?,
    /** Vector clock: Map of deviceId to sequence number */
    val clock: Map<String, Long>,
)
