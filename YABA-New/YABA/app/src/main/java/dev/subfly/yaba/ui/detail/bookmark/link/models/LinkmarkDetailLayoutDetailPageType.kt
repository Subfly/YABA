package dev.subfly.yaba.ui.detail.bookmark.link.models

internal enum class DetailPage(
    val iconName: String,
    val label: String // TODO: LOCALIZATION
) {
    INFO(iconName = "information-circle", label = "Info"),
    VERSIONS(iconName = "clock-02", label = "Versions"),
    ANNOTATIONS(iconName = "sticky-note-03", label = "Annotations"),
    CONTENTS(iconName = "align-box-middle-center", label = "Contents"),
}
