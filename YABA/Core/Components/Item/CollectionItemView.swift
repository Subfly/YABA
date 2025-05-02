//
//  CollectionItemView.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import Foundation
import SwiftUI

struct CollectionItemView: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var isHovered: Bool = false
    
    @State
    private var shouldShowDeleteDialog: Bool = false
    
    @State
    private var shouldShowEditSheet: Bool = false
    
    @State
    private var shouldShowMenuItems: Bool = false
    
    @State
    private var shouldShowCreateBookmarkSheet: Bool = false
    
    @State
    private var selectedCollectionToPerformActions: YabaCollection?
    
    let collection: YabaCollection
    
    @Binding
    var selectedCollection: YabaCollection?
    
    let isInSelectionMode: Bool
    /// MARK: IPAD ONLY
    let isInBookmarkDetail: Bool
    
    let onDeleteCallback: (YabaCollection) -> Void
    let onEditCallback: (YabaCollection) -> Void
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        mainButton
        #if targetEnvironment(macCatalyst)
            .listRowBackground(
                collection.id == selectedCollection?.id
                ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
                : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
            )
        #endif
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                swipeActionItems
            }
            .padding(.leading, isInSelectionMode ? 0 : 8)
            .onHover { hovered in
                isHovered = hovered
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
                isPresented: $shouldShowDeleteDialog,
            ) {
                alertActionItems
            } message: {
                if let selectedCollectionToPerformActions {
                    Text("Delete Content Message \(selectedCollectionToPerformActions.label)")
                }
            }
            .sheet(isPresented: $shouldShowEditSheet) {
                if let selectedCollectionToPerformActions {
                    CollectionCreationContent(
                        collectionType: selectedCollectionToPerformActions.collectionType,
                        collectionToEdit: $selectedCollectionToPerformActions,
                        onEditCallback: onEditCallback
                    )
                }
            }
            .sheet(isPresented: $shouldShowCreateBookmarkSheet) {
                BookmarkCreationContent(
                    bookmarkToEdit: .constant(nil),
                    initialCollection: $selectedCollectionToPerformActions,
                    link: nil,
                    onExitRequested: {}
                )
            }

    }
    
    @ViewBuilder
    private var mainButton: some View {
        if isInSelectionMode {
            mainLabel
        } else {
            if isInBookmarkDetail {
                Button {
                    withAnimation {
                        selectedCollection = collection
                        onNavigationCallback(collection)
                    }
                } label: {
                    mainLabel
                }.buttonStyle(.plain)
            } else {
                mainLabel
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation {
                            selectedCollection = collection
                            onNavigationCallback(collection)
                        }
                    }
            }
        }
    }
    
    @ViewBuilder
    private var mainLabel: some View {
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
                if isHovered && !isInSelectionMode {
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
                    .frame(width: 24, height: 24)
                    .foregroundStyle(.tertiary)
            }
        }
        .contentShape(Rectangle())
    }
    
    @ViewBuilder
    private var menuActionItems: some View {
        Button {
            selectedCollectionToPerformActions = collection
            shouldShowCreateBookmarkSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "bookmark-add-02")
                Text("New")
            }
        }.tint(.mint)
        Button {
            selectedCollectionToPerformActions = collection
            shouldShowEditSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text("Edit")
            }
        }.tint(.orange)
        Button(role: .destructive) {
            selectedCollectionToPerformActions = collection
            shouldShowDeleteDialog = true
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
            selectedCollectionToPerformActions = collection
            shouldShowDeleteDialog = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "delete-02")
                Text("Delete")
            }
        }.tint(.red)
        Button {
            selectedCollectionToPerformActions = collection
            shouldShowEditSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
                Text("Edit")
            }
        }.tint(.orange)
        Button {
            selectedCollectionToPerformActions = collection
            shouldShowCreateBookmarkSheet = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "bookmark-add-02")
                Text("New")
            }
        }.tint(.mint)
    }
    
    @ViewBuilder
    private var alertActionItems: some View {
        Button(role: .cancel) {
            selectedCollectionToPerformActions = nil
            shouldShowDeleteDialog = false
        } label: {
            Text("Cancel")
        }
        Button(role: .destructive) {
            withAnimation {
                if let collection = selectedCollectionToPerformActions {
                    if collection.collectionType == .folder {
                        collection.bookmarks.forEach { bookmark in
                            modelContext.delete(bookmark)
                        }
                    }
                    modelContext.delete(collection)
                    try? modelContext.save()
                    onDeleteCallback(collection)
                    selectedCollectionToPerformActions = nil
                    shouldShowDeleteDialog = false
                }
            }
        } label: {
            Text("Delete")
        }
    }
}
