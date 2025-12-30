package dev.subfly.yabacore.model.utils

enum class SortType {
    CREATED_AT,
    EDITED_AT,
    LABEL,
    CUSTOM,
}

fun SortType.uiIconName(): String =
    when (this) {
        SortType.CREATED_AT -> "clock-04"
        SortType.EDITED_AT -> "edit-02"
        SortType.LABEL -> "sorting-a-z-02"
        SortType.CUSTOM -> "custom-field"
    }

expect fun SortType.uiTitle(): String
