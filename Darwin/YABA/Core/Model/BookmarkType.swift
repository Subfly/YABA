//
//  BookmarkType.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import SwiftUI

enum BookmarkType: Int, Codable, CaseIterable {
    case none = 1
    case webLink = 2
    case video = 3
    case image = 4
    case audio = 5
    case music = 6
    
    func getIconName() -> String {
        return switch self {
        case .none: "bookmark-02"
        case .webLink: "safari"
        case .video: "video-01"
        case .image: "image-03"
        case .audio: "headphones"
        case .music: "music-note-01"
        }
    }
    
    func getUITitle() -> LocalizedStringKey {
        return switch self {
        case .none: LocalizedStringKey("Bookmark Type None")
        case .webLink: LocalizedStringKey("Bookmark Type Link")
        case .video: LocalizedStringKey("Bookmark Type Video")
        case .image: LocalizedStringKey("Bookmark Type Image")
        case .audio: LocalizedStringKey("Bookmark Type Audio")
        case .music: LocalizedStringKey("Bookmark Type Music")
        }
    }
}
