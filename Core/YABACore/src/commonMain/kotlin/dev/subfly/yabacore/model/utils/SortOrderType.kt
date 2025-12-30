package dev.subfly.yabacore.model.utils

enum class SortOrderType {
    ASCENDING,
    DESCENDING,
}

fun SortOrderType.uiIconName(): String =
    when (this) {
        SortOrderType.ASCENDING -> "sorting-1-9"
        SortOrderType.DESCENDING -> "sorting-9-1"
    }

expect fun SortOrderType.uiTitle(): String
