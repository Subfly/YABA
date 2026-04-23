//
//  BookmarkItemView.swift
//  YABA
//
//  List / card / grid bookmark row. Uses v2 `BookmarkModel` (import as `YABACore.BookmarkModel`; `YabaBookmark` is a typealias).
//

import SwiftData
import SwiftUI
import UIKit

private func bookmarkItemShareURL(_ bookmark: BookmarkModel) -> URL? {
    if let urlString = bookmark.linkDetail?.url,
       let u = URL(string: urlString), !urlString.isEmpty
    {
        return u
    }
    return nil
}

struct BookmarkItemView: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ContentAppearance = .list

    @AppStorage(Constants.preferredCardImageSizingKey)
    private var cardSizing: CardImageSizing = .small

    @State
    private var itemState = BookmarkItemState()

    let bookmark: BookmarkModel
    let isInRecents: Bool
    let isSelected: Bool
    let isInSelectionMode: Bool
    let onNavigationCallback: (BookmarkModel) -> Void

    var body: some View {
        bookmarkInteractiveCore
            .modifier(
                BookmarkContextMenuModifier(
                    bookmark: bookmark,
                    isInSelectionMode: isInSelectionMode,
                    itemState: $itemState
                )
            )
    }

    private var effectiveContentAppearance: ContentAppearance {
        isInRecents ? .list : contentAppearance
    }

    private var bookmarkInteractiveCore: some View {
        BookmarkItemBody(
            bookmark: bookmark,
            contentAppearance: effectiveContentAppearance,
            cardSizing: cardSizing,
            isAddedToSelection: isSelected,
            isInSelectionMode: isInSelectionMode,
            itemState: $itemState
        )
        .modifier(BookmarkSheetsAndAlertsModifier(
            bookmark: bookmark,
            itemState: $itemState,
            onNavigationCallback: onNavigationCallback
        ))
    }
}

// MARK: - Context menu

#if !KEYBOARD_EXTENSION
private struct BookmarkContextMenuModifier: ViewModifier {
    let bookmark: BookmarkModel
    let isInSelectionMode: Bool

    @Binding
    var itemState: BookmarkItemState

    func body(content: Content) -> some View {
        if isInSelectionMode {
            content
        } else {
            content.contextMenu {
                BookmarkOverflowMenuContent(bookmark: bookmark, itemState: $itemState)
            }
        }
    }
}
#else
private struct BookmarkContextMenuModifier: ViewModifier {
    let bookmark: BookmarkModel
    let isInSelectionMode: Bool

    @Binding
    var itemState: BookmarkItemState

    func body(content: Content) -> some View {
        content
    }
}
#endif

// MARK: - Body (list / card / grid)

private struct BookmarkItemBody: View {
    let bookmark: BookmarkModel
    let contentAppearance: ContentAppearance
    let cardSizing: CardImageSizing
    let isAddedToSelection: Bool
    let isInSelectionMode: Bool

    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        mainContent
            .contentShape(Rectangle())
            .id(bookmark.persistentModelID)
    }

    @ViewBuilder
    private var mainContent: some View {
        switch contentAppearance {
        case .list:
            BookmarkItemListContent(
                bookmark: bookmark,
                isAddedToSelection: isAddedToSelection,
                isInSelectionMode: isInSelectionMode,
                itemState: $itemState
            )
        case .card:
            switch cardSizing {
            case .big:
                BookmarkItemCardBigContent(
                    bookmark: bookmark,
                    isAddedToSelection: isAddedToSelection,
                    isInSelectionMode: isInSelectionMode,
                    itemState: $itemState
                )
            case .small:
                BookmarkItemCardSmallContent(
                    bookmark: bookmark,
                    isAddedToSelection: isAddedToSelection,
                    isInSelectionMode: isInSelectionMode,
                    itemState: $itemState
                )
            }
        case .grid:
            BookmarkItemGridContent(
                bookmark: bookmark,
                isAddedToSelection: isAddedToSelection,
                isInSelectionMode: isInSelectionMode,
                itemState: $itemState
            )
        }
    }
}

// MARK: - List

private struct BookmarkItemListContent: View {
    let bookmark: BookmarkModel
    let isAddedToSelection: Bool
    let isInSelectionMode: Bool

    @Binding
    var itemState: BookmarkItemState

    private var pinActionLabelKey: String {
        bookmark.isPinned ? "Pinned Tag Label" : "Bookmark Creation Toggle Pinned Title"
    }

    var body: some View {
        let row = HStack(alignment: .center) {
            BookmarkItemImage(
                bookmark: bookmark,
                contentAppearance: .list,
                cardSizing: .small,
                isAddedToSelection: isAddedToSelection
            )
                .clipShape(RoundedRectangle(cornerRadius: 8))
            VStack(alignment: .leading) {
                Text(bookmark.label)
                    .font(.title3)
                    .fontWeight(.medium)
                    .lineLimit(1)
                if let desc = bookmark.bookmarkDescription, !desc.isEmpty {
                    Text(desc)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
            }
            if UIDevice.current.userInterfaceIdiom == .phone {
                Spacer()
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tertiary)
            }
        }
        if isInSelectionMode {
            row
        } else {
            row
        #if !KEYBOARD_EXTENSION
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                Button {
                    itemState.shouldShowDeleteAlert = true
                } label: {
                    VStack {
                        YabaIconView(bundleKey: "delete-02")
                        Text(LocalizedStringKey("Delete"))
                    }
                }
                .tint(.red)
                Button {
                    itemState.shouldShowEditSheet = true
                } label: {
                    VStack {
                        YabaIconView(bundleKey: "edit-02")
                        Text(LocalizedStringKey("Edit"))
                    }
                }
                .tint(.orange)
                Button {
                    AllBookmarksManager.queueToggleBookmarkPinned(bookmarkId: bookmark.bookmarkId)
                } label: {
                    VStack {
                        YabaIconView(bundleKey: bookmark.isPinned ? "pin" : "pin-off")
                        Text(LocalizedStringKey(pinActionLabelKey))
                    }
                }
                .tint(.yellow)
            }
            .swipeActions(edge: .leading, allowsFullSwipe: false) {
                Button {
                    itemState.shouldShowShareSheet = true
                } label: {
                    Label {
                        Text(LocalizedStringKey("Share"))
                    } icon: {
                        YabaIconView(bundleKey: "share-03")
                            .scaledToFit()
                    }
                }
                .tint(.indigo)
                Button {
                    itemState.shouldShowMoveSheet = true
                } label: {
                    Label {
                        Text(LocalizedStringKey("Move"))
                    } icon: {
                        YabaIconView(bundleKey: "arrow-move-up-right")
                            .scaledToFit()
                    }
                }
                .tint(.teal)
            }
        #endif
        }
    }
}

// MARK: - Card

private struct BookmarkItemCardBigContent: View {
    let bookmark: BookmarkModel
    let isAddedToSelection: Bool
    let isInSelectionMode: Bool

    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        VStack(alignment: .leading) {
            BookmarkItemImage(
                bookmark: bookmark,
                contentAppearance: .card,
                cardSizing: .big,
                isAddedToSelection: isAddedToSelection
            )
                .clipShape(RoundedRectangle(cornerRadius: 8))
            HStack {
                if let data = bookmark.iconDataHolder, let img = UIImage(data: data) {
                    Image(uiImage: img)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                }
                Text(bookmark.label)
                    .font(.title2)
                    .fontWeight(.medium)
                    .lineLimit(2)
            }
            .padding(.vertical, 4)
            if let desc = bookmark.bookmarkDescription, !desc.isEmpty {
                Text(desc)
                    .lineLimit(4)
                    .multilineTextAlignment(.leading)
                    .padding(.bottom, 8)
            }
            HStack {
                BookmarkTagsRow(bookmark: bookmark)
                Spacer()
                if !isInSelectionMode {
                    BookmarkOverflowMenuButton(bookmark: bookmark, itemState: $itemState)
                }
            }
        }
    }
}

private struct BookmarkItemCardSmallContent: View {
    let bookmark: BookmarkModel
    let isAddedToSelection: Bool
    let isInSelectionMode: Bool

    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        VStack(alignment: .leading) {
            HStack(alignment: .center) {
                BookmarkItemImage(
                    bookmark: bookmark,
                    contentAppearance: .card,
                    cardSizing: .small,
                    isAddedToSelection: isAddedToSelection
                )
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                Text(bookmark.label)
                    .font(.title2)
                    .fontWeight(.medium)
                    .lineLimit(2)
                Spacer()
            }
            .padding(.vertical, 4)
            if let desc = bookmark.bookmarkDescription, !desc.isEmpty {
                Text(desc)
                    .lineLimit(4)
                    .multilineTextAlignment(.leading)
                    .padding(.bottom, 8)
            }
            HStack {
                BookmarkTagsRow(bookmark: bookmark)
                Spacer()
                HStack {
                    if let data = bookmark.iconDataHolder, let img = UIImage(data: data) {
                        Image(uiImage: img)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                    }
                    if !isInSelectionMode {
                        BookmarkOverflowMenuButton(bookmark: bookmark, itemState: $itemState)
                    }
                }
            }
        }
    }
}

// MARK: - Grid

private struct BookmarkItemGridContent: View {
    let bookmark: BookmarkModel
    let isAddedToSelection: Bool
    let isInSelectionMode: Bool

    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        VStack(spacing: 0) {
            BookmarkItemImage(
                bookmark: bookmark,
                contentAppearance: .grid,
                cardSizing: .small,
                isAddedToSelection: isAddedToSelection
            )
                .clipShape(RoundedRectangle(cornerRadius: 8))
            HStack {
                Text(bookmark.label)
                    .font(.title3)
                    .fontWeight(.medium)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                Spacer()
            }
            .padding(.horizontal)
            .padding(.top)
            if let desc = bookmark.bookmarkDescription, !desc.isEmpty {
                HStack {
                    Text(desc)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.top, 4)
                .padding(.bottom)
            }
        }
        .background {
            RoundedRectangle(cornerRadius: 12)
                .fill(.thinMaterial)
        }
    }
}

// MARK: - Image

private struct BookmarkItemImage: View {
    let bookmark: BookmarkModel
    let contentAppearance: ContentAppearance
    let cardSizing: CardImageSizing
    let isAddedToSelection: Bool

    var body: some View {
        Group {
            if let imageData = bookmark.imageDataHolder, let image = UIImage(data: imageData) {
                switch contentAppearance {
                case .list:
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 50, height: 50)
                case .card:
                    switch cardSizing {
                    case .big:
                        RoundedRectangle(cornerRadius: 8)
                            .frame(height: 150)
                            .overlay {
                                Image(uiImage: image)
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .clipped()
                            }
                    case .small:
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 50, height: 50)
                    }
                case .grid:
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.clear)
                        .frame(maxWidth: .infinity)
                        .frame(height: 128)
                        .overlay {
                            Image(uiImage: image)
                                .resizable()
                                .scaledToFill()
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                .clipped()
                        }
                }
            } else {
                placeholder
            }
        }
        .overlay {
            if isAddedToSelection {
                selectionOverlay
            }
        }
        .animation(.smooth, value: isAddedToSelection)
    }

    @ViewBuilder
    private var placeholder: some View {
        let folderTint = bookmark.getFolderColor()
        switch contentAppearance {
        case .list:
            RoundedRectangle(cornerRadius: 8)
                .fill(folderTint.opacity(0.3))
                .frame(width: 50, height: 50)
                .overlay {
                    YabaIconView(bundleKey: bookmark.kind.getIconName())
                        .scaledToFit()
                        .foregroundStyle(folderTint)
                        .frame(width: 32, height: 32)
                }
        case .card:
            switch cardSizing {
            case .big:
                RoundedRectangle(cornerRadius: 8)
                    .fill(folderTint.opacity(0.3))
                    .frame(height: 150)
                    .overlay {
                        YabaIconView(bundleKey: bookmark.kind.getIconName())
                            .scaledToFit()
                            .foregroundStyle(folderTint)
                            .frame(width: 96, height: 96)
                    }
            case .small:
                RoundedRectangle(cornerRadius: 8)
                    .fill(folderTint.opacity(0.3))
                    .frame(width: 50, height: 50)
                    .overlay {
                        YabaIconView(bundleKey: bookmark.kind.getIconName())
                            .scaledToFit()
                            .foregroundStyle(folderTint)
                            .frame(width: 32, height: 32)
                    }
            }
        case .grid:
            RoundedRectangle(cornerRadius: 8)
                .fill(folderTint.opacity(0.3))
                .frame(maxWidth: .infinity)
                .frame(height: 128)
                .overlay {
                    YabaIconView(bundleKey: bookmark.kind.getIconName())
                        .scaledToFit()
                        .foregroundStyle(folderTint)
                        .frame(width: 48, height: 48)
                }
        }
    }

    private var selectionOverlay: some View {
        let folderTint = bookmark.getFolderColor()
        return RoundedRectangle(cornerRadius: 8)
            .fill(folderTint.opacity(0.75))
            .overlay {
                YabaIconView(bundleKey: "checkmark-circle-02")
                    .foregroundStyle(.white)
                    .frame(width: 30, height: 30)
            }
    }
}

// MARK: - Tags (v2: `BookmarkModel.tags`)

private struct BookmarkTagsRow: View {
    let bookmark: BookmarkModel

    var body: some View {
        let tags = bookmark.tags
        if !tags.isEmpty {
            if tags.count < 6 {
                HStack {
                    HStack(spacing: 0) {
                        ForEach(tags, id: \.tagId) { tag in
                            Rectangle()
                                .fill(tag.color.getUIColor().opacity(0.3))
                                .frame(width: 34, height: 34)
                                .overlay {
                                    YabaIconView(bundleKey: tag.icon)
                                        .foregroundStyle(tag.color.getUIColor())
                                        .frame(width: 24, height: 24)
                                }
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    Spacer()
                }
            } else {
                HStack {
                    HStack(spacing: 0) {
                        ForEach(tags.prefix(5), id: \.tagId) { tag in
                            Rectangle()
                                .fill(tag.color.getUIColor().opacity(0.3))
                                .frame(width: 34, height: 34)
                                .overlay {
                                    YabaIconView(bundleKey: tag.icon)
                                        .foregroundStyle(tag.color.getUIColor())
                                        .frame(width: 24, height: 24)
                                }
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    Text("+\(tags.count - 5)")
                        .font(.caption)
                        .italic()
                    Spacer()
                }
            }
        } else {
            HStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 34, height: 34)
                    .overlay {
                        YabaIconView(bundleKey: "tags")
                            .foregroundStyle(.tint)
                            .frame(width: 24, height: 24)
                    }
                Text(LocalizedStringKey("Bookmark No Tags Added Title"))
                    .font(.caption)
                    .italic()
            }
        }
    }
}

// MARK: - Overflow

private struct BookmarkOverflowMenuButton: View {
    let bookmark: BookmarkModel

    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        #if targetEnvironment(macCatalyst)
        if itemState.isHovered {
            Menu {
                BookmarkOverflowMenuContent(bookmark: bookmark, itemState: $itemState)
            } label: {
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 34, height: 34)
                    .overlay {
                        YabaIconView(bundleKey: "more-horizontal-circle-02")
                            .foregroundStyle(.tint)
                            .frame(width: 24, height: 24)
                    }
            }
        }
        #else
        Menu {
            BookmarkOverflowMenuContent(bookmark: bookmark, itemState: $itemState)
        } label: {
            RoundedRectangle(cornerRadius: 8)
                .fill(.tint.opacity(0.3))
                .frame(width: 34, height: 34)
                .overlay {
                    YabaIconView(bundleKey: "more-horizontal-circle-02")
                        .foregroundStyle(.tint)
                        .frame(width: 24, height: 24)
                }
        }
        #endif
    }
}

private struct BookmarkOverflowMenuContent: View {
    let bookmark: BookmarkModel

    @Binding
    var itemState: BookmarkItemState

    private var pinActionLabelKey: String {
        bookmark.isPinned ? "Pinned Tag Label" : "Bookmark Creation Toggle Pinned Title"
    }

    var body: some View {
        Button {
            itemState.shouldShowEditSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text(LocalizedStringKey("Edit"))
            }
        }
        .tint(.orange)
        Button {
            itemState.shouldShowMoveSheet = true
        } label: {
            Label {
                Text(LocalizedStringKey("Move"))
            } icon: {
                YabaIconView(bundleKey: "arrow-move-up-right")
                    .scaledToFit()
            }
        }
        .tint(.teal)
        Button {
            AllBookmarksManager.queueToggleBookmarkPinned(bookmarkId: bookmark.bookmarkId)
        } label: {
            Label {
                Text(LocalizedStringKey(pinActionLabelKey))
            } icon: {
                YabaIconView(bundleKey: bookmark.isPinned ? "pin" : "pin-off")
                    .scaledToFit()
            }
        }
        .tint(.yellow)
        Button {
            itemState.shouldShowShareSheet = true
        } label: {
            Label {
                Text(LocalizedStringKey("Share"))
            } icon: {
                YabaIconView(bundleKey: "share-03")
                    .scaledToFit()
            }
        }
        .tint(.indigo)
        Divider()
        Button(role: .destructive) {
            itemState.shouldShowDeleteAlert = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "delete-02")
                Text(LocalizedStringKey("Delete"))
            }
        }
        .tint(.red)
    }
}

// MARK: - Sheets & alerts + navigation

private struct BookmarkSheetsAndAlertsModifier: ViewModifier {
    let bookmark: BookmarkModel

    @Binding
    var itemState: BookmarkItemState

    let onNavigationCallback: (BookmarkModel) -> Void

    func body(content: Content) -> some View {
        content
            .alert(
                LocalizedStringKey("Delete Bookmark Title"),
                isPresented: $itemState.shouldShowDeleteAlert
            ) {
                Button(role: .cancel) {
                    itemState.shouldShowDeleteAlert = false
                } label: {
                    Text("Cancel")
                }
                Button(role: .destructive) {
                    AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmark.bookmarkId])
                    itemState.shouldShowDeleteAlert = false
                } label: {
                    Text(LocalizedStringKey("Delete"))
                }
            } message: {
                Text("Delete Content Message \(bookmark.label)")
            }
            .sheet(isPresented: $itemState.shouldShowMoveSheet) {
                NavigationStack {
                    SelectFolderContent(
                        mode: .bookmarksMove,
                        contextFolderId: bookmark.folder?.folderId,
                        contextBookmarkIds: [bookmark.bookmarkId],
                        onPick: { targetFolderId in
                            guard let targetFolderId else { return }
                            AllBookmarksManager.queueMoveBookmarksToFolder(
                                bookmarkIds: [bookmark.bookmarkId],
                                targetFolderId: targetFolderId
                            )
                        }
                    )
                }
            }
            .sheet(isPresented: $itemState.shouldShowEditSheet) {
                BookmarkFlowSheet(context: .edit(bookmarkId: bookmark.bookmarkId))
            }
            .sheet(isPresented: $itemState.shouldShowShareSheet) {
                if let url = bookmarkItemShareURL(bookmark) {
                    ShareSheet(bookmarkLink: url)
                }
            }
            .onHover { hovered in
                itemState.isHovered = hovered
            }
            .onTapGesture {
                onNavigationCallback(bookmark)
            }
    }
}
