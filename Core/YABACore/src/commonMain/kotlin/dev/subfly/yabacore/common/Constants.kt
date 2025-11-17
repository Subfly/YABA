package dev.subfly.yabacore.common

/** Core-wide constants used by the data layer. */
object CoreConstants {
    object Folder {
        object Uncategorized {
            // Stable, reserved UUID for the default "Uncategorized" folder.
            const val ID: String = "11111111-1111-1111-1111-111111111111"

            // Human-readable name, description and a safe default
            // icon name; UI may localize the name and description as needed.
            const val NAME: String = "Uncategorized"
            const val DESCRIPTION: String = "Uncategorized Folder Description"
            const val ICON: String = "folder-01"
        }
    }

    object Tag {
        object Pinned {
            // Stable, reserved UUID for the default "Pinned" Tag.
            const val ID: String = "21111111-1111-1111-1111-111111111112"

            // Human-readable name, and a safe default icon name;
            // UI may localize the name as needed.
            const val NAME: String = "Pinned Tag Label"
            const val ICON: String = "pin"
        }

        object Private {
            // Stable, reserved UUID for the default "Private" Tag.
            const val ID: String = "31111111-1111-1111-1111-111111111113"

            // Human-readable name, and a safe default icon name;
            // UI may localize the name as needed.
            const val NAME: String = "Private Tag Label"
            const val ICON: String = "view-off-slash"
        }
    }
}
