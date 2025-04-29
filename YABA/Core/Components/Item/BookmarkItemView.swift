//
//  BookmarkItemView.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import SwiftUI

struct BookmarkItemView: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var selectedBookmarkToDelete: Bookmark?
    
    @State
    private var selectedBookmarkToEdit: Bookmark?
    
    @State
    private var shouldShowDeleteDialog: Bool = false
    
    @Binding
    var selectedBookmark: Bookmark?
    
    var bookmark: Bookmark
    
    let onNavigationCallback: (Bookmark) -> Void
    
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
            .sheet(item: $selectedBookmarkToEdit) { bookmark in
                BookmarkCreationContent(
                    bookmarkToEdit: Binding(
                        get: { bookmark },
                        set: { newValue in
                            self.selectedBookmarkToEdit = newValue
                        }
                    ),
                    initialCollection: .constant(nil),
                    link: nil,
                    onExitRequested: {}
                )
            }
    }
    
    @ViewBuilder
    private var mainButton: some View {
        #if os(iOS)
        if UIDevice.current.userInterfaceIdiom == .pad {
            NavigationLink(value: bookmark) {
                mainContent
            }.buttonStyle(.plain)
        } else {
            mainContent
                .contentShape(Rectangle())
                .onTapGesture {
                    withAnimation {
                        selectedBookmark = bookmark
                        onNavigationCallback(bookmark)
                    }
                }
        }
        #elseif os(macOS)
        Button {
            withAnimation {
                selectedBookmark = bookmark
            }
        } label: {
            mainContent
        }
        .buttonStyle(.borderless)
        .padding(8)
        .background {
            if selectedBookmark?.id == bookmark.id {
                RoundedRectangle(cornerRadius: 8)
                    .fill(.gray.opacity(0.2))
            }
        }
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
                if !bookmark.bookmarkDescription.isEmpty {
                    Text(bookmark.bookmarkDescription)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
            }
            #if os(iOS)
            if UIDevice.current.userInterfaceIdiom == .phone {
                Spacer()
                Image(systemName: "chevron.right")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 12, height: 12)
                    .foregroundStyle(.tertiary)
            }
            #elseif os(macOS)
            Spacer()
            #endif
        }
    }
    
    @ViewBuilder
    private var bookmarkImage: some View {
        if let imageData = bookmark.imageData,
           let image = UIImage(data: imageData) {
            #if os(iOS)
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 50, height: 50)
            #elseif os(macOS)
            Image(nsImage: image)
                .resizable()
                .aspectRatio(contentMode: .fill)
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
            selectedBookmarkToDelete = bookmark
            shouldShowDeleteDialog = true
        } label: {
            VStack {
                Image(systemName: "trash")
                Text("Delete")
            }
        }.tint(.red)
        Button {
            selectedBookmarkToEdit = bookmark
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
            selectedBookmarkToEdit = bookmark
        } label: {
            VStack {
                Image(systemName: "pencil")
                Text("Edit")
            }
        }.tint(.orange)
        Divider()
        Button(role: .destructive) {
            selectedBookmarkToDelete = bookmark
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
            selectedBookmarkToDelete = nil
            shouldShowDeleteDialog = false
        } label: {
            Text("Cancel")
        }
        Button(role: .destructive) {
            withAnimation {
                if let bookmark = selectedBookmarkToDelete {
                    modelContext.delete(bookmark)
                    try? modelContext.save()
                    selectedBookmarkToDelete = nil
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
        bookmark: .empty(),
        onNavigationCallback: { _ in }
    ).tint(.indigo)
}
