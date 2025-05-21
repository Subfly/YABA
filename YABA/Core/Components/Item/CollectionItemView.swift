//
//  CollectionItemView.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import Foundation
import SwiftUI

@MainActor
@Observable
private class ItemState {
    var isHovered: Bool = false
    var shouldShowDeleteDialog: Bool = false
    var shouldShowEditSheet: Bool = false
    var shouldShowMenuItems: Bool = false
    var shouldShowCreateBookmarkSheet: Bool = false
}

struct CollectionItemView: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @Environment(\.appState)
    private var appState
    
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @State
    private var itemState: ItemState = .init()
    
    let collection: YabaCollection
    let isInSelectionMode: Bool
    let isInBookmarkDetail: Bool
    let onDeleteCallback: (YabaCollection) -> Void
    let onEditCallback: (YabaCollection) -> Void
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        MainLabel(
            collection: collection,
            state: $itemState,
            isInSelectionMode: isInSelectionMode,
            isInBookmarkDetail: isInBookmarkDetail,
            onNavigationCallback: {
                withAnimation {
                    appState.selectedCollection = collection
                    onNavigationCallback(collection)
                }
            }
        )
        .padding(.leading, isInSelectionMode ? 0 : 8)
        #if targetEnvironment(macCatalyst)
        .listRowBackground(
            appState.selectedCollection?.id == collection.id
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
            : itemState.isHovered
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
            : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
        )
        #endif
        .onHover { hovered in
            itemState.isHovered = hovered
        }
        .contextMenu {
            MenuActionItems(
                state: $itemState,
                isInSelectionMode: isInSelectionMode
            )
        }
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
                    if collection.collectionType == .folder {
                        collection.bookmarks.forEach { bookmark in
                            modelContext.delete(bookmark)
                        }
                    }
                    modelContext.delete(collection)
                    try? modelContext.save()
                    if appState.selectedCollection?.id == collection.id {
                        appState.selectedCollection = nil
                    }
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

private struct MainLabel: View {
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
                isInSelectionMode: isInSelectionMode
            )
        } else {
            if isInBookmarkDetail {
                Button {
                    onNavigationCallback()
                } label: {
                    ListView(
                        collection: collection,
                        state: $state,
                        isInSelectionMode: isInSelectionMode
                    )
                }.buttonStyle(.plain)
            } else {
                ListView(
                    collection: collection,
                    state: $state,
                    isInSelectionMode: isInSelectionMode
                )
                .onTapGesture {
                    onNavigationCallback()
                }
            }
        }
    }
}

private struct ListView: View {
    let collection: YabaCollection
    
    @Binding
    var state: ItemState
    
    let isInSelectionMode: Bool
    
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
                            isInSelectionMode: isInSelectionMode
                        )
                    } label: {
                        YabaIconView(bundleKey: "more-horizontal-circle-02")
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                    }
                }
                Text("\(collection.bookmarks.count)")
                    .foregroundStyle(.secondary)
                    .fontWeight(.medium)
            }.foregroundStyle(.secondary)
            if UIDevice.current.userInterfaceIdiom == .phone && !isInSelectionMode {
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tertiary)
            }
        }
        .contentShape(Rectangle())
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            swipeActionItems
        }
    }
    
    @ViewBuilder
    private var swipeActionItems: some View {
        Button {
            state.shouldShowDeleteDialog = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "delete-02")
                Text("Delete")
            }
        }.tint(.red)
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
                                isInSelectionMode: isInSelectionMode
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
                            isInSelectionMode: isInSelectionMode
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

private struct MenuActionItems: View {
    @Binding
    var state: ItemState
    
    let isInSelectionMode: Bool
    
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
