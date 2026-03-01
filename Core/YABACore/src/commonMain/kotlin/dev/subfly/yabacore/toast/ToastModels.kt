package dev.subfly.yabacore.toast

typealias ToastId = String

enum class ToastDuration(val millis: Long) {
    SHORT(4_000),
    LONG(8_000),
}

enum class ToastIconType(val iconName: String) {
    WARNING("alert-02"),
    SUCCESS("checkmark-badge-02"),
    HINT("checkmark-circle-01"),
    ERROR("cancel-circle"),
    NONE("help-circle"),
}

data class ToastItem(
    val id: ToastId,
    val message: PlatformToastText,
    val acceptText: PlatformToastText? = null,
    val iconType: ToastIconType = ToastIconType.NONE,
    val duration: ToastDuration = ToastDuration.SHORT,
    val isVisible: Boolean = true,
)
