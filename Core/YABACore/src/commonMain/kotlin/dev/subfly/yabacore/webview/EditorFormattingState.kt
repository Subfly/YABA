package dev.subfly.yabacore.webview

import androidx.compose.runtime.Stable

/**
 * Active marks and command availability for the note editor toolbar
 * (from [window.YabaEditorBridge.getActiveFormatting] on Android).
 */
@Stable
data class EditorFormattingState(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val subscript: Boolean = false,
    val superscript: Boolean = false,
    val code: Boolean = false,
    val codeBlock: Boolean = false,
    val blockquote: Boolean = false,
    val bulletList: Boolean = false,
    val orderedList: Boolean = false,
    val taskList: Boolean = false,
    val inlineMath: Boolean = false,
    val blockMath: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val canIndent: Boolean = false,
    val canOutdent: Boolean = false,
    val inTable: Boolean = false,
    val canAddRowBefore: Boolean = false,
    val canAddRowAfter: Boolean = false,
    val canDeleteRow: Boolean = false,
    val canAddColumnBefore: Boolean = false,
    val canAddColumnAfter: Boolean = false,
    val canDeleteColumn: Boolean = false,
    val textHighlight: Boolean = false,
)
