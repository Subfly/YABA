@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.preferences

import dev.subfly.yabacore.database.migration.LegacyBookmark
import dev.subfly.yabacore.database.migration.LegacyFolder
import dev.subfly.yabacore.database.migration.LegacySnapshot
import dev.subfly.yabacore.database.migration.LegacyTag
import dev.subfly.yabacore.database.migration.LegacyTagLink
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Small helpers exported for Swift to build fully-typed
 * migration payloads without manual UUID/Instant plumbing.
 */
object LegacySnapshotBuilder {
    fun build(
        folders: List<LegacyFolder>,
        tags: List<LegacyTag>,
        bookmarks: List<LegacyBookmark>,
        tagLinks: List<LegacyTagLink>,
    ): LegacySnapshot = LegacySnapshot(
        folders = folders,
        tags = tags,
        bookmarks = bookmarks,
        tagLinks = tagLinks,
    )
}

fun uuidFromString(value: String): Uuid = Uuid.parse(value)

fun instantFromEpochMillis(epochMillis: Long): Instant =
    Instant.fromEpochMilliseconds(epochMillis)
