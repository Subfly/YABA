//
//  YabaCodableBookmark.swift
//  YABA
//
//  Created by Ali Taha on 23.05.2025.
//

import Foundation
import CoreTransferable

struct YabaCodableBookmark: Codable, Hashable, Transferable {
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
    let version: Int?
    
    // Image data fields for sync (not included in exports)
    let imageData: Data?
    let iconData: Data?
    
    static var transferRepresentation: some TransferRepresentation {
        CodableRepresentation(contentType: .yabaBookmark)
    }
}
