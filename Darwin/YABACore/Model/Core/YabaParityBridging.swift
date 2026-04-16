//
//  YabaParityBridging.swift
//  YABACore
//
//  Compose-parity accessors: legacy-friendly names (`*Code`, `Yaba*` aliases) over v2 SwiftData models.
//

import Foundation
import SwiftData
import SwiftUI

// MARK: - Legacy name aliases (v2 models)

typealias YabaBookmark = BookmarkModel

typealias YabaFolder = FolderModel

typealias YabaTag = TagModel

// MARK: - YabaColor + codes

public extension YabaColor {
    /// Compose `YabaColor.code` parity.
    static func from(colorCode: Int) -> YabaColor {
        YabaColor(rawValue: colorCode) ?? .none
    }

    var colorCode: Int { rawValue }
}

// MARK: - Bookmark

extension BookmarkModel {
    /// Compose `BookmarkKind.code` parity.
    var kindCode: Int { kindRaw }

    var kind: BookmarkKind {
        BookmarkKind(rawValue: kindRaw) ?? .link
    }

    /// Primary share / open URL for link bookmarks.
    var link: String {
        linkDetail?.url ?? ""
    }

    func getFolderColor() -> Color {
        folder?.color.getUIColor() ?? Color.accentColor
    }

    /// Maps v2 `BookmarkKind` into legacy `BookmarkType` for UI that still keys off the old enum.
    var bookmarkType: BookmarkType {
        switch kind {
        case .link: return .webLink
        case .note: return .none
        case .image: return .image
        case .file: return .audio
        case .canvas: return .video
        }
    }

    // MARK: Payload bytes (`*Code` / lazy Data models)

    /// Thumbnail or preview image bytes (`BookmarkImagePayloadModel`).
    var imagePayloadBytes: Data? { imagePayload?.bytes }

    /// Site / app icon bytes (`BookmarkIconPayloadModel`).
    var iconPayloadBytes: Data? { iconPayload?.bytes }

    /// Legacy UI name; reads/writes v2 image payload.
    var imageDataHolder: Data? {
        get { imagePayloadBytes }
        set {
            if let newValue = newValue {
                if let existing = imagePayload {
                    existing.bytes = newValue
                } else {
                    let payload = BookmarkImagePayloadModel(bytes: newValue)
                    payload.bookmark = self
                    imagePayload = payload
                }
            } else {
                imagePayload = nil
            }
        }
    }

    /// Legacy UI name; reads/writes v2 icon payload.
    var iconDataHolder: Data? {
        get { iconPayloadBytes }
        set {
            if let newValue = newValue {
                if let existing = iconPayload {
                    existing.bytes = newValue
                } else {
                    let payload = BookmarkIconPayloadModel(bytes: newValue)
                    payload.bookmark = self
                    iconPayload = payload
                }
            } else {
                iconPayload = nil
            }
        }
    }
}

// MARK: - Folder

extension FolderModel {
    var colorCode: Int { colorRaw }

    var color: YabaColor {
        YabaColor.from(colorCode: colorRaw)
    }

    func getDescendants() -> [FolderModel] {
        var result: [FolderModel] = []
        for child in children {
            result.append(child)
            result.append(contentsOf: child.getDescendants())
        }
        return result
    }
}

// MARK: - Tag

extension TagModel {
    var colorCode: Int { colorRaw }

    var color: YabaColor {
        YabaColor.from(colorCode: colorRaw)
    }
}

// MARK: - Annotation

extension AnnotationModel {
    /// Parsed annotation kind from persisted `typeRaw`.
    var type: AnnotationType {
        AnnotationType(rawValue: typeRaw) ?? .readable
    }

    /// Accent color for list chrome; derived from `colorRoleRaw`, not synced separately.
    var colorRole: YabaColor {
        YabaColor.from(colorCode: colorRoleRaw)
    }
}

// MARK: - Doc bookmark subtype

extension DocBookmarkModel {
    var docmarkType: DocmarkType {
        DocmarkType(rawValue: docmarkTypeRaw) ?? .pdf
    }
}
