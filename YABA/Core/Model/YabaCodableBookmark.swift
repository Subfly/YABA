//
//  YabaCodableBookmark.swift
//  YABA
//
//  Created by Ali Taha on 23.05.2025.
//

import Foundation

struct YabaCodableBookmark: Codable, Hashable {
    let bookmarkId: String?
    let label: String?
    let bookmarkDescription: String?
    let link: String // Only non optional param
    let domain: String?
    let createdAt: String?
    let editedAt: String?
    let imageUrl: String?
    let iconUrl: String?
    let videoUrl: String?
    let readableHTML: String?
    let type: Int?
}
