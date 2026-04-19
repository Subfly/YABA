//
//  TagDetailView.swift
//  YABA
//
//  Tag bookmark list in the detail column (live via @Query).
//

import SwiftData
import SwiftUI

struct TagDetailView: View {
    let tagId: String

    @AppStorage(Constants.preferredBookmarkSortingKey)
    private var preferredSorting: SortType = .editedAt

    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .descending

    @State
    private var machine = TagDetailStateMachine()

    @State
    private var shouldShowDeleteDialog: Bool = false

    @Query
    private var tagResults: [YabaTag]

    init(tagId: String) {
        self.tagId = tagId
        var descriptor = FetchDescriptor<YabaTag>(
            predicate: #Predicate<YabaTag> { $0.tagId == tagId }
        )
        descriptor.fetchLimit = 1
        _tagResults = Query(descriptor, animation: .smooth)
    }

    var body: some View {
        ZStack {
            AnimatedGradient(color: tagTint)
            if tag != nil {
                TagSearchableContent(
                    tagId: tagId,
                    searchQuery: machine.state.query,
                    preferredSorting: preferredSorting,
                    preferredOrder: preferredSortOrder,
                    selectedBookmarkIds: machine.state.selectedBookmarkIds,
                    isInSelectionMode: machine.state.selectionMode,
                    onNavigationCallback: { bookmark in
                        guard machine.state.selectionMode else { return }
                        Task {
                            await machine.send(.onToggleBookmarkSelection(bookmarkId: bookmark.bookmarkId))
                        }
                    }
                )
            } else {
                ContentUnavailableView {
                    Label {
                        Text("No Selected Collection Title")
                    } icon: {
                        YabaIconView(bundleKey: "tag-01")
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                    }
                } description: {
                    Text("No Selected Collection Message")
                }
            }
        }
        .navigationTitle(navigationTitle)
        .searchable(
            text: Binding(
                get: { machine.state.query },
                set: { newValue in
                    Task { await machine.send(.onChangeQuery(newValue)) }
                }
            ),
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: Text("Search Collection \(tagLabelForSearchPrompt)")
        )
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    if machine.state.selectionMode {
                        Button {
                            shouldShowDeleteDialog = true
                        } label: {
                            Label {
                                Text("Bookmark Selection Delete")
                            } icon: {
                                YabaIconView(bundleKey: "delete-02")
                            }
                        }
                        .tint(.red)
                        .disabled(machine.state.selectedBookmarkIds.isEmpty)
                    }
                    Button {
                        Task { await machine.send(.onToggleSelectionMode) }
                    } label: {
                        Label {
                            Text(
                                machine.state.selectionMode
                                    ? "Bookmark Selection Cancel"
                                    : "Bookmark Selection Enable"
                            )
                        } icon: {
                            YabaIconView(
                                bundleKey: machine.state.selectionMode
                                    ? "cancel-circle"
                                    : "checkmark-circle-01"
                            )
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
        .tint(tagTint)
        .task(id: tagId) {
            await machine.send(.onInit(tagId: tagId))
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
        .alert(
            LocalizedStringKey("Bookmark Selection Delete All Message"),
            isPresented: $shouldShowDeleteDialog
        ) {
            Button(role: .cancel) {
                shouldShowDeleteDialog = false
            } label: {
                Text("Cancel")
            }
            Button(role: .destructive) {
                Task {
                    let ids = Array(machine.state.selectedBookmarkIds)
                    await machine.send(.onDeleteSelected(bookmarkIds: ids))
                    await machine.send(.onToggleSelectionMode)
                    shouldShowDeleteDialog = false
                }
            } label: {
                Text("Delete")
            }
        }
    }

    private var tag: YabaTag? {
        tagResults.first
    }

    private var navigationTitle: Text {
        if tagId == Constants.Tag.Pinned.id {
            Text(LocalizedStringKey(Constants.Tag.Pinned.name))
        } else {
            Text(tag?.label ?? "")
        }
    }

    private var tagLabelForSearchPrompt: String {
        if tagId == Constants.Tag.Pinned.id {
            String(localized: String.LocalizationValue(Constants.Tag.Pinned.name))
        } else {
            tag?.label ?? ""
        }
    }

    private var tagTint: Color {
        tag?.color.getUIColor() ?? .accentColor
    }

}

private struct TagSearchableContent: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ContentAppearance = .list

    @Query
    private var pinnedBookmarks: [YabaBookmark]

    @Query
    private var unpinnedBookmarks: [YabaBookmark]

    let tagId: String
    let searchQuery: String
    let selectedBookmarkIds: Set<String>
    let isInSelectionMode: Bool
    let onNavigationCallback: (YabaBookmark) -> Void

    init(
        tagId: String,
        searchQuery: String,
        preferredSorting: SortType,
        preferredOrder: SortOrderType,
        selectedBookmarkIds: Set<String>,
        isInSelectionMode: Bool,
        onNavigationCallback: @escaping (YabaBookmark) -> Void
    ) {
        self.tagId = tagId
        self.searchQuery = searchQuery
        self.selectedBookmarkIds = selectedBookmarkIds
        self.isInSelectionMode = isInSelectionMode
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
        _pinnedBookmarks = Query(
            filter: #Predicate<YabaBookmark> { bookmark in
                if !bookmark.tags.contains(where: { $0.tagId == tagId }) {
                    false
                } else if !bookmark.isPinned {
                    false
                } else if query.isEmpty {
                    true
                } else {
                    bookmark.label.localizedStandardContains(query)
                    || bookmark.bookmarkDescription?.localizedStandardContains(query) == true
                }
            },
            sort: [sortDescriptor],
            animation: .smooth
        )

        _unpinnedBookmarks = Query(
            filter: #Predicate<YabaBookmark> { bookmark in
                if !bookmark.tags.contains(where: { $0.tagId == tagId }) {
                    false
                } else if bookmark.isPinned {
                    false
                } else if query.isEmpty {
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
        if isEmpty {
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
                if shouldSplitPinned {
                    if !pinnedBookmarks.isEmpty {
                        Section {
                            ForEach(pinnedBookmarks) { bookmark in
                                row(for: bookmark)
                            }
                        } header: {
                            Label {
                                Text("Pinned Bookmarks")
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                            } icon: {
                                YabaIconView(bundleKey: "pin")
                                    .frame(width: 18, height: 18)
                            }
                            .foregroundStyle(.secondary)
                        }
                    }
                    ForEach(unpinnedBookmarks) { bookmark in
                        row(for: bookmark)
                    }
                } else if isPinnedSystemTag {
                    ForEach(pinnedBookmarks) { bookmark in
                        row(for: bookmark)
                    }
                } else {
                    ForEach(unpinnedBookmarks) { bookmark in
                        row(for: bookmark)
                    }
                }
            }
            .listRowSeparator(.hidden)
            .listRowSpacing(contentAppearance == .list ? 0 : 8)
            .scrollContentBackground(.hidden)
            .listStyle(.sidebar)
        }
    }

    private var shouldSplitPinned: Bool {
        !isPinnedSystemTag && !pinnedBookmarks.isEmpty
    }

    private var isPinnedSystemTag: Bool {
        tagId == Constants.Tag.Pinned.id
    }

    private var isEmpty: Bool {
        if isPinnedSystemTag {
            return pinnedBookmarks.isEmpty
        }
        return pinnedBookmarks.isEmpty && unpinnedBookmarks.isEmpty
    }

    private func row(for bookmark: YabaBookmark) -> some View {
        BookmarkItemView(
            bookmark: bookmark,
            isInRecents: false,
            isSelected: selectedBookmarkIds.contains(bookmark.bookmarkId),
            isInSelectionMode: isInSelectionMode,
            onNavigationCallback: onNavigationCallback
        )
    }
}
