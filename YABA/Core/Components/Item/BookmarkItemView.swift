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
    
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @State
    private var selectedBookmarkToDelete: Bookmark?
    
    @State
    private var selectedBookmarkToEdit: Bookmark?
    
    @State
    private var shouldShowDeleteDialog: Bool = false
    
    @State
    private var isHovered: Bool = false
    
    @Binding
    var selectedBookmark: Bookmark?
    
    var bookmark: Bookmark
    
    let isSearching: Bool
    
    let onNavigationCallback: (Bookmark) -> Void
    
    var body: some View {
        mainButton
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                swipeActionItems
            }
            .contextMenu {
                menuActionItems
            }
            .onHover { hovered in
                self.isHovered = hovered
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
        mainContent
            .contentShape(Rectangle())
            .onTapGesture {
                withAnimation {
                    selectedBookmark = bookmark
                    onNavigationCallback(bookmark)
                }
            }
    }
    
    @ViewBuilder
    private var mainContent: some View {
        switch contentAppearance {
        case .list:
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
                if UIDevice.current.userInterfaceIdiom == .phone {
                    Spacer()
                    YabaIconView(bundleKey: "arrow-right-01")
                        .scaledToFit()
                        .frame(width: 12, height: 12)
                        .foregroundStyle(.tertiary)
                }
            }
            #if targetEnvironment(macCatalyst)
            .listRowSpacing(isSearching ? 16 : 8)
            .listRowBackground(
                bookmark.id == selectedBookmark?.id
                ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
                : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
            )
            #endif
        case .grid:
            VStack(spacing: 0) {
                bookmarkImage
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(alignment: .topTrailing) {
                        #if targetEnvironment(macCatalyst)
                        if isHovered {
                            Menu {
                                menuActionItems
                            } label: {
                                YabaIconView(bundleKey: "more-horizontal-circle-02")
                                    .scaledToFit()
                                    .frame(width: 22, height: 22)
                                    .foregroundStyle(.secondary)
                            }
                            .tint(.secondary)
                            .padding(4)
                            .background {
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(.gray.opacity(0.8))
                            }
                            .padding([.top, .trailing], 4)
                        }
                        #else
                        Menu {
                            menuActionItems
                        } label: {
                            YabaIconView(bundleKey: "more-horizontal-circle-02")
                                .scaledToFit()
                                .frame(width: 22, height: 22)
                                .foregroundStyle(.secondary)
                        }
                        .tint(.secondary)
                        .padding(4)
                        .background {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(.gray.opacity(0.8))
                        }
                        .padding([.top, .trailing], 4)
                        #endif
                    }
                HStack {
                    Text(bookmark.label)
                        .font(.title3)
                        .fontWeight(.medium)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    Spacer()
                }.padding()
            }
            .background {
                #if targetEnvironment(macCatalyst)
                RoundedRectangle(cornerRadius: 12)
                    .fill(
                        .gray.opacity(
                            bookmark.id == selectedBookmark?.id
                            ? 0.3
                            : isHovered ? 0.2 : 0.1
                        )
                    )
                #else
                RoundedRectangle(cornerRadius: 12)
                    .fill(.thinMaterial)
                #endif
            }
            .contentShape(Rectangle())
        }
    }
    
    @ViewBuilder
    private var bookmarkImage: some View {
        if let imageData = bookmark.imageData,
           let image = UIImage(data: imageData) {
            switch contentAppearance {
            case .list:
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 50, height: 50)
            case .grid:
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .containerRelativeFrame(.horizontal) { size, _ in
                        return size * 0.5 - 20
                    }
            }
        } else {
            switch contentAppearance {
            case .list:
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 50, height: 50)
                    .overlay {
                        YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                            .scaledToFit()
                            .foregroundStyle(.tint)
                            .frame(width: 32, height: 32)
                    }
            case .grid:
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .overlay {
                        YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                            .scaledToFit()
                            .foregroundStyle(.tint)
                            .frame(width: 92, height: 92)
                    }
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
                YabaIconView(bundleKey: "delete-02")
                Text("Delete")
            }
        }.tint(.red)
        Button {
            selectedBookmarkToEdit = bookmark
        } label: {
            VStack {
                YabaIconView(bundleKey: "edit-02")
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
                YabaIconView(bundleKey: "edit-02")
                Text("Edit")
            }
        }.tint(.orange)
        Button(role: .destructive) {
            selectedBookmarkToDelete = bookmark
            shouldShowDeleteDialog = true
        } label: {
            VStack {
                YabaIconView(bundleKey: "delete-02")
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
        isSearching: false,
        onNavigationCallback: { _ in }
    ).tint(.indigo)
}
