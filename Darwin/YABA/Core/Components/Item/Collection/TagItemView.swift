//
//  TagItemView.swift
//  YABA
//
//  Full tag row (Compose parity). Sheets/alerts stubbed — TODO.
//

import SwiftUI

struct TagItemView: View {
    @Environment(\.appState)
    private var appState

    @State
    private var itemState = CollectionItemState()

    let tag: TagModel
    let isInSelectionMode: Bool
    let isInBookmarkDetail: Bool
    let onNavigationCallback: (TagModel) -> Void

    var body: some View {
        HStack {
            HStack {
                YabaIconView(bundleKey: tag.icon)
                    .scaledToFit()
                    .foregroundStyle(tag.color.getUIColor())
                    .frame(width: 24, height: 24)
                Text(tag.label)
            }
            Spacer()
            HStack {
                /**
                CollectionItemOverflowMenu(isHovered: $itemState.isHovered) {
                    Button("TODO") { itemState.shouldShowCreateBookmarkSheet = true }
                    Button("TODO") { itemState.shouldShowEditSheet = true }
                }*/
                Text("\(tag.bookmarks.count)")
                    .foregroundStyle(.secondary)
                    .fontWeight(.medium)
            }
            .foregroundStyle(.secondary)
        }
        .contentShape(Rectangle())
        .listRowBackground(
            ItemListRowChrome.listRowBackground(
                cornerRadius: 8,
                isSelected: appState.selectedTag?.tagId == tag.tagId,
                isHovered: itemState.isHovered
            )
        )
        .onHover { itemState.isHovered = $0 }
        .onTapGesture {
            appState.selectedTag = tag
            appState.selectedFolder = nil
            onNavigationCallback(tag)
        }
        .sheet(isPresented: $itemState.shouldShowEditSheet) {
            // TODO: Tag edit
            EmptyView()
        }
        .sheet(isPresented: $itemState.shouldShowCreateBookmarkSheet) {
            // TODO: Create bookmark with tag
            EmptyView()
        }
        .alert(
            LocalizedStringKey("Delete Tag Title"),
            isPresented: $itemState.shouldShowDeleteDialog
        ) {
            Button(role: .cancel) {
                itemState.shouldShowDeleteDialog = false
            } label: {
                Text("TODO")
            }
            Button(role: .destructive) {
                itemState.shouldShowDeleteDialog = false
            } label: {
                Text("TODO")
            }
        } message: {
            Text("Delete Content Message \(tag.label)")
        }
    }
}
