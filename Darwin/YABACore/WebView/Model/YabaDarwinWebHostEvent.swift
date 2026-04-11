//
//  YabaDarwinWebHostEvent.swift
//  YABACore
//
//  Parity with Compose `YabaWebHostEvent`.
//

import Foundation

public enum YabaDarwinWebHostEvent: Sendable {
    case loadState(YabaDarwinWebLoadState)
    case readerMetrics(YabaDarwinReaderMetricsEvent)
    case htmlConverterSuccess(documentJson: String, linkMetadataJson: String?)
    case htmlConverterFailure(message: String)
    case pdfConverterSuccess(payloadJson: String)
    case pdfConverterFailure(message: String)
    case epubConverterSuccess(payloadJson: String)
    case epubConverterFailure(message: String)
    case initialContentLoad(WebShellLoadResult)
    case noteEditorIdleForAutosave
    case canvasIdleForAutosave
    case canvasMetrics(YabaDarwinCanvasHostMetrics)
    case canvasStyleState(YabaDarwinCanvasHostStyleState)
    case canvasLinkTap(elementId: String, text: String, url: String)
    case canvasMentionTap(
        elementId: String,
        text: String,
        bookmarkId: String,
        bookmarkKindCode: Int,
        bookmarkLabel: String
    )
    case tableOfContentsChanged(toc: YabaDarwinToc?)
}
