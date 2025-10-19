//
//  Bookmark.swift
//  YABA
//
//  Created by Ali Taha on 8.10.2024.
//

import Foundation
import SwiftUI

/// FOR THE MODEL ITSELF, LOOK TO _YABA_SCHEMA_
extension YabaBookmark {
    static func empty() -> YabaBookmark {
        YabaBookmark(
            bookmarkId: UUID().uuidString,
            link: "https://www.google.com/",
            label: "Google",
            bookmarkDescription: "Google Web Site",
            domain: "google.com",
            createdAt: .now,
            editedAt: .now,
            imageDataHolder: nil,
            iconDataHolder: nil,
            imageUrl: nil,
            iconUrl: nil,
            videoUrl: nil,
            readableHTML: nil,
            type: .none,
            version: 0,
            collections: []
        )
    }
    
    static func empty(withLink link: String) -> YabaBookmark {
        YabaBookmark(
            bookmarkId: UUID().uuidString,
            link: link,
            label: "",
            bookmarkDescription: "",
            domain: "",
            createdAt: .now,
            editedAt: .now,
            imageDataHolder: nil,
            iconDataHolder: nil,
            imageUrl: nil,
            iconUrl: nil,
            videoUrl: nil,
            readableHTML: nil,
            type: .none,
            version: 0,
            collections: []
        )
    }
    
    func getFolderColor() -> Color {
        return collections?.first { collection in
            collection.collectionType == .folder
        }?.color.getUIColor() ?? .accentColor
    }
}
