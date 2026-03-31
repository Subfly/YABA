package dev.subfly.yaba.ui.detail.bookmark.doc.models

internal enum class DocmarkDetailPage(
    val iconName: String,
    val label: String,
) {
    INFO(iconName = "information-circle", label = "Info"),
    CONTENTS(iconName = "align-box-middle-center", label = "Contents"),
    ANNOTATIONS(iconName = "sticky-note-03", label = "Annotations"),
}
