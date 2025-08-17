//
//  Constants.swift
//  YABA
//
//  Created by Ali Taha on 18.04.2025.
//

import Foundation

struct Constants {
    /// MARK: URLS
    static let yabaRepoLink = "https://github.com/Subfly/YABA"
    static let eulaLink = "https://github.com/Subfly/YABA/blob/main/EULA.md"
    static let tosLink = "https://github.com/Subfly/YABA/blob/main/TERMS_OF_SERVICE.md"
    static let privacyPolicyLink = "https://github.com/Subfly/YABA/blob/main/PRIVACY_POLICY.md"
    static let developerWebsiteLink = "https://www.subfly.dev/"
    static let feedbackLink = "mailto:alitaha@subfly.dev"
    static let storeLink = "https://apps.apple.com/app/yaba-yet-another-bookmark-app/id6747272081"
    static let updateAnnouncementLink_1_2 = "https://github.com/Subfly/YABA/discussions/4"
    static let updateAnnouncementLink_1_3 = "https://github.com/Subfly/YABA/discussions/6"
    static let announcementCloudKitDropLink_1 = "https://github.com/Subfly/YABA/discussions/5"
    static let announcementCloudKitDropLink_2 = "https://github.com/Subfly/YABA/discussions/7"
    
    /// MARK: SETTINGS
    static let hasPassedOnboardingKey = "hasPassedOnboarding"
    static let preferredThemeKey = "preferredTheme"
    static let preferredContentAppearanceKey = "preferredContentAppearance"
    static let preferredCardImageSizingKey = "preferredCardImageSizing"
    static let preferredSortingKey = "preferredSorting"
    static let preferredSortOrderKey = "preferredSortOrder"
    static let deviceIdKey = "deviceId"
    static let showRecentsKey = "showRecents"
    static let showMenuBarItem = "showMenuBarItem"
    static let useSimplifiedShare = "useSimplifiedShare"
    
    /// MARK: APP-WISE
    static let toastAnimationDuration: UInt64 = 150_000_000
    static let uncategorizedCollectionId: String = "-1"
    static let uncategorizedCollectionLabelKey: String = "Uncategorized Label"
    static let port: Int = 7484
    
    /// MARK: ANNOUNCEMENTS
    static let announcementsYaba1_2UpdateKey = "announcementsYaba1_2UpdateKey"
    static let announcementsYaba1_3UpdateKey = "announcementsYaba1_3UpdateKey"
    static let announcementsCloudKitDropKey = "announcementsCloudKitDropKey"
    static let announcementsCloudKitDropKeyUrgent = "announcementsCloudKitDropKeyUrgent"
    
    /// MARK: MAPPER
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
          "bookmarks": ["bookmark-id-1", "bookmark-id-2"]
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
