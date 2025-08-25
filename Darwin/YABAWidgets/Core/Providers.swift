//
//  Provider.swift
//  YABA
//
//  Created by Ali Taha on 24.08.2025.
//

import WidgetKit

internal struct BookmarkListProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> BookmarksListEntry {
        let defaultConfig = BookmarkListAppIntent()
        return BookmarksListEntry(date: Date(), configuration: defaultConfig)
    }

    func snapshot(
        for configuration: BookmarkListAppIntent,
        in context: Context
    ) async -> BookmarksListEntry {
        let finalConfig = await ensureDefaultConfiguration(configuration)
        return BookmarksListEntry(date: Date(), configuration: finalConfig)
    }
    
    func timeline(
        for configuration: BookmarkListAppIntent,
        in context: Context
    ) async -> Timeline<BookmarksListEntry> {
        let finalConfig = await ensureDefaultConfiguration(configuration)
        var entries: [BookmarksListEntry] = []

        // Generate a timeline consisting of five entries an hour apart, starting from the current date.
        let currentDate = Date()
        for hourOffset in 0 ..< 5 {
            let entryDate = Calendar.current.date(
                byAdding: .hour,
                value: hourOffset,
                to: currentDate
            )!
            let entry = BookmarksListEntry(date: entryDate, configuration: finalConfig)
            entries.append(entry)
        }

        return Timeline(entries: entries, policy: .atEnd)
    }
    
    private func ensureDefaultConfiguration(_ configuration: BookmarkListAppIntent) async -> BookmarkListAppIntent {
        if configuration.selectedFolder == nil {
            let query = CollectionEntityQuery()
            let defaultCollection = await query.defaultResult()
            return BookmarkListAppIntent(selectedFolder: defaultCollection)
        }
        return configuration
    }
}

internal struct CategoryProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> CategoryEntry {
        let defaultConfig = CategoryAppIntent()
        return CategoryEntry(date: Date(), configuration: defaultConfig)
    }

    func snapshot(
        for configuration: CategoryAppIntent,
        in context: Context
    ) async -> CategoryEntry {
        let finalConfig = await ensureDefaultConfiguration(configuration)
        return CategoryEntry(date: Date(), configuration: finalConfig)
    }
    
    func timeline(
        for configuration: CategoryAppIntent,
        in context: Context
    ) async -> Timeline<CategoryEntry> {
        let finalConfig = await ensureDefaultConfiguration(configuration)
        var entries: [CategoryEntry] = []

        // Generate a timeline consisting of five entries an hour apart, starting from the current date.
        let currentDate = Date()
        for hourOffset in 0 ..< 5 {
            let entryDate = Calendar.current.date(
                byAdding: .hour,
                value: hourOffset,
                to: currentDate
            )!
            let entry = CategoryEntry(date: entryDate, configuration: finalConfig)
            entries.append(entry)
        }

        return Timeline(entries: entries, policy: .atEnd)
    }
    
    private func ensureDefaultConfiguration(_ configuration: CategoryAppIntent) async -> CategoryAppIntent {
        if configuration.selectedCollection == nil {
            let query = CategoryCollectionEntityQuery()
            let defaultCollection = await query.defaultResult()
            return CategoryAppIntent(selectedCollection: defaultCollection)
        }
        return configuration
    }
}
