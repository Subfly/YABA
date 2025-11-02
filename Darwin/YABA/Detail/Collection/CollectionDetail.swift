//
//  CollectionDetail.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import SwiftUI
import SwiftData

struct MobileCollectionDetail: View {
    let collection: YabaCollection?
    let onNavigationCallback: (YabaBookmark) -> Void
    let onAcceptKeyboard: () -> Void
    let onDeleteKeyboard: () -> Void
    
    var body: some View {
        CollectionDetail(
            collection: collection,
            onNavigationCallback: onNavigationCallback,
            onAcceptKeyboard: onAcceptKeyboard,
            onDeleteKeyboard: onDeleteKeyboard
        )
    }
}

struct GeneralCollectionDetail: View {
    @Environment(\.modelContext)
    private var modelContext

    @Environment(\.appState)
    private var appState
    
    @Environment(\.deepLinkManager)
    private var deepLinkManager
    
    let onNavigationCallback: (YabaBookmark) -> Void
    let onAcceptKeyboard: () -> Void
    let onDeleteKeyboard: () -> Void
    
    var body: some View {
        CollectionDetail(
            collection: appState.selectedCollection,
            onNavigationCallback: onNavigationCallback,
            onAcceptKeyboard: onAcceptKeyboard,
            onDeleteKeyboard: onDeleteKeyboard
        ).onChange(of: deepLinkManager.openCollectionRequest) { oldValue, newValue in
            if oldValue == nil {
                if let newRequest = newValue {
                    let id = newRequest.collectionId
                    if let collections = try? modelContext.fetch(
                        .init(predicate: #Predicate<YabaCollection> { $0.collectionId ==  id })
                    ), let collection = collections.first {
                        appState.selectedCollection = collection
                    }
                    deepLinkManager.onHandleDeeplink()
                }
            }
        }
    }
}

private struct CollectionDetail: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Environment(\.modelContext)
    private var modelContext
    
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @AppStorage(Constants.preferredBookmarkSortingKey)
    private var preferredSorting: SortType = .createdAt
    
    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .ascending
    
    @State
    private var state: CollectionDetailState = .init()
    
    let collection: YabaCollection?
    let onNavigationCallback: (YabaBookmark) -> Void
    let onAcceptKeyboard: () -> Void
    let onDeleteKeyboard: () -> Void
    
    var body: some View {
        ZStack {
            AnimatedGradient(
                collectionColor: collection?.color.getUIColor() ?? .accentColor
            )
            if let collection {
                if collection.bookmarks?.isEmpty == true {
                    ContentUnavailableView {
                        Label {
                            Text("No Bookmarks Title")
                        } icon: {
                            YabaIconView(bundleKey: "bookmark-02")
                                .scaledToFit()
                                .frame(width: 52, height: 52)
                        }
                    } description: {
                        Text("No Bookmarks Message")
                    }
                } else {
                    SearchableContent(
                        collection: collection,
                        searchQuery: state.searchQuery,
                        selectedBookmarks: $state.selectedBookmarks,
                        isInSelectionMode: state.isInSelectionMode,
                        preferredSorting: preferredSorting,
                        preferredOrder: preferredSortOrder,
                        onNavigationCallback: { bookmark in
                            if state.isInSelectionMode {
                                state.upsertBookmarkInSelections(bookmark)
                            } else {
                                onNavigationCallback(bookmark)
                            }
                        }
                    )
                    #if !KEYBOARD_EXTENSION
                    .searchable(
                        text: $state.searchQuery,
                        placement: .navigationBarDrawer(displayMode: .always),
                        prompt: Text("Search Collection \(collection.label)")
                    )
                    #endif
                }
            } else {
                ContentUnavailableView {
                    Label {
                        Text("No Selected Collection Title")
                    } icon: {
                        YabaIconView(bundleKey: "dashboard-square-02")
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                    }
                } description: {
                    Text("No Selected Collection Message")
                }
            }
        }
        .navigationTitle(
            collection?.collectionId == Constants.uncategorizedCollectionId
            ? Text(LocalizedStringKey(Constants.uncategorizedCollectionLabelKey))
            : Text(collection?.label ?? "")
        )
        .toolbar {
            #if !KEYBOARD_EXTENSION
            ToolbarItem(placement: .primaryAction) {
                ToolbarItems(collection: collection, state: $state)
                    .tint(collection?.color.getUIColor() ?? .accentColor)
            }
            #else
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    onDeleteKeyboard()
                } label: {
                    Label {
                        Text("Delete")
                    } icon: {
                        YabaIconView(bundleKey: "backward-01")
                    }
                }.buttonRepeatBehavior(.enabled)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    onAcceptKeyboard()
                } label: {
                    Label {
                        Text("Done")
                    } icon: {
                        YabaIconView(bundleKey: "checkmark-circle-02")
                    }
                }
            }
            #endif
            
            if UIDevice.current.userInterfaceIdiom == .phone {
                ToolbarItem(placement: .navigation) {
                    Button {
                        dismiss()
                    } label: {
                        YabaIconView(bundleKey: "arrow-left-01")
                    }.buttonRepeatBehavior(.enabled)
                }
            }
        }
        .sheet(isPresented: $state.shouldShowCreateBookmarkSheet) {
            BookmarkCreationContent(
                bookmarkToEdit: nil,
                collectionToFill: collection,
                link: nil,
                onExitRequested: {}
            )
        }
        .sheet(isPresented: $state.shouldShowMoveBookmarksSheet) {
            NavigationView {
                if let collection {
                    SelectFolderContent(
                        selectedFolder: $state.selectedFolderToMove,
                        // Sometimes, swift makes me wtf is this...
                        mode: .moveBookmarks(collection) {
                            state.shouldShowMoveBookmarksSheet = false
                        },
                    )
                }
            }.onDisappear {
                state.handleChangeFolderRequest(with: modelContext)
            }
        }
        .alert(
            LocalizedStringKey("Bookmark Selection Delete All Message"),
            isPresented: $state.shouldShowDeleteDialog
        ) {
            Button(role: .cancel) {
                state.shouldShowDeleteDialog = false
            } label: {
                Text("Cancel")
            }
            Button(role: .destructive) {
                withAnimation {
                    state.handleDeletionRequest(with: modelContext)
                    state.shouldShowDeleteDialog = false
                }
            } label: {
                Text("Delete")
            }
        }
        .tint(collection?.color.getUIColor() ?? .accentColor)
    }
}

private struct SearchableContent: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @Binding
    var selectedBookmarks: Set<YabaBookmark>
    let isInSelectionMode: Bool
    
    private let bookmarks: [YabaBookmark]
    let searchQuery: String
    let onNavigationCallback: (YabaBookmark) -> Void
    
    init(
        collection: YabaCollection,
        searchQuery: String,
        selectedBookmarks: Binding<Set<YabaBookmark>>,
        isInSelectionMode: Bool,
        preferredSorting: SortType,
        preferredOrder: SortOrderType,
        onNavigationCallback: @escaping (YabaBookmark) -> Void
    ) {
        self.searchQuery = searchQuery
        self.onNavigationCallback = onNavigationCallback
        _selectedBookmarks = selectedBookmarks
        self.isInSelectionMode = isInSelectionMode
        
        let sortDescriptor: SortDescriptor<YabaBookmark> = switch preferredSorting {
        case .createdAt:
                .init(\.createdAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .editedAt:
                .init(\.editedAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .label:
                .init(\.label, order: preferredOrder == .ascending ? .forward : .reverse)
        case .custom: // Will not execute as it is not possible
                .init(\.createdAt, order: preferredOrder == .ascending ? .forward : .reverse)
        }
        
        bookmarks = collection.bookmarks?.filter { bookmark in
            if searchQuery.isEmpty {
                true
            } else {
                bookmark.label.localizedStandardContains(searchQuery)
                || bookmark.bookmarkDescription.localizedStandardContains(searchQuery)
            }
        }.sorted(using: sortDescriptor) ?? []
    }
    
    var body: some View {
        if bookmarks.isEmpty {
            ContentUnavailableView {
                Label {
                    Text("Search No Bookmarks Found Title")
                } icon: {
                    YabaIconView(bundleKey: "bookmark-off-02")
                        .scaledToFit()
                        .frame(width: 52, height: 52)
                }
            } description: {
                Text("Search No Bookmarks Found Description \(searchQuery)")
            }
        } else {
            List {
                ForEach(bookmarks) { bookmark in
                    BookmarkItemView(
                        bookmark: bookmark,
                        isInRecents: false,
                        isSelected: selectedBookmarks.contains(bookmark),
                        isInSelectionMode: isInSelectionMode,
                        onNavigationCallback: onNavigationCallback
                    )
                }
            }
            .listRowSeparator(.hidden)
            .listRowSpacing(contentAppearance == .list ? 0 : 8)
            .scrollContentBackground(.hidden)
            .listStyle(.sidebar)
        }
    }
}

private struct ToolbarItems: View {
    let collection: YabaCollection?
    
    @Binding
    var state: CollectionDetailState
    
    var body: some View {
        if collection != nil {
            Menu {
                Button {
                    state.shouldShowCreateBookmarkSheet = true
                } label: {
                    Label {
                        Text("New")
                    } icon: {
                        YabaIconView(bundleKey: "plus-sign-circle")
                    }
                }
                if collection?.collectionType == .folder && state.isInSelectionMode {
                    Divider()
                    Button {
                        state.shouldShowMoveBookmarksSheet = true
                    } label: {
                        Label {
                            Text("Bookmark Selection Move")
                        } icon: {
                            YabaIconView(bundleKey: "arrow-move-up-right")
                        }
                    }
                    .disabled(state.selectedBookmarks.isEmpty)
                    
                    Button {
                        state.shouldShowDeleteDialog = true
                    } label: {
                        Label {
                            Text("Bookmark Selection Delete")
                        } icon: {
                            YabaIconView(bundleKey: "delete-02")
                        }
                    }
                    .tint(.red)
                    .disabled(state.selectedBookmarks.isEmpty)
                }
                if collection?.collectionType == .folder {
                    Button {
                        withAnimation {
                            state.isInSelectionMode.toggle()
                        }
                    } label: {
                        Label {
                            Text(
                                state.isInSelectionMode
                                ? "Bookmark Selection Cancel"
                                : "Bookmark Selection Enable"
                            )
                        } icon: {
                            YabaIconView(
                                bundleKey: state.isInSelectionMode
                                ? "cancel-circle"
                                : "checkmark-circle-01"
                            )
                        }
                    }
                }
                #if !KEYBOARD_EXTENSION
                Divider()
                ContentAppearancePicker()
                SortingPicker(contentType: .bookmark)
                #endif
            } label: {
                YabaIconView(bundleKey: "more-horizontal-circle-02")
            }
        }
    }
}

#Preview {
    CollectionDetail(
        collection: .empty(),
        onNavigationCallback: { _ in },
        onAcceptKeyboard: {},
        onDeleteKeyboard: {}
    )
}
