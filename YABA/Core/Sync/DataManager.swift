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
    case exportableGenerationError(LocalizedStringKey)
    case caseURLColumnNotSelected(LocalizedStringKey)
    case invalidBookmarkUrl(LocalizedStringKey)
    case unkownError(LocalizedStringKey)
}

@MainActor
class DataManager {
    func importJSON(
        from data: Data,
        using modelContext: ModelContext
    ) throws {
        let decoder = JSONDecoder()
        let content = try decoder.decode(YabaCodableContent.self, from: data)
        
        if content.bookmarks.isEmpty {
            throw DataError.emptyJSON("Data Manager Empty JSON Message")
        }
        
        let mappedBookmarks = Dictionary(uniqueKeysWithValues: content.bookmarks.map {
            ($0.bookmarkId, $0.mapToModel())
        })
        
        if let collections = content.collections {
            collections.forEach { collection in
                let collectionModel = collection.mapToModel()
                try? YabaDataLogger.shared.logCollectionChange(
                    old: nil,
                    new: collectionModel,
                    shouldSave: false
                )
                
                collection.bookmarks.forEach { bookmarkId in
                    if let bookmarkModel = mappedBookmarks[bookmarkId] {
                        try? YabaDataLogger.shared.logBookmarkChange(
                            old: nil,
                            new: bookmarkModel,
                            shouldSave: false
                        )
                        collectionModel.bookmarks.append(bookmarkModel)
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
                type: .folder
            )
            
            try? YabaDataLogger.shared.logCollectionChange(
                old: nil,
                new: dummyFolder,
                shouldSave: false
            )
            
            mappedBookmarks.values.forEach { bookmark in
                try? YabaDataLogger.shared.logBookmarkChange(
                    old: nil,
                    new: bookmark,
                    shouldSave: false
                )
                dummyFolder.bookmarks.append(bookmark)
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

        let rows = content.components(separatedBy: "\n")
        guard rows.count > 1 else {
            throw DataError.emptyCSV("Data Manager Empty CSV Message")
        }
        
        let header = rows[0].components(separatedBy: ",")
        guard header.count == 11 else {
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
            type: .folder
        )
        
        try? YabaDataLogger.shared.logCollectionChange(
            old: nil,
            new: dummyFolder,
            shouldSave: false
        )
        
        let bookmarkRows = rows.dropFirst()
        for row in bookmarkRows {
            let columns = parseCSVRow(row)
            guard columns.count == 11 else { continue }

            let bookmark = YabaCodableBookmark(
                bookmarkId: columns[0],
                label: columns[1],
                bookmarkDescription: columns[2],
                link: columns[3],
                domain: columns[4],
                createdAt: columns[5],
                editedAt: columns[6],
                imageUrl: columns[7],
                iconUrl: columns[8],
                videoUrl: columns[9],
                type: Int(columns[10]) ?? 1
            ).mapToModel()
            
            try? YabaDataLogger.shared.logBookmarkChange(
                old: nil,
                new: bookmark,
                shouldSave: false
            )
            
            dummyFolder.bookmarks.append(bookmark)
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
            type: .folder
        )
        
        try? YabaDataLogger.shared.logCollectionChange(
            old: nil,
            new: dummyFolder,
            shouldSave: false
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
                type: 1
            ).mapToModel()
            
            try? YabaDataLogger.shared.logBookmarkChange(
                old: nil,
                new: bookmark,
                shouldSave: false
            )

            dummyFolder.bookmarks.append(bookmark)
        }

        modelContext.insert(dummyFolder)
        try modelContext.save()
        onFinish()
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
                collection.bookmarks.forEach { bookmark in
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
            "createdAt", "editedAt", "imageUrl", "iconUrl", "videoUrl", "type"
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
        let list = [
            bookmark.bookmarkId ?? UUID().uuidString,
            bookmark.label ?? bookmark.link,
            bookmark.bookmarkDescription ?? "",
            bookmark.link,
            bookmark.domain ?? "",
            bookmark.createdAt ?? currentDate,
            bookmark.editedAt ?? currentDate,
            bookmark.imageUrl ?? "",
            bookmark.iconUrl ?? "",
            bookmark.videoUrl ?? "",
            String(bookmark.type ?? 1)
        ]
        return list.map { escapeForCSV($0) }.joined(separator: ",")
    }

    private func parseCSVRow(_ row: String) -> [String] {
        var result: [String] = []
        var field = ""
        var insideQuotes = false

        for char in row {
            if char == "\"" {
                insideQuotes.toggle()
            } else if char == "," && !insideQuotes {
                result.append(field)
                field = ""
            } else {
                field.append(char)
            }
        }
        result.append(field)
        return result.map { $0.replacingOccurrences(of: "\"\"", with: "\"") }
    }
}
