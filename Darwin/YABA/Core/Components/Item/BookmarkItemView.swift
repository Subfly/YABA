//
//  BookmarkItemView.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import SwiftUI
import SwiftData
import WidgetKit

@MainActor
@Observable
private class ItemState {
    var shouldShowEditDialog: Bool = false
    var shouldShowShareDialog: Bool = false
    var shouldShowDeleteDialog: Bool = false
    var shouldShowFolderSelectionSheet: Bool = false
    var isHovered: Bool = false
    var isTargeted: Bool = false
    var selectedFolderToMove: YabaCollection? = nil
    
    func handleFolderChangeRequest(
        for bookmark: YabaBookmark,
        with modelContext: ModelContext
    ) {
        guard let newFolder = selectedFolderToMove else { return }
        
        bookmark.collections?.removeAll { collection in
            collection.collectionType == .folder
        }
        
        bookmark.collections?.append(newFolder)
        
        try? modelContext.save()
    }
}

struct BookmarkItemView: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @Environment(\.appState)
    private var appState
    
    @Environment(\.moveManager)
    private var moveManager
    
    @State
    private var itemState: ItemState = .init()
    
    let bookmark: YabaBookmark
    let isInRecents: Bool
    let isSelected: Bool
    let isInSelectionMode: Bool
    let onNavigationCallback: (YabaBookmark) -> Void
    
    var body: some View {
        selectableContent
            .draggable(bookmark.mapToCodable()) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(bookmark.getFolderColor().opacity(0.5))
                    .frame(width: 54, height: 54)
                    .overlay {
                        BookmarkImage(bookmark: bookmark)
                    }
            }
            .onDrop(
                of: [.yabaCollection],
                isTargeted: $itemState.isTargeted
            ) { providers in
                guard let provider = providers.first else {
                    return false
                }
                
                let _ = provider.loadTransferable(type: YabaCodableCollection.self) { result in
                    switch result {
                    case .success(let item):
                        // Sometimes, just wtf...
                        Task { @MainActor in
                            moveManager.onMoveBookmark(
                                bookmarkID: bookmark.bookmarkId,
                                toCollectionID: item.collectionId
                            )
                        }
                    case .failure: return
                    }
                }
                
                return true
            }
    }
    
    @ViewBuilder
    private var selectableContent: some View {
        HStack(
            alignment: contentAppearance == .list ? .center : .top
        ) {
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
        // Recents view is only available in Mobile
        if isInRecents {
            BookmarkItemViewWrappable(
                itemState: $itemState,
                bookmark: bookmark,
                onNavigationCallback: onNavigationCallback
            )
            .padding()
            .background {
                RoundedRectangle(cornerRadius: 24).fill(Color.gray.opacity(0.05))
            }
            .padding(.horizontal)
            #if !KEYBOARD_EXTENSION
            .contextMenu {
                MenuActionItems(
                    shouldShowEditDialog: $itemState.shouldShowEditDialog,
                    shouldShowFolderSelectionSheet: $itemState.shouldShowFolderSelectionSheet,
                    shouldShowShareDialog: $itemState.shouldShowShareDialog,
                    shouldShowDeleteDialog: $itemState.shouldShowDeleteDialog
                )
            }
            #endif
        } else {
            BookmarkItemViewWrappable(
                itemState: $itemState,
                bookmark: bookmark,
                onNavigationCallback: onNavigationCallback
            )
            #if targetEnvironment(macCatalyst)
            .listRowBackground(
                appState.selectedBookmark?.id == bookmark.id
                ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
                : itemState.isHovered
                ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
                : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
            )
            #else
            .listRowBackground(
                UIDevice.current.userInterfaceIdiom == .phone
                ? nil
                : appState.selectedBookmark?.id == bookmark.id
                ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
                : itemState.isHovered
                ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
                : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
            )
            #endif
            #if !KEYBOARD_EXTENSION
            .contextMenu {
                MenuActionItems(
                    shouldShowEditDialog: $itemState.shouldShowEditDialog,
                    shouldShowFolderSelectionSheet: $itemState.shouldShowFolderSelectionSheet,
                    shouldShowShareDialog: $itemState.shouldShowShareDialog,
                    shouldShowDeleteDialog: $itemState.shouldShowDeleteDialog
                )
            }
            #endif
        }
    }
}

private struct BookmarkItemViewWrappable: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @Environment(\.appState)
    private var appState
    
    @Binding
    var itemState: ItemState
    let bookmark: YabaBookmark
    let onNavigationCallback: (YabaBookmark) -> Void
    
    var body: some View {
        MainLabel(bookmark: bookmark, state: $itemState)
            .alert(
                LocalizedStringKey("Delete Bookmark Title"),
                isPresented: $itemState.shouldShowDeleteDialog,
            ) {
                AlertActionItems(
                    state: $itemState,
                    onDeleteCallback: {
                        UNUserNotificationCenter.current().removePendingNotificationRequests(
                            withIdentifiers: [bookmark.bookmarkId]
                        )
                        
                        try? YabaDataLogger.shared.logBookmarkDelete(
                            id: bookmark.bookmarkId,
                            shouldSave: false
                        )
                        
                        modelContext.delete(bookmark)
                        try? modelContext.save()
                        
                        if appState.selectedBookmark?.id == bookmark.id {
                            appState.selectedBookmark = nil
                        }
                        
                        WidgetCenter.shared.reloadAllTimelines()
                    }
                )
            } message: {
                Text("Delete Content Message \(bookmark.label)")
            }
            .sheet(isPresented: $itemState.shouldShowEditDialog) {
                BookmarkCreationContent(
                    bookmarkToEdit: bookmark,
                    collectionToFill: nil,
                    link: nil,
                    onExitRequested: {}
                )
            }
            .sheet(isPresented: $itemState.shouldShowShareDialog) {
                if let link = URL(string: bookmark.link) {
                    ShareSheet(bookmarkLink: link)
                        .presentationDetents([.medium])
                        .presentationDragIndicator(.visible)
                }
            }
            .sheet(isPresented: $itemState.shouldShowFolderSelectionSheet) {
                NavigationView {
                    if let folder = bookmark.getParentFolder() {
                        SelectFolderContent(
                            selectedFolder: $itemState.selectedFolderToMove,
                            mode: .moveBookmarks(folder) {
                                itemState.shouldShowFolderSelectionSheet = false
                            }
                        ).onDisappear {
                            itemState.handleFolderChangeRequest(
                                for: bookmark,
                                with: modelContext
                            )
                        }
                    }
                }
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

private struct MainLabel: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @AppStorage(Constants.preferredCardImageSizingKey)
    private var cardSizing: CardViewTypeImageSizing = .small
    
    let bookmark: YabaBookmark
    
    @Binding
    var state: ItemState
    
    var body: some View {
        mainContent
            .contentShape(Rectangle())
            .id(bookmark.id)
    }
    
    @ViewBuilder
    private var mainContent: some View {
        switch contentAppearance {
        case .list: ListView(bookmark: bookmark, state: $state)
        case .card: switch cardSizing {
            case .big: BigCardView(bookmark: bookmark, state: $state)
            case .small: SmallCardView(bookmark: bookmark, state: $state)
            }
        }
    }
}

private struct ListView: View {
    let bookmark: YabaBookmark
    
    @Binding
    var state: ItemState
    
    var body: some View {
        HStack(alignment: .center) {
            BookmarkImage(bookmark: bookmark)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            VStack(alignment: .leading) {
                Text(bookmark.label)
                    .font(.title3)
                    .fontWeight(.medium)
                    .lineLimit(1)
                if !bookmark.bookmarkDescription.isEmpty {
                    Text(bookmark.bookmarkDescription)
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
            rightSwipeActionItems
        }
        .swipeActions(edge: .leading, allowsFullSwipe: false) {
            leftSwipeActionItems
        }
        #endif
    }
    
    @ViewBuilder
    private var leftSwipeActionItems: some View {
        Button {
            state.shouldShowShareDialog = true
        } label: {
            Label {
                Text("Share")
            } icon: {
                YabaIconView(bundleKey: "share-03")
                    .scaledToFit()
            }
        }.tint(.indigo)
        Button {
            state.shouldShowFolderSelectionSheet = true
        } label: {
            Label {
                Text("Move")
            } icon: {
                YabaIconView(bundleKey: "arrow-move-up-right")
                    .scaledToFit()
            }
        }.tint(.teal)
    }
    
    @ViewBuilder
    private var rightSwipeActionItems: some View {
        Button {
            state.shouldShowDeleteDialog = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "delete-02")
                Text("Delete")
            }
        }.tint(.red)
        Button {
            state.shouldShowEditDialog = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text("Edit")
            }
        }.tint(.orange)
    }
}

private struct BigCardView: View {
    let bookmark: YabaBookmark
    
    @Binding
    var state: ItemState
    
    var body: some View {
        VStack(alignment: .leading) {
            BookmarkImage(bookmark: bookmark)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            HStack {
                if let miniIconData = bookmark.iconDataHolder,
                   let miniIconImage = UIImage(data: miniIconData) {
                    Image(uiImage: miniIconImage)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                }
                Text(bookmark.label)
                    .font(.title2)
                    .fontWeight(.medium)
                    .lineLimit(2)
            }.padding(.vertical, 4)
            if !bookmark.bookmarkDescription.isEmpty {
                Text(bookmark.bookmarkDescription)
                    .lineLimit(4)
                    .multilineTextAlignment(.leading)
                    .padding(.bottom, 8)
            }
            HStack {
                TagsAreaView(bookmark: bookmark)
                Spacer()
                ClickableOptionsIcon(state: $state)
            }
        }
    }
}

private struct SmallCardView: View {
    let bookmark: YabaBookmark
    
    @Binding
    var state: ItemState
    
    var body: some View {
        VStack(alignment: .leading) {
            HStack(alignment: .center) {
                BookmarkImage(bookmark: bookmark)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                Text(bookmark.label)
                    .font(.title2)
                    .fontWeight(.medium)
                    .lineLimit(2)
                Spacer()
            }.padding(.vertical, 4)
            if !bookmark.bookmarkDescription.isEmpty {
                Text(bookmark.bookmarkDescription)
                    .lineLimit(4)
                    .multilineTextAlignment(.leading)
                    .padding(.bottom, 8)
            }
            HStack {
                TagsAreaView(bookmark: bookmark)
                Spacer()
                HStack {
                    if let miniIconData = bookmark.iconDataHolder,
                       let miniIconImage = UIImage(data: miniIconData) {
                        Image(uiImage: miniIconImage)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                    }
                    ClickableOptionsIcon(state: $state)
                }
            }
        }
    }
}

private struct GridView: View {
    @Environment(\.appState)
    private var appState
    
    let bookmark: YabaBookmark
    
    @Binding
    var state: ItemState
    
    var body: some View {
        VStack(spacing: 0) {
            BookmarkImage(bookmark: bookmark)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(alignment: .topTrailing) {
                    ClickableOptionsIcon(state: $state)
                }
            HStack {
                Text(bookmark.label)
                    .font(.title3)
                    .fontWeight(.medium)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                Spacer()
            }.padding()
        }
        .background {
            #if targetEnvironment(macCatalyst)
            RoundedRectangle(cornerRadius: 12)
                .fill(
                    .gray.opacity(
                        appState.selectedBookmark?.id == bookmark.id
                        ? 0.3
                        : state.isHovered ? 0.2 : 0.1
                    )
                )
            #else
            RoundedRectangle(cornerRadius: 12)
                .fill(.thinMaterial)
            #endif
        }
    }
}

private struct BookmarkImage: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @AppStorage(Constants.preferredCardImageSizingKey)
    private var cardSizing: CardViewTypeImageSizing = .small
    
    let bookmark: YabaBookmark
    
    var body: some View {
        if let imageData = bookmark.imageDataHolder,
           let image = UIImage(data: imageData) {
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
                /**
                 * TODO: OPEN WHEN LAZYVGRID IS RECYCABLE
                    case .grid:
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill()
                            .containerRelativeFrame(.horizontal) { size, _ in
                                return size * 0.5 - 20
                            }
                 */
            }
        } else {
            switch contentAppearance {
            case .list:
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 50, height: 50)
                    .overlay {
                        YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                            .scaledToFit()
                            .foregroundStyle(.tint)
                            .frame(width: 32, height: 32)
                    }
            case .card:
                switch cardSizing {
                case .big:
                    RoundedRectangle(cornerRadius: 8)
                        .fill(.tint.opacity(0.3))
                        .frame(height: 150)
                        .overlay {
                            YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                                .scaledToFit()
                                .foregroundStyle(.tint)
                                .frame(width: 96, height: 96)
                        }
                case .small:
                    RoundedRectangle(cornerRadius: 8)
                        .fill(.tint.opacity(0.3))
                        .frame(width: 50, height: 50)
                        .overlay {
                            YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                                .scaledToFit()
                                .foregroundStyle(.tint)
                                .frame(width: 32, height: 32)
                        }
                }
                /**
                 * TODO: OPEN WHEN LAZYVGRID IS RECYCABLE
                    case .grid:
                        RoundedRectangle(cornerRadius: 8)
                            .fill(.tint.opacity(0.3))
                            .overlay {
                                YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                                    .scaledToFit()
                                    .foregroundStyle(.tint)
                                    .frame(width: 92, height: 92)
                        }
                 */
            }
        }
    }
}

private struct TagsAreaView: View {
    let bookmark: YabaBookmark
    
    var body: some View {
        let tags = bookmark.collections?.filter({ $0.collectionType == .tag }) ?? []
                
        if !tags.isEmpty {
            if tags.count < 6 {
                HStack {
                    HStack(spacing: 0) {
                        ForEach(tags) { tag in
                            Rectangle()
                                .fill(tag.color.getUIColor().opacity(0.3))
                                .frame(width: 34, height: 34)
                                .overlay {
                                    YabaIconView(bundleKey: tag.icon)
                                        .foregroundStyle(tag.color.getUIColor())
                                        .frame(width: 24, height: 24)
                                }
                        }
                    }.clipShape(RoundedRectangle(cornerRadius: 8))
                    Spacer()
                }
            } else {
                HStack {
                    HStack(spacing: 0) {
                        ForEach(0..<5) { index in
                            let tag = tags[index]
                            Rectangle()
                                .fill(tag.color.getUIColor().opacity(0.3))
                                .frame(width: 34, height: 34)
                                .overlay {
                                    YabaIconView(bundleKey: tag.icon)
                                        .foregroundStyle(tag.color.getUIColor())
                                        .frame(width: 24, height: 24)
                                }
                        }
                    }.clipShape(RoundedRectangle(cornerRadius: 8))
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
                Text("Bookmark No Tags Added Title")
                    .font(.caption)
                    .italic()
            }
        }
    }
}

private struct ClickableOptionsIcon: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @Binding
    var state: ItemState
    
    var body: some View {
        #if targetEnvironment(macCatalyst)
        if state.isHovered {
            Menu {
                MenuActionItems(
                    shouldShowEditDialog: $state.shouldShowEditDialog,
                    shouldShowFolderSelectionSheet: $state.shouldShowFolderSelectionSheet,
                    shouldShowShareDialog: $state.shouldShowShareDialog,
                    shouldShowDeleteDialog: $state.shouldShowDeleteDialog
                )
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
            .padding([.top, .trailing], contentAppearance == .card ? 0 : 4)
        }
        #else
        Menu {
            MenuActionItems(
                shouldShowEditDialog: $state.shouldShowEditDialog,
                shouldShowFolderSelectionSheet: $state.shouldShowFolderSelectionSheet,
                shouldShowShareDialog: $state.shouldShowShareDialog,
                shouldShowDeleteDialog: $state.shouldShowDeleteDialog
            )
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
        .padding([.top, .trailing], contentAppearance == .card ? 0 : 4)
        #endif
    }
}

private struct MenuActionItems: View {
    @Binding
    var shouldShowEditDialog: Bool
    
    @Binding
    var shouldShowFolderSelectionSheet: Bool
    
    @Binding
    var shouldShowShareDialog: Bool
    
    @Binding
    var shouldShowDeleteDialog: Bool
    
    var body: some View {
        Button {
            shouldShowEditDialog = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text("Edit")
            }
        }.tint(.orange)
        Button {
            shouldShowFolderSelectionSheet = true
        } label: {
            Label {
                Text("Move")
            } icon: {
                YabaIconView(bundleKey: "arrow-move-up-right")
                    .scaledToFit()
            }
        }.tint(.teal)
        Button {
            shouldShowShareDialog = true
        } label: {
            Label {
                Text("Share")
            } icon: {
                YabaIconView(bundleKey: "share-03")
                    .scaledToFit()
            }
        }.tint(.indigo)
        Button(role: .destructive) {
            shouldShowDeleteDialog = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "delete-02")
                Text("Delete")
            }
        }.tint(.red)
    }
}

private struct AlertActionItems: View {
    @Binding
    var state: ItemState
    
    let onDeleteCallback: () -> Void
    
    var body: some View {
        Button(role: .cancel) {
            state.shouldShowDeleteDialog = false
        } label: {
            Text("Cancel")
        }
        Button(role: .destructive) {
            withAnimation {
                onDeleteCallback()
                state.shouldShowDeleteDialog = false
            }
        } label: {
            Text("Delete")
        }
    }
}
