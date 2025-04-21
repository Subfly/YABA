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
    private var selectedCollectionToPerformActions: YabaCollection?
    
    let collection: YabaCollection
    
    @Binding
    var selectedCollection: YabaCollection?
    
    var body: some View {
        Button {
            selectedCollection = collection
        } label: {
            mainLabel
        }
        .buttonStyle(.plain)
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            swipeActionItems
        }
        #if os(macOS)
        .padding(.leading)
        .onHover { hovered in
            isHovered = hovered
        }
        .contextMenu {
            menuActionItems
        }
        #endif
        .alert(
            "Delete Folder Title",
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
                    collectionToEdit: $selectedCollectionToPerformActions
                )
            }
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
                if isHovered {
                    Menu {
                        menuActionItems
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
                #endif
                Text("\(collection.bookmarks.count)")
                #if os(iOS)
                Image(systemName: "chevron.right")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 12, height: 12)
                #endif
            }.foregroundStyle(.secondary)
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
        Button {
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
            Text(verbatim: "Cancel")
        }
        Button(role: .destructive) {
            withAnimation {
                if let folder = selectedCollectionToPerformActions {
                    folder.bookmarks.forEach { bookmark in
                        modelContext.delete(bookmark)
                    }
                    modelContext.delete(folder)
                    try? modelContext.save()
                    selectedCollectionToPerformActions = nil
                    shouldShowDeleteDialog = false
                }
            }
        } label: {
            Text(verbatim: "Delete")
        }
    }
}
