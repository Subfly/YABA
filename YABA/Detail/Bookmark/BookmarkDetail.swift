//
//  BookmarkDetail.swift
//  YABA
//
//  Created by Ali Taha on 29.04.2025.
//

import SwiftUI

struct BookmarkDetail: View {
    @State
    private var state: BookmarkDetailState = .init()
    
    @Binding
    var selectedCollection: YabaCollection?
    
    @Binding
    var bookmark: Bookmark?
    
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        ZStack {
            AnimatedMeshGradient(
                collectionColor: selectedCollection?.color.getUIColor() ?? .accentColor
            )
            
            if bookmark != nil {
                List {
                    imageSection
                    infoSection
                    folderSection
                    tagsSection
                }
                .scrollContentBackground(.hidden)
                #if os(macOS)
                .listStyle(.sidebar)
                #endif
            } else {
                
            }
        }
        .navigationTitle("Bookmark Detail Title")
    }
    
    @ViewBuilder
    private var imageSection: some View {
        Section {
            if let imageData = self.bookmark?.imageData,
               let image = UIImage(data: imageData) {
                #if os(iOS)
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(idealHeight: 250, alignment: .center)
                #elseif os(macOS)
                Image(nsImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(idealHeight: 250, alignment: .center)
                #endif
            } else {
                ContentUnavailableView(
                    "Bookmark Detail Image Error Title",
                    systemImage: "photo.badge.exclamationmark",
                    description: Text("Bookmark Detail Image Error Description")
                )
            }
        } header: {
            Label(
                "Bookmark Detail Image Header Title",
                systemImage: "photo"
            ).padding(.leading)
        } footer: {
            HStack {
                if let iconData = self.bookmark?.iconData,
                   let image = UIImage(data: iconData) {
                    #if os(iOS)
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 18, height: 18)
                    #elseif os(macOS)
                    Image(nsImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 30, height: 30)
                    #endif
                } else {
                    Image(systemName: "link.circle.fill")
                }
                Text(
                    self.bookmark?.domain.isEmpty == true
                    ? self.bookmark?.link ?? ""
                    : self.bookmark?.domain ?? ""
                ).lineLimit(2)
            }.padding(.leading)
        }
        .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
        .onTapGesture {
            state.onClickOpenLink(using: bookmark)
        }
    }
    
    @ViewBuilder
    private var infoSection: some View {
        Section {
            HStack(alignment: .top) {
                Image(systemName: "t.square")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tint)
                if let label = bookmark?.label {
                    Text(label)
                        .multilineTextAlignment(.leading)
                } else {
                    Text("Bookmark Detail No Label Provided")
                        .foregroundStyle(.secondary)
                        .italic()
                }
            }
            
            HStack(alignment: .top) {
                Image(systemName: "text.page")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tint)
                if let description = bookmark?.bookmarkDescription {
                    if description.isEmpty {
                        Text("Bookmark Detail No Description Provided")
                            .foregroundStyle(.secondary)
                            .italic()
                    } else {
                        Text(description)
                            .multilineTextAlignment(.leading)
                    }
                } else {
                    Text("Bookmark Detail No Description Provided")
                        .foregroundStyle(.secondary)
                        .italic()
                }
            }
            
            HStack {
                HStack {
                    Image(systemName: bookmark?.bookmarkType.getIconName() ?? BookmarkType.none.getIconName())
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 20, height: 20)
                        .foregroundStyle(.tint)
                    Text("Create Bookmark Type Placeholder")
                }
                Spacer()
                Text(bookmark?.bookmarkType.getUITitle() ?? BookmarkType.none.getUITitle())
            }
        } header: {
            Label(
                "Info",
                systemImage: "info.circle"
            )
        }
    }
    
    @ViewBuilder
    private var folderSection: some View {
        if let collections = bookmark?.collections,
           let folder = collections.first(where: { $0.collectionType == .folder }) {
            Section {
                CollectionItemView(
                    collection: folder,
                    selectedCollection: $selectedCollection,
                    isInSelectionMode: false,
                    onDeleteCallback: { _ in },
                    onEditCallback: { _ in },
                    onNavigationCallback: onNavigationCallback
                )
            } header: {
                Label(
                    "Folder",
                    systemImage: "folder"
                )
            }
        }
    }
    
    @ViewBuilder
    private var tagsSection: some View {
        if let collections = bookmark?.collections {
            let tags = collections.filter({ $0.collectionType == .tag })
            if tags.isEmpty {
                
            } else {
                Section {
                    ForEach(tags) { tag in
                        CollectionItemView(
                            collection: tag,
                            selectedCollection: $selectedCollection,
                            isInSelectionMode: false,
                            onDeleteCallback: { _ in },
                            onEditCallback: { _ in },
                            onNavigationCallback: onNavigationCallback
                        )
                    }
                } header: {
                    Label(
                        "Tags Title",
                        systemImage: "tag"
                    )
                }
            }
        }
    }
}

#Preview {
    BookmarkDetail(
        selectedCollection: .constant(nil),
        bookmark: .constant(nil),
        onNavigationCallback: { _ in }
    )
}
