//
//  MenuItemMainContent.swift
//  YABAStatusMenuItem
//
//  Created by Ali Taha on 8.06.2025.
//

import SwiftUI
import SwiftData

struct MenuItemMainContent: View {
    @Query(sort: \YabaBookmark.editedAt, order: .reverse, animation: .smooth)
    private var bookmarks: [YabaBookmark]
    
    let onCreateNewBookmarkRequested: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            if bookmarks.isEmpty {
                noContentView
            } else {
                HStack {
                    Label {
                        Text("Home Recents Label")
                            .font(.title3)
                    } icon: {
                        Image("clock-01")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 18, height: 18)
                    }
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.vertical, 12)
                List {
                    ForEach(bookmarks.prefix(10)) { bookmark in
                        BookmarkItemView(bookmark: bookmark)
                    }
                }.listStyle(.sidebar)
            }
            buttons
                .padding(.horizontal)
                .padding(.vertical, 12)
        }
    }
    
    @ViewBuilder
    private var buttons: some View {
        HStack {
            QuitButton()
            Spacer()
            NewBookmarkButton(
                onCreateNewBookmarkRequested: onCreateNewBookmarkRequested
            )
        }
    }
    
    @ViewBuilder
    private var noContentView: some View {
        ContentUnavailableView {
            Label {
                Text("No Bookmarks Title")
            } icon: {
                Image("bookmark-off-02")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 44, height: 44)
            }
        } description: {
            Text("No Bookmarks Menu Item Message")
        }.padding()
    }
}

private struct BookmarkItemView: View {
    @State
    private var isHovered: Bool = false
    
    let bookmark: YabaBookmark
    
    var body: some View {
        HStack(alignment: .center) {
            generateBookmarkImage(for: bookmark)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            Text(bookmark.label)
                .font(.title3)
                .fontWeight(.medium)
                .lineLimit(1)
        }
        .background {
            if isHovered {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.gray.opacity(0.2))
            }
        }
        .onHover { hovered in
            self.isHovered = hovered
        }
        .contentShape(Rectangle())
        .onTapGesture {
            let id = bookmark.bookmarkId
            if let url = URL(
                string: "yaba://open?id=\(id.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")"
            ) {
                NSWorkspace.shared.open(url)
            }
        }
    }
    
    @ViewBuilder
    private func generateBookmarkImage(for bookmark: YabaBookmark) -> some View {
        if let imageData = bookmark.imageDataHolder?.data,
           let image = NSImage(data: imageData) {
            Image(nsImage: image)
                .resizable()
                .scaledToFill()
                .frame(width: 32, height: 32)
        } else {
            RoundedRectangle(cornerRadius: 8)
                .fill(.tint.opacity(0.3))
                .frame(width: 32, height: 32)
                .overlay {
                    Image(bookmark.bookmarkType.getIconName())
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .foregroundStyle(.tint)
                        .frame(width: 24, height: 24)
                }
        }
    }
}

private struct QuitButton: View {
    @State
    private var isHovered: Bool = false
    
    var body: some View {
        Button {
            exit(0)
        } label: {
            Label {
                Text("Quit")
            } icon: {
                Image("cancel-circle")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
            .foregroundStyle(isHovered ? .red : .white)
        }
        .buttonStyle(.plain)
        .keyboardShortcut("q")
        .onHover { hovered in
            isHovered = hovered
        }
    }
}

private struct NewBookmarkButton: View {
    @State
    private var isHovered: Bool = false
    
    let onCreateNewBookmarkRequested: () -> Void
    
    var body: some View {
        Button {
            NSApplication.shared.activate(ignoringOtherApps: true)
            onCreateNewBookmarkRequested()
        } label: {
            Label {
                Text("New Bookmark")
            } icon: {
                Image("bookmark-add-02")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
            .labelStyle(InverseLabelStyle())
            .foregroundStyle(isHovered ? .blue : .white)
        }
        .buttonStyle(.plain)
        .keyboardShortcut("y", modifiers: [.shift, .command])
        .onHover { hovered in
            isHovered = hovered
        }
    }
}

#Preview {
    MenuItemMainContent(onCreateNewBookmarkRequested: {})
}
