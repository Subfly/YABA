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
    
    var body: some View {
        CollectionDetail(
            collection: collection,
            onNavigationCallback: onNavigationCallback
        )
    }
}

struct GeneralCollectionDetail: View {
    @Environment(\.appState)
    private var appState
    
    let onNavigationCallback: (YabaBookmark) -> Void
    
    var body: some View {
        CollectionDetail(
            collection: appState.selectedCollection,
            onNavigationCallback: onNavigationCallback
        )
    }
}

private struct CollectionDetail: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @AppStorage(Constants.preferredSortingKey)
    private var preferredSorting: SortType = .createdAt
    
    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .ascending
    
    @State
    private var state: CollectionDetailState = .init()
    
    let collection: YabaCollection?
    let onNavigationCallback: (YabaBookmark) -> Void
    
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
                        preferredSorting: preferredSorting,
                        preferredOrder: preferredSortOrder,
                        onNavigationCallback: onNavigationCallback
                    )
                    .searchable(
                        text: $state.searchQuery,
                        placement: .navigationBarDrawer(displayMode: .always),
                        prompt: Text("Search Collection \(collection.label)")
                    )
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
            ToolbarItem(placement: .primaryAction) {
                ToolbarItems(collection: collection, state: $state)
                    .tint(collection?.color.getUIColor() ?? .accentColor)
            }
            
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
        .tint(collection?.color.getUIColor() ?? .accentColor)
    }
}

private struct SearchableContent: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    private let bookmarks: [YabaBookmark]
    let searchQuery: String
    let onNavigationCallback: (YabaBookmark) -> Void
    
    init(
        collection: YabaCollection,
        searchQuery: String,
        preferredSorting: SortType,
        preferredOrder: SortOrderType,
        onNavigationCallback: @escaping (YabaBookmark) -> Void
    ) {
        self.searchQuery = searchQuery
        self.onNavigationCallback = onNavigationCallback
        
        let sortDescriptor: SortDescriptor<YabaBookmark> = switch preferredSorting {
        case .createdAt:
                .init(\.createdAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .editedAt:
                .init(\.editedAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .label:
                .init(\.label, order: preferredOrder == .ascending ? .forward : .reverse)
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
    
    /** TODO: OPEN WHEN LAZYVSTACK RECYCLES
    @ViewBuilder
    private var gridView: some View {
        ScrollView {
            LazyVGrid(
                columns: [
                    .init(.flexible()),
                    .init(.flexible())
                ]
            ) {
                ForEach(collection.bookmarks) { bookmark in
                    BookmarkItemView(
                        bookmark: bookmark,
                        onNavigationCallback: onNavigationCallback
                    )
                }
            }.padding()
        }
        .scrollContentBackground(.hidden)
    }
    */
}

private struct ToolbarItems: View {
    let collection: YabaCollection?
    
    @Binding
    var state: CollectionDetailState
    
    var body: some View {
        if collection != nil {
            #if !targetEnvironment(macCatalyst)
            if UIDevice.current.userInterfaceIdiom == .phone {
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
                    ContentAppearancePicker()
                    SortingPicker()
                } label: {
                    YabaIconView(bundleKey: "more-horizontal-circle-02")
                }
            } else {
                Button {
                    state.shouldShowCreateBookmarkSheet = true
                } label: {
                    YabaIconView(bundleKey: "plus-sign-circle")
                }
            }
            #else
            MacOSHoverableToolbarIcon(
                bundleKey: "plus-sign-circle",
                tooltipKey: "Create Bookmark Title",
                onPressed: {
                    state.shouldShowCreateBookmarkSheet = true
                }
            )
            #endif
        }
    }
}

#Preview {
    CollectionDetail(
        collection: .empty(),
        onNavigationCallback: { _ in }
    )
}
