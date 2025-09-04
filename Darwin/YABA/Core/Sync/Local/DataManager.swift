//
//  DataManager.swift
//  YABA
//
//  Created by Ali Taha on 24.05.2025.
//

import Foundation
import UniformTypeIdentifiers
import SwiftData
import SwiftUI
import UserNotifications

enum MappableCSVHeaderValues: String {
    case url         = "url"
    case label       = "label"
    case description = "description"
    case createdAt   = "createdAt"
}

@MainActor
enum DataError: Error {
    case invalidCSVEncoding(LocalizedStringKey)
    case emptyCSV(LocalizedStringKey)
    case emptyJSON(LocalizedStringKey)
    case invalidJSON(LocalizedStringKey)
    case exportableGenerationError(LocalizedStringKey)
    case caseURLColumnNotSelected(LocalizedStringKey)
    case invalidBookmarkUrl(LocalizedStringKey)
    case unkownError(LocalizedStringKey)
    case invalidHTML(LocalizedStringKey)
    case emptyHTML(LocalizedStringKey)
}

@MainActor
class DataManager {
    func importJSON(
        from data: Data,
        using modelContext: ModelContext
    ) throws {
        let decoder = JSONDecoder()
        
        // Add better error handling for JSON decoding
        let content: YabaCodableContent
        do {
            content = try decoder.decode(YabaCodableContent.self, from: data)
        } catch {
            // If decoding fails, throw a more informative error
            throw DataError.invalidJSON("Data Manager Invalid JSON Format Message")
        }
        
        if content.bookmarks.isEmpty {
            throw DataError.emptyJSON("Data Manager Empty JSON Message")
        }
        
        // Create a mapping of bookmark IDs to bookmark models
        // Generate new IDs for bookmarks that don't have them or have duplicates
        var mappedBookmarks: [String: YabaBookmark] = [:]
        var usedIds: Set<String> = []
        
        for codableBookmark in content.bookmarks {
            var bookmarkId = codableBookmark.bookmarkId ?? UUID().uuidString
            
            // Handle duplicate IDs by generating new ones
            while usedIds.contains(bookmarkId) {
                bookmarkId = UUID().uuidString
            }
            usedIds.insert(bookmarkId)
            
            let bookmarkModel = codableBookmark.mapToModel()
            // Ensure the bookmark has the correct ID
            bookmarkModel.bookmarkId = bookmarkId
            mappedBookmarks[bookmarkId] = bookmarkModel
        }
        
        // First, insert all bookmarks into the model context
        mappedBookmarks.values.forEach { bookmark in
            modelContext.insert(bookmark)
        }
        
        if let collections = content.collections {
            collections.forEach { collection in
                let collectionModel = collection.mapToModel()
                collection.bookmarks.forEach { bookmarkId in
                    if let bookmarkModel = mappedBookmarks[bookmarkId] {
                        collectionModel.bookmarks?.append(bookmarkModel)
                    }
                }
                
                modelContext.insert(collectionModel)
            }
        } else {
            let creationTime = Date.now
            let dummyFolder = YabaCollection(
                collectionId: UUID().uuidString,
                label: "\(Date.now.formatted(date: .abbreviated, time: .shortened))",
                icon: "folder-01",
                createdAt: creationTime,
                editedAt: creationTime,
                color: .none,
                type: .folder,
                version: 0
            )
            
            mappedBookmarks.values.forEach { bookmark in
                dummyFolder.bookmarks?.append(bookmark)
            }
            
            modelContext.insert(dummyFolder)
        }
        
        try modelContext.save()
    }
    
    func importCSV(
        from data: Data,
        using modelContext: ModelContext
    ) throws {
        guard let content = String(data: data, encoding: .utf8) else {
            throw DataError.invalidCSVEncoding("Data Manager Invalid CSV Encoding Message")
        }

        let rows = content.components(separatedBy: "\n").filter {
            !$0.trimmingCharacters(in: .whitespaces).isEmpty
        }
        guard rows.count > 1 else {
            throw DataError.emptyCSV("Data Manager Empty CSV Message")
        }
        
        let header = parseCSVRow(rows[0])
        guard header.count == 12 else {
            throw DataError.invalidCSVEncoding("Data Manager Invalid CSV Encoding Message")
        }
        
        // Validate header content matches YABA's expected format
        let expectedHeaders = [
            "bookmarkId", "label", "bookmarkDescription", "link", "domain",
            "createdAt", "editedAt", "imageUrl", "iconUrl", "videoUrl", 
            "type", "version"
        ]
        guard header == expectedHeaders else {
            throw DataError.invalidCSVEncoding("Data Manager Invalid CSV Encoding Message")
        }
        
        let creationTime = Date.now
        let dummyFolder = YabaCollection(
            collectionId: UUID().uuidString,
            label: "\(Date.now.formatted(date: .abbreviated, time: .shortened))",
            icon: "folder-01",
            createdAt: creationTime,
            editedAt: creationTime,
            color: .none,
            type: .folder,
            version: 0
        )
        
        let bookmarkRows = rows.dropFirst()
        var processedRows = 0
        
        for row in bookmarkRows {
            let columns = parseCSVRow(row)
            
            // Validate exact column count - no more, no less
            guard columns.count == 12 else {
                throw DataError.invalidCSVEncoding("Data Manager Invalid CSV Encoding Message")
            }
            
            // Validate required URL field is not empty
            guard !columns[3].trimmingCharacters(in: .whitespaces).isEmpty else {
                throw DataError.invalidCSVEncoding("Data Manager Invalid CSV Encoding Message")
            }

            let bookmark = YabaCodableBookmark(
                bookmarkId: columns[0].isEmpty ? nil : columns[0],
                label: columns[1].isEmpty ? nil : columns[1],
                bookmarkDescription: columns[2].isEmpty ? nil : columns[2],
                link: columns[3],
                domain: columns[4].isEmpty ? nil : columns[4],
                createdAt: columns[5].isEmpty ? nil : columns[5],
                editedAt: columns[6].isEmpty ? nil : columns[6],
                imageUrl: columns[7].isEmpty ? nil : columns[7],
                iconUrl: columns[8].isEmpty ? nil : columns[8],
                videoUrl: columns[9].isEmpty ? nil : columns[9],
                readableHTML: nil, // Set to nil to prevent CSV parsing issues
                type: Int(columns[10]) ?? 1,
                version: Int(columns[11]) ?? 0,
            ).mapToModel()
            
            dummyFolder.bookmarks?.append(bookmark)
            processedRows += 1
        }
        
        // Ensure we actually processed some valid rows
        guard processedRows > 0 else {
            throw DataError.invalidCSVEncoding("Data Manager Invalid CSV Encoding Message")
        }
        
        modelContext.insert(dummyFolder)
        try modelContext.save()
    }
    
    func importFixedCSV(
        using modelContext: ModelContext,
        headers: Dictionary<MappableCSVHeaderValues, Int?>,
        data: Data?,
        onFinish: @escaping () -> Void
    ) throws {
        if headers[.url] == nil {
            throw DataError.caseURLColumnNotSelected(
                LocalizedStringKey("Data Manager Bookmark URL Column Not Selected Message")
            )
        }
        guard let data, let content = String(data: data, encoding: .utf8) else {
            throw DataError.invalidCSVEncoding("Data Manager Invalid CSV Encoding Message")
        }

        let rows = content.components(separatedBy: "\n").filter {
            !$0.trimmingCharacters(in: .whitespaces).isEmpty
        }
        guard rows.count > 1 else {
            throw DataError.emptyCSV("Data Manager Empty CSV Message")
        }

        let bookmarkRows = rows.dropFirst()
        let now = Date.now

        let dummyFolder = YabaCollection(
            collectionId: UUID().uuidString,
            label: "\(now.formatted(date: .abbreviated, time: .shortened))",
            icon: "folder-01",
            createdAt: now,
            editedAt: now,
            color: .none,
            type: .folder,
            version: 0
        )

        for row in bookmarkRows {
            let columns = parseCSVRow(row)
            guard let urlIndex = headers[.url] ?? nil, urlIndex < columns.count else { continue }

            let urlString = columns[urlIndex].trimmingCharacters(in: .whitespacesAndNewlines)
            guard let url = URL(string: urlString),
                  url.scheme?.hasPrefix("http") == true else {
                throw DataError.invalidBookmarkUrl("Data Manager Invalid Bookmark URL Message \(urlString)")
            }

            let label = headers[.label].flatMap { index in
                if let index = index, index < columns.count {
                    return columns[index]
                }
                return nil
            } ?? urlString

            let description = headers[.description].flatMap { index in
                if let index = index, index < columns.count {
                    return columns[index]
                }
                return nil
            } ?? ""

            let createdAtStr = headers[.createdAt].flatMap { index in
                if let index = index, index < columns.count {
                    return columns[index]
                }
                return nil
            }
            
            let createdAt = createdAtStr.flatMap {
                ISO8601DateFormatter().date(from: $0)
            } ?? now

            let bookmark = YabaCodableBookmark(
                bookmarkId: UUID().uuidString,
                label: label,
                bookmarkDescription: description,
                link: urlString,
                domain: url.host,
                createdAt: ISO8601DateFormatter().string(from: createdAt),
                editedAt: ISO8601DateFormatter().string(from: createdAt),
                imageUrl: nil,
                iconUrl: nil,
                videoUrl: nil,
                readableHTML: nil,
                type: 1,
                version: 0,
            ).mapToModel()

            dummyFolder.bookmarks?.append(bookmark)
        }

        modelContext.insert(dummyFolder)
        try modelContext.save()
        onFinish()
    }
    
    func importHTML(
        from data: Data,
        using modelContext: ModelContext
    ) throws {
        guard let htmlString = String(data: data, encoding: .utf8) else {
            throw DataError.invalidHTML("Data Manager Invalid HTML Encoding Message")
        }
        
        // Parse HTML and extract folders with bookmarks
        let parser = HTMLParser()
        let folders = try parser.parse(htmlString)
        
        if folders.isEmpty {
            throw DataError.emptyHTML("Data Manager Empty HTML Message")
        }
        
        let creationTime = Date.now
        
        // Create folder models and their bookmarks
        for folder in folders {
            var folderModel: YabaCollection? = nil

            if folder.label == "-1" {
                let id = Constants.uncategorizedCollectionId // Thanks #Predicate for that...
                let descriptor = FetchDescriptor<YabaCollection>(
                    predicate: #Predicate<YabaCollection> { collection in
                        collection.collectionId == id
                    }
                )

                if let existingUncatagorizedFolder = try? modelContext.fetch(descriptor).first {
                    folderModel = existingUncatagorizedFolder
                } else {
                    let creationTime = Date.now
                    folderModel = YabaCollection(
                        collectionId: Constants.uncategorizedCollectionId,
                        label: Constants.uncategorizedCollectionLabelKey,
                        icon: "folder-01",
                        createdAt: creationTime,
                        editedAt: creationTime,
                        color: .none,
                        type: .folder,
                        version: 0,
                    )
                }
            } else {
                folderModel = YabaCollection(
                    collectionId: UUID().uuidString,
                    label: folder.label,
                    icon: "folder-01",
                    createdAt: creationTime,
                    editedAt: creationTime,
                    color: .none,
                    type: .folder,
                    version: 0
                )
            }
            
            // Create bookmark models and add them to the folder
            for bookmark in folder.bookmarks {
                let bookmarkModel = YabaBookmark(
                    bookmarkId: UUID().uuidString,
                    link: bookmark.url,
                    label: bookmark.title,
                    bookmarkDescription: "",
                    domain: URL(string: bookmark.url)?.host ?? "",
                    createdAt: creationTime,
                    editedAt: creationTime,
                    imageDataHolder: nil,
                    iconDataHolder: nil,
                    imageUrl: nil,
                    iconUrl: nil,
                    videoUrl: nil,
                    readableHTML: nil,
                    type: .none,
                    version: 0
                )
                folderModel?.bookmarks?.append(bookmarkModel)
            }
            
            if let folderModel {
                modelContext.insert(folderModel)
            }
        }
        
        try modelContext.save()
    }
    
    func prepareExportableData(
        using modelContext: ModelContext,
        withType type: UTType,
        onReady: @escaping (Data) -> Void
    ) throws {
        let descriptor = FetchDescriptor<YabaCollection>(
            predicate: #Predicate { _ in true }
        )
        
        if let collections = try? modelContext.fetch(descriptor) {
            let exportableCollections = collections.map { $0.mapToCodable() }
            var exportableBookmarks: Set<YabaCodableBookmark> = []
            collections.forEach { collection in
                collection.bookmarks?.forEach { bookmark in
                    exportableBookmarks.insert(bookmark.mapToCodable())
                }
            }
            
            if type == .commaSeparatedText {
                if let content = makeCSV(from: Array(exportableBookmarks)) {
                    onReady(content)
                } else {
                    throw DataError.exportableGenerationError(
                        LocalizedStringKey("Data Manager Error During Generating Exportable Message")
                    )
                }
            } else if type == .json {
                let content = YabaCodableContent(
                    id: UUID().uuidString,
                    exportedFrom: "YABA",
                    collections: exportableCollections,
                    bookmarks: Array(exportableBookmarks)
                )
                
                if let jsonData = try? JSONEncoder().encode(content) {
                    onReady(jsonData)
                } else {
                    throw DataError.exportableGenerationError(
                        LocalizedStringKey("Data Manager Error During Generating Exportable Message")
                    )
                }
            } else if type == .plainText {
                if let markdownContent = makeMarkdown(
                    from: exportableCollections, 
                    with: Array(exportableBookmarks)
                ) {
                    onReady(markdownContent)
                } else {
                    throw DataError.exportableGenerationError(
                        LocalizedStringKey("Data Manager Error During Generating Exportable Message")
                    )
                }
            }
        } else {
            throw DataError.unkownError(LocalizedStringKey("Data Manager Unknown Error Message"))
        }
    }
    
    func deleteAllData(
        using modelContext: ModelContext,
        onFinishCallback: @escaping () -> Void
    ) {
        Task { @MainActor in
            UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
            UNUserNotificationCenter.current().removeAllDeliveredNotifications()
            
            try? YabaDataLogger.shared.logBulkDelete(shouldSave: false)
            
            try? modelContext.delete(model: YabaBookmark.self)
            try? await Task.sleep(for: .seconds(1))
            
            // Thanks to nullify collection on tags...
            let descriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate { _ in true }
            )
            if let collections = try? modelContext.fetch(descriptor) {
                collections.forEach { collection in
                    modelContext.delete(collection)
                }
                try? await Task.sleep(for: .seconds(1))
            }
            
            try? modelContext.save()
            
            await MainActor.run {
                onFinishCallback()
            }
        }
    }
    
    func extractCSVHeaders(from data: Data) -> [String] {
        guard let content = String(data: data, encoding: .utf8) else { return [] }

        let rows = content.components(separatedBy: "\n").filter {
            !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
        
        guard let headerRow = rows.first else { return [] }

        return parseCSVRow(headerRow)
    }
    
    private func makeCSV(from bookmarks: [YabaCodableBookmark]) -> Data? {
        let header = [
            "bookmarkId", "label", "bookmarkDescription", "link", "domain",
            "createdAt", "editedAt", "imageUrl", "iconUrl", "videoUrl", 
            "type", "version"
            // Removed readableHTML to prevent CSV parsing issues
        ]
        
        let rows: [String] = bookmarks.map(toCSVRow(_:))
        let csvString = ([header.joined(separator: ",")] + rows).joined(separator: "\n")
        
        return csvString.data(using: .utf8)
    }
    
    private func makeMarkdown(
        from collections: [YabaCodableCollection], 
        with allBookmarks: [YabaCodableBookmark]
    ) -> Data? {
        var markdownLines: [String] = []
        
        // H1 title - only one H1 as requested
        markdownLines.append("# 📚 YABA")
        markdownLines.append("")
        
        // Create a dictionary to quickly find bookmarks by ID
        var bookmarkDict: [String: YabaCodableBookmark] = [:]
        for bookmark in allBookmarks {
            guard let bookmarkId = bookmark.bookmarkId else { continue }
            bookmarkDict[bookmarkId] = bookmark // This safely overwrites duplicates
        }
        
        // Process each collection as a folder
        for collection in collections {
            if collection.type == CollectionType.tag.rawValue {
                continue
            }
            
            // Find bookmarks for this collection
            let collectionBookmarks = collection.bookmarks.compactMap { bookmarkId in
                bookmarkDict[bookmarkId]
            }
            
            // Skip empty folders
            if collectionBookmarks.isEmpty {
                continue
            }
            
            // H2 for collection name
            markdownLines.append("## 📁 \(collection.label)")
            markdownLines.append("")
            
            // Process each bookmark in this collection
            for bookmark in collectionBookmarks {
                // H3 for bookmark title
                let bookmarkTitle = bookmark.label ?? bookmark.link
                markdownLines.append("### 🔖 \(bookmarkTitle)")
                markdownLines.append("")
                
                // Add the link
                markdownLines.append("🔗 \(bookmark.link)")
                markdownLines.append("")
                
                // Add description if available
                if let description = bookmark.bookmarkDescription, !description.isEmpty {
                    markdownLines.append("📝 \(description)")
                    markdownLines.append("")
                }
            }
        }
        
        // Handle bookmarks that don't belong to any collection
        let bookmarksWithoutCollections = allBookmarks.filter { bookmark in
            !collections.contains { collection in
                collection.bookmarks.contains(bookmark.bookmarkId ?? "")
            }
        }
        
        if !bookmarksWithoutCollections.isEmpty {
            markdownLines.append("## 📂 Uncategorized")
            markdownLines.append("")
            
            for bookmark in bookmarksWithoutCollections {
                // H3 for bookmark title
                let bookmarkTitle = bookmark.label ?? bookmark.link
                markdownLines.append("### 🔖 \(bookmarkTitle)")
                markdownLines.append("")
                
                // Add the link
                markdownLines.append("🔗 \(bookmark.link)")
                markdownLines.append("")
                
                // Add description if available
                if let description = bookmark.bookmarkDescription, !description.isEmpty {
                    markdownLines.append("📝 \(description)")
                    markdownLines.append("")
                }
            }
        }
        
        let markdownString = markdownLines.joined(separator: "\n")
        return markdownString.data(using: .utf8)
    }
    
    private func escapeForCSV(_ field: String) -> String {
        let needsQuotes = field.contains(",") || field.contains("\"") || field.contains("\n")
        var escaped = field.replacingOccurrences(of: "\"", with: "\"\"")
        if needsQuotes {
            escaped = "\"\(escaped)\""
        }
        return escaped
    }
    
    private func toCSVRow(_ bookmark: YabaCodableBookmark) -> String {
        let currentDate = Date.now.ISO8601Format()
        var list = [
            bookmark.bookmarkId ?? UUID().uuidString,
            bookmark.label ?? bookmark.link,
            bookmark.bookmarkDescription ?? "",
            bookmark.link,
            bookmark.domain ?? "",
            bookmark.createdAt ?? currentDate,
            bookmark.editedAt ?? currentDate,
            bookmark.imageUrl ?? "",
            bookmark.iconUrl ?? "",
            bookmark.videoUrl ?? ""
            // Removed readableHTML to prevent CSV parsing issues
        ]
        list.append(String(bookmark.type ?? 1)) // Thanks Swift for not allowing me to put more than 10 items in the list above...
        list.append(String(bookmark.version ?? 0)) // Add missing version column
        return list.map { escapeForCSV($0) }.joined(separator: ",")
    }

    private func parseCSVRow(_ row: String) -> [String] {
        var result: [String] = []
        var field = ""
        var insideQuotes = false
        var i = row.startIndex

        while i < row.endIndex {
            let char = row[i]
            
            if char == "\"" {
                // Check for escaped quotes
                let nextIndex = row.index(after: i)
                if nextIndex < row.endIndex && row[nextIndex] == "\"" && insideQuotes {
                    // This is an escaped quote
                    field.append("\"")
                    i = row.index(after: nextIndex)
                } else {
                    // Toggle quote state
                    insideQuotes.toggle()
                    i = nextIndex
                }
            } else if char == "," && !insideQuotes {
                result.append(field.trimmingCharacters(in: .whitespaces))
                field = ""
                i = row.index(after: i)
            } else {
                field.append(char)
                i = row.index(after: i)
            }
        }
        
        result.append(field.trimmingCharacters(in: .whitespaces))
        return result
    }
    
    // MARK: - Sync Data Operations
    
    /// Prepare local data for synchronization
    func prepareSyncData(
        using modelContext: ModelContext,
        deviceId: String,
        deviceName: String,
        ipAddress: String
    ) throws -> SyncRequest {
        // Fetch all collections
        let collectionsDescriptor = FetchDescriptor<YabaCollection>(
            predicate: #Predicate { _ in true }
        )
        let collections = try modelContext.fetch(collectionsDescriptor)
        
        // Convert collections to codable format
        let codableCollections = collections.map { $0.mapToCodable() }
        
        // Fetch all bookmarks
        var codableBookmarks: Set<YabaCodableBookmark> = []
        collections.forEach { collection in
            collection.bookmarks?.forEach { bookmark in
                codableBookmarks.insert(bookmark.mapToCodable())
            }
        }
        
        return SyncRequest(
            deviceId: deviceId,
            deviceName: deviceName,
            timestamp: ISO8601DateFormatter().string(from: Date()),
            ipAddress: ipAddress,
            bookmarks: Array(codableBookmarks),
            collections: codableCollections
        )
    }
    
    /// Handle incoming sync request and merge data
    func handleIncomingSyncRequest(
        _ request: SyncRequest,
        using modelContext: ModelContext,
        deviceId: String,
        deviceName: String,
        ipAddress: String
    ) async throws -> SyncResponse {
        // Merge incoming data with local data
        let _ = try await mergeIncomingData(request, using: modelContext)
        
        // Prepare response with current data
        let localData = try prepareSyncData(
            using: modelContext,
            deviceId: deviceId,
            deviceName: deviceName,
            ipAddress: ipAddress
        )
        
        return SyncResponse(
            deviceId: deviceId,
            deviceName: deviceName,
            timestamp: ISO8601DateFormatter().string(from: Date()),
            bookmarks: localData.bookmarks,
            collections: localData.collections
        )
    }
    
    /// Merge incoming sync data with local data
    private func mergeIncomingData(
        _ request: SyncRequest,
        using modelContext: ModelContext
    ) async throws -> SyncMergeResult {
        var mergedBookmarks = 0
        var mergedCollections = 0
        var conflicts: [SyncConflict] = []
        
        // Get deletion logs to check for deleted items
        let deletionLogs = try modelContext.fetch(
            .init(predicate: #Predicate<YabaDataLog> { _ in true })
        )
        let deletedBookmarkIds = Set(deletionLogs.filter { $0.entityType == .bookmark }.map { $0.entityId })
        let deletedCollectionIds = Set(deletionLogs.filter { $0.entityType == .collection }.map { $0.entityId })
        
        // Create mapping of incoming bookmarks by ID
        var incomingBookmarkMap: [String: YabaCodableBookmark] = [:]
        for bookmark in request.bookmarks {
            if let bookmarkId = bookmark.bookmarkId {
                incomingBookmarkMap[bookmarkId] = bookmark
            }
        }
        
        // Merge bookmarks using deletion logs and versioning
        for incomingBookmark in request.bookmarks {
            guard let bookmarkId = incomingBookmark.bookmarkId else { continue }
            
            // Skip if we have deleted this bookmark
            if deletedBookmarkIds.contains(bookmarkId) {
                // Check if incoming version is newer than our deletion
                if let deletionLog = deletionLogs.first(where: { $0.entityId == bookmarkId }),
                   let incomingEditDate = parseDate(incomingBookmark.editedAt),
                   incomingEditDate > deletionLog.timestamp {
                    // Incoming is newer than deletion, restore the bookmark
                    let newBookmark = incomingBookmark.mapToModel()
                    await addBookmarkToAppropriateCollection(newBookmark, using: modelContext)
                    mergedBookmarks += 1
                    
                    // Remove deletion log
                    modelContext.delete(deletionLog)
                }
                continue
            }
            
            // Check if bookmark exists locally
            let localDescriptor = FetchDescriptor<YabaBookmark>(
                predicate: #Predicate<YabaBookmark> { bookmark in
                    bookmark.bookmarkId == bookmarkId
                }
            )
            
            if let existingBookmark = try modelContext.fetch(localDescriptor).first {
                // Handle version and timestamp conflict resolution
                let conflictResolution = resolveBookmarkConflict(
                    local: existingBookmark,
                    incoming: incomingBookmark
                )
                
                switch conflictResolution {
                case .keepRemote:
                    updateBookmarkFromCodable(existingBookmark, from: incomingBookmark)
                    mergedBookmarks += 1
                case .keepLocal:
                    conflicts.append(SyncConflict(
                        type: .bookmark,
                        itemId: bookmarkId,
                        localTimestamp: existingBookmark.editedAt,
                        remoteTimestamp: parseDate(incomingBookmark.editedAt) ?? Date.distantPast,
                        resolution: .keepLocal
                    ))
                case .needsManualResolution:
                    conflicts.append(SyncConflict(
                        type: .bookmark,
                        itemId: bookmarkId,
                        localTimestamp: existingBookmark.editedAt,
                        remoteTimestamp: parseDate(incomingBookmark.editedAt) ?? Date.distantPast,
                        resolution: .needsManualResolution
                    ))
                }
            } else {
                // New bookmark - add it
                let newBookmark = incomingBookmark.mapToModel()
                await addBookmarkToAppropriateCollection(newBookmark, using: modelContext)
                mergedBookmarks += 1
            }
        }
        
        // Merge collections
        for incomingCollection in request.collections {
            let collectionId = incomingCollection.collectionId
            
            // Skip if we have deleted this collection
            if deletedCollectionIds.contains(collectionId) {
                if let deletionLog = deletionLogs.first(where: { $0.entityId == collectionId }),
                   let incomingEditDate = parseDate(incomingCollection.editedAt),
                   incomingEditDate > deletionLog.timestamp {
                    // Incoming is newer than deletion, restore the collection
                    let newCollection = incomingCollection.mapToModel()
                    
                    // Add bookmarks to collection
                    for bookmarkId in incomingCollection.bookmarks {
                        if let incomingBookmark = incomingBookmarkMap[bookmarkId] {
                            let bookmarkModel = incomingBookmark.mapToModel()
                            newCollection.bookmarks?.append(bookmarkModel)
                        }
                    }
                    
                    modelContext.insert(newCollection)
                    mergedCollections += 1
                    
                    // Remove deletion log
                    modelContext.delete(deletionLog)
                }
                continue
            }
            
            let localDescriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate<YabaCollection> { collection in
                    collection.collectionId == collectionId
                }
            )
            
            if let existingCollection = try modelContext.fetch(localDescriptor).first {
                // Handle collection conflict
                let conflictResolution = resolveCollectionConflict(
                    local: existingCollection,
                    incoming: incomingCollection
                )
                
                switch conflictResolution {
                case .keepRemote:
                    updateCollectionFromCodable(
                        existingCollection,
                        from: incomingCollection,
                        bookmarkMap: incomingBookmarkMap,
                        using: modelContext
                    )
                    mergedCollections += 1
                case .keepLocal:
                    conflicts.append(SyncConflict(
                        type: .collection,
                        itemId: collectionId,
                        localTimestamp: existingCollection.editedAt,
                        remoteTimestamp: parseDate(incomingCollection.editedAt) ?? Date.distantPast,
                        resolution: .keepLocal
                    ))
                case .needsManualResolution:
                    conflicts.append(SyncConflict(
                        type: .collection,
                        itemId: collectionId,
                        localTimestamp: existingCollection.editedAt,
                        remoteTimestamp: parseDate(incomingCollection.editedAt) ?? Date.distantPast,
                        resolution: .needsManualResolution
                    ))
                }
            } else {
                // New collection - add it
                let newCollection = incomingCollection.mapToModel()
                
                // Add bookmarks to collection
                for bookmarkId in incomingCollection.bookmarks {
                    if let incomingBookmark = incomingBookmarkMap[bookmarkId] {
                        let bookmarkModel = incomingBookmark.mapToModel()
                        newCollection.bookmarks?.append(bookmarkModel)
                    }
                }
                
                modelContext.insert(newCollection)
                mergedCollections += 1
            }
        }
        
        // Save changes
        try modelContext.save()
        
        return SyncMergeResult(
            mergedBookmarks: mergedBookmarks,
            mergedCollections: mergedCollections,
            conflicts: conflicts
        )
    }
    
    // MARK: - Private Sync Helper Methods
    
    /// Resolve bookmark conflict based on version and timestamp
    private func resolveBookmarkConflict(
        local: YabaBookmark,
        incoming: YabaCodableBookmark
    ) -> SyncConflict.ConflictResolution {
        let localVersion = local.version
        let incomingVersion = incoming.version ?? 0
        
        // Compare versions first
        if incomingVersion > localVersion {
            return .keepRemote
        } else if localVersion > incomingVersion {
            return .keepLocal
        }
        
        // Same version - compare timestamps
        let localTimestamp = local.editedAt
        let incomingTimestamp = parseDate(incoming.editedAt) ?? Date.distantPast
        
        if incomingTimestamp > localTimestamp {
            return .keepRemote
        } else if localTimestamp > incomingTimestamp {
            return .keepLocal
        }
        
        // Same timestamp and version - check content differences
        if bookmarksAreEqual(local, incoming) {
            return .keepLocal // No changes needed
        }
        
        // Content differs with same timestamp/version - manual resolution needed
        return .needsManualResolution
    }
    
    /// Resolve collection conflict based on version and timestamp
    private func resolveCollectionConflict(
        local: YabaCollection,
        incoming: YabaCodableCollection
    ) -> SyncConflict.ConflictResolution {
        let localVersion = local.version
        let incomingVersion = incoming.version
        
        // Compare versions first
        if incomingVersion > localVersion {
            return .keepRemote
        } else if localVersion > incomingVersion {
            return .keepLocal
        }
        
        // Same version - compare timestamps
        let localTimestamp = local.editedAt
        let incomingTimestamp = parseDate(incoming.editedAt) ?? Date.distantPast
        
        if incomingTimestamp > localTimestamp {
            return .keepRemote
        } else if localTimestamp > incomingTimestamp {
            return .keepLocal
        }
        
        // Same timestamp and version - check content differences
        if collectionsAreEqual(local, incoming) {
            return .keepLocal // No changes needed
        }
        
        // Content differs with same timestamp/version - manual resolution needed
        return .needsManualResolution
    }
    
    private func parseDate(_ dateString: String?) -> Date? {
        guard let dateString = dateString else { return nil }
        return ISO8601DateFormatter().date(from: dateString)
    }
    
    private func bookmarksAreEqual(
        _ local: YabaBookmark,
        _ remote: YabaCodableBookmark
    ) -> Bool {
        return local.link == remote.link &&
               local.label == remote.label &&
               local.bookmarkDescription == remote.bookmarkDescription
    }
    
    private func collectionsAreEqual(
        _ local: YabaCollection,
        _ remote: YabaCodableCollection
    ) -> Bool {
        return local.label == remote.label &&
               local.icon == remote.icon &&
               local.color.rawValue == remote.color
    }
    
    private func updateBookmarkFromCodable(
        _ bookmark: YabaBookmark,
        from codable: YabaCodableBookmark
    ) {
        bookmark.label = codable.label ?? bookmark.label
        bookmark.bookmarkDescription = codable.bookmarkDescription ?? bookmark.bookmarkDescription
        bookmark.link = codable.link
        bookmark.domain = codable.domain ?? bookmark.domain
        bookmark.imageUrl = codable.imageUrl
        bookmark.iconUrl = codable.iconUrl
        bookmark.videoUrl = codable.videoUrl
        bookmark.readableHTML = codable.readableHTML
        bookmark.version = codable.version ?? bookmark.version
        
        if let editedAtString = codable.editedAt,
           let editedAt = parseDate(editedAtString) {
            bookmark.editedAt = editedAt
        }
    }
    
    private func updateCollectionFromCodable(
        _ collection: YabaCollection,
        from codable: YabaCodableCollection,
        bookmarkMap: [String: YabaCodableBookmark],
        using modelContext: ModelContext
    ) {
        collection.label = codable.label
        collection.icon = codable.icon
        collection.color = YabaColor(rawValue: codable.color) ?? .none
        collection.version = codable.version
        
        if let editedAt = parseDate(codable.editedAt) {
            collection.editedAt = editedAt
        }
        
        // Update bookmark associations
        collection.bookmarks?.removeAll()
        for bookmarkId in codable.bookmarks {
            // Find existing bookmark or create from incoming data
            let descriptor = FetchDescriptor<YabaBookmark>(
                predicate: #Predicate<YabaBookmark> { bookmark in
                    bookmark.bookmarkId == bookmarkId
                }
            )
            
            if let existingBookmark = try? modelContext.fetch(descriptor).first {
                collection.bookmarks?.append(existingBookmark)
            } else if let incomingBookmark = bookmarkMap[bookmarkId] {
                let bookmarkModel = incomingBookmark.mapToModel()
                modelContext.insert(bookmarkModel)
                collection.bookmarks?.append(bookmarkModel)
            }
        }
    }
    
    private func addBookmarkToAppropriateCollection(
        _ bookmark: YabaBookmark,
        using modelContext: ModelContext
    ) async {
        // Try to find uncategorized collection
        let uncategorizedId = Constants.uncategorizedCollectionId
        let descriptor = FetchDescriptor<YabaCollection>(
            predicate: #Predicate<YabaCollection> { collection in
                collection.collectionId == uncategorizedId
            }
        )
        
        if let uncategorizedCollection = try? modelContext.fetch(descriptor).first {
            uncategorizedCollection.bookmarks?.append(bookmark)
        } else {
            // Create uncategorized collection
            let creationTime = Date.now
            let uncategorizedCollection = YabaCollection(
                collectionId: uncategorizedId,
                label: Constants.uncategorizedCollectionLabelKey,
                icon: "folder-01",
                createdAt: creationTime,
                editedAt: creationTime,
                color: .none,
                type: .folder,
                version: 0
            )
            uncategorizedCollection.bookmarks?.append(bookmark)
            modelContext.insert(uncategorizedCollection)
        }
        
        modelContext.insert(bookmark)
    }
}

// HTML Parser class to handle HTML parsing with relaxed format support
private class HTMLParser {
    struct ParsedBookmark {
        let title: String
        let url: String
    }
    
    struct ParsedFolder {
        let label: String
        var bookmarks: [ParsedBookmark]
    }
    
    func parse(_ htmlString: String) throws -> [ParsedFolder] {
        var folders: [ParsedFolder] = []
        var currentFolder: ParsedFolder?
        var allBookmarks: [ParsedBookmark] = []
        
        // Normalize the HTML string for better parsing
        let normalizedHTML = normalizeHTML(htmlString)
        
        // Use regex to find all DT elements (folders and bookmarks)
        let dtPattern = #"<DT>.*?(?=<DT>|$)"#
        let regex = try NSRegularExpression(pattern: dtPattern, options: [.caseInsensitive, .dotMatchesLineSeparators])
        let matches = regex.matches(in: normalizedHTML, range: NSRange(normalizedHTML.startIndex..., in: normalizedHTML))
        
        for match in matches {
            guard let range = Range(match.range, in: normalizedHTML) else { continue }
            let dtElement = String(normalizedHTML[range]).trimmingCharacters(in: .whitespaces)
            
            // Skip empty elements
            if dtElement.isEmpty { continue }
            
            // Check if this is a folder (H3 tag) but not a document title
            if let folderName = extractFolderName(from: dtElement), !isDocumentTitle(dtElement) {
                // Save current folder if it exists and has bookmarks
                if let folder = currentFolder, !folder.bookmarks.isEmpty {
                    folders.append(folder)
                }
                
                // Create new folder
                currentFolder = ParsedFolder(label: folderName, bookmarks: [])
                continue
            }
            
            // Check if this is a bookmark
            if let bookmark = parseBookmark(from: dtElement) {
                // Add to current folder if exists, otherwise collect all bookmarks
                if currentFolder != nil {
                    currentFolder?.bookmarks.append(bookmark)
                } else {
                    allBookmarks.append(bookmark)
                }
            }
        }
        
        // Add the last folder if exists and has bookmarks
        if let folder = currentFolder, !folder.bookmarks.isEmpty {
            folders.append(folder)
        }
        
        // If we found bookmarks but no folders, create an uncategorized folder
        if folders.isEmpty && !allBookmarks.isEmpty {
            folders.append(ParsedFolder(label: "-1", bookmarks: allBookmarks))
        } else if !allBookmarks.isEmpty {
            // If we have both folders and loose bookmarks, add loose ones to uncategorized
            folders.append(ParsedFolder(label: "-1", bookmarks: allBookmarks))
        }
        
        return folders
    }
    
    private func normalizeHTML(_ html: String) -> String {
        // Make case-insensitive and normalize common variations
        return html
            .replacingOccurrences(of: "<dt>", with: "<DT>")
            .replacingOccurrences(of: "<DT >", with: "<DT>")
            .replacingOccurrences(of: "<h3>", with: "<H3>")
            .replacingOccurrences(of: "<H3 >", with: "<H3>")
            .replacingOccurrences(of: "</h3>", with: "</H3>")
            .replacingOccurrences(of: "<a ", with: "<A ")
            .replacingOccurrences(of: "<A  ", with: "<A ")
            .replacingOccurrences(of: "href=", with: "HREF=")
            .replacingOccurrences(of: "HREF =", with: "HREF=")
            .replacingOccurrences(of: "</a>", with: "</A>")
            .replacingOccurrences(of: "<dl>", with: "<DL>")
            .replacingOccurrences(of: "</dl>", with: "</DL>")
            .replacingOccurrences(of: "<dd>", with: "<DD>")
            .replacingOccurrences(of: "</dd>", with: "</DD>")
            .replacingOccurrences(of: "<p>", with: "<P>")
            .replacingOccurrences(of: "</p>", with: "</P>")
    }
    
    private func extractFolderName(from line: String) -> String? {
        // Multiple patterns for folder detection (more focused on H3 tags)
        let patterns = [
            #"<DT><H3[^>]*>([^<]+)</H3>"#,           // Standard Netscape format
            #"<H3[^>]*>([^<]+)</H3>"#,               // H3 without DT (but not H1)
            #"<folder[^>]*>([^<]+)</folder>"#         // Some custom formats
        ]
        
        for pattern in patterns {
            if let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive),
               let match = regex.firstMatch(in: line, range: NSRange(line.startIndex..., in: line)),
               let range = Range(match.range(at: 1), in: line) {
                let folderName = String(line[range])
                    .trimmingCharacters(in: .whitespaces)
                
                // Skip empty folder names
                if !folderName.isEmpty {
                    return decodeHTMLEntities(folderName)
                }
            }
        }
        
        return nil
    }
    
    private func isDocumentTitle(_ line: String) -> Bool {
        // Skip H1 tags and TITLE tags as they're usually document titles, not folders
        let titlePatterns = [
            #"<H1[^>]*>"#,
            #"<TITLE[^>]*>"#,
            #"<title[^>]*>"#
        ]
        
        for pattern in titlePatterns {
            if let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive),
               regex.firstMatch(in: line, range: NSRange(line.startIndex..., in: line)) != nil {
                return true
            }
        }
        
        return false
    }
    
    private func parseBookmark(from line: String) -> ParsedBookmark? {
        // Try to extract HREF and title using a more robust approach
        // First, find the HREF attribute value
        guard let hrefValue = extractHrefValue(from: line) else {
            return nil
        }
        
        // Then extract the title between > and </A>
        guard let title = extractBookmarkTitle(from: line) else {
            return nil
        }
        
        // Validate URL format
        guard !hrefValue.isEmpty && (hrefValue.hasPrefix("http") || hrefValue.hasPrefix("ftp") || hrefValue.hasPrefix("javascript")),
              !title.isEmpty else {
            return nil
        }
        
        return ParsedBookmark(
            title: decodeHTMLEntities(title),
            url: hrefValue
        )
    }
    
    private func extractHrefValue(from line: String) -> String? {
        // Multiple patterns to find HREF value, ordered by specificity
        let hrefPatterns = [
            #"HREF=\"([^\"]+)\""#,      // HREF="url"
            #"HREF='([^']+)'"#,         // HREF='url' 
            #"HREF=([^\s>]+)"#          // HREF=url (no quotes)
        ]
        
        for pattern in hrefPatterns {
            if let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive),
               let match = regex.firstMatch(in: line, range: NSRange(line.startIndex..., in: line)),
               let range = Range(match.range(at: 1), in: line) {
                return String(line[range]).trimmingCharacters(in: .whitespaces)
            }
        }
        
        return nil
    }
    
    private func extractBookmarkTitle(from line: String) -> String? {
        // Extract text between the last > and </A>
        let pattern = #">([^<]+)</A>"#
        
        if let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive),
           let match = regex.firstMatch(in: line, range: NSRange(line.startIndex..., in: line)),
           let range = Range(match.range(at: 1), in: line) {
            return String(line[range]).trimmingCharacters(in: .whitespaces)
        }
        
        return nil
    }
    
    private func decodeHTMLEntities(_ text: String) -> String {
        return text
            .replacingOccurrences(of: "&amp;", with: "&")
            .replacingOccurrences(of: "&lt;", with: "<")
            .replacingOccurrences(of: "&gt;", with: ">")
            .replacingOccurrences(of: "&quot;", with: "\"")
            .replacingOccurrences(of: "&#39;", with: "'")
            .replacingOccurrences(of: "&apos;", with: "'")
            .replacingOccurrences(of: "&nbsp;", with: " ")
    }
}
