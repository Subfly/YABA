//
//  MenuItemMainContent.swift
//  YABAStatusMenuItem
//
//  Created by Ali Taha on 8.06.2025.
//

import SwiftUI
import SwiftData

struct MenuItemMainContent: View {
    @State
    private var searchQuery: String = ""
    
    let onCreateNewBookmarkRequested: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 2) {
                Label {
                    Text("YABA").font(.headline)
                } icon: {
                    Image("bookmark-02")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
                Spacer()
                TextField("", text: $searchQuery, prompt: Text("Search Prompt"))
            }
            .padding(.horizontal)
            .padding(.vertical, 12)
            SearchableContent(searchQuery: searchQuery)
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
}

private struct SearchableContent: View {
    @Query
    private var bookmarks: [YabaBookmark]
    
    let searchQuery: String
    
    init(searchQuery: String) {
        self.searchQuery = searchQuery
        
        _bookmarks = .init(
            filter: #Predicate<YabaBookmark> { bookmark in
                if searchQuery.isEmpty {
                    true
                } else {
                    bookmark.label.localizedStandardContains(searchQuery)
                    || bookmark.bookmarkDescription.localizedStandardContains(searchQuery)
                }
            },
            sort: [SortDescriptor<YabaBookmark>(\.editedAt, order: .reverse)],
            animation: .smooth
        )
    }
    
    var body: some View {
        if bookmarks.isEmpty {
            if searchQuery.isEmpty {
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
            } else {
                ContentUnavailableView {
                    Label {
                        Text("Search No Bookmarks Found Title")
                    } icon: {
                        Image("bookmark-off-02")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 44, height: 44)
                    }
                } description: {
                    Text("Search No Bookmarks Found Description \(searchQuery)")
                }.padding()
            }
        } else {
            List {
                ForEach(bookmarks) { bookmark in
                    BookmarkItemView(bookmark: bookmark)
                }
            }.listStyle(.sidebar)
        }
    }
}

private struct BookmarkItemView: View {
    @Environment(\.modelContext)
    private var modelContext
    
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
            Spacer()
            if isHovered {
                Menu {
                    menuItems
                } label: {
                    Image("more-horizontal-circle-02")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }.buttonStyle(.plain)
            }
        }
        .listRowBackground(
            isHovered
            ? RoundedRectangle(cornerRadius: 8).fill(.gray.opacity(0.2))
            : nil
        )
        .contentShape(Rectangle())
        .onHover { hovered in
            isHovered = hovered
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button {
                if let url = URL(string: bookmark.link) {
                    NSWorkspace.shared.open(url)
                }
            } label: {
                Image("internet")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
            }.tint(.teal)
            Button {
                let id = bookmark.bookmarkId
                let encodedID = id.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)
                if let url = URL(
                    string: "yaba://open?id=\(encodedID ?? "")"
                ) {
                    NSWorkspace.shared.open(url)
                }
            } label: {
                Image("bookmark-02")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
            }.tint(.blue)
            Button(role: .destructive) {
                modelContext.delete(bookmark)
                try? modelContext.save()
            } label: {
                Image("delete-02")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
            }.tint(.red)
        }
        .contextMenu {
            menuItems
        }
        .onTapGesture {
            if let url = URL(string: bookmark.link) {
                NSWorkspace.shared.open(url)
            }
        }
    }
    
    @ViewBuilder
    private func generateBookmarkImage(for bookmark: YabaBookmark) -> some View {
        if let imageData = bookmark.imageDataHolder,
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
    
    @ViewBuilder
    private var menuItems: some View {
        Button {
            if let url = URL(string: bookmark.link) {
                NSWorkspace.shared.open(url)
            }
        } label: {
            VStack {
                Image("internet")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
                    .foregroundStyle(.teal)
                Text("Menu Item Open Web Label")
            }
        }
        Button {
            let id = bookmark.bookmarkId
            let encodedID = id.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)
            if let url = URL(
                string: "yaba://open?id=\(encodedID ?? "")"
            ) {
                NSWorkspace.shared.open(url)
            }
        } label: {
            VStack {
                Image("bookmark-02")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
                    .foregroundStyle(.blue)
                Text("Menu Item Open Yaba Label")
            }
        }
        Divider()
        Button(role: .destructive) {
            modelContext.delete(bookmark)
            try? modelContext.save()
        } label: {
            VStack {
                Image("delete-02")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
                    .foregroundStyle(.red)
                Text("Delete")
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
