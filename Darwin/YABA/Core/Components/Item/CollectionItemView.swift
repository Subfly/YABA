//
//  CollectionItemView.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import Foundation
import SwiftUI
import WidgetKit

@MainActor
@Observable
private class ItemState {
    var isHovered: Bool = false
    var shouldShowDeleteDialog: Bool = false
    var shouldShowEditSheet: Bool = false
    var shouldShowMenuItems: Bool = false
    var shouldShowCreateBookmarkSheet: Bool = false
    var shouldShowCreateFolderSheet: Bool = false
    var shouldShowMoveSheet: Bool = false
}

struct CollectionItemView: View {
    @Environment(\.appState)
    private var appState
    
    @State
    private var itemState: ItemState = .init()
    
    let collection: YabaCollection
    let inSelectionModeAndSelected: Bool
    let isInCreationMode: Bool
    let isInBookmarkDetail: Bool
    let onDeleteCallback: (YabaCollection) -> Void
    let onEditCallback: (YabaCollection) -> Void
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        if collection.collectionType == .folder {
            if collection.children.isEmpty {
                CollectionItemViewWrappable(
                    itemState: $itemState,
                    collection: collection,
                    inSelectionModeAndSelected: inSelectionModeAndSelected,
                    isCurrentFocus: appState.selectedCollection?.collectionId == collection.collectionId,
                    isInCreationMode: isInCreationMode,
                    isInBookmarkDetail: isInBookmarkDetail,
                    onDeleteCallback: onDeleteCallback,
                    onEditCallback: onEditCallback,
                    onNavigationCallback: { selected in
                        withAnimation {
                            if !inSelectionModeAndSelected {
                                appState.selectedCollection = selected
                            }
                            onNavigationCallback(collection)
                        }
                    },
                    onFoucDeleteCallback: {
                        appState.selectedCollection = nil
                    }
                )
            } else {
                DisclosureGroup {
                    ForEach(collection.children) { child in
                        CollectionItemView(
                            collection: child,
                            inSelectionModeAndSelected: inSelectionModeAndSelected,
                            isInCreationMode: isInCreationMode,
                            isInBookmarkDetail: isInBookmarkDetail,
                            onDeleteCallback: onDeleteCallback,
                            onEditCallback: onEditCallback,
                            onNavigationCallback: onNavigationCallback
                        )
                    }
                } label: {
                    CollectionItemViewWrappable(
                        itemState: $itemState,
                        collection: collection,
                        inSelectionModeAndSelected: inSelectionModeAndSelected,
                        isCurrentFocus: appState.selectedCollection?.collectionId == collection.collectionId,
                        isInCreationMode: isInCreationMode,
                        isInBookmarkDetail: isInBookmarkDetail,
                        onDeleteCallback: onDeleteCallback,
                        onEditCallback: onEditCallback,
                        onNavigationCallback: { selected in
                            withAnimation {
                                if !inSelectionModeAndSelected {
                                    appState.selectedCollection = selected
                                }
                                onNavigationCallback(collection)
                            }
                        },
                        onFoucDeleteCallback: {
                            appState.selectedCollection = nil
                        }
                    )
                }
                #if targetEnvironment(macCatalyst)
                .listRowBackground(
                    inSelectionModeAndSelected
                    ? RoundedRectangle(cornerRadius: 8).fill(collection.color.getUIColor().opacity(0.25))
                    : appState.selectedCollection?.id == collection.id
                    ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
                    : itemState.isHovered
                    ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
                    : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
                )
                #else
                .listRowBackground(
                    inSelectionModeAndSelected
                    ? RoundedRectangle(cornerRadius: 0).fill(collection.color.getUIColor().opacity(0.25))
                    : UIDevice.current.userInterfaceIdiom == .phone
                    ? nil
                    : appState.selectedCollection?.id == collection.id
                    ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
                    : itemState.isHovered
                    ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
                    : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
                )
                #endif
            }
        } else {
            CollectionItemViewWrappable(
                itemState: $itemState,
                collection: collection,
                inSelectionModeAndSelected: inSelectionModeAndSelected,
                isCurrentFocus: appState.selectedCollection?.collectionId == collection.collectionId,
                isInCreationMode: isInCreationMode,
                isInBookmarkDetail: isInBookmarkDetail,
                onDeleteCallback: onDeleteCallback,
                onEditCallback: onEditCallback,
                onNavigationCallback: { selected in
                    withAnimation {
                        if !inSelectionModeAndSelected {
                            appState.selectedCollection = selected
                        }
                        onNavigationCallback(collection)
                    }
                },
                onFoucDeleteCallback: {
                    appState.selectedCollection = nil
                }
            )
        }
    }
}

private struct CollectionItemViewWrappable: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @Binding
    var itemState: ItemState
    
    let collection: YabaCollection
    let inSelectionModeAndSelected: Bool
    let isCurrentFocus: Bool
    let isInCreationMode: Bool
    let isInBookmarkDetail: Bool
    let onDeleteCallback: (YabaCollection) -> Void
    let onEditCallback: (YabaCollection) -> Void
    let onNavigationCallback: (YabaCollection) -> Void
    let onFoucDeleteCallback: () -> Void
    
    var body: some View {
        MainLabel(
            collection: collection,
            state: $itemState,
            isInCreationMode: isInCreationMode,
            isInBookmarkDetail: isInBookmarkDetail,
            onNavigationCallback: onNavigationCallback,
        )
        #if targetEnvironment(macCatalyst)
        .listRowBackground(
            inSelectionModeAndSelected
            ? RoundedRectangle(cornerRadius: 8).fill(collection.color.getUIColor().opacity(0.25))
            : isCurrentFocus
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
            : itemState.isHovered
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
            : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
        )
        #else
        .listRowBackground(
            inSelectionModeAndSelected
            ? RoundedRectangle(cornerRadius: 0).fill(collection.color.getUIColor().opacity(0.25))
            : UIDevice.current.userInterfaceIdiom == .phone
            ? nil
            : isCurrentFocus
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
            : itemState.isHovered
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
            : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
        )
        #endif
        .onHover { hovered in
            itemState.isHovered = hovered
        }
        #if !KEYBOARD_EXTENSION
        .contextMenu {
            MenuActionItems(
                state: $itemState,
                isTag: collection.collectionType == .tag,
                isInCreationMode: isInCreationMode,
                isInBookmarkDetail: isInBookmarkDetail
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
                    if isCurrentFocus {
                        onFoucDeleteCallback()
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
                collectionToAdd: nil,
                collectionToEdit: collection,
                onEditCallback: onEditCallback
            )
        }
        .sheet(isPresented: $itemState.shouldShowCreateFolderSheet) {
            CollectionCreationContent(
                collectionType: .folder,
                collectionToAdd: nil,
                collectionToEdit: nil,
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
        .sheet(isPresented: $itemState.shouldShowMoveSheet) {
            NavigationView {
                SelectFolderContent(
                    mode: .moving,
                    folderInAction: collection,
                    selectedFolder: collection.parentCollection,
                    onSelectNewFolder: { newParent in
                        collection.parentCollection = newParent
                        try? modelContext.save()
                    },
                    onEditSelectedFolderDuringCreation: { _ in
                        // No need to do anything...
                    },
                    onDeleteSelectedFolderDuringCreation: {
                        // No need to do anything...
                    }
                )
            }
        }
        .id(collection.id)
    }
}

private struct MainLabel: View {
    let collection: YabaCollection
    
    @Binding
    var state: ItemState
    
    let isInCreationMode: Bool
    let isInBookmarkDetail: Bool
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        if isInCreationMode {
            ListView(
                collection: collection,
                state: $state,
                isInCreationMode: isInCreationMode,
                isInBookmarkDetail: isInBookmarkDetail
            ).onTapGesture {
                onNavigationCallback(collection)
            }
        } else {
            if isInBookmarkDetail {
                Button {
                    onNavigationCallback(collection)
                } label: {
                    ListView(
                        collection: collection,
                        state: $state,
                        isInCreationMode: isInCreationMode,
                        isInBookmarkDetail: isInBookmarkDetail
                    )
                }.buttonStyle(.plain)
            } else {
                ListView(
                    collection: collection,
                    state: $state,
                    isInCreationMode: isInCreationMode,
                    isInBookmarkDetail: isInBookmarkDetail
                ).onTapGesture {
                    onNavigationCallback(collection)
                }
            }
        }
    }
}

private struct ListView: View {
    let collection: YabaCollection
    
    @Binding
    var state: ItemState
    
    let isInCreationMode: Bool
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
                if state.isHovered && !isInCreationMode {
                    Menu {
                        MenuActionItems(
                            state: $state,
                            isTag: collection.collectionType == .tag,
                            isInCreationMode: isInCreationMode,
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
        }
        .contentShape(Rectangle())
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
    private var rightSwipeActionItems: some View {
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
        if !isInCreationMode && !isInBookmarkDetail && collection.collectionType != .tag {
            Button {
                state.shouldShowMoveSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "arrow-move-down-right")
                    Text("Move")
                }
            }.tint(.cyan)
        }
    }
    
    @ViewBuilder
    private var leftSwipeActionItems: some View {
        if !isInCreationMode {
            Button {
                state.shouldShowCreateBookmarkSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "bookmark-add-02")
                    Text("New Bookmark")
                }
            }.tint(.mint)
        }
        if !isInCreationMode && !isInBookmarkDetail && collection.collectionType != .tag {
            Button {
                state.shouldShowCreateFolderSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "folder-add")
                    Text("New Folder")
                }
            }.tint(.green)
        }
    }
}

private struct GridView: View {
    let collection: YabaCollection
    
    @Binding
    var state: ItemState
    
    let isInCreationMode: Bool
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
                    if state.isHovered && !isInCreationMode {
                        Menu {
                            MenuActionItems(
                                state: $state,
                                isTag: collection.collectionType == .tag,
                                isInCreationMode: isInCreationMode,
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
                            isTag: collection.collectionType == .tag,
                            isInCreationMode: isInCreationMode,
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
    }
}

private struct MenuActionItems: View {
    @Binding
    var state: ItemState
    
    let isTag: Bool
    let isInCreationMode: Bool
    let isInBookmarkDetail: Bool
    
    var body: some View {
        if !isInCreationMode {
            Button {
                state.shouldShowCreateBookmarkSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "bookmark-add-02")
                    Text("New Bookmark")
                }
            }.tint(.mint)
        }
        if !isInCreationMode && !isInBookmarkDetail && !isTag {
            Button {
                state.shouldShowCreateFolderSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "folder-add")
                    Text("New Folder")
                }
            }.tint(.green)
        }
        Divider()
        Button {
            state.shouldShowEditSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text("Edit")
            }
        }.tint(.orange)
        if !isInCreationMode && !isInBookmarkDetail && !isTag {
            Button {
                state.shouldShowMoveSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "arrow-move-down-right")
                    Text("Move")
                }
            }.tint(.cyan)
        }
        Divider()
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
