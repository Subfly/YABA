//
//  SelectFolderContent.swift
//  YABA
//
//  Created by Ali Taha on 22.04.2025.
//

#if false

import SwiftData
import SwiftUI

enum FolderSelectionMode {
    case folderSelection
    case parentSelection(
        FolderModel?,
        (Bool) -> Void
    )
}

struct SelectFolderContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @State
    private var searchQuery: String = ""

    @Binding
    var selectedFolder: FolderModel?

    let mode: FolderSelectionMode

    var body: some View {
        SelectFolderSearchableContent(
            mode: mode,
            selectedFolder: $selectedFolder,
            searchQuery: $searchQuery
        )
        #if !os(visionOS)
        .scrollDismissesKeyboard(.immediately)
        #endif
        .searchable(text: $searchQuery, prompt: "Folder Search Prompt")
        .navigationTitle("Select Folder Title")
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .navigation) {
                Button {
                    dismiss()
                } label: {
                    YabaIconView(bundleKey: "arrow-left-01")
                }.buttonRepeatBehavior(.enabled)
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    // Folder creation UI archived (`YABA/Creation/Collection`).
                } label: {
                    YabaIconView(bundleKey: "folder-add")
                        .scaledToFit()
                }
                .disabled(true)
            }
        }
    }
}

private struct SelectFolderSearchableContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @Query
    private var allFolders: [FolderModel]

    @Binding
    var selectedFolder: FolderModel?

    @Binding
    var searchQuery: String

    let mode: FolderSelectionMode

    init(
        mode: FolderSelectionMode,
        selectedFolder: Binding<FolderModel?>,
        searchQuery: Binding<String>
    ) {
        self.mode = mode
        let query = searchQuery.wrappedValue
        _allFolders = Query(
            filter: #Predicate<FolderModel> {
                query.isEmpty || $0.label.localizedStandardContains(query)
            },
            sort: [SortDescriptor(\.label)],
            animation: .smooth
        )
        _selectedFolder = selectedFolder
        _searchQuery = searchQuery
    }

    var body: some View {
        let foldersToBeShown = if case .parentSelection(let current, _) = mode {
            availableParentFolders(for: current, from: allFolders)
        } else {
            allFolders
        }
        List {
            if foldersToBeShown.isEmpty {
                if searchQuery.isEmpty {
                    ContentUnavailableView {
                        Label {
                            Text("Select Folder No Folders Available Title")
                        } icon: {
                            YabaIconView(bundleKey: "folder-01")
                                .scaledToFit()
                                .frame(width: 52, height: 52)
                        }
                    } description: {
                        Text(
                            LocalizedStringKey(
                                "Select Folder No Folders Available Description"
                            )
                        )
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
                                "Select Folder No Folder Found In Search Description \(searchQuery)"
                            )
                        )
                    }
                }
            } else {
                ForEach(foldersToBeShown) { folder in
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
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation {
                            dismiss()
                            if case .parentSelection(_, let onSelected) = mode {
                                onSelected(false)
                            }
                            selectedFolder = folder
                        }
                    }
                }
            }
        }
        .listRowSpacing(0)
    }

    func availableParentFolders(
        for current: FolderModel?,
        from allFolders: [FolderModel]
    ) -> [FolderModel] {
        guard let current else { return allFolders }

        let descendantIds = Set(current.getDescendants().map(\.folderId))

        return allFolders.filter { folder in
            folder.folderId != current.folderId &&
                folder.folderId != current.parent?.folderId &&
                !descendantIds.contains(folder.folderId)
        }
    }
}

#Preview {
    SelectFolderContent(
        selectedFolder: .constant(nil),
        mode: .folderSelection
    )
}

#endif
