//
//  Bookmark.swift
//  YABA
//
//  Created by Ali Taha on 8.10.2024.
//

import Foundation
import SwiftData
import SwiftUI

@Model
final class Bookmark {
    @Attribute(.externalStorage, .allowsCloudEncryption)
    var imageData: Data?
    
    @Attribute(.externalStorage, .allowsCloudEncryption)
    var iconData: Data?
    
    @Attribute(.spotlight)
    var label: String
    
    @Attribute(.spotlight)
    var bookmarkDescription: String
    
    var link: String
    var domain: String
    var createdAt: Date
    var videoUrl: String?
    var collections: [YabaCollection]

    init(
        link: String,
        label: String,
        bookmarkDescription: String,
        domain: String,
        createdAt: Date,
        imageData: Data?,
        iconData: Data?,
        videoUrl: String?,
        collections: [YabaCollection] = []
    ) {
        self.link = link
        self.label = label
        self.bookmarkDescription = bookmarkDescription
        self.domain = domain
        self.createdAt = createdAt
        self.imageData = imageData
        self.iconData = iconData
        self.videoUrl = videoUrl
        self.collections = collections
    }
    
    static func empty() -> Bookmark {
        Bookmark(
            link: "",
            label: "",
            bookmarkDescription: "",
            domain: "",
            createdAt: .now,
            imageData: nil,
            iconData: nil,
            videoUrl: nil,
            collections: []
        )
    }
    
    static func empty(withLink link: String) -> Bookmark {
        Bookmark(
            link: link,
            label: "",
            bookmarkDescription: "",
            domain: "",
            createdAt: .now,
            imageData: nil,
            iconData: nil,
            videoUrl: nil,
            collections: []
        )
    }
}
