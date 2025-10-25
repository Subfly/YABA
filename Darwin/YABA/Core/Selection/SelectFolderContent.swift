//
//  SelectFolderContent.swift
//  YABA
//
//  Created by Ali Taha on 22.04.2025.
//

import SwiftUI
import SwiftData

enum FolderSelectionMode {
    case folderSelection
    case parentSelection
    case moveBookmarks(YabaCollection, () -> Void) // onSelectCallback for selection
}

struct SelectFolderContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @State
    private var showFolderCreationSheet: Bool = false
    
    @State
    private var searchQuery: String = ""
    
    @Binding
    var selectedFolder: YabaCollection?
    
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
                    showFolderCreationSheet = true
                } label: {
                    YabaIconView(bundleKey: "folder-add")
                        .scaledToFit()
                }
            }
        }
        .sheet(isPresented: $showFolderCreationSheet) {
            CollectionCreationContent(
                collectionType: .folder,
                collectionToEdit: nil,
                onEditCallback: { _ in }
            )
        }
    }
}

private struct SelectFolderSearchableContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Query
    private var allFolders: [YabaCollection]
    
    @Binding
    var selectedFolder: YabaCollection?
    
    @Binding
    var searchQuery: String
    
    let mode: FolderSelectionMode
    
    init(
        mode: FolderSelectionMode,
        selectedFolder: Binding<YabaCollection?>,
        searchQuery: Binding<String>
    ) {
        self.mode = mode
    
        var parentFolder: YabaCollection? = nil
        if case .moveBookmarks(let yabaCollection, _) = mode {
            parentFolder = yabaCollection
        }
        
        let compareValue = CollectionType.folder.rawValue
        let query = searchQuery.wrappedValue
        let parentCollectionId = parentFolder?.collectionId ?? ""
        _allFolders = Query(
            filter: #Predicate<YabaCollection> {
                if query.isEmpty {
                    $0.type == compareValue
                    && $0.collectionId != parentCollectionId
                } else {
                    $0.type == compareValue
                    && $0.label.localizedStandardContains(query)
                    && $0.collectionId != parentCollectionId
                }
            }
        )
        
        _selectedFolder = selectedFolder
        _searchQuery = searchQuery
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
                ForEach(allFolders) { folder in
                    ListCollectionItemView(
                        collection: folder,
                        isInSelectionMode: true,
                        isInBookmarkDetail: false,
                        onDeleteCallback: { collection in
                            withAnimation {
                                if selectedFolder?.id == collection.id {
                                    selectedFolder = nil
                                }
                            }
                        },
                        onEditCallback: { collection in
                            withAnimation {
                                if selectedFolder?.id == collection.id {
                                    selectedFolder = collection
                                }
                            }
                        },
                        onNavigationCallback: { _ in }
                    )
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation {
                            dismiss()
                            /**
                             * Worst and best thing I've ever seen in a language
                             */
                            if case .moveBookmarks(_, let onSelected) = mode {
                                onSelected()
                            }
                            selectedFolder = folder
                        }
                    }
                }
            }
        }.listRowSpacing(0)
    }
}

#Preview {
    SelectFolderContent(
        selectedFolder: .constant(nil),
        mode: .folderSelection
    )
}
