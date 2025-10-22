//
//  CollectionItemView.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import Foundation
import SwiftUI
import WidgetKit
import UniformTypeIdentifiers

@MainActor
@Observable
private class ItemState {
    var isHovered: Bool = false
    var isTargeted: Bool = false
    var isExpanded: Bool = false
    var shouldShowDeleteDialog: Bool = false
    var shouldShowEditSheet: Bool = false
    var shouldShowMenuItems: Bool = false
    var shouldShowCreateBookmarkSheet: Bool = false
    
    func onDropTakeAction(
        providers: [NSItemProvider],
        onMoveFolder: @escaping (String) -> Void,
        onMoveBookmark: @escaping (String) -> Void
    ) -> Bool {
        guard let provider = providers.first else {
            return false
        }
        
        // Try YabaCollection first
        let _ = provider.loadTransferable(type: YabaCodableCollection.self) { result in
            switch result {
            case .success(let item):
                Task { @MainActor in
                    onMoveFolder(item.collectionId)
                }
            case .failure:
                // If not a collection, try bookmark
                Task { @MainActor in
                    self.unwrapBookmarkAndTakeAction(
                        provider: provider,
                        onMoveBookmark: onMoveBookmark
                    )
                }
            }
        }
        
        return true
    }
    
    private func unwrapBookmarkAndTakeAction(
        provider: NSItemProvider,
        onMoveBookmark: @escaping (String) -> Void
    ) {
        let _ = provider.loadTransferable(type: YabaCodableBookmark.self) { result in
            switch result {
            case .success(let bookmark):
                Task { @MainActor in
                    if let bookmarkId = bookmark.bookmarkId {
                        onMoveBookmark(bookmarkId)
                    }
                }
            case .failure:
                break
            }
        }
    }
}

/**
 * COLLECTION ITEM VIEW THAT SHOULD BE
 * USED ONLY IN HOME VIEW TO SUPPORT
 * CORRECT DRAG AND DROP FUNCTIONALITY
 * IN HOME VIEW
 */
struct HomeCollectionItemView: View {
    @AppStorage(Constants.preferredCollectionSortingKey)
    private var preferredSortingForFolders: SortType = .createdAt
    
    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrderForFolders: SortOrderType = .ascending
    
    @Environment(\.appState)
    private var appState
    
    @State
    private var itemState: ItemState = .init()
    
    let collection: YabaCollection
    let isInSelectionMode: Bool
    let isInBookmarkDetail: Bool
    let onDeleteCallback: (YabaCollection) -> Void
    let onEditCallback: (YabaCollection) -> Void
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        if collection.collectionType == .folder {
            if collection.children.isEmpty {
                wrappable
            } else {
                wrappable
                if itemState.isExpanded {
                    let sortDescriptor: SortDescriptor<YabaCollection> = switch preferredSortingForFolders {
                    case .createdAt:
                            .init(\.createdAt, order: preferredSortOrderForFolders == .ascending ? .forward : .reverse)
                    case .editedAt:
                            .init(\.editedAt, order: preferredSortOrderForFolders == .ascending ? .forward : .reverse)
                    case .label:
                            .init(\.label, order: preferredSortOrderForFolders == .ascending ? .forward : .reverse)
                    case .custom:
                            .init(\.order, order: .forward)
                    }
                    
                    let children = collection.children.sorted(using: sortDescriptor)
                    
                    ForEach(children) { child in
                        if child.collectionId == collection.children.first?.collectionId {
                            SeparatorItemView()
                        }
                        HomeCollectionItemView(
                            collection: child,
                            isInSelectionMode: isInSelectionMode,
                            isInBookmarkDetail: isInBookmarkDetail,
                            onDeleteCallback: onDeleteCallback,
                            onEditCallback: onEditCallback,
                            onNavigationCallback: onNavigationCallback
                        )
                        if child.collectionId != collection.children.last?.collectionId {
                            SeparatorItemView()
                        }
                    }
                }
            }
        } else {
            wrappable
        }
    }
    
    @ViewBuilder
    private var wrappable: some View {
        SeparatorItemView()
        HStack {
            let parentColors = collection.getParentColorsInOrder()
            ForEach(parentColors.indices, id: \.self) { index in
                let color = parentColors[index]
                color.getUIColor()
                    .frame(width: 4, height: 20)
                    .clipShape(RoundedRectangle(cornerRadius: 1))
            }
            InteractableView(
                itemState: $itemState,
                collection: collection,
                isInSelectionMode: isInSelectionMode,
                isInBookmarkDetail: isInBookmarkDetail,
                onDeleteCallback: onDeleteCallback,
                onEditCallback: onEditCallback,
                onNavigationCallback: onNavigationCallback
            )
            .padding()
            .background {
                #if targetEnvironment(macCatalyst)
                if itemState.isTargeted {
                    RoundedRectangle(cornerRadius: 16).fill(collection.color.getUIColor().opacity(0.2))
                } else if !isInBookmarkDetail && appState.selectedCollection?.collectionId == collection.collectionId {
                    RoundedRectangle(cornerRadius: 16).fill(Color.gray.opacity(0.2))
                } else if !isInBookmarkDetail && itemState.isHovered {
                    RoundedRectangle(cornerRadius: 16).fill(Color.gray.opacity(0.1))
                } else {
                    RoundedRectangle(cornerRadius: 16).fill(Color.clear)
                }
                #else
                if itemState.isTargeted {
                    RoundedRectangle(cornerRadius: 24).fill(collection.color.getUIColor().opacity(0.1))
                } else if UIDevice.current.userInterfaceIdiom == .phone {
                    RoundedRectangle(cornerRadius: 24).fill(Color.gray.opacity(0.05))
                } else if appState.selectedCollection?.collectionId == collection.collectionId {
                    RoundedRectangle(cornerRadius: 24).fill(Color.gray.opacity(0.2))
                } else if itemState.isHovered {
                    RoundedRectangle(cornerRadius: 24).fill(Color.gray.opacity(0.1))
                } else {
                    RoundedRectangle(cornerRadius: 24).fill(Color.clear)
                }
                #endif
            }
        }
        .padding(.horizontal)
        SeparatorItemView()
    }
}

/**
 * COLLECTION ITEM VIEW THAT SHOULD BE
 * USED IN VIEWS THAT USES LISTS
 */
struct ListCollectionItemView: View {
    @Environment(\.appState)
    private var appState
    
    @State
    private var itemState: ItemState = .init()
    
    let collection: YabaCollection
    let isInSelectionMode: Bool
    let isInBookmarkDetail: Bool
    let onDeleteCallback: (YabaCollection) -> Void
    let onEditCallback: (YabaCollection) -> Void
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        InteractableView(
            itemState: $itemState,
            collection: collection,
            isInSelectionMode: isInSelectionMode,
            isInBookmarkDetail: isInBookmarkDetail,
            onDeleteCallback: onDeleteCallback,
            onEditCallback: onEditCallback,
            onNavigationCallback: onNavigationCallback
        )
        #if targetEnvironment(macCatalyst)
        .listRowBackground(
            appState.selectedCollection?.id == collection.id
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
            : itemState.isHovered
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
            : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
        )
        #else
        .listRowBackground(
            UIDevice.current.userInterfaceIdiom == .phone
            ? nil
            : appState.selectedCollection?.id == collection.id
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
            : itemState.isHovered
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
            : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
        )
        #endif
    }
}

/**
 * ITEM VIEW THAT ADDS ALERTS, DIALOGS, SWIPES AND
 * DRAG FUNCTIONALITIES TO CONDITIONAL VIEW
 */
private struct InteractableView: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @Environment(\.appState)
    private var appState
    
    @Environment(\.moveManager)
    private var moveManager
    
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @Binding
    var itemState: ItemState
    let collection: YabaCollection
    let isInSelectionMode: Bool
    let isInBookmarkDetail: Bool
    let onDeleteCallback: (YabaCollection) -> Void
    let onEditCallback: (YabaCollection) -> Void
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        ConditionalView(
            collection: collection,
            state: $itemState,
            isInSelectionMode: isInSelectionMode,
            isInBookmarkDetail: isInBookmarkDetail,
            onNavigationCallback: {
                appState.selectedCollection = collection
                onNavigationCallback(collection)
            }
        )
        .padding(.leading, isInSelectionMode ? 0 : 8)
        .onHover { hovered in
            itemState.isHovered = hovered
        }
#if !KEYBOARD_EXTENSION
        .contextMenu {
            MenuActionItems(
                state: $itemState,
                isInSelectionMode: isInSelectionMode,
                isInBookmarkDetail: isInBookmarkDetail
            )
        }
        .draggable(collection.mapToCodable()) {
            RoundedRectangle(cornerRadius: 12)
                .fill(collection.color.getUIColor().opacity(0.5))
                .frame(width: 54, height: 54)
                .overlay {
                    YabaIconView(bundleKey: collection.icon)
                        .frame(width: 32, height: 32)
                        .foregroundStyle(collection.color.getUIColor())
                }
        }
        .onDrop(
            of: [.yabaCollection, .yabaBookmark],
            isTargeted: $itemState.isTargeted
        ) { providers in
            itemState.onDropTakeAction(
                providers: providers,
                onMoveFolder: { fromItemId in
                    moveManager.onMoveFolder(
                        from: fromItemId,
                        to: collection.collectionId
                    )
                },
                onMoveBookmark: { bookmarkId in
                    moveManager.onMoveBookmark(
                        bookmarkID: bookmarkId,
                        toCollectionID: collection.collectionId
                    )
                }
            )
        }
#endif
        .alert(
            LocalizedStringKey(
                collection.collectionType == .folder
                ? "Delete Folder Title"
                : "Delete Tag Title"
            ),
            isPresented: $itemState.shouldShowDeleteDialog,
        ) {
            AlertActionItems(
                state: $itemState,
                onDeleteCallback: {
                    let bookmarkIds = collection.bookmarks?.map { $0.bookmarkId } ?? []
                    UNUserNotificationCenter.current().removePendingNotificationRequests(
                        withIdentifiers: bookmarkIds
                    )
                    
                    try? YabaDataLogger.shared.logCollectionDelete(
                        id: collection.collectionId,
                        shouldSave: false
                    )
                    
                    if collection.collectionType == .folder {
                        collection.bookmarks?.forEach { bookmark in
                            modelContext.delete(bookmark)
                            
                            try? YabaDataLogger.shared.logBookmarkDelete(
                                id: bookmark.bookmarkId,
                                shouldSave: false
                            )
                        }
                    }
                    modelContext.delete(collection)
                    
                    try? modelContext.save()
                    if appState.selectedCollection?.id == collection.id {
                        appState.selectedCollection = nil
                    }
                    
                    WidgetCenter.shared.reloadAllTimelines()
                    
                    onDeleteCallback(collection)
                }
            )
        } message: {
            Text("Delete Content Message \(collection.label)")
        }
        .sheet(isPresented: $itemState.shouldShowEditSheet) {
            CollectionCreationContent(
                collectionType: collection.collectionType,
                collectionToEdit: collection,
                onEditCallback: onEditCallback
            )
        }
        .sheet(isPresented: $itemState.shouldShowCreateBookmarkSheet) {
            BookmarkCreationContent(
                bookmarkToEdit: nil,
                collectionToFill: collection,
                link: nil,
                onExitRequested: {}
            )
        }
        .sensoryFeedback(
            .impact(weight: .heavy, intensity: 1),
            trigger: itemState.isTargeted
        )
        .id(collection.id)
    }
}

/**
 * ITEM VIEW THAT DECIDE WHICH VIEW SHOULD BE SHOWN
 * BASED ON THE GIVEN VIEW CONDITIONS
 */
private struct ConditionalView: View {
    let collection: YabaCollection
    
    @Binding
    var state: ItemState
    
    let isInSelectionMode: Bool
    let isInBookmarkDetail: Bool
    let onNavigationCallback: () -> Void
    
    var body: some View {
        if isInSelectionMode {
            ListView(
                collection: collection,
                state: $state,
                isInSelectionMode: isInSelectionMode,
                isInBookmarkDetail: isInBookmarkDetail
            )
        } else {
            if isInBookmarkDetail {
                Button {
                    onNavigationCallback()
                } label: {
                    ListView(
                        collection: collection,
                        state: $state,
                        isInSelectionMode: isInSelectionMode,
                        isInBookmarkDetail: isInBookmarkDetail
                    )
                }.buttonStyle(.plain)
            } else {
                ListView(
                    collection: collection,
                    state: $state,
                    isInSelectionMode: isInSelectionMode,
                    isInBookmarkDetail: isInBookmarkDetail
                )
                .onTapGesture {
                    onNavigationCallback()
                }
            }
        }
    }
}

/// MARK: BASE ITEMS
private struct ListView: View {
    let collection: YabaCollection
    
    @Binding
    var state: ItemState
    
    let isInSelectionMode: Bool
    let isInBookmarkDetail: Bool
    
    var body: some View {
        HStack {
            HStack {
                YabaIconView(bundleKey: collection.icon)
                    .scaledToFit()
                    .foregroundStyle(collection.color.getUIColor())
                    .frame(width: 24, height: 24)
                if collection.collectionId == Constants.uncategorizedCollectionId {
                    Text(LocalizedStringKey(Constants.uncategorizedCollectionLabelKey))
                } else {
                    Text(collection.label)
                }
            }
            Spacer()
            HStack {
                if state.isHovered && !isInSelectionMode {
                    Menu {
                        MenuActionItems(
                            state: $state,
                            isInSelectionMode: isInSelectionMode,
                            isInBookmarkDetail: isInBookmarkDetail
                        )
                    } label: {
                        YabaIconView(bundleKey: "more-horizontal-circle-02")
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                    }
                }
                Text("\(collection.bookmarks?.count ?? 0)")
                    .foregroundStyle(.secondary)
                    .fontWeight(.medium)
            }.foregroundStyle(.secondary)
            if !isInSelectionMode && !isInBookmarkDetail && !collection.children.isEmpty {
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(collection.color.getUIColor())
                    .rotationEffect(state.isExpanded ? .init(degrees: 90) : .zero)
                    .animation(.smooth, value: state.isExpanded)
                    .onTapGesture {
                        withAnimation {
                            state.isExpanded.toggle()
                        }
                    }
            }
        }
        .contentShape(Rectangle())
#if !KEYBOARD_EXTENSION
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            swipeActionItems
        }
#endif
    }
    
    @ViewBuilder
    private var swipeActionItems: some View {
        if !isInBookmarkDetail {
            Button {
                state.shouldShowDeleteDialog = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "delete-02")
                    Text("Delete")
                }
            }.tint(.red)
        }
        Button {
            state.shouldShowEditSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text("Edit")
            }
        }.tint(.orange)
        if !isInSelectionMode {
            Button {
                state.shouldShowCreateBookmarkSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "bookmark-add-02")
                    Text("New")
                }
            }.tint(.mint)
        }
    }
}

private struct GridView: View {
    let collection: YabaCollection
    
    @Binding
    var state: ItemState
    
    let isInSelectionMode: Bool
    let isInBookmarkDetail: Bool
    
    var body: some View {
        VStack(spacing: 8) {
            HStack {
                YabaIconView(bundleKey: collection.icon)
                    .scaledToFit()
                    .foregroundStyle(collection.color.getUIColor())
                    .frame(width: 48, height: 48)
                Spacer()
                VStack {
#if targetEnvironment(macCatalyst)
                    if state.isHovered && !isInSelectionMode {
                        Menu {
                            MenuActionItems(
                                state: $state,
                                isInSelectionMode: isInSelectionMode,
                                isInBookmarkDetail: isInBookmarkDetail
                            )
                        } label: {
                            YabaIconView(bundleKey: "more-horizontal-circle-02")
                                .scaledToFit()
                                .frame(width: 22, height: 22)
                                .foregroundStyle(.secondary)
                        }.tint(.secondary)
                    }
#else
                    Menu {
                        MenuActionItems(
                            state: $state,
                            isInSelectionMode: isInSelectionMode,
                            isInBookmarkDetail: isInBookmarkDetail
                        )
                    } label: {
                        YabaIconView(bundleKey: "more-horizontal-circle-02")
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.secondary)
                    }.tint(.secondary)
#endif
                    Spacer()
                }
            }
            HStack {
                Text(collection.label)
                    .font(.title3)
                    .fontWeight(.medium)
                    .lineLimit(1)
                Spacer()
            }
        }
        /**
         .padding()
         .background {
         #if targetEnvironment(macCatalyst)
         RoundedRectangle(cornerRadius: 12)
         .fill(
         .gray.opacity(
         appState.selectedCollection?.id == collection.id
         ? 0.3
         : itemState.isHovered ? 0.2 : 0.1
         )
         )
         #else
         RoundedRectangle(cornerRadius: 12)
         .fill(.thickMaterial)
         #endif
         }
         .contentShape(Rectangle())
         */
    }
}


/// MARK: HELPER ITEMS
private struct MenuActionItems: View {
    @Binding
    var state: ItemState
    
    let isInSelectionMode: Bool
    let isInBookmarkDetail: Bool
    
    var body: some View {
        if !isInSelectionMode {
            Button {
                state.shouldShowCreateBookmarkSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "bookmark-add-02")
                    Text("New")
                }
            }.tint(.mint)
        }
        Button {
            state.shouldShowEditSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text("Edit")
            }
        }.tint(.orange)
        if !isInBookmarkDetail {
            Button(role: .destructive) {
                state.shouldShowDeleteDialog = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "delete-02")
                    Text("Delete")
                }
            }.tint(.red)
        }
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
