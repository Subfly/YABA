//
//  MarkdownKMPStreamController.swift
//  YABACore
//
//  Mirrors the streaming document state in `Markdown/markdown-renderer/.../Markdown.kt`:
//  one persistent KMP `MarkdownParser`, `beginStream` / `append` / `endStream`, and
//  full `parse` only when non-streaming input actually changes.
//

import Foundation
import MarkdownParser

/// Publishes the current KMP `Document` for SwiftUI. Parser instance is long-lived
/// so incremental work inside KMP reuses `Node` object identity.
@MainActor
public final class MarkdownKMPStreamController: ObservableObject {
    @Published public private(set) var document: Document?
    @Published public private(set) var isParserStreaming: Bool = false

    private var parser: MarkdownParser
    private var lastParsedLength: Int = 0
    private var wasStreaming: Bool = false
    private var lastNonStreamingMarkdown: String = ""

    public init(
        flavour: ExtendedFlavour = ExtendedFlavour(),
        appendCoalesceThreshold: Int = 0
    ) {
        self.parser = MarkdownParser(
            flavour: flavour,
            customEmojiMap: [:],
            enableAsciiEmoticons: false,
            enableLinting: false,
            appendCoalesceThreshold: Int32(appendCoalesceThreshold)
        )
    }

    /// Preview / tests: skip parser and use a pre-built KMP `Document`.
    public init(injectingDocument document: Document) {
        self.parser = MarkdownParser(
            flavour: ExtendedFlavour(),
            customEmojiMap: [:],
            enableAsciiEmoticons: false,
            enableLinting: false,
            appendCoalesceThreshold: 0
        )
        self.document = document
        self.lastNonStreamingMarkdown = ""
    }

    /// Call when `markdown` or `isStreaming` changes. Safe to call every render with same args (cheap no-ops).
    public func update(markdown: String, isStreaming: Bool) {
        if isStreaming, !wasStreaming {
            parser.beginStream()
            lastParsedLength = 0
            document = nil
            wasStreaming = true
        }

        if isStreaming {
            if markdown.count > lastParsedLength {
                let start = markdown.index(markdown.startIndex, offsetBy: lastParsedLength)
                let chunk = String(markdown[start...])
                if !chunk.isEmpty {
                    let doc = parser.append(chunk: chunk)
                    lastParsedLength = markdown.count
                    document = doc
                }
            }
            isParserStreaming = true
            return
        }

        if wasStreaming {
            if markdown.count > lastParsedLength {
                let start = markdown.index(markdown.startIndex, offsetBy: lastParsedLength)
                let tail = String(markdown[start...])
                if !tail.isEmpty { _ = parser.append(chunk: tail) }
            }
            let doc = parser.endStream()
            lastParsedLength = markdown.count
            document = doc
            wasStreaming = false
            lastNonStreamingMarkdown = markdown
            isParserStreaming = false
            return
        }

        if markdown == lastNonStreamingMarkdown, document != nil {
            isParserStreaming = false
            return
        }

        let doc = parser.parse(input: markdown)
        lastParsedLength = markdown.count
        lastNonStreamingMarkdown = markdown
        document = doc
        isParserStreaming = false
    }
}
