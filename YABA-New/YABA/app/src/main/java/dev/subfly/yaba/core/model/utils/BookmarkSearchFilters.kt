package dev.subfly.yaba.core.model.utils

data class BookmarkSearchFilters(
    val folderIds: Set<String>? = null,
    val tagIds: Set<String>? = null,
)
