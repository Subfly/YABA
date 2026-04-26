//
//  DocmarkCreationEvent.swift
//  YABACore
//

import Foundation

public enum DocmarkCreationEvent: Sendable {
    case onInit(
        docmarkId: String?,
        initialFolderId: String?,
        initialTagIds: [String]?,
        uncategorizedFolderCreationRequired: Bool
    )
    case onPickDocument
    case onClearDocument
    case onDocumentFromShare(Data, sourceFileName: String?, docmarkType: DocmarkType)
    case onCyclePreviewAppearance
    case onDocumentMetadataExtracted(
        metadataTitle: String?,
        metadataDescription: String?,
        metadataAuthor: String?,
        metadataDate: String?
    )
    case onSetGeneratedPreview(imageData: Data?, fileExtension: String)
    case onChangeLabel(String)
    case onChangeDescription(String)
    case onApplyFromMetadata
    case onSelectFolderId(String?)
    case onSelectTagIds([String])
    case onDocumentExtractionFinished
    case onSave
    case onTogglePinned

    case create(
        bookmarkId: String,
        folderId: String,
        label: String,
        bookmarkDescription: String?,
        isPinned: Bool,
        tagIds: [String]
    )
}
