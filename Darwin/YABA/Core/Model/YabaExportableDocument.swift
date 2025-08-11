//
//  YabaExportableDocument.swift
//  YABA
//
//  Created by Ali Taha on 23.05.2025.
//

import Foundation
import UniformTypeIdentifiers
import SwiftUI

struct YabaExportableJsonDocument: FileDocument {
    static var readableContentTypes: [UTType] = [.json]
    
    var data: Data
    
    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents else { throw NSError() }
        self.data = data
    }
    
    init(data: Data) {
        self.data = data
    }
    
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: self.data)
    }
}

struct YabaExportableCsvDocument: FileDocument {
    static var readableContentTypes: [UTType] = [.commaSeparatedText]
    
    var data: Data
    
    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents else { throw NSError() }
        self.data = data
    }
    
    init(data: Data) {
        self.data = data
    }
    
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: self.data)
    }
}

struct YabaExportableHtmlDocument: FileDocument {
    static var readableContentTypes: [UTType] = [.html]
    
    var data: Data
    
    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents else { throw NSError() }
        self.data = data
    }
    
    init(data: Data) {
        self.data = data
    }
    
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: self.data)
    }
}

struct YabaExportableMarkupDocument: FileDocument {
    static var readableContentTypes: [UTType] = [.plainText]
    
    var data: Data
    
    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents else { throw NSError() }
        self.data = data
    }
    
    init(data: Data) {
        self.data = data
    }
    
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: self.data)
    }
}
