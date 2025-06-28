//
//  YabaCodableCollection.swift
//  YABA
//
//  Created by Ali Taha on 23.05.2025.
//

import Foundation

struct YabaCodableCollection: Codable, Hashable {
    let collectionId: String
    let label: String
    let icon: String
    let createdAt: String
    let editedAt: String
    let color: Int
    let type: Int
    let bookmarks: [String] // String id's of stored bookmarks
    let version: Int
}
