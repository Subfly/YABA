//
//  SearchView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftData
import SwiftUI

struct SearchView: View {
    @Environment(\.dismiss)
    private var dismiss

    @AppStorage(Constants.preferredBookmarkSortingKey)
    private var preferredSorting: SortType = .createdAt

    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .ascending

    @State
    private var machine = SearchStateMachine()

    var body: some View {
        ZStack {
            AnimatedGradient(color: .accentColor)
            SearchableContent(
                searchQuery: machine.state.query,
                preferredSorting: preferredSorting,
                preferredOrder: preferredSortOrder,
                onNavigationCallback: { _ in }
            )
        }
        .navigationTitle("Search Title")
        .searchable(
            text: Binding(
                get: { machine.state.query },
                set: { newValue in
                    Task { await machine.send(.onChangeQuery(newValue)) }
                }
            ),
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: "Search Prompt"
        )
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .navigation) {
                Button {
                    dismiss()
                } label: {
                    YabaIconView(bundleKey: "arrow-left-01")
                }
                .buttonRepeatBehavior(.enabled)
            }
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    ContentAppearancePicker()
                    SortingPicker(contentType: .bookmark)
                } label: {
                    YabaIconView(bundleKey: "more-horizontal-circle-02")
                }
            }
        }
        .task {
            await machine.send(.onInit)
            await machine.send(.onChangeSort(sortType: preferredSorting, sortOrder: preferredSortOrder))
        }
        .onChange(of: preferredSorting) { _, newValue in
            Task {
                await machine.send(.onChangeSort(sortType: newValue, sortOrder: preferredSortOrder))
            }
        }
        .onChange(of: preferredSortOrder) { _, newValue in
            Task {
                await machine.send(.onChangeSort(sortType: preferredSorting, sortOrder: newValue))
            }
        }
    }
}

private struct SearchableContent: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ContentAppearance = .list

    @Query
    private var bookmarks: [YabaBookmark]

    let searchQuery: String
    let onNavigationCallback: (YabaBookmark) -> Void

    init(
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

        let query = searchQuery
        _bookmarks = Query(
            filter: #Predicate { bookmark in
                if query.isEmpty {
                    true
                } else {
                    bookmark.label.localizedStandardContains(query)
                    || bookmark.bookmarkDescription?.localizedStandardContains(query) == true
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
                        isInRecents: false,
                        isSelected: false,
                        isInSelectionMode: false,
                        onNavigationCallback: onNavigationCallback
                    )
                    .listRowSeparator(.hidden)
                }
            }
            .listRowSpacing(contentAppearance == .list ? 0 : 8)
            .scrollContentBackground(.hidden)
            .listStyle(.sidebar)
            #if !os(visionOS)
            .scrollDismissesKeyboard(.immediately)
            #endif
        }
    }
}
