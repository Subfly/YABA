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
    
    @AppStorage(Constants.preferredCardImageSizingKey)
    private var cardSizing: CardViewTypeImageSizing = .small
    
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
        case .list: listLabel
        case .card: cardLabel
        }
    }
    
    @ViewBuilder
    private var listLabel: some View {
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
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tertiary)
            }
        }
        #if targetEnvironment(macCatalyst)
        .listRowBackground(
            bookmark.id == selectedBookmark?.id
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
            : isHovered
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
            : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
        )
        #endif
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            swipeActionItems
        }
    }
    
    @ViewBuilder
    private var cardLabel: some View {
        VStack(alignment: .leading) {
            switch cardSizing {
            case .big: bigCardLabel
            case .small: smallCardLabel
            }
        }
        #if targetEnvironment(macCatalyst)
        .listRowBackground(
            bookmark.id == selectedBookmark?.id
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.2))
            : isHovered
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
            : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
        )
        #endif
    }
    
    @ViewBuilder
    private var bigCardLabel: some View {
        bookmarkImage
            .clipShape(RoundedRectangle(cornerRadius: 8))
        HStack {
            if let miniIconData = bookmark.iconData,
               let miniIconImage = UIImage(data: miniIconData) {
                Image(uiImage: miniIconImage)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
            }
            Text(bookmark.label)
                .font(.title2)
                .fontWeight(.medium)
                .lineLimit(2)
        }.padding(.vertical, 4)
        if !bookmark.bookmarkDescription.isEmpty {
            Text(bookmark.bookmarkDescription)
                .lineLimit(4)
                .multilineTextAlignment(.leading)
                .padding(.bottom, 8)
        }
        HStack {
            generateTagIcons()
            Spacer()
            optionsClickableIcon
        }
    }
    
    @ViewBuilder
    private var smallCardLabel: some View {
        HStack(alignment: .center) {
            bookmarkImage
                .clipShape(RoundedRectangle(cornerRadius: 8))
            Text(bookmark.label)
                .font(.title2)
                .fontWeight(.medium)
                .lineLimit(2)
            Spacer()
        }.padding(.vertical, 4)
        if !bookmark.bookmarkDescription.isEmpty {
            Text(bookmark.bookmarkDescription)
                .lineLimit(4)
                .multilineTextAlignment(.leading)
                .padding(.bottom, 8)
        }
        HStack {
            generateTagIcons()
            Spacer()
            HStack {
                if let miniIconData = bookmark.iconData,
                   let miniIconImage = UIImage(data: miniIconData) {
                    Image(uiImage: miniIconImage)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                }
                optionsClickableIcon
            }
        }
    }
    
    @ViewBuilder
    private var gridLabel: some View {
        VStack(spacing: 0) {
            bookmarkImage
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(alignment: .topTrailing) {
                    optionsClickableIcon
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
            case .card:
                switch cardSizing {
                case .big:
                    RoundedRectangle(cornerRadius: 8)
                        .frame(height: 150)
                        .overlay {
                            Image(uiImage: image)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .clipped()
                        }
                case .small:
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 50, height: 50)
                }
                /**
                 * TODO: OPEN WHEN LAZYVGRID IS RECYCABLE
                    case .grid:
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill()
                            .containerRelativeFrame(.horizontal) { size, _ in
                                return size * 0.5 - 20
                            }
                 */
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
            case .card:
                switch cardSizing {
                case .big:
                    RoundedRectangle(cornerRadius: 8)
                        .fill(.tint.opacity(0.3))
                        .frame(height: 150)
                        .overlay {
                            YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                                .scaledToFit()
                                .foregroundStyle(.tint)
                                .frame(width: 96, height: 96)
                        }
                case .small:
                    RoundedRectangle(cornerRadius: 8)
                        .fill(.tint.opacity(0.3))
                        .frame(width: 50, height: 50)
                        .overlay {
                            YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                                .scaledToFit()
                                .foregroundStyle(.tint)
                                .frame(width: 32, height: 32)
                        }
                }
                /**
                 * TODO: OPEN WHEN LAZYVGRID IS RECYCABLE
                    case .grid:
                        RoundedRectangle(cornerRadius: 8)
                            .fill(.tint.opacity(0.3))
                            .overlay {
                                YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                                    .scaledToFit()
                                    .foregroundStyle(.tint)
                                    .frame(width: 92, height: 92)
                        }
                 */
            }
        }
    }
    
    @ViewBuilder
    private func generateTagIcons() -> some View {
        let tags = bookmark.collections.filter({ $0.collectionType == .tag })
                
        if !tags.isEmpty {
            if tags.count < 6 {
                HStack {
                    HStack(spacing: 0) {
                        ForEach(tags) { tag in
                            Rectangle()
                                .fill(tag.color.getUIColor().opacity(0.3))
                                .frame(width: 34, height: 34)
                                .overlay {
                                    YabaIconView(bundleKey: tag.icon)
                                        .foregroundStyle(tag.color.getUIColor())
                                        .frame(width: 24, height: 24)
                                }
                        }
                    }.clipShape(RoundedRectangle(cornerRadius: 8))
                    Spacer()
                }
            } else {
                HStack {
                    HStack(spacing: 0) {
                        ForEach(0..<5) { index in
                            let tag = tags[index]
                            Rectangle()
                                .fill(tag.color.getUIColor().opacity(0.3))
                                .frame(width: 34, height: 34)
                                .overlay {
                                    YabaIconView(bundleKey: tag.icon)
                                        .foregroundStyle(tag.color.getUIColor())
                                        .frame(width: 24, height: 24)
                                }
                        }
                    }.clipShape(RoundedRectangle(cornerRadius: 8))
                    Text("+\(tags.count - 5)")
                        .font(.caption)
                        .italic()
                    Spacer()
                }
            }
        } else {
            HStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 34, height: 34)
                    .overlay {
                        YabaIconView(bundleKey: "tags")
                            .foregroundStyle(.tint)
                            .frame(width: 24, height: 24)
                    }
                Text("Bookmark No Tags Added Title")
                    .font(.caption)
                    .italic()
            }
        }
    }
    
    @ViewBuilder
    private var optionsClickableIcon: some View {
        #if targetEnvironment(macCatalyst)
        if isHovered {
            Menu {
                menuActionItems
            } label: {
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 34, height: 34)
                    .overlay {
                        YabaIconView(bundleKey: "more-horizontal-circle-02")
                            .foregroundStyle(.tint)
                            .frame(width: 24, height: 24)
                    }
            }
            .padding([.top, .trailing], contentAppearance == .card ? 0 : 4)
        }
        #else
        Menu {
            menuActionItems
        } label: {
            RoundedRectangle(cornerRadius: 8)
                .fill(.tint.opacity(0.3))
                .frame(width: 34, height: 34)
                .overlay {
                    YabaIconView(bundleKey: "more-horizontal-circle-02")
                        .foregroundStyle(.tint)
                        .frame(width: 24, height: 24)
                }
        }
        .padding([.top, .trailing], contentAppearance == .card ? 0 : 4)
        #endif
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
