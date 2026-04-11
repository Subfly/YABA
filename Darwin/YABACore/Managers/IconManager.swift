//
//  IconManager.swift
//  YABACore
//
//  Bundled icon taxonomy (Compose `IconManager`). Loads metadata JSON via `BundleReader` from
//  `Assets/Metadata/` under `Darwin/YABACore/Assets`.
//

import Foundation

public struct YabaIconCategory: Sendable, Identifiable, Hashable, Codable {
    public var id: String
    public var name: String
    public var iconCount: Int
    public var filename: String
    public var headerIcon: String
    public var color: Int

    enum CodingKeys: String, CodingKey {
        case id, name, filename, color
        case iconCount = "icon_count"
        case headerIcon = "header_icon"
    }

    public init(
        id: String,
        name: String,
        iconCount: Int,
        filename: String,
        headerIcon: String,
        color: Int
    ) {
        self.id = id
        self.name = name
        self.iconCount = iconCount
        self.filename = filename
        self.headerIcon = headerIcon
        self.color = color
    }
}

public struct YabaIconItem: Sendable, Hashable, Codable {
    public var name: String

    public init(name: String) {
        self.name = name
    }
}

private struct IconHeaderFile: Decodable {
    let categories: [YabaIconCategory]
}

private struct IconCategoryFile: Decodable {
    let icons: [YabaIconItem]
}

public enum IconManager {
    private static let headerAssetPath = "Assets/Metadata/icon_categories_header.json"

    /// Returns categories from `Assets/Metadata/icon_categories_header.json` in the given bundle, or `[]` if missing.
    public static func loadAllCategories(in bundle: Bundle = .main) async -> [YabaIconCategory] {
        await Task.detached {
            guard let url = BundleReader.url(forAssetPath: headerAssetPath, in: bundle),
                  let data = try? Data(contentsOf: url),
                  let decoded = try? JSONDecoder().decode(IconHeaderFile.self, from: data)
            else {
                return []
            }
            return decoded.categories
        }.value
    }

    /// Loads icon name list from `Assets/Metadata/<category.filename>` in the bundle, or `[]` if missing.
    public static func loadIconsForCategory(_ category: YabaIconCategory, in bundle: Bundle = .main) async -> [YabaIconItem] {
        await Task.detached {
            let name = category.filename.replacingOccurrences(of: ".json", with: "")
            let assetPath = "Assets/Metadata/\(name).json"
            guard let url = BundleReader.url(forAssetPath: assetPath, in: bundle),
                  let data = try? Data(contentsOf: url),
                  let decoded = try? JSONDecoder().decode(IconCategoryFile.self, from: data)
            else {
                return []
            }
            return decoded.icons
        }.value
    }
}
