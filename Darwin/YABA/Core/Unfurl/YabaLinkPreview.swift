//
//  YabaLinkPreview.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import Foundation

struct YabaLinkPreview {
    let url: String
    let title: String?
    let description: String?
    let host: String?
    let iconURL: String?
    let imageURL: String? // Keep for backward compatibility
    let videoURL: String?
    let iconData: Data?
    let imageData: Data? // Keep for backward compatibility
    let imageOptions: [String: Data] // New: hashmap of imageURL to imageData
    let readableHTML: String?
}
