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
        let rules = FolderSelectionRules.build(
            mode: mode,
            contextFolderId: contextFolderId,
            allFolders: allFoldersQuery
        )
        SelectFolderQueryList(
            rules: rules,
            searchQuery: machine.state.searchQuery,
            onPick: onPick
        )
        .id("\(machine.state.searchQuery)\(rules.stableKey)")
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
}

// MARK: - Query-backed list

/// Lists folders using a SwiftData `@Query` + `#Predicate` so filtering runs in the store (not on a copied array).
private struct SelectFolderQueryList: View {
    @Environment(\.dismiss)
    private var dismiss

    @Query
    private var folders: [FolderModel]

    let rules: FolderSelectionRules
    let trimmedSearch: String
    let onPick: (String?) -> Void

    init(
        rules: FolderSelectionRules,
        searchQuery: String,
        onPick: @escaping (String?) -> Void
    ) {
        self.rules = rules
        let trimmed = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        self.trimmedSearch = trimmed
        self.onPick = onPick

        let uncategorizedId = Constants.Folder.Uncategorized.id
        let excludeSystem = rules.excludeSystemFolderTargets
        let hideEmptyUncategorized = rules.hideEmptyUncategorizedWithNoBookmarks
        let excluded = rules.excludedFolderIds
        let parentEx = rules.excludeParentFolderId

        _folders = Query(
            filter: #Predicate<FolderModel> { folder in
                (trimmed.isEmpty || folder.label.localizedStandardContains(trimmed))
                    && (!excludeSystem || folder.folderId != uncategorizedId)
                    && !excluded.contains(folder.folderId)
                    && (parentEx.isEmpty || folder.folderId != parentEx)
                    && (!hideEmptyUncategorized
                        || folder.folderId != uncategorizedId
                        || !folder.bookmarks.isEmpty)
            },
            sort: [SortDescriptor(\FolderModel.label)],
            animation: .smooth
        )
    }

    var body: some View {
        List {
            if rules.canMoveToRoot {
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

            if folders.isEmpty {
                if trimmedSearch.isEmpty {
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
                                "Select Folder No Folder Found In Search Description \(trimmedSearch)"
                            )
                        )
                    }
                }
            } else {
                ForEach(folders, id: \.folderId) { folder in
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
    }
}

// MARK: - Rules (same logic as previous `SelectionSnapshot`, used to build the predicate)

private struct FolderSelectionRules: Equatable {
    let mode: FolderSelectionMode
    /// Sorted for stable `stableKey` / `Equatable`.
    let excludedFolderIds: [String]
    let excludeParentFolderId: String
    let excludeSystemFolderTargets: Bool
    let hideEmptyUncategorizedWithNoBookmarks: Bool
    let canMoveToRoot: Bool

    var stableKey: String {
        excludedFolderIds.joined(separator: "\u{1e}")
            + "|" + excludeParentFolderId
            + "|" + String(excludeSystemFolderTargets)
            + "|" + String(hideEmptyUncategorizedWithNoBookmarks)
            + "|" + String(canMoveToRoot)
            + "|" + mode.rawValue
    }

    static func build(
        mode: FolderSelectionMode,
        contextFolderId: String?,
        allFolders: [FolderModel]
    ) -> FolderSelectionRules {
        let hideEmptyUncategorizedWithNoBookmarks =
            !(mode == .folderSelection || mode == .parentSelection)

        let excludeSystemFolderTargets =
            mode == .parentSelection
            || mode == .folderMove
            || mode == .bookmarksMove

        switch mode {
        case .folderSelection:
            return FolderSelectionRules(
                mode: mode,
                excludedFolderIds: [],
                excludeParentFolderId: "",
                excludeSystemFolderTargets: excludeSystemFolderTargets,
                hideEmptyUncategorizedWithNoBookmarks: hideEmptyUncategorizedWithNoBookmarks,
                canMoveToRoot: false
            )

        case .parentSelection, .folderMove:
            if let fid = contextFolderId,
               let folder = allFolders.first(where: { $0.folderId == fid })
            {
                let descendants = Set(folder.getDescendants().map(\.folderId))
                let excluded = Set([fid]).union(descendants).sorted()
                return FolderSelectionRules(
                    mode: mode,
                    excludedFolderIds: excluded,
                    excludeParentFolderId: folder.parent?.folderId ?? "",
                    excludeSystemFolderTargets: excludeSystemFolderTargets,
                    hideEmptyUncategorizedWithNoBookmarks: hideEmptyUncategorizedWithNoBookmarks,
                    canMoveToRoot: folder.parent != nil
                )
            }
            return FolderSelectionRules(
                mode: mode,
                excludedFolderIds: [],
                excludeParentFolderId: "",
                excludeSystemFolderTargets: excludeSystemFolderTargets,
                hideEmptyUncategorizedWithNoBookmarks: hideEmptyUncategorizedWithNoBookmarks,
                canMoveToRoot: false
            )

        case .bookmarksMove:
            let excluded: [String] = contextFolderId.map { [$0] } ?? []
            return FolderSelectionRules(
                mode: mode,
                excludedFolderIds: excluded,
                excludeParentFolderId: "",
                excludeSystemFolderTargets: excludeSystemFolderTargets,
                hideEmptyUncategorizedWithNoBookmarks: hideEmptyUncategorizedWithNoBookmarks,
                canMoveToRoot: false
            )
        }
    }
}
