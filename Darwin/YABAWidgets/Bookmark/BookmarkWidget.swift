//
//  BookmarkWidget.swift
//  YABAKeyboard
//
//  Created by Ali Taha on 24.08.2025.
//

import SwiftUI
import SwiftData
import WidgetKit

internal struct BookmarkWidget: Widget {
    let kind: String = "YABA Bookmark Widget"
    
    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: BookmarkListAppIntent.self,
            provider: BookmarkListProvider()
        ) { entry in
            BookmarkWidgetView(entry: entry)
                .containerBackground(.clear, for: .widget)
                .modelContext(YabaModelContainer.getContext())
        }
        .supportedFamilies([.systemLarge, .systemExtraLarge])
        .contentMarginsDisabled()
    }
}

private struct BookmarkWidgetView: View {
    @Query
    private var bookmarks: [YabaBookmark]
    
    var entry: BookmarkListProvider.Entry
    
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: .accentColor)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            VStack {
                HStack {
                    Label {
                        Text("Home Recents Label")
                            .fontWeight(.semibold)
                    } icon: {
                        YabaIconView(bundleKey: "clock-01")
                            .scaledToFit()
                            .frame(width: 18, height: 18)
                    }
                    Spacer()
                    Button {
                        // Dummy button :D It will auto open the app :D
                    } label: {
                        YabaIconView(bundleKey: "bookmark-02")
                            .scaledToFit()
                            .frame(width: 18, height: 18)
                    }
                }.padding(.bottom)
                ForEach(bookmarks.prefix(5)) { bookmark in
                    if let url = URL(string: "yaba://open?id=\(bookmark.bookmarkId)") {
                        Link(destination: url) {
                            BookmarkView(bookmark: bookmark)
                        }
                    }
                }
            }.padding()
        }
    }
}

private struct BookmarkView: View {
    let bookmark: YabaBookmark
    
    var body: some View {
        HStack(alignment: .center) {
            if let imageData = bookmark.imageDataHolder,
               let image = UIImage(data: imageData) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 50, height: 50)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            } else {
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 50, height: 50)
                    .overlay {
                        YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                            .scaledToFit()
                            .foregroundStyle(.tint)
                            .frame(width: 32, height: 32)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            VStack(alignment: .leading) {
                Text(bookmark.label)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(1)
                if !bookmark.bookmarkDescription.isEmpty {
                    Text(bookmark.bookmarkDescription)
                        .font(.caption2)
                        .lineLimit(1)
                        .multilineTextAlignment(.leading)
                }
            }
            Spacer()
        }.frame(maxWidth: .infinity)
    }
}

#Preview(as: .systemSmall) {
    BookmarkWidget()
} timeline: {
    BookmarksListEntry(date: .now, configuration: BookmarkListAppIntent())
}
