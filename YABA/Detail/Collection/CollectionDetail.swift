//
//  CollectionDetail.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import SwiftUI
import SwiftData

struct CollectionDetail: View {
    @State
    private var state: CollectionDetailState = .init()
    
    @Binding
    var collection: YabaCollection?
    
    @Binding
    var selectedBookmark: Bookmark?
    
    let onNavigationCallback: (Bookmark) -> Void
    
    var body: some View {
        ZStack {
            AnimatedMeshGradient(
                collectionColor: collection?.color.getUIColor() ?? .accentColor
            )
            if let collection {
                if collection.bookmarks.isEmpty {
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
                    let filtered = collection.bookmarks.filter {
                        if state.searchQuery.isEmpty {
                            true
                        } else {
                            $0.label.localizedStandardContains(state.searchQuery)
                            || $0.bookmarkDescription.localizedStandardContains(state.searchQuery)
                        }
                    }
                    Group {
                        if filtered.isEmpty {
                            ContentUnavailableView {
                                Label {
                                    Text("Search No Bookmarks Found Title")
                                } icon: {
                                    YabaIconView(bundleKey: "bookmark-off-02")
                                        .scaledToFit()
                                        .frame(width: 52, height: 52)
                                }
                            } description: {
                                Text("Search No Bookmarks Found Description \(state.searchQuery)")
                            }
                        } else {
                            List(selection: $selectedBookmark) {
                                ForEach(filtered) { bookmark in
                                    BookmarkItemView(
                                        selectedBookmark: $selectedBookmark,
                                        bookmark: bookmark,
                                        isSearching: false,
                                        onNavigationCallback: onNavigationCallback
                                    )
                                }
                            }
                            .listStyle(.sidebar)
                            .scrollContentBackground(.hidden)
                        }
                    }
                    .searchable(
                        text: $state.searchQuery,
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
        .navigationTitle(collection?.label ?? "")
        .toolbar {
            if collection != nil {
                ToolbarItem(placement: .primaryAction) {
                    #if targetEnvironment(macCatalyst)
                    MacOSHoverableToolbarIcon(
                        bundleKey: "plus-sign-circle",
                        onPressed: {
                            state.shouldShowCreateBookmarkSheet = true
                        }
                    )
                    #else
                    Button {
                        state.shouldShowCreateBookmarkSheet = true
                    } label: {
                        YabaIconView(bundleKey: "plus-sign-circle")
                    }
                    #endif
                }
            }
        }
        .sheet(isPresented: $state.shouldShowCreateBookmarkSheet) {
            BookmarkCreationContent(
                bookmarkToEdit: .constant(nil),
                initialCollection: $collection,
                link: nil,
                onExitRequested: {}
            )
        }
    }
}

#Preview {
    CollectionDetail(
        collection: .constant(.empty()),
        selectedBookmark: .constant(.empty()),
        onNavigationCallback: { _ in }
    )
}
