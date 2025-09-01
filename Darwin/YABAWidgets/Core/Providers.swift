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
        
        // Create a single entry for manual updates via app CRUD operations
        let currentDate = Date()
        let entry = BookmarksListEntry(date: currentDate, configuration: finalConfig)
        let entries = [entry]

        return Timeline(entries: entries, policy: .never)
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
        
        // Create a single entry for manual updates via app CRUD operations
        let currentDate = Date()
        let entry = CategoryEntry(date: currentDate, configuration: finalConfig)
        let entries = [entry]

        return Timeline(entries: entries, policy: .never)
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
