//
//  YabaSchemaV2.swift
//  YABACore
//

import SwiftData

/// Compose-parity SwiftData schema (version 2).
enum YabaSchemaV2: VersionedSchema {
    static var versionIdentifier: Schema.Version {
        Schema.Version(2, 0, 0)
    }

    static var models: [any PersistentModel.Type] {
        [
            FolderModel.self,
            TagModel.self,
            BookmarkModel.self,
            BookmarkImagePayloadModel.self,
            BookmarkIconPayloadModel.self,
            LinkBookmarkModel.self,
            ImageBookmarkModel.self,
            DocBookmarkModel.self,
            DocBookmarkPayloadModel.self,
            NoteBookmarkModel.self,
            NoteBookmarkPayloadModel.self,
            CanvasBookmarkModel.self,
            CanvasBookmarkPayloadModel.self,
            ReadableVersionModel.self,
            ReadableVersionPayloadModel.self,
            AnnotationModel.self,
            DataLogV2Model.self,
        ]
    }
}
