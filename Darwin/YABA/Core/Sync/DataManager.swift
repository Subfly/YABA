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
}

// HTML Parser class to handle HTML parsing
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
        
        // Split the HTML into lines for easier processing
        let lines = htmlString.components(separatedBy: .newlines)
        
        for line in lines {
            let trimmedLine = line.trimmingCharacters(in: .whitespaces)
            // Check for folder (H3 tag)
            if trimmedLine.hasPrefix("<DT><H3>") && trimmedLine.hasSuffix("</H3>") {
                // If we have a current folder, add it to the list
                if let folder = currentFolder {
                    folders.append(folder)
                }
                
                // Extract folder name
                let folderName = trimmedLine
                    .replacingOccurrences(of: "<DT><H3>", with: "")
                    .replacingOccurrences(of: "</H3>", with: "")
                    .trimmingCharacters(in: .whitespaces)
                
                // Create new folder
                currentFolder = ParsedFolder(label: folderName, bookmarks: [])
            }
            
            // Check for bookmark (A tag)
            if trimmedLine.hasPrefix("<DT><A") {
                if let bookmark = parseBookmark(from: trimmedLine) {
                    currentFolder?.bookmarks.append(bookmark)
                }
            }
        }
        
        // Add the last folder if exists
        if let folder = currentFolder {
            folders.append(folder)
        }
        
        // If no folders were found but we have bookmarks, create an uncategorized folder
        if folders.isEmpty && currentFolder?.bookmarks.isEmpty == false {
            folders.append(ParsedFolder(label: "-1", bookmarks: currentFolder?.bookmarks ?? []))
        }
        
        return folders
    }
    
    private func parseBookmark(from line: String) -> ParsedBookmark? {
        // Extract URL and title using more precise pattern
        let pattern = #"<DT><A HREF="([^"]+)">([^<]+)</A>"#
        guard let regex = try? NSRegularExpression(pattern: pattern),
              let match = regex.firstMatch(in: line, range: NSRange(line.startIndex..., in: line)),
              let urlRange = Range(match.range(at: 1), in: line),
              let titleRange = Range(match.range(at: 2), in: line) else {
            return nil
        }
        
        let urlString = String(line[urlRange])
        let title = String(line[titleRange])
            .trimmingCharacters(in: .whitespaces)
        
        return ParsedBookmark(
            title: title,
            url: urlString
        )
    }
}
