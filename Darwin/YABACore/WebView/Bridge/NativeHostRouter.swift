//
//  NativeHostRouterDarwin.swift
//  YABACore
//
//  Parity with Compose `YabaNativeHostRouter.kt` / `createNativeHostMessageHandler`.
//

import Foundation

public enum NativeHostRouterDarwin {
    public static let nativeHostScriptMessageName = "yabaNativeHost"

    /// Builds a JSON message handler callback for one WebView instance.
    public static func createMessageHandler(
        expectedBridgeFeature: String?,
        onBridgeReady: @escaping () -> Void,
        onHostEvent: @escaping (WebHostEvent) -> Void,
        onAnnotationTap: @escaping (String) -> Void,
        onMathTap: @escaping (MathTapEvent) -> Void,
        onInlineLinkTap: @escaping (InlineLinkTapEvent) -> Void,
        onInlineMentionTap: @escaping (InlineMentionTapEvent) -> Void
    ) -> (String) -> Void {
        { json in
            guard let root = try? JSONSerialization.jsonObject(with: Data(json.utf8)) as? [String: Any],
                  let type = root["type"] as? String
            else {
                return
            }

            switch type {
            case "bridgeReady":
                if let expectedBridgeFeature,
                   let feature = root["feature"] as? String,
                   feature == expectedBridgeFeature {
                    onBridgeReady()
                }
            case "converterJob":
                ConverterJobRegistry.shared.handleConverterJobMessage(root)
            case "editorPdfExport":
                EditorPdfExportJobRegistry.shared.handleMessage(root)
            default:
                if let event = NativeHostMessageParserDarwin.parse(
                    json: json,
                    onAnnotationTap: onAnnotationTap,
                    onMathTap: onMathTap,
                    onInlineLinkTap: onInlineLinkTap,
                    onInlineMentionTap: onInlineMentionTap
                ) {
                    onHostEvent(event)
                }
            }
        }
    }
}
