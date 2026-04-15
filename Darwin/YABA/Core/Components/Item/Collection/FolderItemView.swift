//
//  FolderItemView.swift
//  YABA
//
//  Full folder row (parity with Compose `FolderItemView`). Sheets/alerts are stubbed — TODO wire call sites + flows.
//

import SwiftUI

struct FolderItemView: View {
    @Environment(\.appState)
    private var appState

    @State
    private var itemState = CollectionItemState()

    let folder: FolderModel
    let parentColors: [YabaColor]
    let hasChildren: Bool
    let isExpanded: Bool
    let isInSelectionMode: Bool
    let isInBookmarkDetail: Bool
    let onToggleExpanded: () -> Void
    let onNavigationCallback: (FolderModel) -> Void

    var body: some View {
        BaseCollectionItemView(parentColors: parentColors) {
            rowLabel
        }
        .listRowBackground(
            ItemListRowChrome.listRowBackground(
                cornerRadius: 8,
                isSelected: appState.selectedFolder?.folderId == folder.folderId,
                isHovered: itemState.isHovered
            )
        )
        .onHover { itemState.isHovered = $0 }
        .sheet(isPresented: $itemState.shouldShowEditSheet) {
            FolderCreationContent(existingFolderId: folder.folderId)
        }
        .sheet(isPresented: $itemState.shouldShowCreateBookmarkSheet) {
            // TODO: Create bookmark in folder
            EmptyView()
        }
        .alert(
            LocalizedStringKey("Delete Folder Title"),
            isPresented: $itemState.shouldShowDeleteDialog
        ) {
            Button(role: .cancel) {
                itemState.shouldShowDeleteDialog = false
            } label: {
                Text("Cancel")
            }
            Button(role: .destructive) {
                // TODO: Confirm delete via managers / queue
                itemState.shouldShowDeleteDialog = false
            } label: {
                Text("Delete")
            }
        } message: {
            Text("Delete Content Message \(folder.label)")
        }
    }

    private var rowLabel: some View {
        HStack {
            HStack {
                YabaIconView(bundleKey: folder.icon)
                    .scaledToFit()
                    .foregroundStyle(folder.color.getUIColor())
                    .frame(width: 24, height: 24)
                if folder.folderId == Constants.uncategorizedCollectionId {
                    Text(LocalizedStringKey(Constants.uncategorizedCollectionLabelKey))
                } else {
                    Text(folder.label)
                }
            }
            Spacer()
            HStack {
                /**
                CollectionItemOverflowMenu(isHovered: $itemState.isHovered) {
                    // TODO: Menu actions (new bookmark, edit, move, delete)
                    Button("TODO") { itemState.shouldShowCreateBookmarkSheet = true }
                    Button("TODO") { itemState.shouldShowEditSheet = true }
                }*/
                Text("\(folder.bookmarks.count)")
                    .foregroundStyle(.secondary)
                    .fontWeight(.medium)
            }
            .foregroundStyle(.secondary)
            if !isInSelectionMode && !isInBookmarkDetail && hasChildren {
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(folder.color.getUIColor())
                    .rotationEffect(isExpanded ? .degrees(90) : .zero)
                    .animation(.smooth, value: isExpanded)
                    .onTapGesture {
                        withAnimation {
                            onToggleExpanded()
                        }
                    }
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            appState.selectedFolder = folder
            appState.selectedTag = nil
            onNavigationCallback(folder)
        }
    }
}
