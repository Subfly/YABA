package dev.subfly.yabacore.model.utils

/**
 * How note body changes are persisted from the editor.
 */
enum class NoteSaveMode {
    /** Default: debounced save after ~3 seconds without edits. */
    AUTOSAVE_3S_INACTIVITY,

    /** User explicitly triggers save (e.g. toolbar / shortcut). */
    MANUAL,
}
