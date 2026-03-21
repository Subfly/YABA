package dev.subfly.yaba.ui.detail.bookmark.note.models

internal enum class NotemarkDetailPage(
    val iconName: String,
    val label: String,
) {
    INFO(iconName = "information-circle", label = "Info"),
    HIGHLIGHTS(iconName = "highlighter", label = "Highlights"),
}
