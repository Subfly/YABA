//
//  BookmarkItemView.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import SwiftUI
import Kingfisher

struct BookmarkItemView: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var selectedBookmarkToPerformActions: Bookmark?
    
    @State
    private var shouldShowDeleteDialog: Bool = false
    
    @State
    private var shouldShowEditSheet: Bool = false
    
    @Binding
    var selectedBookmark: Bookmark?
    
    var bookmark: Bookmark
    
    var body: some View {
        mainButton
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                swipeActionItems
            }
            .contextMenu {
                menuActionItems
            }
            .alert(
                LocalizedStringKey("Delete Bookmark Title"),
                isPresented: $shouldShowDeleteDialog,
            ) {
                alertActionItems
            } message: {
                Text("Delete Content Message \(bookmark.label)")
            }
            .sheet(isPresented: $shouldShowEditSheet) {
                if selectedBookmarkToPerformActions != nil {
                    BookmarkCreationContent(
                        bookmarkToEdit: $selectedBookmarkToPerformActions,
                        initialCollection: .constant(nil)
                    )
                }
            }
    }
    
    @ViewBuilder
    private var mainButton: some View {
        #if os(iOS)
        NavigationLink(value: bookmark) {
            mainContent
        }.buttonStyle(.plain)
        #elseif os(macOS)
        Button {
            selectedBookmark = bookmark
        } label: {
            mainContent
        }.buttonStyle(.borderless)
        #endif
    }
    
    @ViewBuilder
    private var mainContent: some View {
        HStack(alignment: .center) {
            bookmarkImage
                .clipShape(RoundedRectangle(cornerRadius: 8))
            VStack(alignment: .leading) {
                Text(bookmark.label)
                    .font(.title3)
                    .fontWeight(.medium)
                    .lineLimit(1)
                Text(bookmark.bookmarkDescription)
                    .lineLimit(2)
            }
        }
    }
    
    @ViewBuilder
    private var bookmarkImage: some View {
        if let imageData = bookmark.imageData,
           let image = UIImage(data: imageData) {
            #if os(iOS)
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 50, height: 50)
            #elseif os(macOS)
            Image(nsImage: image)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 50, height: 50)
            #endif
        } else {
            RoundedRectangle(cornerRadius: 8)
                .fill(.tint.opacity(0.3))
                .frame(width: 64, height: 64)
                .overlay {
                    Image(systemName: bookmark.bookmarkType.getIconName())
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .foregroundStyle(.tint)
                        .frame(width: 32, height: 32)
                }
        }
    }
    
    @ViewBuilder
    private var swipeActionItems: some View {
        Button {
            selectedBookmarkToPerformActions = bookmark
            shouldShowDeleteDialog = true
        } label: {
            VStack {
                Image(systemName: "trash")
                Text("Delete")
            }
        }.tint(.red)
        Button {
            selectedBookmarkToPerformActions = bookmark
            shouldShowEditSheet = true
        } label: {
            VStack {
                Image(systemName: "pencil")
                Text("Edit")
            }
        }.tint(.orange)
    }
    
    @ViewBuilder
    private var menuActionItems: some View {
        Button {
            selectedBookmarkToPerformActions = bookmark
            shouldShowEditSheet = true
        } label: {
            VStack {
                Image(systemName: "pencil")
                Text("Edit")
            }
        }.tint(.orange)
        Divider()
        Button(role: .destructive) {
            selectedBookmarkToPerformActions = bookmark
            shouldShowDeleteDialog = true
        } label: {
            VStack {
                Image(systemName: "trash")
                Text("Delete")
            }
        }.tint(.red)
    }
    
    @ViewBuilder
    private var alertActionItems: some View {
        Button(role: .cancel) {
            selectedBookmarkToPerformActions = nil
            shouldShowDeleteDialog = false
        } label: {
            Text("Cancel")
        }
        Button(role: .destructive) {
            withAnimation {
                if let bookmark = selectedBookmarkToPerformActions {
                    modelContext.delete(bookmark)
                    try? modelContext.save()
                    selectedBookmarkToPerformActions = nil
                    shouldShowDeleteDialog = false
                }
            }
        } label: {
            Text("Delete")
        }
    }
}

#Preview {
    BookmarkItemView(
        selectedBookmark: .constant(.empty()),
        bookmark: .empty()
    ).tint(.indigo)
}
