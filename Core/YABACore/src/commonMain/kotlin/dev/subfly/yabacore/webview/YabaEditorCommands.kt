package dev.subfly.yabacore.webview

object YabaEditorCommands {
    const val ToggleBold = """{"type":"toggleBold"}"""
    const val ToggleItalic = """{"type":"toggleItalic"}"""
    const val ToggleUnderline = """{"type":"toggleUnderline"}"""
    const val ToggleStrikethrough = """{"type":"toggleStrikethrough"}"""
    const val ToggleSubscript = """{"type":"toggleSubscript"}"""
    const val ToggleSuperscript = """{"type":"toggleSuperscript"}"""
    const val ToggleCode = """{"type":"toggleCode"}"""
    const val ToggleQuote = """{"type":"toggleQuote"}"""
    const val InsertHr = """{"type":"insertHr"}"""
    const val ToggleBulletedList = """{"type":"toggleBulletedList"}"""
    const val ToggleNumberedList = """{"type":"toggleNumberedList"}"""
    const val ToggleTaskList = """{"type":"toggleTaskList"}"""
    const val Indent = """{"type":"indent"}"""
    const val Outdent = """{"type":"outdent"}"""
    const val Undo = """{"type":"undo"}"""
    const val Redo = """{"type":"redo"}"""

    /** JSON for [window.YabaEditorBridge.dispatch] — inserts raw text at the selection (escapes for JS JSON). */
    fun insertTextPayload(text: String): String {
        val escaped = buildString {
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
        return """{"type":"insertText","text":"$escaped"}"""
    }

    fun markdownHeadingPrefix(level: Int): String = "#".repeat(level.coerceIn(1, 6)) + " "

    fun hasAnyTextMark(formatting: EditorFormattingState): Boolean =
        formatting.bold ||
            formatting.italic ||
            formatting.underline ||
            formatting.strikethrough ||
            formatting.subscript ||
            formatting.superscript
}
