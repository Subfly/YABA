//
//  Provider.swift
//  YABA
//
//  Created by Ali Taha on 24.08.2025.
//

import WidgetKit

internal struct BookmarkListProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> BookmarksListEntry {
        BookmarksListEntry(date: Date(), configuration: BookmarkListAppIntent())
    }

    func snapshot(
        for configuration: BookmarkListAppIntent,
        in context: Context
    ) async -> BookmarksListEntry {
        BookmarksListEntry(date: Date(), configuration: configuration)
    }
    
    func timeline(
        for configuration: BookmarkListAppIntent,
        in context: Context
    ) async -> Timeline<BookmarksListEntry> {
        var entries: [BookmarksListEntry] = []

        // Generate a timeline consisting of five entries an hour apart, starting from the current date.
        let currentDate = Date()
        for hourOffset in 0 ..< 5 {
            let entryDate = Calendar.current.date(
                byAdding: .hour,
                value: hourOffset,
                to: currentDate
            )!
            let entry = BookmarksListEntry(date: entryDate, configuration: configuration)
            entries.append(entry)
        }

        return Timeline(entries: entries, policy: .atEnd)
    }
}
