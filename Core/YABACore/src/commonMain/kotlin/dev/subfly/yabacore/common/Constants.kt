package dev.subfly.yabacore.common

/** Core-wide constants used by the data layer. */
object CoreConstants {
    // Stable, reserved UUID for the default "Uncategorized" folder.
    const val UNCATEGORIZED_FOLDER_ID: String = "11111111-1111-1111-1111-111111111111"

    // Human-readable name and a safe default icon name; UI may localize the name as needed.
    const val UNCATEGORIZED_FOLDER_NAME: String = "Uncategorized"
    const val UNCATEGORIZED_FOLDER_ICON: String = "folder-01"
}
