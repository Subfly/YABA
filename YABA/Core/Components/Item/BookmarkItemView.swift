//
//  BookmarkItemView.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import SwiftUI
import Kingfisher

struct BookmarkItemView: View {
    @Binding
    var selectedBookmark: Bookmark?
    
    let bookmark: Bookmark
    
    var body: some View {
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
}

#Preview {
    BookmarkItemView(
        selectedBookmark: .constant(.empty()),
        bookmark: .empty()
    ).tint(.indigo)
}
