//
//  WebHostEvent.swift
//  YABACore
//
//  Parity with Compose `WebHostEvent`.
//

import Foundation

public enum WebHostEvent: Sendable {
    case loadState(WebLoadState)
    case readerMetrics(ReaderMetricsEvent)
    case htmlConverterSuccess(WebConverterResult)
    case htmlConverterFailure(message: String)
    case pdfConverterSuccess(payloadJson: String)
    case pdfConverterFailure(message: String)
    case epubConverterSuccess(payloadJson: String)
    case epubConverterFailure(message: String)
    case initialContentLoad(WebShellLoadResult)
    case noteEditorIdleForAutosave
    case canvasIdleForAutosave
    case canvasMetrics(CanvasHostMetrics)
    case canvasStyleState(CanvasHostStyleState)
    case canvasLinkTap(elementId: String, text: String, url: String)
    case canvasMentionTap(
        elementId: String,
        text: String,
        bookmarkId: String,
        bookmarkKindCode: Int,
        bookmarkLabel: String
    )
    case tableOfContentsChanged(toc: Toc?)
}
