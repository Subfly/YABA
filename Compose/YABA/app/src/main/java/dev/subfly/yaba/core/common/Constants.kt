package dev.subfly.yaba.core.common

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
         * Set of reserved folder IDs that are system folders. System folders cannot be truly
         * deleted - they self-heal.
         */
        val SYSTEM_FOLDER_IDS: Set<String> = setOf(Uncategorized.ID)

        /** Checks if a folder ID is a system folder. */
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
            const val ICON: String = "circle-lock-02"
        }

        /**
         * Set of reserved tag IDs that are system tags. System tags cannot be truly deleted - they
         * self-heal.
         */
        val SYSTEM_TAG_IDS: Set<String> = setOf(Pinned.ID, Private.ID)

        /** Checks if a tag ID is a system tag. */
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

        /** Bookmark-kind-neutral root: bookmarks/<id>/ */
        fun bookmarkFolder(bookmarkId: String): String = join(BOOKMARKS_DIR, bookmarkId)

        object Linkmark {
            const val LINK_IMAGE_BASENAME = "link_image"
            const val DOMAIN_ICON_BASENAME = "domain_icon"
            const val HTML_EXPORTS_DIR = "html_exports"
            const val READABLE_DIR = "readable"
            const val ASSETS_DIR = "assets"

            fun linkImagePath(
                bookmarkId: String,
                extension: String = "jpeg",
            ): String =
                join(
                    bookmarkFolder(bookmarkId),
                    "$LINK_IMAGE_BASENAME.$extension",
                )

            fun domainIconPath(
                bookmarkId: String,
                extension: String = "png",
            ): String =
                join(
                    bookmarkFolder(bookmarkId),
                    "$DOMAIN_ICON_BASENAME.$extension",
                )

            fun readableDir(bookmarkId: String): String =
                join(bookmarkFolder(bookmarkId), READABLE_DIR)

            /**
             * Path for readable document JSON (linkmarks + notemark mirror):
             * readable/<versionId>.json
             */
            fun readableVersionPath(bookmarkId: String, versionId: String): String =
                join(readableDir(bookmarkId), "$versionId.json")

            fun assetsDir(bookmarkId: String): String = join(bookmarkFolder(bookmarkId), ASSETS_DIR)

            fun assetPath(bookmarkId: String, assetId: String, extension: String): String =
                join(assetsDir(bookmarkId), "$assetId.$extension")
        }

        object Imagemark {
            const val IMAGE_BASENAME = "image"

            fun imagePath(
                bookmarkId: String,
                extension: String = "jpeg",
            ): String =
                join(
                    bookmarkFolder(bookmarkId),
                    "$IMAGE_BASENAME.$extension",
                )
        }

        object Docmark {
            private const val DOC_SUBDIR = "file"
            private const val DOC_BASENAME = "document"
            private const val DEFAULT_DOC_EXTENSION = "pdf"

            /** On-disk document: `bookmarks/<id>/file/document.<ext>`. */
            fun documentPath(
                bookmarkId: String,
                extension: String = DEFAULT_DOC_EXTENSION,
            ): String =
                join(
                    bookmarkFolder(bookmarkId),
                    DOC_SUBDIR,
                    "$DOC_BASENAME.$extension",
                )
        }

        object Notemark {
            private const val NOTE_SUBDIR = "note"
            private const val NOTE_BODY_BASENAME = "body"

            /** Canonical note body (document JSON): bookmarks/<id>/note/body.json */
            fun documentBodyPath(bookmarkId: String, extension: String = "json"): String =
                join(
                    bookmarkFolder(bookmarkId),
                    NOTE_SUBDIR,
                    "$NOTE_BODY_BASENAME.$extension",
                )
        }

        object Canvmark {
            private const val CANVAS_SUBDIR = "canvas"
            private const val SCENE_BASENAME = "scene"

            /** Canonical canvmark scene JSON: bookmarks/<id>/canvas/scene.json */
            fun scenePath(bookmarkId: String, extension: String = "json"): String =
                join(
                    bookmarkFolder(bookmarkId),
                    CANVAS_SUBDIR,
                    "$SCENE_BASENAME.$extension",
                )
        }

        fun bookmarkFolderPath(
            bookmarkId: String,
            subtypeDirectory: String? = null,
        ): String =
            join(
                BOOKMARKS_DIR,
                bookmarkId,
                subtypeDirectory,
            )

        fun join(vararg segments: String?): String =
            segments.filterNot { it.isNullOrBlank() }.joinToString(separator = "/") {
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
        const val PRIVATE_BOOKMARK_PASSWORD_HASH = "privateBookmarkPasswordHash"
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
