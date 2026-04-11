//
//  YabaWebHostEvent.swift
//  YABACore
//
//  Parity with Compose `YabaWebHostEvent`.
//

import Foundation

public enum YabaWebHostEvent: Sendable {
    case loadState(YabaWebLoadState)
    case readerMetrics(YabaReaderMetricsEvent)
    case htmlConverterSuccess(YabaWebConverterResult)
    case htmlConverterFailure(message: String)
    case pdfConverterSuccess(payloadJson: String)
    case pdfConverterFailure(message: String)
    case epubConverterSuccess(payloadJson: String)
    case epubConverterFailure(message: String)
    case initialContentLoad(WebShellLoadResult)
    case noteEditorIdleForAutosave
    case canvasIdleForAutosave
    case canvasMetrics(YabaCanvasHostMetrics)
    case canvasStyleState(YabaCanvasHostStyleState)
    case canvasLinkTap(elementId: String, text: String, url: String)
    case canvasMentionTap(
        elementId: String,
        text: String,
        bookmarkId: String,
        bookmarkKindCode: Int,
        bookmarkLabel: String
    )
    case tableOfContentsChanged(toc: YabaToc?)
}
