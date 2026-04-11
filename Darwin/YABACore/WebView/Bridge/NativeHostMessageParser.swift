//
//  NativeHostMessageParserDarwin.swift
//  YABACore
//
//  Parity with Compose `YabaNativeHostMessageParser.kt` — must stay aligned with
//  `Extensions/yaba-web-components/src/bridge/yaba-native-host.ts`.
//

import Foundation

public enum NativeHostMessageParserDarwin {
    public static func parse(
        json: String,
        onAnnotationTap: ((String) -> Void)? = nil,
        onMathTap: ((MathTapEvent) -> Void)? = nil,
        onInlineLinkTap: ((InlineLinkTapEvent) -> Void)? = nil,
        onInlineMentionTap: ((InlineMentionTapEvent) -> Void)? = nil
    ) -> WebHostEvent? {
        guard let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let type = root["type"] as? String
        else {
            return nil
        }

        switch type {
        case "shellLoad":
            return parseShellLoad(root)
        case "toc":
            return parseToc(root)
        case "noteAutosaveIdle":
            return .noteEditorIdleForAutosave
        case "canvasAutosaveIdle":
            return .canvasIdleForAutosave
        case "readerMetrics":
            return parseReaderMetrics(root)
        case "canvasMetrics":
            return parseCanvasMetrics(root)
        case "canvasStyleState":
            return parseCanvasStyleState(root)
        case "annotationTap":
            let id = root["id"] as? String ?? ""
            if !id.isEmpty { onAnnotationTap?(id) }
            return nil
        case "mathTap":
            let kind = root["kind"] as? String ?? ""
            let pos = root["pos"] as? Int ?? -1
            let latex = root["latex"] as? String ?? ""
            if pos >= 0 {
                onMathTap?(
                    MathTapEvent(
                        isBlock: kind == "block",
                        documentPos: pos,
                        latex: latex
                    )
                )
            }
            return nil
        case "inlineLinkTap":
            let pos = root["pos"] as? Int ?? -1
            let text = root["text"] as? String ?? ""
            let url = root["url"] as? String ?? ""
            if pos >= 0, !url.isEmpty {
                onInlineLinkTap?(
                    InlineLinkTapEvent(documentPos: pos, text: text, url: url)
                )
            }
            return nil
        case "inlineMentionTap":
            let pos = root["pos"] as? Int ?? -1
            let text = root["text"] as? String ?? ""
            let bookmarkId = root["bookmarkId"] as? String ?? ""
            let bookmarkKindCode = root["bookmarkKindCode"] as? Int ?? 0
            let bookmarkLabel = root["bookmarkLabel"] as? String ?? ""
            if pos >= 0, !bookmarkId.isEmpty {
                onInlineMentionTap?(
                    InlineMentionTapEvent(
                        documentPos: pos,
                        text: text,
                        bookmarkId: bookmarkId,
                        bookmarkKindCode: bookmarkKindCode,
                        bookmarkLabel: bookmarkLabel
                    )
                )
            }
            return nil
        case "canvasLinkTap":
            let elementId = root["elementId"] as? String ?? ""
            let text = root["text"] as? String ?? ""
            let url = root["url"] as? String ?? ""
            if !elementId.isEmpty, !url.isEmpty {
                return .canvasLinkTap(elementId: elementId, text: text, url: url)
            }
            return nil
        case "canvasMentionTap":
            let elementId = root["elementId"] as? String ?? ""
            let text = root["text"] as? String ?? ""
            let bookmarkId = root["bookmarkId"] as? String ?? ""
            let bookmarkKindCode = root["bookmarkKindCode"] as? Int ?? 0
            let bookmarkLabel = root["bookmarkLabel"] as? String ?? ""
            if !elementId.isEmpty, !bookmarkId.isEmpty {
                return .canvasMentionTap(
                    elementId: elementId,
                    text: text,
                    bookmarkId: bookmarkId,
                    bookmarkKindCode: bookmarkKindCode,
                    bookmarkLabel: bookmarkLabel
                )
            }
            return nil
        case "converterJob", "bridgeReady":
            return nil
        default:
            return nil
        }
    }

    private static func parseShellLoad(_ root: [String: Any]) -> WebHostEvent {
        let result = root["result"] as? String ?? ""
        let shell: WebShellLoadResult = (result == "loaded") ? .loaded : .error
        return .initialContentLoad(shell)
    }

    private static func parseToc(_ root: [String: Any]) -> WebHostEvent {
        guard let tocObj = root["toc"] as? [String: Any] else {
            return .tableOfContentsChanged(toc: nil)
        }
        let itemsArr = tocObj["items"] as? [[String: Any]] ?? []
        let items = parseTocItems(itemsArr)
        return .tableOfContentsChanged(toc: Toc(items: items))
    }

    private static func parseTocItems(_ arr: [[String: Any]]) -> [TocItem] {
        arr.compactMap { o -> TocItem? in
            let id = o["id"] as? String ?? ""
            let title = o["title"] as? String ?? ""
            let level = o["level"] as? Int ?? 1
            let childrenArr = o["children"] as? [[String: Any]] ?? []
            let extrasRaw = o["extrasJson"] as? String
            let trimmed = extrasRaw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let extrasNorm: String? = trimmed.isEmpty ? nil : trimmed
            return TocItem(
                id: id,
                title: title,
                level: level,
                children: parseTocItems(childrenArr),
                extrasJson: extrasNorm
            )
        }
    }

    private static func parseReaderMetrics(_ root: [String: Any]) -> WebHostEvent {
        let can = root["canCreateAnnotation"] as? Bool ?? false
        let page = max(1, root["currentPage"] as? Int ?? 1)
        let count = max(1, root["pageCount"] as? Int ?? 1)
        var formatting: EditorFormattingState?
        if let fmt = root["formatting"] as? [String: Any] {
            formatting = parseEditorFormatting(fmt)
        }
        return .readerMetrics(
            ReaderMetricsEvent(
                canCreateAnnotation: can,
                currentPage: page,
                pageCount: count,
                formatting: formatting
            )
        )
    }

    private static func parseEditorFormatting(_ json: [String: Any]) -> EditorFormattingState {
        EditorFormattingState(
            headingLevel: json["headingLevel"] as? Int ?? 0,
            bold: json["bold"] as? Bool ?? false,
            italic: json["italic"] as? Bool ?? false,
            underline: json["underline"] as? Bool ?? false,
            strikethrough: json["strikethrough"] as? Bool ?? false,
            subscriptEnabled: json["subscript"] as? Bool ?? false,
            superscript: json["superscript"] as? Bool ?? false,
            code: json["code"] as? Bool ?? false,
            codeBlock: json["codeBlock"] as? Bool ?? false,
            blockquote: json["blockquote"] as? Bool ?? false,
            bulletList: json["bulletList"] as? Bool ?? false,
            orderedList: json["orderedList"] as? Bool ?? false,
            taskList: json["taskList"] as? Bool ?? false,
            inlineMath: json["inlineMath"] as? Bool ?? false,
            blockMath: json["blockMath"] as? Bool ?? false,
            canUndo: json["canUndo"] as? Bool ?? false,
            canRedo: json["canRedo"] as? Bool ?? false,
            canIndent: json["canIndent"] as? Bool ?? false,
            canOutdent: json["canOutdent"] as? Bool ?? false,
            inTable: json["inTable"] as? Bool ?? false,
            canAddRowBefore: json["canAddRowBefore"] as? Bool ?? false,
            canAddRowAfter: json["canAddRowAfter"] as? Bool ?? false,
            canDeleteRow: json["canDeleteRow"] as? Bool ?? false,
            canAddColumnBefore: json["canAddColumnBefore"] as? Bool ?? false,
            canAddColumnAfter: json["canAddColumnAfter"] as? Bool ?? false,
            canDeleteColumn: json["canDeleteColumn"] as? Bool ?? false,
            textHighlight: json["textHighlight"] as? Bool ?? false
        )
    }

    private static func parseCanvasMetrics(_ root: [String: Any]) -> WebHostEvent {
        .canvasMetrics(
            CanvasHostMetrics(
                activeTool: root["activeTool"] as? String ?? "selection",
                hasSelection: root["hasSelection"] as? Bool ?? false,
                canUndo: root["canUndo"] as? Bool ?? false,
                canRedo: root["canRedo"] as? Bool ?? false,
                gridModeEnabled: root["gridModeEnabled"] as? Bool ?? true,
                objectsSnapModeEnabled: root["objectsSnapModeEnabled"] as? Bool ?? true
            )
        )
    }

    private static func parseCanvasStyleState(_ root: [String: Any]) -> WebHostEvent {
        .canvasStyleState(
            CanvasHostStyleState(
                hasSelection: root["hasSelection"] as? Bool ?? false,
                selectionCount: root["selectionCount"] as? Int ?? 0,
                selectionElementTypes: parseStringArray(root, key: "selectionElementTypes"),
                primaryElementType: root["primaryElementType"] as? String ?? "",
                elementTypeMixed: root["elementTypeMixed"] as? Bool ?? false,
                availableOptionGroups: parseStringArray(root, key: "availableOptionGroups"),
                strokeYabaCode: root["strokeYabaCode"] as? Int ?? 0,
                backgroundYabaCode: root["backgroundYabaCode"] as? Int ?? 0,
                strokeWidthKey: root["strokeWidthKey"] as? String ?? "thin",
                strokeStyle: root["strokeStyle"] as? String ?? "solid",
                roughnessKey: root["roughnessKey"] as? String ?? "architect",
                edgeKey: root["edgeKey"] as? String ?? "sharp",
                fontSizeKey: root["fontSizeKey"] as? String ?? "M",
                opacityStep: min(10, max(0, root["opacityStep"] as? Int ?? 10)),
                mixedStroke: root["mixedStroke"] as? Bool ?? false,
                mixedBackground: root["mixedBackground"] as? Bool ?? false,
                mixedStrokeWidth: root["mixedStrokeWidth"] as? Bool ?? false,
                mixedStrokeStyle: root["mixedStrokeStyle"] as? Bool ?? false,
                mixedRoughness: root["mixedRoughness"] as? Bool ?? false,
                mixedEdge: root["mixedEdge"] as? Bool ?? false,
                mixedFontSize: root["mixedFontSize"] as? Bool ?? false,
                mixedOpacity: root["mixedOpacity"] as? Bool ?? false,
                arrowTypeKey: root["arrowTypeKey"] as? String ?? "sharp",
                mixedArrowType: root["mixedArrowType"] as? Bool ?? false,
                startArrowheadKey: root["startArrowheadKey"] as? String ?? "none",
                endArrowheadKey: root["endArrowheadKey"] as? String ?? "none",
                mixedStartArrowhead: root["mixedStartArrowhead"] as? Bool ?? false,
                mixedEndArrowhead: root["mixedEndArrowhead"] as? Bool ?? false,
                availableStartArrowheads: parseStringArray(root, key: "availableStartArrowheads"),
                availableEndArrowheads: parseStringArray(root, key: "availableEndArrowheads"),
                fillStyleKey: root["fillStyleKey"] as? String ?? "solid",
                mixedFillStyle: root["mixedFillStyle"] as? Bool ?? false
            )
        )
    }

    private static func parseStringArray(_ root: [String: Any], key: String) -> [String] {
        guard let arr = root[key] as? [Any] else { return [] }
        return arr.compactMap { $0 as? String }.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
    }
}

public struct MathTapEvent: Sendable {
    public var isBlock: Bool
    public var documentPos: Int
    public var latex: String
}

public struct InlineLinkTapEvent: Sendable {
    public var documentPos: Int
    public var text: String
    public var url: String
}

public struct InlineMentionTapEvent: Sendable {
    public var documentPos: Int
    public var text: String
    public var bookmarkId: String
    public var bookmarkKindCode: Int
    public var bookmarkLabel: String
}
