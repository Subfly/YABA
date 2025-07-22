//
//  YabaIcon.swift
//  YABA
//
//  Created by Ali Taha on 2.05.2025.
//

import Foundation

struct YabaIcon: Hashable, Codable {
    let name: String
    let tags: String
    let category: String
}

struct PreloadYabaIconHolder: Codable {
    let icons: [YabaIcon]
}

// MARK: - New Hierarchical Icon Models

struct IconMetadata: Codable {
    let totalCategories: Int
    let totalSubcategories: Int
    let totalIcons: Int
    let version: String
    let description: String
    
    enum CodingKeys: String, CodingKey {
        case totalCategories = "total_categories"
        case totalSubcategories = "total_subcategories"
        case totalIcons = "total_icons"
        case version, description
    }
}

struct IconSubcategory: Codable, Hashable {
    let id: String
    let name: String
    let description: String
    let headerIcon: String
    let color: Int
    let iconCount: Int
    let filename: String
    
    enum CodingKeys: String, CodingKey {
        case id, name, description, color, filename
        case headerIcon = "header_icon"
        case iconCount = "icon_count"
    }
}

struct IconCategory: Codable, Hashable {
    let id: String
    let name: String
    let description: String
    let iconCount: Int
    let filename: String
    let headerIcon: String
    let color: Int
    let subcategories: [IconSubcategory]
    
    enum CodingKeys: String, CodingKey {
        case id, name, description, filename, color, subcategories
        case iconCount = "icon_count"
        case headerIcon = "header_icon"
    }
}

struct IconHeader: Codable {
    let metadata: IconMetadata
    let categories: [IconCategory]
}


