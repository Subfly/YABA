//
//  FolderCreationStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class FolderCreationStateMachine: YabaBaseObservableState<FolderCreationUIState>, YabaScreenStateMachine {
    public override init(initialState: FolderCreationUIState = FolderCreationUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: FolderCreationEvent) async {
        switch event {
        case let .onInitWithFolder(folderId):
            apply { $0.existingFolderId = folderId }
        case let .onSelectNewParent(parent):
            apply { $0.parentFolderId = parent }
        case let .onSelectNewColor(c):
            apply { $0.colorRole = c }
        case let .onSelectNewIcon(icon):
            apply { $0.icon = icon }
        case let .onChangeLabel(label):
            apply { $0.label = label }
        case let .onChangeDescription(text):
            apply { $0.folderDescription = text }
        case .onSave:
            let label = state.label.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !label.isEmpty else {
                apply { $0.lastError = "Label required" }
                return
            }
            apply { $0.lastError = nil }
            let colorRaw = state.colorRole.rawValue
            if let id = state.existingFolderId {
                FolderManager.queueUpdateFolderMetadata(
                    folderId: id,
                    label: label,
                    folderDescription: state.folderDescription.nilIfEmpty,
                    icon: state.icon,
                    colorRaw: colorRaw
                )
                if let parent = state.parentFolderId {
                    FolderManager.queueMoveFolder(folderId: id, newParentFolderId: parent)
                }
            } else {
                let newId = UUID().uuidString
                FolderManager.queueCreateFolder(
                    folderId: newId,
                    label: label,
                    folderDescription: state.folderDescription.nilIfEmpty,
                    icon: state.icon,
                    colorRaw: colorRaw,
                    parentFolderId: state.parentFolderId
                )
            }
        case let .create(folderId, label, description, parent):
            FolderManager.queueCreateFolder(
                folderId: folderId,
                label: label,
                folderDescription: description,
                icon: "folder-01",
                colorRaw: 0,
                parentFolderId: parent
            )
        }
    }
}
