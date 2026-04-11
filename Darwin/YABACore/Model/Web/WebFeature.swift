//
//  WebFeature.swift
//  YABACore
//
//  Which bundled shell to load and what data drives it — parity with Compose `WebFeature`.
//

import Foundation

/// Bundled web feature selection for `WKWebViewRuntime`.
public enum WebFeature: Sendable {
    case readableViewer(
        initialDocumentJson: String,
        assetsBaseUrl: String?,
        readerTheme: ReaderTheme,
        readerFontSize: ReaderFontSize,
        readerLineHeight: ReaderLineHeight,
        platform: WebPlatform,
        appearance: WebAppearance,
        annotationsJson: String
    )

    case editor(
        initialDocumentJson: String,
        assetsBaseUrl: String?,
        placeholderText: String?,
        platform: WebPlatform,
        appearance: WebAppearance,
        readerTheme: ReaderTheme,
        readerFontSize: ReaderFontSize,
        readerLineHeight: ReaderLineHeight,
        documentLoadGeneration: Int
    )

    case canvas(
        initialSceneJson: String,
        platform: WebPlatform,
        appearance: WebAppearance,
        sceneLoadGeneration: Int
    )

    case htmlConverter(inputHtml: String?, baseUrl: String?)

    case pdfExtractor(input: WebPdfExtractorInput?)

    case epubExtractor(input: WebEpubExtractorInput?)

    case pdfViewer(
        pdfUrl: String,
        platform: WebPlatform,
        appearance: WebAppearance,
        annotationsJson: String
    )

    case epubViewer(
        epubUrl: String,
        readerTheme: ReaderTheme,
        readerFontSize: ReaderFontSize,
        readerLineHeight: ReaderLineHeight,
        platform: WebPlatform,
        appearance: WebAppearance,
        annotationsJson: String
    )

    /// Bridge `feature` string expected in `bridgeReady` messages (matches Compose hosts).
    public var expectedBridgeFeature: String {
        switch self {
        case .readableViewer: return "viewer"
        case .editor: return "editor"
        case .canvas: return "canvas"
        case .htmlConverter, .pdfExtractor, .epubExtractor: return "converter"
        case .pdfViewer: return "pdf"
        case .epubViewer: return "epub"
        }
    }
}

public struct WebPdfExtractorInput: Sendable {
    public var pdfUrl: String?
    public init(pdfUrl: String? = nil) {
        self.pdfUrl = pdfUrl
    }
}

public struct WebEpubExtractorInput: Sendable {
    public var epubUrl: String?
    public init(epubUrl: String? = nil) {
        self.epubUrl = epubUrl
    }
}
