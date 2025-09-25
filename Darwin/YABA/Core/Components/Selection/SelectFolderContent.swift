//
//  SelectFolderContent.swift
//  YABA
//
//  Created by Ali Taha on 22.04.2025.
//

import SwiftUI
import SwiftData

enum FolderSelectionMode {
    case parent, moving
}

struct SelectFolderContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @State
    private var showFolderCreationSheet: Bool = false
    
    @State
    private var searchQuery: String = ""
    
    @State
    private var tempSelectedFolder: YabaCollection? = nil
    
    let mode: FolderSelectionMode
    let folderInAction: YabaCollection?
    let selectedFolder: YabaCollection?
    let onSelectNewFolder: (YabaCollection) -> Void
    let onEditSelectedFolderDuringCreation: (YabaCollection) -> Void
    let onDeleteSelectedFolderDuringCreation: () -> Void
    
    var body: some View {
        SelectFolderSearchableContent(
            folderInAction: folderInAction,
            selectedFolder: selectedFolder,
            tempSelectedFolder: $tempSelectedFolder,
            searchQuery: $searchQuery,
            mode: mode,
            onSelectNewFolder: onSelectNewFolder,
            onEditSelectedFolderDuringCreation: onEditSelectedFolderDuringCreation,
            onDeleteSelectedFolderDuringCreation: onDeleteSelectedFolderDuringCreation
        )
        .scrollDismissesKeyboard(.immediately)
        .searchable(text: $searchQuery, prompt: "Folder Search Prompt")
        .navigationTitle("Select Folder Title")
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .navigation) {
                Button {
                    dismiss()
                } label: {
                    if mode == .parent {
                        YabaIconView(bundleKey: "arrow-left-01")
                    } else {
                        Text("Cancel")
                    }
                }.buttonRepeatBehavior(.enabled)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showFolderCreationSheet = true
                } label: {
                    YabaIconView(bundleKey: "folder-add")
                        .scaledToFit()
                }
            }
            if mode == .moving {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                        if let tempSelectedFolder {
                            onSelectNewFolder(tempSelectedFolder)
                        }
                    } label: {
                        Text("Done")
                    }
                }
            }
        }
        .sheet(isPresented: $showFolderCreationSheet) {
            CollectionCreationContent(
                collectionType: .folder,
                collectionToAdd: nil,
                collectionToEdit: nil,
                onEditCallback: { _ in }
            )
        }.onAppear {
            tempSelectedFolder = selectedFolder
        }
    }
}

private struct SelectFolderSearchableContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Query
    private var allFolders: [YabaCollection]
    
    @Binding
    private var searchQuery: String
    
    @Binding
    private var tempSelectedFolder: YabaCollection?
    
    let mode: FolderSelectionMode
    let folderInAction: YabaCollection?
    let selectedFolder: YabaCollection?
    let onSelectNewFolder: (YabaCollection) -> Void
    let onEditSelectedFolderDuringCreation: (YabaCollection) -> Void
    let onDeleteSelectedFolderDuringCreation: () -> Void
    
    init(
        folderInAction: YabaCollection?,
        selectedFolder: YabaCollection?,
        tempSelectedFolder: Binding<YabaCollection?>,
        searchQuery: Binding<String>,
        mode: FolderSelectionMode,
        onSelectNewFolder: @escaping (YabaCollection) -> Void,
        onEditSelectedFolderDuringCreation: @escaping (YabaCollection) -> Void,
        onDeleteSelectedFolderDuringCreation: @escaping () -> Void,
    ) {
        let compareValue = CollectionType.folder.rawValue
        let query = searchQuery.wrappedValue
        _allFolders = Query(
            filter: #Predicate<YabaCollection> {
                if query.isEmpty {
                    $0.type == compareValue
                } else {
                    $0.type == compareValue
                    && $0.label.localizedStandardContains(query)
                }
            }
        )
        self.selectedFolder = selectedFolder
        self.folderInAction = folderInAction
        self.mode = mode
        self.onSelectNewFolder = onSelectNewFolder
        self.onEditSelectedFolderDuringCreation = onEditSelectedFolderDuringCreation
        self.onDeleteSelectedFolderDuringCreation = onDeleteSelectedFolderDuringCreation
        _searchQuery = searchQuery
        _tempSelectedFolder = tempSelectedFolder
    }
    
    var body: some View {
        List {
            if allFolders.isEmpty {
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
                let selectableFolders = allFolders.filter { folder in
                    guard let moving = folderInAction else { return true }
                    
                    return folder.collectionType == .folder &&
                           folder.collectionId != moving.collectionId &&
                           folder.collectionId != moving.parentCollection?.collectionId &&
                           !isDescendant(moving, of: folder)
                }
                
                ForEach(selectableFolders) { folder in
                    let isInSelectionModeAndSelected: Bool = if selectedFolder != nil {
                        folder.collectionId == selectedFolder?.collectionId
                    } else if tempSelectedFolder != nil {
                        folder.collectionId == tempSelectedFolder?.collectionId
                    } else {
                        false
                    }
                    
                    CollectionItemView(
                        collection: folder,
                        inSelectionModeAndSelected: isInSelectionModeAndSelected,
                        isInCreationMode: true,
                        isInBookmarkDetail: false,
                        onDeleteCallback: { collection in
                            withAnimation {
                                if selectedFolder?.collectionId == collection.collectionId {
                                    onDeleteSelectedFolderDuringCreation()
                                }
                            }
                        },
                        onEditCallback: { collection in
                            withAnimation {
                                if selectedFolder?.collectionId == collection.collectionId {
                                    onEditSelectedFolderDuringCreation(collection)
                                }
                            }
                        },
                        onNavigationCallback: { selection in
                            withAnimation {
                                if mode != .moving {
                                    dismiss()
                                    onSelectNewFolder(selection)
                                } else {
                                    print(selection.label)
                                    tempSelectedFolder = selection
                                }
                            }
                        }
                    )
                }
            }
        }.listRowSpacing(0)
    }
    
    func isDescendant(_ candidate: YabaCollection, of folder: YabaCollection) -> Bool {
        folder.children.contains(where: { $0.collectionId == candidate.collectionId }) ||
        folder.children.contains { isDescendant(candidate, of: $0) }
    }
}
