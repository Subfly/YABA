package dev.subfly.yabacore.common

/** Core-wide constants used by the data layer. */
object CoreConstants {
    object Folder {
        object Uncategorized {
            // Stable, reserved UUID for the default "Uncategorized" folder.
            const val ID: String = "11111111-1111-1111-1111-111111111111"

            // Human-readable name and a safe default icon name; UI may localize the name as needed.
            const val NAME: String = "Uncategorized"
            const val ICON: String = "folder-01"
        }
    }

    object Tag
}
