//
//  YabaExportableContent.swift
//  YABA
//
//  Created by Ali Taha on 23.05.2025.
//

import Foundation

struct YabaCodableContent: Codable, Hashable {
    let id: String?
    let exportedFrom: String?
    let collections: [YabaCodableCollection]?
    let bookmarks: [YabaCodableBookmark]
}
