//
//  LinkmarkCreationEvent.swift
//  YABACore
//
//  Parity with Compose `LinkmarkCreationEvent` — payloads are host-friendly (`String` JSON where needed).
//

import Foundation

public enum LinkmarkCreationEvent: Sendable {
    case onInit(
        linkmarkId: String?,
        initialUrl: String?,
        initialFolderId: String?,
        initialTagIds: [String]?
    )
    case onCyclePreviewAppearance
    case onChangeUrl(String)
    case onChangeLabel(String)
    case onChangeDescription(String)
    case onSelectFolderId(String?)
    case onSelectTagIds([String])
    case onClearLabel
    case onClearDescription
    case onApplyFromMetadata
    case onRefetch
    case onConverterSucceeded(documentJson: String, linkMetadataJson: String?)
    case onConverterFailed(errorMessage: String)
    case onSave
    case onTogglePrivate
    case onTogglePinned

    /// One-shot create/update via direct values (interop).
    case create(
        bookmarkId: String,
        folderId: String,
        label: String,
        bookmarkDescription: String?,
        isPrivate: Bool,
        isPinned: Bool,
        tagIds: [String],
        url: String,
        domain: String?,
        videoUrl: String?,
        audioUrl: String?,
        metadataTitle: String?,
        metadataDescription: String?,
        metadataAuthor: String?,
        metadataDate: String?
    )
}
