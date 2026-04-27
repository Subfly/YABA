//
//  MarkdownPreview.swift
//  YABACore
//
//  Public SwiftUI entry for native Markdown preview (table-backed, KMP `Document` end-to-end).
//

import MarkdownParser
import SwiftUI

public struct MarkdownPreview: View {
    public var configuration: MarkdownPreviewConfiguration
    public var theme: MarkdownThemeTokens?
    @StateObject private var stream: MarkdownKMPStreamController
    public var markdown: String
    public var isStreaming: Bool
    private var usesInjectedDocument: Bool

    /// Renders a caller-owned KMP `Document` (e.g. tests or a shared parser).
    public init(
        document: Document,
        configuration: MarkdownPreviewConfiguration = .init(),
        theme: MarkdownThemeTokens? = nil
    ) {
        _stream = StateObject(wrappedValue: MarkdownKMPStreamController(injectingDocument: document))
        self.markdown = ""
        self.isStreaming = false
        self.usesInjectedDocument = true
        self.configuration = configuration
        self.theme = theme
    }

    /// Parses and renders Markdown, optionally in streaming mode (KMP `beginStream` / `append` / `endStream`).
    public init(
        markdown: String,
        isStreaming: Bool = false,
        configuration: MarkdownPreviewConfiguration = .init(),
        theme: MarkdownThemeTokens? = nil
    ) {
        _stream = StateObject(wrappedValue: MarkdownKMPStreamController())
        self.markdown = markdown
        self.isStreaming = isStreaming
        self.usesInjectedDocument = false
        self.configuration = configuration
        self.theme = theme
    }

    public var body: some View {
        Group {
            if let doc = stream.document {
                MarkdownPreviewTable(
                    document: doc,
                    configuration: configuration,
                    theme: theme
                )
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, minHeight: 120, alignment: .center)
            }
        }
        .onAppear {
            if !usesInjectedDocument {
                stream.update(markdown: markdown, isStreaming: isStreaming)
            }
        }
        .onChange(of: markdown) { _, newValue in
            if !usesInjectedDocument {
                stream.update(markdown: newValue, isStreaming: isStreaming)
            }
        }
        .onChange(of: isStreaming) { _, newValue in
            if !usesInjectedDocument {
                stream.update(markdown: markdown, isStreaming: newValue)
            }
        }
    }
}
