//
//  YabaDarwinWebFeature.swift
//  YABACore
//
//  Which bundled shell to load and what data drives it — parity with Compose `YabaWebFeature`.
//

import Foundation

/// Bundled web feature selection for `YabaWKWebViewRuntime`.
public enum YabaDarwinWebFeature: Sendable {
    case readableViewer(
        initialDocumentJson: String,
        assetsBaseUrl: String?,
        readerTheme: YabaCoreReaderTheme,
        readerFontSize: YabaCoreReaderFontSize,
        readerLineHeight: YabaCoreReaderLineHeight,
        platform: YabaDarwinWebPlatform,
        appearance: YabaDarwinWebAppearance,
        annotationsJson: String
    )

    case editor(
        initialDocumentJson: String,
        assetsBaseUrl: String?,
        placeholderText: String?,
        platform: YabaDarwinWebPlatform,
        appearance: YabaDarwinWebAppearance,
        readerTheme: YabaCoreReaderTheme,
        readerFontSize: YabaCoreReaderFontSize,
        readerLineHeight: YabaCoreReaderLineHeight,
        documentLoadGeneration: Int
    )

    case canvas(
        initialSceneJson: String,
        platform: YabaDarwinWebPlatform,
        appearance: YabaDarwinWebAppearance,
        sceneLoadGeneration: Int
    )

    case htmlConverter(inputHtml: String?, baseUrl: String?)

    case pdfExtractor(input: YabaDarwinWebPdfExtractorInput?)

    case epubExtractor(input: YabaDarwinWebEpubExtractorInput?)

    case pdfViewer(
        pdfUrl: String,
        platform: YabaDarwinWebPlatform,
        appearance: YabaDarwinWebAppearance,
        annotationsJson: String
    )

    case epubViewer(
        epubUrl: String,
        readerTheme: YabaCoreReaderTheme,
        readerFontSize: YabaCoreReaderFontSize,
        readerLineHeight: YabaCoreReaderLineHeight,
        platform: YabaDarwinWebPlatform,
        appearance: YabaDarwinWebAppearance,
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

public struct YabaDarwinWebPdfExtractorInput: Sendable {
    public var pdfUrl: String?
    public init(pdfUrl: String? = nil) {
        self.pdfUrl = pdfUrl
    }
}

public struct YabaDarwinWebEpubExtractorInput: Sendable {
    public var epubUrl: String?
    public init(epubUrl: String? = nil) {
        self.epubUrl = epubUrl
    }
}
