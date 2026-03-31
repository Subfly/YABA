package dev.subfly.yaba.core.webview

/**
 * User tapped an inline or block math node in the note editor (see yaba-web-components Mathematics onClick).
 */
data class MathTapEvent(
    val isBlock: Boolean,
    /** ProseMirror document position of the math node (for [YabaEditorCommands.updateInlineMathPayload]). */
    val documentPos: Int,
    val latex: String,
)
