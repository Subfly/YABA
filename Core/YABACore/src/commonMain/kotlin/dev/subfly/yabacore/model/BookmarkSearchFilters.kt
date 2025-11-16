package dev.subfly.yabacore.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class BookmarkSearchFilters(
        val folderIds: Set<Uuid>? = null,
        val tagIds: Set<Uuid>? = null,
        val kinds: Set<BookmarkKind>? = null,
)
