//
//  ImagemarkCreationEvent.swift
//  YABACore
//

import Foundation

public enum ImagemarkCreationEvent: Sendable {
    case onInit(
        imagemarkId: String?,
        initialFolderId: String?,
        initialTagIds: [String]?,
        uncategorizedFolderCreationRequired: Bool
    )
    case onCyclePreviewAppearance
    case onPickFromGallery
    case onImageFromShare(Data, fileExtension: String)
    case onCaptureFromCamera
    case onClearImage
    case onChangeLabel(String)
    case onChangeDescription(String)
    case onSelectFolderId(String?)
    case onSelectTagIds([String])
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
