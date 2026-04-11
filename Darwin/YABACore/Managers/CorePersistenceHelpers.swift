//
//  YabaCorePersistenceHelpers.swift
//  YABACore
//

import Foundation
import SwiftData

enum YabaCorePersistenceHelpers {
    static func folder(folderId: String, context: ModelContext) throws -> FolderModel? {
        let predicate = #Predicate<FolderModel> { $0.folderId == folderId }
        var descriptor = FetchDescriptor(predicate: predicate)
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    static func tag(tagId: String, context: ModelContext) throws -> TagModel? {
        let predicate = #Predicate<TagModel> { $0.tagId == tagId }
        var descriptor = FetchDescriptor(predicate: predicate)
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    static func bookmark(bookmarkId: String, context: ModelContext) throws -> BookmarkModel? {
        let predicate = #Predicate<BookmarkModel> { $0.bookmarkId == bookmarkId }
        var descriptor = FetchDescriptor(predicate: predicate)
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }
}
