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

    /**
     * Shared key names used for user preferences. Keep values stable to allow migrations from
     * legacy AppStorage / UserDefaults.
     */
    object Settings {
        const val HAS_PASSED_ONBOARDING = "hasPassedOnboarding"
        const val HAS_NAMED_DEVICE = "hasNamedDevice"
        const val PREFERRED_THEME = "preferredTheme"
        const val PREFERRED_BOOKMARK_APPEARANCE = "preferredBookmarkAppearance"
        const val PREFERRED_CARD_IMAGE_SIZING = "preferredCardImageSizing"
        const val PREFERRED_COLLECTION_SORTING = "preferredCollectionSorting"
        const val PREFERRED_COLLECTION_SORT_ORDER = "preferredCollectionSortOrder"
        const val PREFERRED_BOOKMARK_SORTING = "preferredBookmarkSorting"
        const val PREFERRED_BOOKMARK_SORT_ORDER = "preferredBookmarkSortOrder"
        const val PREFERRED_FAB_POSITION = "preferredFabPosition"
        const val DISABLE_BACKGROUND_ANIMATION = "disableBackgroundAnimation"
        const val DEVICE_ID = "deviceId"
        const val DEVICE_NAME = "deviceName"
        const val SHOW_RECENTS = "showRecents"
        const val SHOW_MENU_BAR_ITEM = "showMenuBarItem" // mac / catalyst only
        const val USE_SIMPLIFIED_SHARE = "useSimplifiedShare" // share extension uses app-group
        const val PREVENT_DELETION_SYNC = "preventDeletionSync"
    }

    /**
     * Announcement toggle keys – we keep the legacy names so we can suppress already-viewed
     * announcements on migrated installs.
     */
    object Announcements {
        const val YABA_1_2_UPDATE = "announcementsYaba1_2UpdateKey"
        const val YABA_1_3_UPDATE = "announcementsYaba1_3UpdateKey"
        const val YABA_1_4_UPDATE = "announcementsYaba1_4UpdateKey"
        const val YABA_1_5_UPDATE = "announcementsYaba1_5UpdateKey"
        const val CLOUDKIT_DROP = "announcementsCloudKitDropKey"
        const val CLOUDKIT_DROP_URGENT = "announcementsCloudKitDropKeyUrgent"
        const val CLOUDKIT_DATABASE_WIPE = "announcementsCloudKitDatabaseWipe"
        const val LEGALS_UPDATE = "announcementsLegalsUpdate"
        const val LEGALS_UPDATE_2 = "announcementsLegalsUpdate_2"
    }

    object Urls {
        const val YABA_REPO = "https://github.com/Subfly/YABA"
        const val EULA = "https://github.com/Subfly/YABA/blob/main/EULA.md"
        const val TOS = "https://github.com/Subfly/YABA/blob/main/TERMS_OF_SERVICE.md"
        const val PRIVACY_POLICY = "https://github.com/Subfly/YABA/blob/main/PRIVACY_POLICY.md"
        const val DEVELOPER_SITE = "https://www.subfly.dev/"
        const val OFFICIAL_REDDIT = "https://www.reddit.com/r/YetAnotherBookmarkApp/"
        const val FEEDBACK_EMAIL = "mailto:alitaha@subfly.dev"
        const val STORE_LINK =
            "https://apps.apple.com/app/yaba-yet-another-bookmark-app/id6747272081"
        const val UPDATE_1_2 = "https://github.com/Subfly/YABA/discussions/4"
        const val UPDATE_1_3 = "https://github.com/Subfly/YABA/discussions/6"
        const val UPDATE_1_4 = "https://github.com/Subfly/YABA/discussions/8"
        const val UPDATE_1_5 = "https://github.com/Subfly/YABA/discussions/12"
        const val CLOUDKIT_DROP_1 = "https://github.com/Subfly/YABA/discussions/5"
        const val CLOUDKIT_DROP_2 = "https://github.com/Subfly/YABA/discussions/7"
        const val CLOUDKIT_DROP_3 = "https://github.com/Subfly/YABA/discussions/10"
        const val LEGALS_UPDATE_1 = "https://github.com/Subfly/YABA/discussions/9"
        const val LEGALS_UPDATE_2 = "https://github.com/Subfly/YABA/discussions/13"
    }
}
