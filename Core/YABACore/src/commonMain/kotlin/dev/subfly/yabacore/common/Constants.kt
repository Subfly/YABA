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

        /**
         * Set of reserved folder IDs that are system folders.
         * System folders cannot be truly deleted - they self-heal.
         */
        val SYSTEM_FOLDER_IDS: Set<String> = setOf(Uncategorized.ID)

        /**
         * Checks if a folder ID is a system folder.
         */
        fun isSystemFolder(folderId: String): Boolean = folderId in SYSTEM_FOLDER_IDS
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

        /**
         * Set of reserved tag IDs that are system tags.
         * System tags cannot be truly deleted - they self-heal.
         */
        val SYSTEM_TAG_IDS: Set<String> = setOf(Pinned.ID, Private.ID)

        /**
         * Checks if a tag ID is a system tag.
         */
        fun isSystemTag(tagId: String): Boolean = tagId in SYSTEM_TAG_IDS
    }

    /**
     * Shared filesystem layout definitions.
     *
     * All on-disk bookmark assets must remain under the app-owned root so we can move, delete and
     * sync them deterministically across platforms.
     */
    object FileSystem {
        const val ROOT_DIR = "YABA"
        const val BOOKMARKS_DIR = "bookmarks"
        const val FOLDERS_DIR = "folders"
        const val TAGS_DIR = "tags"
        const val SYNC_DIR = "sync"
        const val CONTENT_DIR = "content"

        // JSON file names
        const val META_JSON = "meta.json"
        const val LINK_JSON = "link.json"
        const val DELETED_JSON = "deleted.json"

        // Events database
        const val EVENTS_DB = "events.sqlite"

        // Entity folder paths
        fun folderPath(folderId: String): String = join(FOLDERS_DIR, folderId)
        fun tagPath(tagId: String): String = join(TAGS_DIR, tagId)
        fun bookmarkPath(bookmarkId: String): String = join(BOOKMARKS_DIR, bookmarkId)

        // JSON file paths for folders
        fun folderMetaPath(folderId: String): String = join(folderPath(folderId), META_JSON)
        fun folderDeletedPath(folderId: String): String = join(folderPath(folderId), DELETED_JSON)

        // JSON file paths for tags
        fun tagMetaPath(tagId: String): String = join(tagPath(tagId), META_JSON)
        fun tagDeletedPath(tagId: String): String = join(tagPath(tagId), DELETED_JSON)

        // JSON file paths for bookmarks
        fun bookmarkMetaPath(bookmarkId: String): String = join(bookmarkPath(bookmarkId), META_JSON)
        fun bookmarkLinkPath(bookmarkId: String): String = join(bookmarkPath(bookmarkId), LINK_JSON)
        fun bookmarkDeletedPath(bookmarkId: String): String = join(bookmarkPath(bookmarkId), DELETED_JSON)
        fun bookmarkContentPath(bookmarkId: String): String = join(bookmarkPath(bookmarkId), CONTENT_DIR)

        // Sync directory path
        fun syncPath(): String = SYNC_DIR
        fun eventsDbPath(): String = join(SYNC_DIR, EVENTS_DB)

        object Linkmark {
            const val LINK_IMAGE_BASENAME = "link_image"
            const val DOMAIN_ICON_BASENAME = "domain_icon"
            const val HTML_EXPORTS_DIR = "html_exports"
            fun bookmarkFolder(bookmarkId: String): String = bookmarkFolderPath(bookmarkId, CONTENT_DIR)

            fun linkImagePath(
                bookmarkId: String,
                extension: String = "jpeg",
            ): String = join(
                bookmarkFolder(bookmarkId),
                "$LINK_IMAGE_BASENAME.$extension",
            )

            fun domainIconPath(
                bookmarkId: String,
                extension: String = "png",
            ): String = join(
                bookmarkFolder(bookmarkId),
                "$DOMAIN_ICON_BASENAME.$extension",
            )

            fun htmlExportsDir(bookmarkId: String): String =
                join(bookmarkFolder(bookmarkId), HTML_EXPORTS_DIR)

            fun htmlExportPath(
                bookmarkId: String,
                exportFileName: String,
            ): String = join(htmlExportsDir(bookmarkId), exportFileName)
        }

        fun bookmarkFolderPath(
            bookmarkId: String,
            subtypeDirectory: String? = null,
        ): String = join(
            BOOKMARKS_DIR,
            bookmarkId,
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
