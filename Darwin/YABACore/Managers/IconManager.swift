//
//  IconManager.swift
//  YABACore
//
//  Bundled icon taxonomy (Compose `IconManager`). Darwin: load from app bundle resources when
//  `icon_categories_header.json` (and per-category files) are added to the YABACore or app target.
//

import Foundation

public struct YabaIconCategory: Sendable, Identifiable {
    public var id: String
    public var filename: String

    public init(id: String, filename: String) {
        self.id = id
        self.filename = filename
    }
}

public struct YabaIconItem: Sendable, Hashable {
    public var name: String

    public init(name: String) {
        self.name = name
    }
}

public enum IconManager {
    private static let headerResource = "icon_categories_header"
    private static let metadataSubdirectory = "metadata"

    /// Returns categories from `metadata/icon_categories_header.json` in the given bundle, or `[]` if missing.
    public static func loadAllCategories(in bundle: Bundle = .main) async -> [YabaIconCategory] {
        await Task.detached {
            guard let url = bundle.url(forResource: headerResource, withExtension: "json", subdirectory: metadataSubdirectory),
                  let data = try? Data(contentsOf: url),
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let categories = json["categories"] as? [[String: Any]]
            else {
                return []
            }
            return categories.compactMap { row in
                guard let id = row["id"] as? String ?? row["name"] as? String else { return nil }
                let filename = row["filename"] as? String ?? "\(id).json"
                return YabaIconCategory(id: id, filename: filename)
            }
        }.value
    }

    /// Loads icon name list from `metadata/<category.filename>` in the bundle, or `[]` if missing.
    public static func loadIconsForCategory(_ category: YabaIconCategory, in bundle: Bundle = .main) async -> [YabaIconItem] {
        await Task.detached {
            let name = category.filename.replacingOccurrences(of: ".json", with: "")
            guard let url = bundle.url(forResource: name, withExtension: "json", subdirectory: metadataSubdirectory),
                  let data = try? Data(contentsOf: url),
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let icons = json["icons"] as? [[String: Any]]
            else {
                return []
            }
            return icons.compactMap { row in
                guard let n = row["name"] as? String ?? row["id"] as? String else { return nil }
                return YabaIconItem(name: n)
            }
        }.value
    }
}
