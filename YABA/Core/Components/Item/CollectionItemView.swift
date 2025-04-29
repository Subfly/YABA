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
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                swipeActionItems
            }
            #if os(macOS)
            .padding(.leading, isInSelectionMode ? 0 : 8)
            .onHover { hovered in
                isHovered = hovered
            }
            #endif
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
    }
    
    @ViewBuilder
    private var mainButton: some View {
        if isInSelectionMode {
            mainLabel
        } else {
            #if os(macOS)
            Button {
                withAnimation {
                    selectedCollection = collection
                }
            } label: {
                mainLabel
            }.buttonStyle(.plain)
            #elseif os(iOS)
            if UIDevice.current.userInterfaceIdiom == .pad {
                if isInBookmarkDetail {
                    Button {
                        withAnimation {
                            selectedCollection = collection
                        }
                    } label: {
                        mainLabel
                    }.buttonStyle(.plain)
                } else {
                    NavigationLink(value: collection) {
                        mainLabel
                    }.buttonStyle(.plain)
                }
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
            #endif
        }
    }
    
    @ViewBuilder
    private var mainLabel: some View {
        HStack {
            HStack {
                Image(systemName: collection.icon)
                    .foregroundStyle(collection.color.getUIColor())
                Text(collection.label)
            }
            Spacer()
            HStack {
                #if os(macOS)
                if isHovered && !isInSelectionMode {
                    Menu {
                        menuActionItems
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                    .menuStyle(.button)
                }
                #endif
                Text("\(collection.bookmarks.count)")
                    .foregroundStyle(.secondary)
                    .fontWeight(.medium)
            }.foregroundStyle(.secondary)
            #if os(iOS)
            if UIDevice.current.userInterfaceIdiom == .phone && !isInSelectionMode {
                Image(systemName: "chevron.right")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 12, height: 12)
                    .foregroundStyle(.tertiary)
            }
            #endif
        }
        .contentShape(Rectangle())
    }
    
    @ViewBuilder
    private var menuActionItems: some View {
        Button {
            selectedCollectionToPerformActions = collection
            shouldShowEditSheet = true
        } label: {
            VStack {
                Image(systemName: "pencil")
                Text("Edit")
            }
        }.tint(.orange)
        Divider()
        Button(role: .destructive) {
            selectedCollectionToPerformActions = collection
            shouldShowDeleteDialog = true
        } label: {
            VStack {
                Image(systemName: "trash")
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
                Image(systemName: "trash")
                Text("Delete")
            }
        }.tint(.red)
        Button {
            selectedCollectionToPerformActions = collection
            shouldShowEditSheet = true
        } label: {
            VStack {
                Image(systemName: "pencil")
                Text("Edit")
            }
        }.tint(.orange)
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
