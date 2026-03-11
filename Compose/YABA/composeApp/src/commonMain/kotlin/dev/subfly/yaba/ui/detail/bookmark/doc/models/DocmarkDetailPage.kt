package dev.subfly.yaba.ui.detail.bookmark.doc.models

internal enum class DocmarkDetailPage(
    val iconName: String,
    val label: String,
) {
    INFO(iconName = "information-circle", label = "Info"),
    HIGHLIGHTS(iconName = "highlighter", label = "Highlights"),
}
