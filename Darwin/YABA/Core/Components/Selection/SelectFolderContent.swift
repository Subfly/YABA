//
//  SelectFolderContent.swift
//  YABA
//
//  Folder picker aligned with Compose `FolderSelectionContent` + `FolderSelectionStateMachine`:
//  mode-based exclusions, system-folder rules, optional “move to root”, and search.
//

import SwiftData
import SwiftUI

struct SelectFolderContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @State
    private var machine = FolderSelectionStateMachine()

    @Query(sort: [SortDescriptor(\FolderModel.label)])
    private var allFoldersQuery: [FolderModel]

    let mode: FolderSelectionMode
    let contextFolderId: String?
    let contextBookmarkIds: [String]?
    let onPick: (String?) -> Void

    init(
        mode: FolderSelectionMode = .parentSelection,
        contextFolderId: String? = nil,
        contextBookmarkIds: [String]? = nil,
        onPick: @escaping (String?) -> Void
    ) {
        self.mode = mode
        self.contextFolderId = contextFolderId
        self.contextBookmarkIds = contextBookmarkIds
        self.onPick = onPick
    }

    var body: some View {
        List {
            if snapshot.canMoveToRoot {
                Button {
                    onPick(nil)
                    dismiss()
                } label: {
                    HStack(spacing: 12) {
                        YabaIconView(bundleKey: "arrow-move-up-right")
                            .scaledToFit()
                            .frame(width: 24, height: 24)
                            .foregroundStyle(.primary)
                        Text("Select Folder Move To Root Label")
                        Spacer()
                    }
                }
                .buttonStyle(.plain)
            }

            if visibleFolders.isEmpty {
                if machine.state.searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    ContentUnavailableView {
                        Label {
                            Text("Select Folder No Folders Available Title")
                        } icon: {
                            YabaIconView(bundleKey: "folder-01")
                                .scaledToFit()
                                .frame(width: 52, height: 52)
                        }
                    } description: {
                        Text("Select Folder No Folders Available Description")
                    }
                } else {
                    ContentUnavailableView {
                        Label {
                            Text("Select Folder No Folder Found In Search Title")
                        } icon: {
                            YabaIconView(bundleKey: "search-01")
                                .scaledToFit()
                                .frame(width: 52, height: 52)
                        }
                    } description: {
                        Text(
                            LocalizedStringKey(
                                "Select Folder No Folder Found In Search Description \(machine.state.searchQuery)"
                            )
                        )
                    }
                }
            } else {
                ForEach(visibleFolders, id: \.folderId) { folder in
                    Button {
                        onPick(folder.folderId)
                        dismiss()
                    } label: {
                        HStack {
                            YabaIconView(bundleKey: folder.icon)
                                .scaledToFit()
                                .foregroundStyle(folder.color.getUIColor())
                                .frame(width: 24, height: 24)
                            if folder.folderId == Constants.uncategorizedCollectionId {
                                Text(LocalizedStringKey(Constants.uncategorizedCollectionLabelKey))
                            } else {
                                Text(folder.label)
                            }
                            Spacer()
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .listRowSpacing(0)
        #if !os(visionOS)
        .scrollDismissesKeyboard(.immediately)
        #endif
        .searchable(
            text: Binding(
                get: { machine.state.searchQuery },
                set: { newValue in
                    Task {
                        await machine.send(.onSearchQueryChanged(newValue))
                    }
                }
            ),
            prompt: Text("Folder Search Prompt")
        )
        .navigationTitle("Select Folder Title")
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                if mode == .bookmarksMove || mode == .folderMove {
                    Button(role: .cancel) {
                        dismiss()
                    } label: {
                        Text("Cancel")
                    }
                    .foregroundStyle(.red)
                } else {
                    Button {
                        dismiss()
                    } label: {
                        YabaIconView(bundleKey: "arrow-left-01")
                    }
                    .buttonRepeatBehavior(.enabled)
                }
            }
        }
        .task {
            await machine.send(
                .onInit(
                    mode: mode,
                    contextFolderId: contextFolderId,
                    contextBookmarkIds: contextBookmarkIds
                )
            )
        }
    }

    // MARK: - Compose parity (FolderSelectionStateMachine.applyModeExclusions + observe rules)

    private var snapshot: SelectionSnapshot {
        SelectionSnapshot(
            mode: mode,
            contextFolderId: contextFolderId,
            allFolders: allFoldersQuery
        )
    }

    private var visibleFolders: [FolderModel] {
        let q = machine.state.searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        let base = snapshot.filteredFolders
        guard !q.isEmpty else { return base }
        let lower = q.lowercased()
        return base.filter { $0.label.lowercased().contains(lower) }
    }
}

// MARK: - Selection snapshot

/// Mirrors Compose `FolderSelectionStateMachine` init + `applyModeExclusions` + `observeAllFoldersSorted` empty-system-folder rule.
private struct SelectionSnapshot {
    let mode: FolderSelectionMode
    private let afterEmptySystemFilter: [FolderModel]
    private let excludedIds: Set<String>
    private let currentParentId: String?
    let canMoveToRoot: Bool

    init(mode: FolderSelectionMode, contextFolderId: String?, allFolders: [FolderModel]) {
        self.mode = mode

        let includeEmptySystemFolders = mode == .folderSelection || mode == .parentSelection
        if includeEmptySystemFolders {
            self.afterEmptySystemFilter = allFolders
        } else {
            self.afterEmptySystemFilter = allFolders.filter { folder in
                if Constants.Folder.isSystemFolder(folder.folderId), folder.bookmarks.isEmpty {
                    return false
                }
                return true
            }
        }

        switch mode {
        case .folderSelection:
            self.excludedIds = []
            self.currentParentId = nil
            self.canMoveToRoot = false

        case .parentSelection, .folderMove:
            if let fid = contextFolderId,
               let folder = allFolders.first(where: { $0.folderId == fid })
            {
                let descendants = Set(folder.getDescendants().map(\.folderId))
                self.excludedIds = Set([fid]).union(descendants)
                self.currentParentId = folder.parent?.folderId
                self.canMoveToRoot = folder.parent != nil
            } else {
                self.excludedIds = []
                self.currentParentId = nil
                self.canMoveToRoot = false
            }

        case .bookmarksMove:
            self.excludedIds = contextFolderId.map { Set([$0]) } ?? []
            self.currentParentId = nil
            self.canMoveToRoot = false
        }
    }

    var filteredFolders: [FolderModel] {
        let excludeSystemAsTargets =
            mode == .parentSelection ||
            mode == .folderMove ||
            mode == .bookmarksMove

        var result = afterEmptySystemFilter

        if excludeSystemAsTargets {
            result = result.filter { !Constants.Folder.isSystemFolder($0.folderId) }
        }

        if !excludedIds.isEmpty {
            result = result.filter { !excludedIds.contains($0.folderId) }
        }

        let excludeCurrentParent = mode == .parentSelection || mode == .folderMove
        if excludeCurrentParent, let p = currentParentId {
            result = result.filter { $0.folderId != p }
        }

        return result
    }
}
