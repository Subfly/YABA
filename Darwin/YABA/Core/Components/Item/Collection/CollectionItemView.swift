//
//  CollectionItemView.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import Foundation
import SwiftUI
import SwiftData
import WidgetKit

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
    private var itemState: CollectionItemState = .init()

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
                        HomeCollectionItemView(
                            collection: child,
                            isInSelectionMode: isInSelectionMode,
                            isInBookmarkDetail: isInBookmarkDetail,
                            onDeleteCallback: onDeleteCallback,
                            onEditCallback: onEditCallback,
                            onNavigationCallback: onNavigationCallback
                        )
                    }
                }
            }
        } else {
            wrappable
        }
    }
    
    @ViewBuilder
    private var wrappable: some View {
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
                if !isInBookmarkDetail && appState.selectedCollection?.collectionId == collection.collectionId {
                    RoundedRectangle(cornerRadius: 16).fill(Color.gray.opacity(0.2))
                } else if !isInBookmarkDetail && itemState.isHovered {
                    RoundedRectangle(cornerRadius: 16).fill(Color.gray.opacity(0.1))
                } else {
                    RoundedRectangle(cornerRadius: 16).fill(Color.clear)
                }
                #else
                if UIDevice.current.userInterfaceIdiom == .phone {
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
    private var itemState: CollectionItemState = .init()
    
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
    
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ContentAppearance = .list
    
    @Binding
    var itemState: CollectionItemState
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
    var state: CollectionItemState
    
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
    var state: CollectionItemState
    
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
            rightActionItems
        }
        .swipeActions(edge: .leading, allowsFullSwipe: false) {
            leftActionItems
        }
#endif
    }
    
    @ViewBuilder
    private var rightActionItems: some View {
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
    }
    
    @ViewBuilder
    private var leftActionItems: some View {
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
    var state: CollectionItemState
    
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
    var state: CollectionItemState
    
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
            Divider()
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
    var state: CollectionItemState
    
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
