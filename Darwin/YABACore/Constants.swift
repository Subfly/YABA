//
//  Constants.swift
//  YABACore
//
//  Merged from Compose `CoreConstants` and legacy Darwin `Constants`.
//

import Foundation

// swiftlint:disable type_body_length file_length

/// App-wide constants (URLs, settings keys, system folder/tag ids).
enum Constants {

    // MARK: - Compose parity — Folder

    enum Folder {
        enum Uncategorized {
            /// Stable reserved id for the default uncategorized folder (matches Compose `CoreConstants.Folder.Uncategorized.ID`).
            static let id = "11111111-1111-1111-1111-111111111111"
            static let name = "Uncategorized"
            static let descriptionText = "Uncategorized Folder Description"
            static let icon = "folder-01"
        }

        static var systemFolderIds: Set<String> { [Uncategorized.id] }

        static func isSystemFolder(_ folderId: String) -> Bool {
            systemFolderIds.contains(folderId)
        }
    }

    // MARK: - Compose parity — Tag

    enum Tag {
        enum Pinned {
            static let id = "21111111-1111-1111-1111-111111111112"
            static let name = "Pinned Tag Label"
            static let icon = "pin"
        }

        enum Private {
            static let id = "31111111-1111-1111-1111-111111111113"
            static let name = "Private Tag Label"
            static let icon = "circle-lock-02"
        }

        static var systemTagIds: Set<String> { [Pinned.id, Private.id] }

        static func isSystemTag(_ tagId: String) -> Bool {
            systemTagIds.contains(tagId)
        }
    }

    // MARK: - Compose parity — Settings keys (string values match Compose `CoreConstants.Settings`)

    enum SettingsKeys {
        static let hasPassedOnboarding = "hasPassedOnboarding"
        static let hasNamedDevice = "hasNamedDevice"
        static let preferredTheme = "preferredTheme"
        static let preferredBookmarkAppearance = "preferredBookmarkAppearance"
        static let preferredCardImageSizing = "preferredCardImageSizing"
        static let preferredCollectionSorting = "preferredCollectionSorting"
        static let preferredCollectionSortOrder = "preferredCollectionSortOrder"
        static let preferredBookmarkSorting = "preferredBookmarkSorting"
        static let preferredBookmarkSortOrder = "preferredBookmarkSortOrder"
        static let preferredFabPosition = "preferredFabPosition"
        static let disableBackgroundAnimation = "disableBackgroundAnimation"
        static let deviceId = "deviceId"
        static let deviceName = "deviceName"
        static let showRecents = "showRecents"
        static let showMenuBarItem = "showMenuBarItem"
        static let useSimplifiedShare = "useSimplifiedShare"
        static let preventDeletionSync = "preventDeletionSync"
        static let privateBookmarkPasswordHash = "privateBookmarkPasswordHash"
    }

    // MARK: - Compose parity — Announcement keys

    enum AnnouncementKeys {
        static let yaba1_2Update = "announcementsYaba1_2UpdateKey"
        static let yaba1_3Update = "announcementsYaba1_3UpdateKey"
        static let yaba1_4Update = "announcementsYaba1_4UpdateKey"
        static let yaba1_5Update = "announcementsYaba1_5UpdateKey"
        static let cloudKitDrop = "announcementsCloudKitDropKey"
        static let cloudKitDropUrgent = "announcementsCloudKitDropKeyUrgent"
        static let cloudKitDatabaseWipe = "announcementsCloudKitDatabaseWipe"
        static let legalsUpdate = "announcementsLegalsUpdate"
        static let legalsUpdate2 = "announcementsLegalsUpdate_2"
    }

    // MARK: - Compose parity — URLs

    enum Urls {
        static let yabaRepo = "https://github.com/Subfly/YABA"
        static let eula = "https://github.com/Subfly/YABA/blob/main/EULA.md"
        static let tos = "https://github.com/Subfly/YABA/blob/main/TERMS_OF_SERVICE.md"
        static let privacyPolicy = "https://github.com/Subfly/YABA/blob/main/PRIVACY_POLICY.md"
        static let developerSite = "https://www.subfly.dev/"
        static let officialReddit = "https://www.reddit.com/r/YetAnotherBookmarkApp/"
        static let feedbackEmail = "mailto:alitaha@subfly.dev"
        static let store =
            "https://apps.apple.com/app/yaba-yet-another-bookmark-app/id6747272081"
        static let update1_2 = "https://github.com/Subfly/YABA/discussions/4"
        static let update1_3 = "https://github.com/Subfly/YABA/discussions/6"
        static let update1_4 = "https://github.com/Subfly/YABA/discussions/8"
        static let update1_5 = "https://github.com/Subfly/YABA/discussions/12"
        static let cloudKitDrop1 = "https://github.com/Subfly/YABA/discussions/5"
        static let cloudKitDrop2 = "https://github.com/Subfly/YABA/discussions/7"
        static let cloudKitDrop3 = "https://github.com/Subfly/YABA/discussions/10"
        static let legalsUpdate1 = "https://github.com/Subfly/YABA/discussions/9"
        static let legalsUpdate2 = "https://github.com/Subfly/YABA/discussions/13"
    }

    // MARK: - Darwin flat API (legacy call sites)

    static let yabaRepoLink = Urls.yabaRepo
    static let eulaLink = Urls.eula
    static let tosLink = Urls.tos
    static let privacyPolicyLink = Urls.privacyPolicy
    static let developerWebsiteLink = Urls.developerSite
    static let officialRedditLink = Urls.officialReddit
    static let feedbackLink = Urls.feedbackEmail
    static let storeLink = Urls.store

    static let updateAnnouncementLink_1_2 = Urls.update1_2
    static let updateAnnouncementLink_1_3 = Urls.update1_3
    static let updateAnnouncementLink_1_4 = Urls.update1_4
    static let updateAnnouncementLink_1_5 = Urls.update1_5
    static let announcementCloudKitDropLink_1 = Urls.cloudKitDrop1
    static let announcementCloudKitDropLink_2 = Urls.cloudKitDrop2
    static let announcementCloudKitDropLink_3 = Urls.cloudKitDrop3
    static let announcementLegalsUpdateLink_1 = Urls.legalsUpdate1
    static let announcementLegalsUpdateLink_2 = Urls.legalsUpdate2

    static let hasPassedOnboardingKey = SettingsKeys.hasPassedOnboarding
    static let hasNamedDeviceKey = SettingsKeys.hasNamedDevice
    static let preferredThemeKey = SettingsKeys.preferredTheme
    /// Legacy Darwin key (differs from Compose name); kept for `UserDefaults` / `@AppStorage` continuity.
    static let preferredContentAppearanceKey = "preferredContentAppearance"
    static let preferredCardImageSizingKey = SettingsKeys.preferredCardImageSizing
    static let preferredCollectionSortingKey = SettingsKeys.preferredCollectionSorting
    static let preferredBookmarkSortingKey = SettingsKeys.preferredBookmarkSorting
    static let preferredSortOrderKey = "preferredSortOrder"
    static let preferredFabPositionKey = SettingsKeys.preferredFabPosition
    static let disableBackgroundAnimationKey = SettingsKeys.disableBackgroundAnimation
    static let deviceIdKey = SettingsKeys.deviceId
    static let deviceNameKey = SettingsKeys.deviceName
    static let showRecentsKey = SettingsKeys.showRecents
    static let showMenuBarItem = SettingsKeys.showMenuBarItem
    static let useSimplifiedShare = SettingsKeys.useSimplifiedShare
    static let preventDeletionSyncKey = SettingsKeys.preventDeletionSync

    static let toastAnimationDuration: UInt64 = 150_000_000
    static let port: Int = 7484

    /// Canonical uncategorized folder id (Compose `CoreConstants.Folder.Uncategorized.ID`).
    static let uncategorizedCollectionId = Folder.Uncategorized.id

    /// Legacy Darwin uncategorized collection id before parity with Compose (`"-1"`).
    static let legacyUncategorizedCollectionId = "-1"

    static let uncategorizedCollectionLabelKey: String = "Uncategorized Label"

    static let announcementsYaba1_2UpdateKey = AnnouncementKeys.yaba1_2Update
    static let announcementsYaba1_3UpdateKey = AnnouncementKeys.yaba1_3Update
    static let announcementsYaba1_4UpdateKey = AnnouncementKeys.yaba1_4Update
    static let announcementsYaba1_5UpdateKey = AnnouncementKeys.yaba1_5Update
    static let announcementsCloudKitDropKey = AnnouncementKeys.cloudKitDrop
    static let announcementsCloudKitDropKeyUrgent = AnnouncementKeys.cloudKitDropUrgent
    static let announcementsCloudKitDatabaseWipeKey = AnnouncementKeys.cloudKitDatabaseWipe
    static let announcementsLegalsUpdateKey = AnnouncementKeys.legalsUpdate
    static let announcementsLegalsUpdate_2Key = AnnouncementKeys.legalsUpdate2

    static let jsonExample: String = """
    {
      "id": "unique-import-id",
      "exportedFrom": "app-name-or-source",
      "collections": [
        {
          "collectionId": "uuid",
          "label": "Work Stuff",
          "icon": "briefcase",
          "createdAt": "2025-05-23T10:00:00Z",
          "editedAt": "2025-05-23T10:30:00Z",
          "color": 1,
          "type": 1,
          "bookmarks": ["bookmark-uuid-1", "bookmark-uuid-2"],
          "version": 1,
          "parent": "parent-id",
          "children": ["child-folder-uuid-1", "child-folder-uuid-2"],
          "order": 0
        }
      ],
      "bookmarks": [
        {
          "bookmarkId": "bookmark-id-1",
          "label": "Swift Documentation",
          "bookmarkDescription": "Official docs for Swift programming language.",
          "link": "https://swift.org/documentation/",
          "domain": "swift.org",
          "createdAt": "2025-05-20T08:00:00Z",
          "editedAt": "2025-05-21T09:00:00Z",
          "imageUrl": "https://example.com/image.png",
          "iconUrl": "https://example.com/favicon.ico",
          "videoUrl": null,
          "type": 2,
          "version": 1
        }
      ]
    }
    """
}
