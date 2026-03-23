package dev.subfly.yabacore.webview

import dev.subfly.yabacore.model.utils.YabaColor

object YabaEditorCommands {
    const val ToggleBold = """{"type":"toggleBold"}"""
    const val ToggleItalic = """{"type":"toggleItalic"}"""
    const val ToggleUnderline = """{"type":"toggleUnderline"}"""
    const val ToggleStrikethrough = """{"type":"toggleStrikethrough"}"""
    const val ToggleSubscript = """{"type":"toggleSubscript"}"""
    const val ToggleSuperscript = """{"type":"toggleSuperscript"}"""
    const val ToggleCode = """{"type":"toggleCode"}"""
    const val ToggleCodeBlock = """{"type":"toggleCodeBlock"}"""
    const val ToggleQuote = """{"type":"toggleQuote"}"""
    const val InsertHr = """{"type":"insertHr"}"""
    const val ToggleBulletedList = """{"type":"toggleBulletedList"}"""
    const val ToggleNumberedList = """{"type":"toggleNumberedList"}"""
    const val ToggleTaskList = """{"type":"toggleTaskList"}"""
    const val Indent = """{"type":"indent"}"""
    const val Outdent = """{"type":"outdent"}"""
    const val Undo = """{"type":"undo"}"""
    const val Redo = """{"type":"redo"}"""
    const val AddRowBefore = """{"type":"addRowBefore"}"""
    const val AddRowAfter = """{"type":"addRowAfter"}"""
    const val DeleteRow = """{"type":"deleteRow"}"""
    const val AddColumnBefore = """{"type":"addColumnBefore"}"""
    const val AddColumnAfter = """{"type":"addColumnAfter"}"""
    const val DeleteColumn = """{"type":"deleteColumn"}"""
    const val UnsetTextHighlight = """{"type":"unsetTextHighlight"}"""

    /** JSON for [window.YabaEditorBridge.dispatch] — inserts raw text at the selection (escapes for JS JSON). */
    fun insertTextPayload(text: String): String {
        val escaped = escapeJsonString(text)
        return """{"type":"insertText","text":"$escaped"}"""
    }

    fun insertTablePayload(
        rows: Int,
        cols: Int,
        withHeaderRow: Boolean = false,
    ): String {
        val r = rows.coerceIn(1, 20)
        val c = cols.coerceIn(1, 20)
        return """{"type":"insertTable","rows":$r,"cols":$c,"withHeaderRow":$withHeaderRow}"""
    }

    fun insertImagePayload(src: String): String {
        val escaped = escapeJsonString(src)
        return """{"type":"insertImage","src":"$escaped"}"""
    }

    fun insertInlineMathPayload(latex: String): String {
        val escaped = escapeJsonString(latex)
        return """{"type":"insertInlineMath","latex":"$escaped"}"""
    }

    fun insertBlockMathPayload(latex: String): String {
        val escaped = escapeJsonString(latex)
        return """{"type":"insertBlockMath","latex":"$escaped"}"""
    }

    fun updateInlineMathPayload(latex: String, pos: Int): String {
        val escaped = escapeJsonString(latex)
        val p = pos.coerceAtLeast(0)
        return """{"type":"updateInlineMath","latex":"$escaped","pos":$p}"""
    }

    fun updateBlockMathPayload(latex: String, pos: Int): String {
        val escaped = escapeJsonString(latex)
        val p = pos.coerceAtLeast(0)
        return """{"type":"updateBlockMath","latex":"$escaped","pos":$p}"""
    }

    private fun escapeJsonString(text: String): String =
        buildString {
            for (c in text) {
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    else -> append(c)
                }
            }
        }

    /** JSON for [window.YabaEditorBridge.dispatch] — sets the current block to a heading level */
    fun setHeadingPayload(level: Int): String {
        val l = level.coerceIn(1, 6)
        return """{"type":"setHeading","level":$l}"""
    }

    /** Highlight mark; [colorRole] is [YabaColor.name] (e.g. BLUE, NONE maps to folder default in web). */
    fun setTextHighlightPayload(colorRole: YabaColor): String {
        val role = escapeJsonString(colorRole.name)
        return """{"type":"setTextHighlight","colorRole":"$role"}"""
    }

    fun hasAnyTextMark(formatting: EditorFormattingState): Boolean =
        formatting.bold ||
            formatting.italic ||
            formatting.underline ||
            formatting.strikethrough ||
            formatting.subscript ||
            formatting.superscript ||
            formatting.textHighlight

    /** Block/list toggles shown under the insert ("add") toolbar menu. */
    fun hasAnyInsertMenuToggle(formatting: EditorFormattingState): Boolean =
        formatting.code ||
            formatting.codeBlock ||
            formatting.blockquote ||
            formatting.bulletList ||
            formatting.orderedList ||
            formatting.taskList ||
            formatting.inlineMath ||
            formatting.blockMath
}
