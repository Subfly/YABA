package dev.subfly.yabacore.model.utils

/**
 * Defines the context in which folder selection is being performed. This affects which folders are
 * shown and how filtering is applied.
 */
enum class FolderSelectionMode {
    /** Simple folder selection for bookmarks. Shows all folders without exclusions. */
    FOLDER_SELECTION,

    /**
     * Selecting a parent folder for another folder during creation/editing. Excludes the folder
     * itself, its current parent, and all descendants to prevent circular references.
     */
    PARENT_SELECTION,

    /**
     * Moving a folder directly to another folder. Same filtering as PARENT_SELECTION: excludes the
     * folder itself, its current parent, and all descendants to prevent circular references.
     */
    FOLDER_MOVE,

    /**
     * Moving bookmarks to a different folder. Excludes the bookmarks' current containing folder.
     */
    BOOKMARKS_MOVE,
}
