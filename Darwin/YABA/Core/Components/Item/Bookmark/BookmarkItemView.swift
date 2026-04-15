//
//  BookmarkItemView.swift
//  YABA
//
//  List / card / grid bookmark row. Uses v2 `BookmarkModel` (import as `YABACore.BookmarkModel`; `YabaBookmark` is a typealias).
//

import SwiftData
import SwiftUI
import UIKit

struct BookmarkItemView: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ContentAppearance = .list

    @AppStorage(Constants.preferredCardImageSizingKey)
    private var cardSizing: CardImageSizing = .small

    @Environment(\.appState)
    private var appState

    @State
    private var itemState = BookmarkItemState()

    /// V2 base bookmark model (`YabaBookmark` == `BookmarkModel` in YABACore).
    let bookmark: BookmarkModel
    let isInRecents: Bool
    let isSelected: Bool
    let isInSelectionMode: Bool
    let onNavigationCallback: (BookmarkModel) -> Void

    var body: some View {
        selectableContent
            .modifier(BookmarkListRowBackgroundModifier(
                isSelected: appState.selectedBookmark?.bookmarkId == bookmark.bookmarkId,
                isHovered: itemState.isHovered
            ))
    }

    @ViewBuilder
    private var selectableContent: some View {
        HStack(alignment: contentAppearance == .list ? .center : .top) {
            if isInSelectionMode {
                YabaIconView(
                    bundleKey: isSelected
                        ? "checkmark-circle-01"
                        : "circle"
                )
                .frame(width: 24, height: 24)
                .foregroundStyle(bookmark.getFolderColor())
            }
            content
        }
    }

    @ViewBuilder
    private var content: some View {
        if isInRecents {
            bookmarkInteractiveCore
                .padding()
                .background {
                    RoundedRectangle(cornerRadius: 24).fill(Color.gray.opacity(0.05))
                }
                .padding(.horizontal)
                .modifier(BookmarkContextMenuModifier(itemState: $itemState))
        } else {
            bookmarkInteractiveCore
                .modifier(BookmarkContextMenuModifier(itemState: $itemState))
        }
    }

    private var bookmarkInteractiveCore: some View {
        BookmarkItemBody(
            bookmark: bookmark,
            contentAppearance: contentAppearance,
            cardSizing: cardSizing,
            itemState: $itemState
        )
        .background {
            if contentAppearance == .grid {
                gridCellChrome
            }
        }
        .modifier(BookmarkSheetsAndAlertsModifier(
            bookmark: bookmark,
            itemState: $itemState,
            onNavigationCallback: onNavigationCallback
        ))
    }

    @ViewBuilder
    private var gridCellChrome: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(gridCellFill)
    }

    private var gridCellFill: Color {
        #if targetEnvironment(macCatalyst)
        Color.gray.opacity(
            appState.selectedBookmark?.persistentModelID == bookmark.persistentModelID
                ? 0.3
                : itemState.isHovered ? 0.2 : 0.1
        )
        #else
        Color(UIColor.secondarySystemFill)
        #endif
    }
}

// MARK: - List row background

private struct BookmarkListRowBackgroundModifier: ViewModifier {
    let isSelected: Bool
    let isHovered: Bool

    func body(content: Content) -> some View {
        content
            .listRowBackground(
                ItemListRowChrome.listRowBackground(
                    cornerRadius: 8,
                    isSelected: isSelected,
                    isHovered: isHovered
                )
            )
    }
}

// MARK: - Context menu

#if !KEYBOARD_EXTENSION
private struct BookmarkContextMenuModifier: ViewModifier {
    @Binding
    var itemState: BookmarkItemState

    func body(content: Content) -> some View {
        content.contextMenu {
            BookmarkOverflowMenuContent(itemState: $itemState)
        }
    }
}
#else
private struct BookmarkContextMenuModifier: ViewModifier {
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
            BookmarkItemListContent(bookmark: bookmark, itemState: $itemState)
        case .card:
            switch cardSizing {
            case .big:
                BookmarkItemCardBigContent(bookmark: bookmark, itemState: $itemState)
            case .small:
                BookmarkItemCardSmallContent(bookmark: bookmark, itemState: $itemState)
            }
        case .grid:
            BookmarkItemGridContent(bookmark: bookmark, itemState: $itemState)
        }
    }
}

// MARK: - List

private struct BookmarkItemListContent: View {
    let bookmark: BookmarkModel

    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        HStack(alignment: .center) {
            BookmarkItemImage(bookmark: bookmark, contentAppearance: .list, cardSizing: .small)
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
        #if !KEYBOARD_EXTENSION
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button {
                itemState.shouldShowDeleteAlert = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "delete-02")
                    Text("TODO")
                }
            }
            .tint(.red)
            Button {
                itemState.shouldShowEditSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "edit-02")
                    Text("TODO")
                }
            }
            .tint(.orange)
        }
        .swipeActions(edge: .leading, allowsFullSwipe: false) {
            Button {
                itemState.shouldShowShareSheet = true
            } label: {
                Label {
                    Text("TODO")
                } icon: {
                    YabaIconView(bundleKey: "share-03")
                        .scaledToFit()
                }
            }
            .tint(.indigo)
        }
        #endif
    }
}

// MARK: - Card

private struct BookmarkItemCardBigContent: View {
    let bookmark: BookmarkModel

    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        VStack(alignment: .leading) {
            BookmarkItemImage(bookmark: bookmark, contentAppearance: .card, cardSizing: .big)
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
                BookmarkOverflowMenuButton(itemState: $itemState)
            }
        }
    }
}

private struct BookmarkItemCardSmallContent: View {
    let bookmark: BookmarkModel

    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        VStack(alignment: .leading) {
            HStack(alignment: .center) {
                BookmarkItemImage(bookmark: bookmark, contentAppearance: .card, cardSizing: .small)
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
                    BookmarkOverflowMenuButton(itemState: $itemState)
                }
            }
        }
    }
}

// MARK: - Grid

private struct BookmarkItemGridContent: View {
    let bookmark: BookmarkModel

    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        VStack(spacing: 0) {
            BookmarkItemImage(bookmark: bookmark, contentAppearance: .grid, cardSizing: .small)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(alignment: .topTrailing) {
                    BookmarkOverflowMenuButton(itemState: $itemState)
                }
            HStack {
                Text(bookmark.label)
                    .font(.title3)
                    .fontWeight(.medium)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                Spacer()
            }
            .padding()
        }
    }
}

// MARK: - Image

private struct BookmarkItemImage: View {
    let bookmark: BookmarkModel
    let contentAppearance: ContentAppearance
    let cardSizing: CardImageSizing

    var body: some View {
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
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(height: 128)
                    .frame(maxWidth: .infinity)
                    .clipped()
            }
        } else {
            placeholder
        }
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
                .frame(height: 128)
                .overlay {
                    YabaIconView(bundleKey: bookmark.kind.getIconName())
                        .scaledToFit()
                        .foregroundStyle(folderTint)
                        .frame(width: 48, height: 48)
                }
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
    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        #if targetEnvironment(macCatalyst)
        if itemState.isHovered {
            Menu {
                BookmarkOverflowMenuContent(itemState: $itemState)
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
            BookmarkOverflowMenuContent(itemState: $itemState)
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
    @Binding
    var itemState: BookmarkItemState

    var body: some View {
        Button {
            itemState.shouldShowEditSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text("TODO")
            }
        }
        .tint(.orange)
        Button {
            itemState.shouldShowShareSheet = true
        } label: {
            Label {
                Text("TODO")
            } icon: {
                YabaIconView(bundleKey: "share-03")
                    .scaledToFit()
            }
        }
        .tint(.indigo)
        Button(role: .destructive) {
            itemState.shouldShowDeleteAlert = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "delete-02")
                Text("TODO")
            }
        }
        .tint(.red)
    }
}

// MARK: - Sheets & alerts + navigation

private struct BookmarkSheetsAndAlertsModifier: ViewModifier {
    @Environment(\.appState)
    private var appState

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
                    Text("TODO")
                }
                Button(role: .destructive) {
                    // TODO: Route deletion through queue / managers when parity wiring lands.
                    itemState.shouldShowDeleteAlert = false
                } label: {
                    Text("TODO")
                }
            } message: {
                Text("Delete Content Message \(bookmark.label)")
            }
            .sheet(isPresented: $itemState.shouldShowEditSheet) {
                // TODO: Hook `BookmarkCreationContent` / kind-specific editors when parity sheets are ready.
                EmptyView()
            }
            .sheet(isPresented: $itemState.shouldShowShareSheet) {
                // TODO: `ShareSheet` or share payload when wired.
                EmptyView()
            }
            .onHover { hovered in
                itemState.isHovered = hovered
            }
            .onTapGesture {
                appState.selectedBookmark = bookmark
                onNavigationCallback(bookmark)
            }
    }
}
