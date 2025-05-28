//
//  SearchView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI
import SwiftData

// TODO: MOVE IT TO DETAIL, NOT LET BE A PART OF HOME
struct SearchView: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @AppStorage(Constants.preferredSortingKey)
    private var preferredSorting: SortType = .createdAt
    
    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .ascending
    
    @State
    private var searchQuery: String = ""
    
    // !iOS only
    let onCloseRequested: () -> Void
    let onSelectBookmark: (YabaBookmark) -> Void
    
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: .accentColor)
            SearchableContent(
                searchQuery: $searchQuery,
                preferredSorting: preferredSorting,
                preferredOrder: preferredSortOrder,
                onNavigationCallback: onSelectBookmark
            )
        }
        .navigationTitle("Search Title")
        .searchable(
            text: $searchQuery,
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: "Search Prompt"
        )
        .toolbar {
            #if targetEnvironment(macCatalyst)
            ToolbarItem(placement: .primaryAction) {
                MacOSHoverableToolbarIcon(
                    bundleKey: "cancel-circle",
                    onPressed: onCloseRequested
                )
            }
            #else
            if UIDevice.current.userInterfaceIdiom == .pad {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        onCloseRequested()
                    } label: {
                        YabaIconView(bundleKey: "cancel-circle")
                    }
                }
            } else {
                if UIDevice.current.userInterfaceIdiom == .phone {
                    ToolbarItem(placement: .navigation) {
                        Button {
                            dismiss()
                        } label: {
                            YabaIconView(bundleKey: "arrow-left-01")
                        }.buttonRepeatBehavior(.enabled)
                    }
                }
                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        ContentAppearancePicker()
                        SortingPicker()
                    } label: {
                        YabaIconView(bundleKey: "more-horizontal-circle-02")
                    }
                }
            }
            #endif
        }
    }
}

private struct SearchableContent: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @Query
    private var bookmarks: [YabaBookmark]
    
    @Binding
    var searchQuery: String
    let onNavigationCallback: (YabaBookmark) -> Void
    
    init(
        searchQuery: Binding<String>,
        preferredSorting: SortType,
        preferredOrder: SortOrderType,
        onNavigationCallback: @escaping (YabaBookmark) -> Void
    ) {
        _searchQuery = searchQuery
        self.onNavigationCallback = onNavigationCallback
        
        let sortDescriptor: SortDescriptor<YabaBookmark> = switch preferredSorting {
        case .createdAt:
                .init(\.createdAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .editedAt:
                .init(\.editedAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .label:
                .init(\.label, order: preferredOrder == .ascending ? .forward : .reverse)
        }
        
        let query = searchQuery.wrappedValue
        _bookmarks = Query(
            filter: #Predicate { bookmark in
                if query.isEmpty {
                    true
                } else {
                    bookmark.label.localizedStandardContains(query)
                    || bookmark.bookmarkDescription.localizedStandardContains(query)
                }
            },
            sort: [sortDescriptor],
            animation: .smooth
        )
    }
    
    var body: some View {
        if bookmarks.isEmpty {
            if searchQuery.isEmpty {
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
            }
        } else {
            List {
                ForEach(bookmarks) { bookmark in
                    BookmarkItemView(
                        bookmark: bookmark,
                        onNavigationCallback: onNavigationCallback
                    )
                    .listRowSeparator(.hidden)
                }
            }
            .listRowSpacing(contentAppearance == .list ? 0 : 8)
            .scrollContentBackground(.hidden)
            #if !os(visionOS)
            .scrollDismissesKeyboard(.immediately)
            #endif
        }
    }
}
