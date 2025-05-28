//
//  Constants.swift
//  YABA
//
//  Created by Ali Taha on 18.04.2025.
//

import Foundation

struct Constants {
    /// MARK: SETTINGS
    static let hasPassedOnboardingKey = "hasPassedOnboarding"
    static let preferredThemeKey = "preferredTheme"
    static let preferredContentAppearanceKey = "preferredContentAppearance"
    static let preferredCardImageSizingKey = "preferredCardImageSizing"
    static let preferredSortingKey = "preferredSorting"
    static let preferredSortOrderKey = "preferredSortOrder"
    
    /// MARK: APP-WISE
    static let toastAnimationDuration: UInt64 = 3_000_000
    static let uncategorizedCollectionId: String = "-1"
    static let uncategorizedCollectionLabelKey: String = "Uncategorized Label"
    static let port: Int = 8888
    
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
              "type": 2
            }
          ]
        }
        """
}
