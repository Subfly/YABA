package dev.subfly.yabacore.model.utils

data class BookmarkSearchFilters(
    val folderIds: Set<String>? = null,
    val tagIds: Set<String>? = null,
)
