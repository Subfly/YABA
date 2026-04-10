//
//  AnnotationManager.swift
//  YABACore
//
//  Annotation CRUD (Compose `AnnotationManager` surface — extend as features land).
//

import Foundation
import SwiftData

public enum AnnotationManager {
    public static func queueInsertAnnotation(
        bookmarkId: String,
        readableVersionId: String?,
        type: YabaCoreAnnotationType,
        annotationId: String = UUID().uuidString,
        colorRoleRaw: Int = 0,
        note: String? = nil,
        quoteText: String? = nil,
        extrasJson: String? = nil
    ) {
        YabaCoreOperationQueue.shared.queue(name: "InsertAnnotation:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
                return
            }
            var readable: ReadableVersionModel?
            if let readableVersionId {
                let p = #Predicate<ReadableVersionModel> { $0.readableVersionId == readableVersionId }
                var d = FetchDescriptor(predicate: p)
                d.fetchLimit = 1
                readable = try context.fetch(d).first
            }
            let model = AnnotationModel(
                annotationId: annotationId,
                typeRaw: type.rawValue,
                colorRoleRaw: colorRoleRaw,
                note: note,
                quoteText: quoteText,
                extrasJson: extrasJson,
                createdAt: .now,
                editedAt: .now,
                bookmark: bookmark,
                readableVersion: readable
            )
            context.insert(model)
            bookmark.editedAt = .now
        }
    }

    public static func queueUpdateAnnotation(annotationId: String, colorRoleRaw: Int, note: String?) {
        YabaCoreOperationQueue.shared.queue(name: "UpdateAnnotation:\(annotationId)") { context in
            let p = #Predicate<AnnotationModel> { $0.annotationId == annotationId }
            var d = FetchDescriptor(predicate: p)
            d.fetchLimit = 1
            guard let ann = try context.fetch(d).first else { return }
            ann.colorRoleRaw = colorRoleRaw
            ann.note = note
            ann.editedAt = .now
            ann.bookmark?.editedAt = .now
        }
    }

    public static func queueDeleteAnnotation(annotationId: String) {
        YabaCoreOperationQueue.shared.queue(name: "DeleteAnnotation:\(annotationId)") { context in
            let p = #Predicate<AnnotationModel> { $0.annotationId == annotationId }
            var d = FetchDescriptor(predicate: p)
            d.fetchLimit = 1
            if let ann = try context.fetch(d).first {
                context.delete(ann)
            }
        }
    }
}
