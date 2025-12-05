package dev.subfly.yabacore.common

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

    /**
     * Shared filesystem layout definitions.
     *
     * All on-disk bookmark assets must remain under the app-owned root so we can move, delete and
     * sync them deterministically across platforms.
     */

    @OptIn(ExperimentalUuidApi::class)
    object FileSystem {
        const val ROOT_DIR = "YABA"
        const val BOOKMARKS_DIR = "bookmarks"

        object Linkmark {
            const val DIRECTORY = "linkmark"
            const val LINK_IMAGE_BASENAME = "link_image"
            const val DOMAIN_ICON_BASENAME = "domain_icon"
            const val HTML_EXPORTS_DIR = "html_exports"
            fun bookmarkFolder(bookmarkId: Uuid): String = bookmarkFolderPath(bookmarkId, DIRECTORY)

            fun linkImagePath(
                bookmarkId: Uuid,
                extension: String = "jpeg",
            ): String = join(
                bookmarkFolder(bookmarkId),
                "$LINK_IMAGE_BASENAME.$extension",
            )

            fun domainIconPath(
                bookmarkId: Uuid,
                extension: String = "png",
            ): String = join(
                bookmarkFolder(bookmarkId),
                "$DOMAIN_ICON_BASENAME.$extension",
            )

            fun htmlExportsDir(bookmarkId: Uuid): String =
                join(bookmarkFolder(bookmarkId), HTML_EXPORTS_DIR)

            fun htmlExportPath(
                bookmarkId: Uuid,
                exportFileName: String,
            ): String = join(htmlExportsDir(bookmarkId), exportFileName)
        }

        fun bookmarkFolderPath(
            bookmarkId: Uuid,
            subtypeDirectory: String? = null,
        ): String = join(
            BOOKMARKS_DIR,
            bookmarkId.toString(),
            subtypeDirectory,
        )

        fun join(vararg segments: String?): String =
            segments
                .filterNot { it.isNullOrBlank() }
                .joinToString(separator = "/") {
                    it!!.trim('/')
                }
    }
}
