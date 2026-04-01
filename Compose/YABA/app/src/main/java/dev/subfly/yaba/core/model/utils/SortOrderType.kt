package dev.subfly.yaba.core.model.utils

import kotlinx.serialization.Serializable

@Serializable
enum class SortOrderType {
    ASCENDING,
    DESCENDING,
}

fun SortOrderType.uiIconName(): String =
    when (this) {
        SortOrderType.ASCENDING -> "sorting-1-9"
        SortOrderType.DESCENDING -> "sorting-9-1"
    }
