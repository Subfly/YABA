package dev.subfly.yabacore.model.utils

/**
 * Defines the context in which folder selection is being performed.
 * This affects which folders are shown and how filtering is applied.
 */
enum class FolderSelectionMode {
    /**
     * Simple folder selection for bookmarks.
     * Shows all folders without exclusions.
     */
    FOLDER_SELECTION,

    /**
     * Selecting a parent folder for another folder.
     * Excludes the folder itself, its current parent, and all descendants
     * to prevent circular references.
     */
    PARENT_SELECTION,

    /**
     * Moving a bookmark to a different folder.
     * Excludes the bookmark's current containing folder.
     */
    BOOKMARK_MOVE,
}
