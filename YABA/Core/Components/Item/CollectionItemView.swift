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
        mainButton
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
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                swipeActionItems
            }
            .onHover { hovered in
                itemState.isHovered = hovered
            }
            .contextMenu {
                menuActionItems
            }
            .alert(
                LocalizedStringKey(
                    collection.collectionType == .folder
                    ? "Delete Folder Title"
                    : "Delete Tag Title"
                ),
                isPresented: $itemState.shouldShowDeleteDialog,
            ) {
                alertActionItems
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
    
    @ViewBuilder
    private var mainButton: some View {
        if isInSelectionMode {
           listMainLabel
        } else {
            if isInBookmarkDetail {
                Button {
                    withAnimation {
                        onNavigationCallback(collection)
                    }
                } label: {
                    listMainLabel
                }.buttonStyle(.plain)
            } else {
                listMainLabel
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation {
                            onNavigationCallback(collection)
                        }
                    }
            }
        }
    }
    
    @ViewBuilder
    private var mainLabel: some View {
        /**
         * TODO: CHANGE THIS WHEN GRIDS ARE RECYCLED
            if isInBookmarkDetail || isInSelectionMode {
                listMainLabel
            } else {
                switch contentAppearance {
                case .list: listMainLabel
                case .grid: gridMainLabel
                }
            }
         * Currently, it is only planned to have grid and list for collections as
         * information is so less to have an "expanded" state.
         */
    }
    
    @ViewBuilder
    private var listMainLabel: some View {
        HStack {
            HStack {
                YabaIconView(bundleKey: collection.icon)
                    .scaledToFit()
                    .foregroundStyle(collection.color.getUIColor())
                    .frame(width: 24, height: 24)
                Text(collection.label)
            }
            Spacer()
            HStack {
                if itemState.isHovered && !isInSelectionMode {
                    Menu {
                        menuActionItems
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
    }
    
    @ViewBuilder
    private var gridMainLabel: some View {
        VStack(spacing: 8) {
            HStack {
                YabaIconView(bundleKey: collection.icon)
                    .scaledToFit()
                    .foregroundStyle(collection.color.getUIColor())
                    .frame(width: 48, height: 48)
                Spacer()
                VStack {
                    #if targetEnvironment(macCatalyst)
                    if itemState.isHovered && !isInSelectionMode {
                        Menu {
                            menuActionItems
                        } label: {
                            YabaIconView(bundleKey: "more-horizontal-circle-02")
                                .scaledToFit()
                                .frame(width: 22, height: 22)
                                .foregroundStyle(.secondary)
                        }.tint(.secondary)
                    }
                    #else
                    Menu {
                        menuActionItems
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
    }
    
    @ViewBuilder
    private var menuActionItems: some View {
        if !isInSelectionMode {
            Button {
                itemState.shouldShowCreateBookmarkSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "bookmark-add-02")
                    Text("New")
                }
            }.tint(.mint)
        }
        Button {
            itemState.shouldShowEditSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text("Edit")
            }
        }.tint(.orange)
        Button(role: .destructive) {
            itemState.shouldShowDeleteDialog = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "delete-02")
                Text("Delete")
            }
        }.tint(.red)
    }
    
    @ViewBuilder
    private var swipeActionItems: some View {
        Button {
            itemState.shouldShowDeleteDialog = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "delete-02")
                Text("Delete")
            }
        }.tint(.red)
        Button {
            itemState.shouldShowEditSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text("Edit")
            }
        }.tint(.orange)
        if !isInSelectionMode {
            Button {
                itemState.shouldShowCreateBookmarkSheet = true
            } label: {
                VStack {
                    YabaIconView(bundleKey: "bookmark-add-02")
                    Text("New")
                }
            }.tint(.mint)
        }
    }
    
    @ViewBuilder
    private var alertActionItems: some View {
        Button(role: .cancel) {
            itemState.shouldShowDeleteDialog = false
        } label: {
            Text("Cancel")
        }
        Button(role: .destructive) {
            withAnimation {
                if collection.collectionType == .folder {
                    collection.bookmarks.forEach { bookmark in
                        modelContext.delete(bookmark)
                    }
                }
                modelContext.delete(collection)
                try? modelContext.save()
                onDeleteCallback(collection)
                itemState.shouldShowDeleteDialog = false
            }
        } label: {
            Text("Delete")
        }
    }
}
