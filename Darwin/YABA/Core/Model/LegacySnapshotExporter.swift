//
//  LegacySnapshotExporter.swift
//  YABA
//
//  Exports SwiftData content into the Kotlin LegacySnapshot used by MigrationManager.
//  Deletion logs are intentionally omitted (user will be warned separately).
//

import Foundation
import SwiftData
import YABACore
import KotlinBase

struct LegacySnapshotExporter {
    /// Export all SwiftData collections/bookmarks into a LegacySnapshot for migration.
    /// - Parameter context: SwiftData ModelContext with YabaSchemaV1 registered.
    /// - Returns: LegacySnapshot ready for `MigrationManager.migrate(snapshot:)`.
    static func export(using context: ModelContext) throws -> LegacySnapshot {
        let collections = try context.fetch(FetchDescriptor<YabaCollection>())
        let bookmarks = try context.fetch(FetchDescriptor<YabaBookmark>())

        let folders = collections
            .filter { $0.collectionType == .folder }
            .map { collection in
                LegacyFolder(
                    id: uuidFromString(collection.collectionId),
                    parentId: collection.parent.map { uuidFromString($0.collectionId) },
                    label: collection.label,
                    description: nil, // SwiftData schema has no collection description
                    icon: collection.icon,
                    colorCode: Int32(collection.color.rawValue),
                    order: Int32(collection.order),
                    createdAt: instantFromEpochMillis(collection.createdAt.timeIntervalSince1970Millis),
                    editedAt: instantFromEpochMillis(collection.editedAt.timeIntervalSince1970Millis)
                )
            }

        let tags = collections
            .filter { $0.collectionType == .tag }
            .map { tag in
                LegacyTag(
                    id: uuidFromString(tag.collectionId),
                    label: tag.label,
                    icon: tag.icon,
                    colorCode: Int32(tag.color.rawValue),
                    order: Int32(tag.order),
                    createdAt: instantFromEpochMillis(tag.createdAt.timeIntervalSince1970Millis),
                    editedAt: instantFromEpochMillis(tag.editedAt.timeIntervalSince1970Millis)
                )
            }

        let legacyBookmarks = bookmarks.map { bookmark in
            let parentFolder = bookmark.collections?.first { $0.collectionType == .folder }
            let folderIdString = parentFolder?.collectionId
                ?? "11111111-1111-1111-1111-111111111111" // fallback to default uncategorized

            return LegacyBookmark(
                id: uuidFromString(bookmark.bookmarkId),
                folderId: uuidFromString(folderIdString),
                label: bookmark.label,
                description: bookmark.bookmarkDescription,
                url: bookmark.link,
                domain: bookmark.domain,
                linkTypeCode: Int32(bookmark.type),
                createdAt: instantFromEpochMillis(bookmark.createdAt.timeIntervalSince1970Millis),
                editedAt: instantFromEpochMillis(bookmark.editedAt.timeIntervalSince1970Millis),
                previewImageUrl: bookmark.imageUrl,
                previewIconUrl: bookmark.iconUrl,
                videoUrl: bookmark.videoUrl,
                previewImageData: bookmark.imageDataHolder?.toKotlinByteArray(),
                previewIconData: bookmark.iconDataHolder?.toKotlinByteArray()
            )
        }

        let tagLinks: [LegacyTagLink] = bookmarks.flatMap { bookmark in
            (bookmark.collections ?? [])
                .filter { $0.collectionType == .tag }
                .map { tag in
                    LegacyTagLink(
                        tagId: uuidFromString(tag.collectionId),
                        bookmarkId: uuidFromString(bookmark.bookmarkId)
                    )
                }
        }

        return LegacySnapshotBuilder.build(
            folders: folders,
            tags: tags,
            bookmarks: legacyBookmarks,
            tagLinks: tagLinks
        )
    }
}

private extension Date {
    var timeIntervalSince1970Millis: Int64 {
        Int64((timeIntervalSince1970 * 1000.0).rounded())
    }
}

private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let byteArray = KotlinByteArray(size: Int32(count))
        withUnsafeBytes { rawBuffer in
            guard let baseAddress = rawBuffer.baseAddress else { return }
            let buffer = baseAddress.assumingMemoryBound(to: UInt8.self)
            for idx in 0..<count {
                byteArray.set(index: Int32(idx), value: KotlinByte(value: buffer[idx]))
            }
        }
        return byteArray
    }
}

