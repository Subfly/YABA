//
//  MarkdownExportSupport.swift
//  YABACore
//
//  Shared markdown export to a user-picked directory: `note.md` + `assets/` (Compose / Android parity).
//

import Foundation
import SwiftUI
#if canImport(UIKit)
import UIKit
import UniformTypeIdentifiers
#elseif canImport(AppKit)
import AppKit
#endif

// MARK: - Models

public struct MarkdownExportAsset: Sendable, Equatable {
    public let fileName: String
    public let bytes: Data

    public init(fileName: String, bytes: Data) {
        self.fileName = fileName
        self.bytes = bytes
    }
}

public struct MarkdownExportRequest: Sendable, Equatable {
    public let markdown: String
    public let baseFolderName: String
    public let assets: [MarkdownExportAsset]

    public init(markdown: String, baseFolderName: String, assets: [MarkdownExportAsset]) {
        self.markdown = markdown
        self.baseFolderName = baseFolderName
        self.assets = assets
    }
}

/// App-agnostic inline image payload for building export filenames (`<id>.<ext>`).
public struct MarkdownExportInlineSource: Sendable, Equatable {
    public var assetId: String
    public var pathExtension: String
    public var bytes: Data

    public init(assetId: String, pathExtension: String, bytes: Data) {
        self.assetId = assetId
        self.pathExtension = pathExtension
        self.bytes = bytes
    }
}

// MARK: - Writing + helpers

public enum MarkdownExportSupport {
    /// Writes `<parent>/<baseFolderName>/note.md` and `<parent>/<baseFolderName>/assets/*`.
    public static func writeBundle(_ request: MarkdownExportRequest, into selectedDirectory: URL) -> Bool {
        let scoped = selectedDirectory.startAccessingSecurityScopedResource()
        defer {
            if scoped { selectedDirectory.stopAccessingSecurityScopedResource() }
        }
        let fileManager = FileManager.default
        do {
            let exportRoot = selectedDirectory
                .appendingPathComponent(request.baseFolderName, isDirectory: true)
            let assetsDir = exportRoot.appendingPathComponent("assets", isDirectory: true)
            try fileManager.createDirectory(at: exportRoot, withIntermediateDirectories: true)
            try fileManager.createDirectory(at: assetsDir, withIntermediateDirectories: true)
            let markdownFile = exportRoot.appendingPathComponent("note.md", isDirectory: false)
            guard let markdownData = request.markdown.data(using: .utf8) else { return false }
            try markdownData.write(to: markdownFile, options: .atomic)
            for asset in request.assets {
                let file = assetsDir.appendingPathComponent(asset.fileName, isDirectory: false)
                try asset.bytes.write(to: file, options: .atomic)
            }
            return true
        } catch {
            return false
        }
    }

    public static func sanitizeBaseFolderName(_ label: String, emptyFallback: String = "note") -> String {
        let trimmed = label.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return emptyFallback }
        let sanitized = String(trimmed.map { char in
            if char.isLetter || char.isNumber || char == "_" || char == "-" {
                return char
            }
            return "_"
        })
        return sanitized.isEmpty ? emptyFallback : sanitized
    }

    /// Maps inline sources to on-disk `assets/` filenames (`<assetId>.<ext>`).
    public static func exportAssets(from sources: [MarkdownExportInlineSource]) -> [MarkdownExportAsset] {
        sources.map { item in
            let ext = item.pathExtension.trimmingCharacters(in: .whitespacesAndNewlines)
            let cleanedExt = ext.isEmpty ? "bin" : ext
            return MarkdownExportAsset(fileName: "\(item.assetId).\(cleanedExt)", bytes: item.bytes)
        }
    }
}

// MARK: - Directory picker (export destination)

#if canImport(UIKit)
public struct MarkdownExportDirectoryPicker: UIViewControllerRepresentable {
    public let onPicked: (URL?) -> Void

    public init(onPicked: @escaping (URL?) -> Void) {
        self.onPicked = onPicked
    }

    public func makeCoordinator() -> Coordinator {
        Coordinator(onPicked: onPicked)
    }

    public func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(
            forOpeningContentTypes: [.folder],
            asCopy: false
        )
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        picker.modalPresentationStyle = .formSheet
        return picker
    }

    public func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    public final class Coordinator: NSObject, UIDocumentPickerDelegate {
        private let onPicked: (URL?) -> Void
        private var didFinish = false

        public init(onPicked: @escaping (URL?) -> Void) {
            self.onPicked = onPicked
        }

        public func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard !didFinish else { return }
            didFinish = true
            onPicked(urls.first)
        }

        public func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
            guard !didFinish else { return }
            didFinish = true
            onPicked(nil)
        }
    }
}
#elseif canImport(AppKit)
public struct MarkdownExportDirectoryPicker: NSViewControllerRepresentable {
    public let onPicked: (URL?) -> Void

    public init(onPicked: @escaping (URL?) -> Void) {
        self.onPicked = onPicked
    }

    public func makeCoordinator() -> Coordinator {
        Coordinator(onPicked: onPicked)
    }

    public func makeNSViewController(context: Context) -> NSViewController {
        let controller = NSViewController()
        context.coordinator.presentOpenPanelIfNeeded()
        return controller
    }

    public func updateNSViewController(_ nsViewController: NSViewController, context: Context) {}

    public final class Coordinator: NSObject {
        private let onPicked: (URL?) -> Void
        private var didPresent = false

        public init(onPicked: @escaping (URL?) -> Void) {
            self.onPicked = onPicked
        }

        fileprivate func presentOpenPanelIfNeeded() {
            guard !didPresent else { return }
            didPresent = true
            DispatchQueue.main.async { [onPicked] in
                let panel = NSOpenPanel()
                panel.canChooseFiles = false
                panel.canChooseDirectories = true
                panel.allowsMultipleSelection = false
                panel.canCreateDirectories = true
                if panel.runModal() == .OK, let url = panel.url {
                    onPicked(url)
                } else {
                    onPicked(nil)
                }
            }
        }
    }
}
#endif
