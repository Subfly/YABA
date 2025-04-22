//
//  BookmarkType.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

enum BookmarkType: Int, Codable, CaseIterable {
    case none = 1
    case webLink = 2
    case video = 3
    case image = 4
    case audio = 5
    case music = 6
    
    func getIconName() -> String {
        return switch self {
        case .none: "bookmark"
        case .webLink: "safari"
        case .video: "video"
        case .image: "photo"
        case .audio: "headphones"
        case .music: "music.note"
        }
    }
    
    func getUITitle() -> String {
        return switch self {
        case .none: "Other"
        case .webLink: "Web Link"
        case .video: "Video"
        case .image: "Image"
        case .audio: "Audio"
        case .music: "Music"
        }
    }
}
