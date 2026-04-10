//
//  YabaV1ToV2Migrator.swift
//  YABACore
//
//  Migrates released `YabaSchemaV1` into `YabaSchemaV2`. All legacy bookmarks become linkmarks.
//

import Foundation
import SwiftData

enum YabaV1ToV2Migrator {
    /// Runs inside `MigrationStage.custom` `willMigrate`.
    static func run(context: ModelContext) throws {
        let collections = try context.fetch(FetchDescriptor<YabaSchemaV1.Collection>())
        let bookmarks = try context.fetch(FetchDescriptor<YabaSchemaV1.Bookmark>())

        var folderByLegacyId: [String: FolderModel] = [:]
        var tagByLegacyId: [String: TagModel] = [:]

        for collection in collections {
            if collection.collectionType == .folder {
                let legacyId = collection.collectionId
                let canonicalFolderId = Self.canonicalFolderId(forLegacyCollectionId: legacyId)
                let folder = FolderModel(
                    folderId: canonicalFolderId,
                    label: collection.label,
                    folderDescription: nil,
                    icon: collection.icon,
                    colorRaw: collection.color.rawValue,
                    createdAt: collection.createdAt,
                    editedAt: collection.editedAt,
                    isHidden: false,
                    parent: nil,
                    children: [],
                    bookmarks: []
                )
                context.insert(folder)
                folderByLegacyId[legacyId] = folder
                if legacyId == Constants.legacyUncategorizedCollectionId {
                    folderByLegacyId[Constants.Folder.Uncategorized.id] = folder
                }
            } else if collection.collectionType == .tag {
                let tag = TagModel(
                    tagId: collection.collectionId,
                    label: collection.label,
                    icon: collection.icon,
                    colorRaw: collection.color.rawValue,
                    createdAt: collection.createdAt,
                    editedAt: collection.editedAt,
                    isHidden: false,
                    bookmarks: []
                )
                context.insert(tag)
                tagByLegacyId[collection.collectionId] = tag
            }
        }

        // Folder hierarchy (folders only)
        for collection in collections where collection.collectionType == .folder {
            guard let childFolder = folderByLegacyId[collection.collectionId],
                  let parent = collection.parent,
                  parent.collectionType == .folder,
                  let parentFolder = folderByLegacyId[parent.collectionId]
            else { continue }
            childFolder.parent = parentFolder
            if !parentFolder.children.contains(where: { $0.folderId == childFolder.folderId }) {
                parentFolder.children.append(childFolder)
            }
        }

        // Uncategorized folder if missing (Compose stable id, not legacy "-1")
        if folderByLegacyId[Constants.legacyUncategorizedCollectionId] == nil,
           folderByLegacyId[Constants.Folder.Uncategorized.id] == nil {
            let uncategorized = FolderModel(
                folderId: Constants.Folder.Uncategorized.id,
                label: Constants.Folder.Uncategorized.name,
                folderDescription: Constants.Folder.Uncategorized.descriptionText,
                icon: Constants.Folder.Uncategorized.icon,
                colorRaw: 0,
                createdAt: .now,
                editedAt: .now,
                isHidden: false,
                parent: nil,
                children: [],
                bookmarks: []
            )
            context.insert(uncategorized)
            folderByLegacyId[Constants.legacyUncategorizedCollectionId] = uncategorized
            folderByLegacyId[Constants.Folder.Uncategorized.id] = uncategorized
        }

        for legacy in bookmarks {
            let folder = resolveOwningFolder(
                legacy: legacy,
                folderByLegacyId: folderByLegacyId
            )

            let tagModels: [TagModel] = (legacy.collections ?? [])
                .filter { $0.collectionType == .tag }
                .compactMap { tagByLegacyId[$0.collectionId] }

            let bookmark = BookmarkModel(
                bookmarkId: legacy.bookmarkId,
                kindRaw: YabaCoreBookmarkKind.link.rawValue,
                label: legacy.label,
                bookmarkDescription: legacy.bookmarkDescription.isEmpty ? nil : legacy.bookmarkDescription,
                createdAt: legacy.createdAt,
                editedAt: legacy.editedAt,
                viewCount: 0,
                isPrivate: false,
                isPinned: false,
                folder: folder,
                tags: tagModels
            )
            context.insert(bookmark)

            let link = LinkBookmarkModel(
                url: legacy.link,
                domain: legacy.domain,
                videoUrl: legacy.videoUrl,
                audioUrl: nil,
                metadataTitle: legacy.label,
                metadataDescription: legacy.bookmarkDescription.isEmpty ? nil : legacy.bookmarkDescription,
                metadataAuthor: nil,
                metadataDate: nil,
                bookmark: bookmark
            )
            context.insert(link)
            bookmark.linkDetail = link

            if let imageData = legacy.imageDataHolder {
                let payload = BookmarkImagePayloadModel(bytes: imageData, bookmark: bookmark)
                context.insert(payload)
                bookmark.imagePayload = payload
            }
            if let iconData = legacy.iconDataHolder {
                let payload = BookmarkIconPayloadModel(bytes: iconData, bookmark: bookmark)
                context.insert(payload)
                bookmark.iconPayload = payload
            }
        }

        try context.save()
    }

    private static func resolveOwningFolder(
        legacy: YabaSchemaV1.Bookmark,
        folderByLegacyId: [String: FolderModel]
    ) -> FolderModel? {
        let folders = (legacy.collections ?? []).filter { $0.collectionType == .folder }
        if let firstId = folders.first?.collectionId,
           let folder = folderByLegacyId[firstId] {
            return folder
        }
        return folderByLegacyId[Constants.legacyUncategorizedCollectionId]
            ?? folderByLegacyId[Constants.Folder.Uncategorized.id]
    }

    /// Legacy Darwin used `"-1"` for the default uncategorized folder; v2 stores the Compose stable UUID.
    private static func canonicalFolderId(forLegacyCollectionId legacyId: String) -> String {
        if legacyId == Constants.legacyUncategorizedCollectionId {
            return Constants.Folder.Uncategorized.id
        }
        return legacyId
    }
}
