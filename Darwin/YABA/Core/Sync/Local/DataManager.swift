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
        // Update editedAt and version to prevent deleteAll conflicts
        let now = Date.now
        mappedBookmarks.values.forEach { bookmark in
            bookmark.editedAt = now
            bookmark.version += 1
            modelContext.insert(bookmark)
        }
        
        if let collections = content.collections {
            // Create a mapping of collection IDs to collection models
            var mappedCollections: [String: YabaCollection] = [:]
            var usedCollectionIds: Set<String> = []
            
            // First pass: Create all collections without relationships
            for codableCollection in collections {
                var collectionId = codableCollection.collectionId
                
                // Handle duplicate IDs by generating new ones
                while usedCollectionIds.contains(collectionId) {
                    collectionId = UUID().uuidString
                }
                usedCollectionIds.insert(collectionId)
                
                let collectionModel = codableCollection.mapToModel()
                // Ensure the collection has the correct ID
                collectionModel.collectionId = collectionId
                // Update editedAt and version to prevent deleteAll conflicts
                collectionModel.editedAt = now
                collectionModel.version += 1
                
                mappedCollections[collectionId] = collectionModel
                modelContext.insert(collectionModel)
            }
            
            // Second pass: Establish parent/children relationships
            for collectionModel in mappedCollections.values {
                // Find the corresponding codable collection by matching IDs
                guard let codableCollection = collections.first(where: { $0.collectionId == collectionModel.collectionId }) else {
                    continue
                }
                
                // Set parent relationship
                if let parentId = codableCollection.parent,
                   let parentCollection = mappedCollections[parentId] {
                    collectionModel.parent = parentCollection
                }
                
                // Set children relationships
                for childId in codableCollection.children {
                    if let childCollection = mappedCollections[childId] {
                        collectionModel.children.append(childCollection)
                    }
                }
                
                // Set bookmark relationships
                codableCollection.bookmarks.forEach { bookmarkId in
                    if let bookmarkModel = mappedBookmarks[bookmarkId] {
                        collectionModel.bookmarks?.append(bookmarkModel)
                    }
                }
            }
        } else {
            let creationTime = Date.now
            let dummyFolder = YabaCollection(
                collectionId: UUID().uuidString,
                label: "\(Date.now.formatted(date: .abbreviated, time: .shortened))",
                icon: "folder-01",
                createdAt: creationTime,
                editedAt: now, // Use now to prevent deleteAll conflicts
                color: .none,
                type: .folder,
                version: 1 // Start with version 1 for import
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
            editedAt: creationTime, // Already using current time
            color: .none,
            type: .folder,
            version: 1 // Start with version 1 for import
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
                imageData: nil,
                iconData: nil
            ).mapToModel()
            
            // Update editedAt and version to prevent deleteAll conflicts
            bookmark.editedAt = creationTime
            bookmark.version += 1
            
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
            editedAt: now, // Already using current time
            color: .none,
            type: .folder,
            version: 1 // Start with version 1 for import
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
                imageData: nil,
                iconData: nil,
            ).mapToModel()
            
            // Update editedAt and version to prevent deleteAll conflicts
            bookmark.editedAt = now
            bookmark.version += 1
            
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

        // Parse HTML into a hierarchical folder/bookmark tree
        let importer = HTMLBookmarksImporter()
        let parsed = importer.parse(htmlString)

        // Ensure there is something to import
        if parsed.folders.isEmpty && parsed.rootBookmarks.isEmpty {
            throw DataError.emptyHTML("Data Manager Empty HTML Message")
        }

        let creationTime = Date.now
        var orderCounter = 0

        // Helper to create collections recursively and return the created collection
        func createCollection(from node: HTMLBookmarksImporter.FolderNode, parent: YabaCollection?) -> YabaCollection {
            let collection = YabaCollection(
                collectionId: UUID().uuidString,
                label: node.label,
                icon: "folder-01",
                createdAt: creationTime,
                editedAt: creationTime,
                color: .none,
                type: .folder,
                version: 1,
                parent: parent,
                children: [],
                order: orderCounter
            )

            // Increment order for the next created collection
            orderCounter += 1

            // Add bookmarks to this collection
            for bookmark in node.bookmarks {
                let model = YabaBookmark(
                    bookmarkId: UUID().uuidString,
                    link: bookmark.url,
                    label: bookmark.title.isEmpty ? bookmark.url : bookmark.title,
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
                    version: 1
                )
                // Ensure the bookmark is inserted into the context before linking
                modelContext.insert(model)
                collection.bookmarks?.append(model)
            }

            // Insert collection before creating its children (SwiftData relationship-friendly)
            modelContext.insert(collection)

            // Recursively create child collections and attach them
            for child in node.children {
                let childCollection = createCollection(from: child, parent: collection)
                collection.children.append(childCollection)
            }

            return collection
        }

        // Create all top-level collections
        for folder in parsed.folders {
            _ = createCollection(from: folder, parent: nil)
        }

        // If there are root-level bookmarks without a folder, group them under a timestamped folder
        if !parsed.rootBookmarks.isEmpty {
            let uncategorized = YabaCollection(
                collectionId: UUID().uuidString,
                label: "\(creationTime.formatted(date: .abbreviated, time: .shortened))",
                icon: "folder-01",
                createdAt: creationTime,
                editedAt: creationTime,
                color: .none,
                type: .folder,
                version: 1,
                parent: nil,
                children: [],
                order: orderCounter
            )

            orderCounter += 1

            for bookmark in parsed.rootBookmarks {
                let model = YabaBookmark(
                    bookmarkId: UUID().uuidString,
                    link: bookmark.url,
                    label: bookmark.title.isEmpty ? bookmark.url : bookmark.title,
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
                    version: 1
                )
                // Ensure the bookmark is inserted into the context before linking
                modelContext.insert(model)
                uncategorized.bookmarks?.append(model)
            }

            modelContext.insert(uncategorized)
        }

        do {
            try modelContext.save()
        } catch {
            print("LELE")
            print(error.localizedDescription)
            throw error
        }
    }

    // New HTML importer implemented below
    
    func prepareExportableData(
        using modelContext: ModelContext,
        withType type: UTType,
        onReady: @escaping (Data) -> Void
    ) throws {
        let descriptor = FetchDescriptor<YabaCollection>(
            predicate: #Predicate { _ in true },
            sortBy: [SortDescriptor(\.order)]
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
            } else if type == .html {
                if let htmlContent = makeHTML(
                    from: exportableCollections,
                    with: Array(exportableBookmarks)
                ) {
                    onReady(htmlContent)
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
    
    private func makeHTML(
        from collections: [YabaCodableCollection],
        with allBookmarks: [YabaCodableBookmark]
    ) -> Data? {
        var htmlLines: [String] = []
        
        // HTML document header
        htmlLines.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
        htmlLines.append("<!-- This is an automatically generated file.")
        htmlLines.append("     It will be read and overwritten.")
        htmlLines.append("     DO NOT EDIT! -->")
        htmlLines.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
        htmlLines.append("<TITLE>Bookmarks</TITLE>")
        htmlLines.append("<H1>Bookmarks</H1>")
        htmlLines.append("")
        htmlLines.append("<DL><p>")
        
        // Create a dictionary to quickly find bookmarks by ID
        var bookmarkDict: [String: YabaCodableBookmark] = [:]
        for bookmark in allBookmarks {
            guard let bookmarkId = bookmark.bookmarkId else { continue }
            bookmarkDict[bookmarkId] = bookmark
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
            
            // Generate folder structure (include empty folders to preserve structure)
            generateHTMLFolder(collection: collection, bookmarks: collectionBookmarks, htmlLines: &htmlLines)
        }
        
        // Handle bookmarks that don't belong to any collection
        let bookmarksWithoutCollections = allBookmarks.filter { bookmark in
            !collections.contains { collection in
                collection.bookmarks.contains(bookmark.bookmarkId ?? "")
            }
        }
        
        if !bookmarksWithoutCollections.isEmpty {
            // Create an "Uncategorized" folder for orphaned bookmarks
            htmlLines.append("    <DT><H3>Uncategorized</H3>")
            htmlLines.append("    <DL><p>")
            
            for bookmark in bookmarksWithoutCollections {
                let bookmarkTitle = escapeHTML(bookmark.label ?? bookmark.link)
                let bookmarkUrl = bookmark.link
                htmlLines.append("        <DT><A HREF=\"\(bookmarkUrl)\">\(bookmarkTitle)</A>")
            }
            
            htmlLines.append("    </DL><p>")
        }
        
        // Close the main DL
        htmlLines.append("</DL><p>")
        
        let htmlString = htmlLines.joined(separator: "\n")
        return htmlString.data(using: .utf8)
    }
    
    private func generateHTMLFolder(
        collection: YabaCodableCollection,
        bookmarks: [YabaCodableBookmark],
        htmlLines: inout [String]
    ) {
        let folderName = escapeHTML(collection.label)
        
        htmlLines.append("    <DT><H3>\(folderName)</H3>")
        htmlLines.append("    <DL><p>")
        
        // Add bookmarks in this folder
        for bookmark in bookmarks {
            let bookmarkTitle = escapeHTML(bookmark.label ?? bookmark.link)
            let bookmarkUrl = bookmark.link
            htmlLines.append("        <DT><A HREF=\"\(bookmarkUrl)\">\(bookmarkTitle)</A>")
        }
        
        // Close this folder
        htmlLines.append("    </DL><p>")
    }
    
    private func escapeHTML(_ text: String) -> String {
        return text
            .replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
            .replacingOccurrences(of: "\"", with: "&quot;")
            .replacingOccurrences(of: "'", with: "&#39;")
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
            predicate: #Predicate { _ in true },
            sortBy: [SortDescriptor(\.order)]
        )
        let collections = try modelContext.fetch(collectionsDescriptor)
        
        // Convert collections to codable format
        let codableCollections = collections.map { $0.mapToCodable() }
        
        // Fetch all bookmarks
        var codableBookmarks: Set<YabaCodableBookmark> = []
        collections.forEach { collection in
            collection.bookmarks?.forEach { bookmark in
                codableBookmarks.insert(bookmark.mapToCodableForSync())
            }
        }
        
        // Fetch deletion logs
        let deletionLogsDescriptor = FetchDescriptor<YabaDataLog>(
            predicate: #Predicate { _ in true }
        )
        let deletionLogs = try modelContext.fetch(deletionLogsDescriptor)
        
        // Convert deletion logs to codable format
        let codableDeletionLogs = deletionLogs.map { log in
            YabaCodableDeletionLog(
                entityId: log.entityId,
                entityType: log.entityType.rawValue,
                timestamp: ISO8601DateFormatter().string(from: log.timestamp)
            )
        }
        
        return SyncRequest(
            deviceId: deviceId,
            deviceName: deviceName,
            timestamp: ISO8601DateFormatter().string(from: Date()),
            ipAddress: ipAddress,
            bookmarks: Array(codableBookmarks),
            collections: codableCollections,
            deletionLogs: codableDeletionLogs
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
        // Prepare response with current data (this will use mapToCodableForSync)
        let localData = try prepareSyncData(
            using: modelContext,
            deviceId: deviceId,
            deviceName: deviceName,
            ipAddress: ipAddress
        )
        
        // Merge incoming data with local data
        let _ = try await mergeIncomingData(request, using: modelContext)
        
        return SyncResponse(
            deviceId: deviceId,
            deviceName: deviceName,
            timestamp: ISO8601DateFormatter().string(from: Date()),
            bookmarks: localData.bookmarks,
            collections: localData.collections,
            deletionLogs: localData.deletionLogs
        )
    }
    
    /// Merge incoming sync data with local data using simplified logic
    private func mergeIncomingData(
        _ request: SyncRequest,
        using modelContext: ModelContext
    ) async throws -> SyncMergeResult {
        var mergedBookmarks = 0
        var mergedCollections = 0
        
        // Check if deletion sync is prevented
        let preventDeletionSync = UserDefaults.standard.bool(forKey: Constants.preventDeletionSyncKey)
        
        // STEP 0: Process "deleteAll" deletion logs FIRST (before merging any content)
        // Only process incoming deleteAll logs if deletion sync is not prevented
        if !preventDeletionSync {
            let incomingDeleteAllLogs = request.deletionLogs.filter { $0.entityType == "deleteAll" || $0.entityType == "all" }
            
            if !incomingDeleteAllLogs.isEmpty {
                
                // Get our local deleteAll logs
                let localDeleteLogsDescriptor = FetchDescriptor<YabaDataLog>(
                    predicate: #Predicate<YabaDataLog> { _ in true}
                )
                let localDeleteLogs = try modelContext.fetch(localDeleteLogsDescriptor)
                let localDeleteAllLogs = localDeleteLogs.filter { log in log.actionType == .deletedAll }
                
                // Find the newest incoming deleteAll
                let newestIncomingDeleteAll = incomingDeleteAllLogs.max { log1, log2 in
                    guard let date1 = parseDate(log1.timestamp),
                          let date2 = parseDate(log2.timestamp) else { return false }
                    return date1 < date2
                }
                
                if let newestIncoming = newestIncomingDeleteAll,
                   let incomingDeleteAllTimestamp = parseDate(newestIncoming.timestamp) {
                    
                    // Find our newest local deleteAll
                    let newestLocalDeleteAll = localDeleteAllLogs.max { $0.timestamp < $1.timestamp }
                    let localDeleteAllTimestamp = newestLocalDeleteAll?.timestamp ?? Date.distantPast
                    
                    // If remote deleteAll is newer than our local deleteAll, apply it
                    if incomingDeleteAllTimestamp > localDeleteAllTimestamp {
                        // Add a 3-second safety buffer to prevent deleting recently imported content
                        let deleteAllWithBuffer = incomingDeleteAllTimestamp.addingTimeInterval(-3)
                        
                        // Delete ALL collections and bookmarks created before the deleteAll timestamp (with buffer)
                        let collectionsToDeleteDescriptor = FetchDescriptor<YabaCollection>(
                            predicate: #Predicate<YabaCollection> { collection in
                                collection.editedAt < deleteAllWithBuffer
                            }
                        )
                        let collectionsToDelete = try modelContext.fetch(collectionsToDeleteDescriptor)
                        collectionsToDelete.forEach { modelContext.delete($0) }
                        
                        let bookmarksToDeleteDescriptor = FetchDescriptor<YabaBookmark>(
                            predicate: #Predicate<YabaBookmark> { bookmark in
                                bookmark.editedAt < deleteAllWithBuffer
                            }
                        )
                        let bookmarksToDelete = try modelContext.fetch(bookmarksToDeleteDescriptor)
                        bookmarksToDelete.forEach { modelContext.delete($0) }
                        
                        // Remove old local deleteAll logs and add the new one
                        localDeleteAllLogs.forEach { modelContext.delete($0) }
                        
                        // Create and insert the new deleteAll log in our local database
                        let newDeleteAllLog = YabaDataLog(
                            entityId: UUID().uuidString,
                            entityType: .all,
                            actionType: .deletedAll,
                            timestamp: incomingDeleteAllTimestamp
                        )
                        modelContext.insert(newDeleteAllLog)
                    }
                }
            }
        } // End of !preventDeletionSync check for deleteAll logs
        
        // Get local deletion logs to check against incoming items (only if deletion sync is not prevented)
        var localCollectionDeletionIds: Set<String> = []
        var localBookmarkDeletionIds: Set<String> = []
        
        if !preventDeletionSync {
            let allLocalDeletionLogsDescriptor = FetchDescriptor<YabaDataLog>(
                predicate: #Predicate<YabaDataLog> { _ in true }
            )
            let allLocalDeletionLogs = try modelContext.fetch(allLocalDeletionLogsDescriptor)
            localCollectionDeletionIds = Set(allLocalDeletionLogs.filter { $0.entityType == .collection }.map { $0.entityId })
            localBookmarkDeletionIds = Set(allLocalDeletionLogs.filter { $0.entityType == .bookmark }.map { $0.entityId })
        }
        
        // STEP 1: Create and insert all collections first (following SwiftData best practices)
        for incomingCollection in request.collections {
            let collectionId = incomingCollection.collectionId
            
            // Skip if we have a local deletion log for this collection
            if localCollectionDeletionIds.contains(collectionId) {
                continue
            }
            
            let localDescriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate<YabaCollection> { collection in
                    collection.collectionId == collectionId
                }
            )
            
            if let existingCollection = try modelContext.fetch(localDescriptor).first {
                // Check if incoming should overwrite local (version first, then timestamp)
                if shouldOverwrite(
                    localVersion: existingCollection.version,
                    localEditedAt: existingCollection.editedAt,
                    incomingVersion: incomingCollection.version,
                    incomingEditedAt: parseDate(incomingCollection.editedAt)
                ) {
                    // Update existing collection metadata only (no relationships yet)
                    updateCollectionMetadataFromCodable(existingCollection, from: incomingCollection)
                    mergedCollections += 1
                }
            } else {
                // New collection - create and insert it (no relationships yet)
                let newCollection = incomingCollection.mapToModel()
                // Clear any relationships that might have been set during mapping
                newCollection.bookmarks?.removeAll()
                modelContext.insert(newCollection)
                mergedCollections += 1
            }
        }
        
        // STEP 2: Create and insert all bookmarks (following SwiftData best practices)
        for incomingBookmark in request.bookmarks {
            guard let bookmarkId = incomingBookmark.bookmarkId else { continue }
            
            // Skip if we have a local deletion log for this bookmark
            if localBookmarkDeletionIds.contains(bookmarkId) {
                continue
            }
            
            let localDescriptor = FetchDescriptor<YabaBookmark>(
                predicate: #Predicate<YabaBookmark> { bookmark in
                    bookmark.bookmarkId == bookmarkId
                }
            )
            
            if let existingBookmark = try modelContext.fetch(localDescriptor).first {
                // Check if incoming should overwrite local (version first, then timestamp)
                if shouldOverwrite(
                    localVersion: existingBookmark.version,
                    localEditedAt: existingBookmark.editedAt,
                    incomingVersion: incomingBookmark.version ?? 0,
                    incomingEditedAt: parseDate(incomingBookmark.editedAt)
                ) {
                    // Update existing bookmark with incoming data
                    updateBookmarkFromCodable(existingBookmark, from: incomingBookmark)
                    mergedBookmarks += 1
                }
            } else {
                // New bookmark - create and insert it WITHOUT relationships
                let newBookmark = incomingBookmark.mapToModel()
                modelContext.insert(newBookmark)
                mergedBookmarks += 1
            }
        }
        
        // STEP 3: NOW manipulate relationships after all objects are inserted (SwiftData requirement)
        for incomingCollection in request.collections {
            let collectionId = incomingCollection.collectionId
            
            // Skip if we have a local deletion log for this collection (only if deletion sync is not prevented)
            if !preventDeletionSync && localCollectionDeletionIds.contains(collectionId) {
                continue
            }
            
            let collectionDescriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate<YabaCollection> { collection in
                    collection.collectionId == collectionId
                }
            )
            
            if let localCollection = try? modelContext.fetch(collectionDescriptor).first {
                // Check if incoming collection should set up relationships
                // This includes both "new" collections and collections that should overwrite local ones
                let shouldSetupRelationships = shouldOverwrite(
                    localVersion: localCollection.version,
                    localEditedAt: localCollection.editedAt,
                    incomingVersion: incomingCollection.version,
                    incomingEditedAt: parseDate(incomingCollection.editedAt)
                ) || localCollection.bookmarks?.isEmpty == true // Also set up if local has no bookmarks
                
                if shouldSetupRelationships {
                    // Clear existing relationships
                    localCollection.bookmarks?.removeAll()
                    localCollection.children.removeAll()
                    localCollection.parent = nil

                    // Add bookmarks from incoming data
                    for bookmarkId in incomingCollection.bookmarks {
                        let bookmarkDescriptor = FetchDescriptor<YabaBookmark>(
                            predicate: #Predicate<YabaBookmark> { bookmark in
                                bookmark.bookmarkId == bookmarkId
                            }
                        )
                        if let bookmark = try? modelContext.fetch(bookmarkDescriptor).first {
                            localCollection.bookmarks?.append(bookmark)
                        }
                    }

                    // Add parent relationship from incoming data
                    if let parentId = incomingCollection.parent {
                        let parentDescriptor = FetchDescriptor<YabaCollection>(
                            predicate: #Predicate<YabaCollection> { collection in
                                collection.collectionId == parentId
                            }
                        )
                        if let parentCollection = try? modelContext.fetch(parentDescriptor).first {
                            localCollection.parent = parentCollection
                        }
                    }

                    // Add children relationships from incoming data
                    for childId in incomingCollection.children {
                        let childDescriptor = FetchDescriptor<YabaCollection>(
                            predicate: #Predicate<YabaCollection> { collection in
                                collection.collectionId == childId
                            }
                        )
                        if let childCollection = try? modelContext.fetch(childDescriptor).first {
                            localCollection.children.append(childCollection)
                        }
                    }
                }
            }
        }
        
        // STEP 4: Handle orphaned bookmarks (bookmarks not in any collection)
        let allBookmarksDescriptor = FetchDescriptor<YabaBookmark>(predicate: #Predicate<YabaBookmark> { _ in true })
        let allBookmarks = try modelContext.fetch(allBookmarksDescriptor)
        
        let allCollectionsDescriptor = FetchDescriptor<YabaCollection>(predicate: #Predicate<YabaCollection> { _ in true })
        let allCollections = try modelContext.fetch(allCollectionsDescriptor)
        
        // Find bookmarks that are not in any collection
        for bookmark in allBookmarks {
            let isInAnyCollection = allCollections.contains { collection in
                collection.bookmarks?.contains(where: { $0.bookmarkId == bookmark.bookmarkId }) == true
            }
            
            if !isInAnyCollection {
                // Add to uncategorized collection
                await addBookmarkToUncategorizedCollection(bookmark, using: modelContext)
            }
        }
        
        // STEP 5: Process individual deletion logs (collection and bookmark deletions)
        // Only process incoming deletion logs if deletion sync is not prevented
        if !preventDeletionSync {
            let individualDeletionLogs = request.deletionLogs.filter {
                $0.entityType != "deleteAll" && $0.entityType != "all"
            }
            
            if !individualDeletionLogs.isEmpty {
                
                for deletionLog in individualDeletionLogs {
                    if deletionLog.entityType == "collection" {
                        let descriptor = FetchDescriptor<YabaCollection>(
                            predicate: #Predicate<YabaCollection> { collection in
                                collection.collectionId == deletionLog.entityId
                            }
                        )
                        if let collectionToDelete = try modelContext.fetch(descriptor).first {
                            modelContext.delete(collectionToDelete)
                        }
                    } else if deletionLog.entityType == "bookmark" {
                        let descriptor = FetchDescriptor<YabaBookmark>(
                            predicate: #Predicate<YabaBookmark> { bookmark in
                                bookmark.bookmarkId == deletionLog.entityId
                            }
                        )
                        if let bookmarkToDelete = try modelContext.fetch(descriptor).first {
                            modelContext.delete(bookmarkToDelete)
                        }
                    }
                }
            }
        } // End of !preventDeletionSync check
        
        // Save all changes
        try modelContext.save()
        
        return SyncMergeResult(
            mergedBookmarks: mergedBookmarks,
            mergedCollections: mergedCollections,
            conflicts: [] // No conflicts in simplified logic
        )
    }
    
    // MARK: - Private Sync Helper Methods
    
    /// Simple logic to determine if incoming data should overwrite local data
    private func shouldOverwrite(
        localVersion: Int,
        localEditedAt: Date,
        incomingVersion: Int,
        incomingEditedAt: Date?
    ) -> Bool {
        // First check versions - higher version wins
        if incomingVersion > localVersion {
            return true
        } else if localVersion > incomingVersion {
            return false
        }
        
        // Same version - check timestamps
        guard let incomingEditedAt = incomingEditedAt else { return false }
        return incomingEditedAt > localEditedAt
    }
    
    private func parseDate(_ dateString: String?) -> Date? {
        guard let dateString = dateString else { return nil }
        return ISO8601DateFormatter().date(from: dateString)
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
        
        // Update image data if available from sync
        if let imageData = codable.imageData {
            bookmark.imageDataHolder = imageData
        }
        if let iconData = codable.iconData {
            bookmark.iconDataHolder = iconData
        }
        
        if let editedAtString = codable.editedAt,
           let editedAt = parseDate(editedAtString) {
            bookmark.editedAt = editedAt
        }
    }
    
    private func updateCollectionMetadataFromCodable(
        _ collection: YabaCollection,
        from codable: YabaCodableCollection
    ) {
        collection.label = codable.label
        collection.icon = codable.icon
        collection.color = YabaColor(rawValue: codable.color) ?? .none
        collection.version = codable.version
        collection.order = codable.order

        if let editedAt = parseDate(codable.editedAt) {
            collection.editedAt = editedAt
        }

        // Don't touch relationships - they're handled separately
    }
    
    private func updateCollectionFromCodable(
        _ collection: YabaCollection,
        from codable: YabaCodableCollection,
        bookmarkMap: [String: YabaBookmark],
        using modelContext: ModelContext
    ) {
        collection.label = codable.label
        collection.icon = codable.icon
        collection.color = YabaColor(rawValue: codable.color) ?? .none
        collection.version = codable.version
        
        if let editedAt = parseDate(codable.editedAt) {
            collection.editedAt = editedAt
        }
        
        // Update bookmark associations using the bookmark map for better performance
        collection.bookmarks?.removeAll()
        for bookmarkId in codable.bookmarks {
            if let bookmark = bookmarkMap[bookmarkId] {
                collection.bookmarks?.append(bookmark)
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
    
    private func addBookmarkToUncategorizedCollection(
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
    }
}

// New robust HTML bookmarks importer with nested folder support
class HTMLBookmarksImporter {
    struct BookmarkNode {
        let title: String
        let url: String
    }

    struct FolderNode {
        let label: String
        var bookmarks: [BookmarkNode]
        var children: [FolderNode]
    }

    struct ParsedRoot {
        var folders: [FolderNode]
        var rootBookmarks: [BookmarkNode]
    }

    func parse(_ html: String) -> ParsedRoot {
        let normalized = normalize(html)
        if let firstDLStart = normalized.range(of: "<DL>", options: .caseInsensitive),
           let fullDLRange = findBalancedRange(tag: "DL", in: normalized, from: firstDLStart.lowerBound) {
            let inner = String(normalized[fullDLRange].dropFirst(4).dropLast(5))
            let (folders, bookmarks, _) = parseList(in: inner, from: inner.startIndex)
            return ParsedRoot(folders: folders, rootBookmarks: bookmarks)
        }
        let (folders, bookmarks, _) = parseList(in: normalized, from: normalized.startIndex)
        return ParsedRoot(folders: folders, rootBookmarks: bookmarks)
    }

    // MARK: - Core Parsing

    private func parseList(in html: String, from start: String.Index) -> ([FolderNode], [BookmarkNode], String.Index) {
        var folders: [FolderNode] = []
        var bookmarks: [BookmarkNode] = []
        var index = start

        while index < html.endIndex {
            guard let dtRange = html.range(of: "<DT>", options: .caseInsensitive, range: index..<html.endIndex) else {
                break
            }
            index = dtRange.upperBound
            // Allow whitespace/newlines between <DT> and the next tag
            skipWhitespace(in: html, index: &index)

            // Folder: <H3> ... </H3> followed by optional <DL> ... </DL>
            if let h3Open = html.range(of: "<H3", options: .caseInsensitive, range: index..<html.endIndex),
               h3Open.lowerBound == index,
               let h3CloseStart = html.range(of: ">", options: .caseInsensitive, range: h3Open.upperBound..<html.endIndex)?.upperBound,
               let h3End = html.range(of: "</H3>", options: .caseInsensitive, range: h3CloseStart..<html.endIndex) {
                let rawLabel = String(html[h3CloseStart..<h3End.lowerBound]).trimmingCharacters(in: .whitespacesAndNewlines)
                let label = decodeEntities(rawLabel)

                var childFolders: [FolderNode] = []
                var childBookmarks: [BookmarkNode] = []
                var afterFolderIndex = h3End.upperBound
                // Allow whitespace/newlines between </H3> and <DL>
                skipWhitespace(in: html, index: &afterFolderIndex)

                if let dlStart = html.range(of: "<DL>", options: .caseInsensitive, range: afterFolderIndex..<html.endIndex),
                   let balanced = findBalancedRange(tag: "DL", in: html, from: dlStart.lowerBound) {
                    let inner = String(html[balanced].dropFirst(4).dropLast(5))
                    let (childrenFolders, childrenBookmarks, _) = parseList(in: inner, from: inner.startIndex)
                    childFolders = childrenFolders
                    childBookmarks = childrenBookmarks
                    afterFolderIndex = balanced.upperBound
                }

                folders.append(FolderNode(label: label, bookmarks: childBookmarks, children: childFolders))
                index = afterFolderIndex
                continue
            }

            // Bookmark: <A HREF="...">title</A>
            if let aOpen = html.range(of: "<A ", options: .caseInsensitive, range: index..<html.endIndex),
               aOpen.lowerBound == index,
               let hrefValueRange = findAttribute(named: "HREF", in: html, from: aOpen.upperBound),
               let aEnd = html.range(of: ">", options: .caseInsensitive, range: hrefValueRange.upperBound..<html.endIndex)?.upperBound,
               let closeRange = html.range(of: "</A>", options: .caseInsensitive, range: aEnd..<html.endIndex) {
                let url = String(html[hrefValueRange]).trimmingCharacters(in: .whitespacesAndNewlines)
                let titleRaw = String(html[aEnd..<closeRange.lowerBound]).trimmingCharacters(in: .whitespacesAndNewlines)
                let title = decodeEntities(titleRaw)
                if !url.isEmpty {
                    bookmarks.append(BookmarkNode(title: title, url: url))
                }
                index = closeRange.upperBound
                continue
            }

            index = html.index(after: index)
        }

        return (folders, bookmarks, index)
    }

    // MARK: - Utilities

    private func findBalancedRange(tag: String, in html: String, from start: String.Index) -> Range<String.Index>? {
        let open = "<\(tag)>"
        let close = "</\(tag)>"
        var depth = 0
        var idx = start
        var startIdx: String.Index?
        while idx < html.endIndex {
            if let openRange = html.range(of: open, options: .caseInsensitive, range: idx..<html.endIndex),
               (startIdx == nil || openRange.lowerBound < html.range(of: close, options: .caseInsensitive, range: idx..<html.endIndex)?.lowerBound ?? html.endIndex) {
                if startIdx == nil { startIdx = openRange.lowerBound }
                depth += 1
                idx = openRange.upperBound
                continue
            }
            if let closeRange = html.range(of: close, options: .caseInsensitive, range: idx..<html.endIndex) {
                depth -= 1
                idx = closeRange.upperBound
                if depth == 0, let s = startIdx { return s..<closeRange.upperBound }
                continue
            }
            break
        }
        return nil
    }

    private func findAttribute(named name: String, in html: String, from start: String.Index) -> Range<String.Index>? {
        // Matches name="..."; forgiving about spacing
        let search = start..<html.endIndex
        if let nameRange = html.range(of: name + "=\"", options: [.caseInsensitive], range: search) {
            let valueStart = nameRange.upperBound
            if let endQuote = html.range(of: "\"", options: [], range: valueStart..<html.endIndex) {
                return valueStart..<endQuote.lowerBound
            }
        }
        return nil
    }

    private func normalize(_ html: String) -> String {
        return html
            .replacingOccurrences(of: "<dt>", with: "<DT>")
            .replacingOccurrences(of: "<DT >", with: "<DT>")
            .replacingOccurrences(of: "<h3", with: "<H3")
            .replacingOccurrences(of: "</h3>", with: "</H3>")
            .replacingOccurrences(of: "<a ", with: "<A ")
            .replacingOccurrences(of: "href=", with: "HREF=")
            .replacingOccurrences(of: "</a>", with: "</A>")
            .replacingOccurrences(of: "<dl>", with: "<DL>")
            .replacingOccurrences(of: "</dl>", with: "</DL>")
            .replacingOccurrences(of: "<dd>", with: "")
            .replacingOccurrences(of: "</dd>", with: "")
            .replacingOccurrences(of: "<p>", with: "")
            .replacingOccurrences(of: "</p>", with: "")
    }

    private func decodeEntities(_ text: String) -> String {
        return text
            .replacingOccurrences(of: "&amp;", with: "&")
            .replacingOccurrences(of: "&lt;", with: "<")
            .replacingOccurrences(of: "&gt;", with: ">")
            .replacingOccurrences(of: "&quot;", with: "\"")
            .replacingOccurrences(of: "&#39;", with: "'")
            .replacingOccurrences(of: "&apos;", with: "'")
            .replacingOccurrences(of: "&nbsp;", with: " ")
    }

    private func skipWhitespace(in html: String, index: inout String.Index) {
        while index < html.endIndex, html[index].isWhitespace {
            index = html.index(after: index)
        }
    }
}
