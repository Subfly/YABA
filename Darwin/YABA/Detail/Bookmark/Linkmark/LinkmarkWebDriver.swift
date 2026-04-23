//
//  Created by Ali Taha on 20.04.2026.
//

import Foundation

/// Retains access to the active readable `WKWebViewRuntime` for bridge commands (export, etc.).
@MainActor
final class LinkmarkWebDriver {
    weak var runtime: WKWebViewRuntime?

    func getSelectionDraft(bookmarkId: String, readableVersionId: String) async -> ReadableSelectionDraft? {
        // Do not require `getCanCreateAnnotation` here: reader metrics and this check can disagree,
        // and the snapshot/selectedText path is authoritative for opening the sheet.
        guard let quote = await getSelectionQuoteSnapshot() else { return nil }
        return ReadableSelectionDraft(
            bookmarkId: bookmarkId,
            readableVersionId: readableVersionId,
            quoteText: quote.selectedText,
            extrasJson: nil,
            annotationType: .readable
        )
    }

    func canCreateAnnotation() async -> Bool {
        guard let runtime else { return false }
        let script = WebViewerBridgeScripts.getCanCreateAnnotation()
        let raw = (try? await runtime.evaluateJavaScriptStringResult(script)) ?? "0"
        return raw == "1" || raw.lowercased() == "true"
    }

    func applyAnnotationToSelection(annotationId: String) async -> Bool {
        guard let runtime else { return false }
        let script = WebViewerBridgeScripts.applyAnnotationToSelection(annotationId: annotationId)
        let raw = (try? await runtime.evaluateJavaScriptStringResult(script)) ?? "0"
        return raw == "1" || raw.lowercased() == "true"
    }

    func removeAnnotationFromDocument(annotationId: String) async -> Int {
        guard let runtime else { return 0 }
        let script = WebViewerBridgeScripts.removeAnnotationFromDocument(annotationId: annotationId)
        let raw = (try? await runtime.evaluateJavaScriptStringResult(script)) ?? "0"
        return max(0, Int(raw) ?? 0)
    }

    func getDocumentJson() async -> String {
        guard let runtime else { return "" }
        return (try? await runtime.evaluateJavaScriptStringResult(WebReaderBridgeFacades.getDocumentJsonScript)) ?? ""
    }

    func setAnnotations(jsonArrayBody: String) async {
        guard let runtime else { return }
        let script = WebViewerBridgeScripts.setAnnotations(jsonArrayBody: jsonArrayBody)
        _ = try? await runtime.evaluateJavaScriptStringResult(script)
    }

    func exportMarkdown() async -> String {
        guard let runtime else { return "" }
        // Match Android behavior: unfocus first, then retry briefly while bridge settles.
        let unfocusScript =
            """
            (function(){
              try {
                var b = window.YabaEditorBridge;
                if (b && b.unFocus) { b.unFocus(); return "ok"; }
                var active = document.activeElement;
                if (active && active.blur) { active.blur(); }
                return "ok";
              } catch(e) { return ""; }
            })();
            """
        _ = try? await runtime.evaluateJavaScriptStringResult(unfocusScript)
        let script = WebViewerBridgeScripts.exportMarkdown()
        for attempt in 0 ..< 4 {
            let markdown = (try? await runtime.evaluateJavaScriptStringResult(script)) ?? ""
            if !markdown.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                return markdown
            }
            if attempt < 3 {
                try? await Task.sleep(nanoseconds: 120_000_000)
            }
        }
        return ""
    }

    func startPdfExportJob(jobId: String) async {
        guard let runtime else { return }
        let script = WebViewerBridgeScripts.startPdfExportJob(jobId: jobId)
        _ = try? await runtime.evaluateJavaScriptStringResult(script)
    }

    private func getSelectionQuoteSnapshot() async -> AnnotationQuoteSnapshot? {
        guard let runtime else { return nil }
        let script = WebViewerBridgeScripts.getSelectionSnapshot()
        let raw = (try? await runtime.evaluateJavaScriptStringResult(script)) ?? ""
        if let fromJson = Self.quoteFromSelectionSnapshotJson(raw) {
            return fromJson
        }
        // Fallback: bridge returns selected text only (snapshot JSON failed or bridge mismatch).
        let textScript = WebViewerBridgeScripts.getSelectedText()
        let textRaw = (try? await runtime.evaluateJavaScriptStringResult(textScript)) ?? ""
        let text = textRaw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return nil }
        return AnnotationQuoteSnapshot.fromSelectedText(text)
    }

    private static func quoteFromSelectionSnapshotJson(_ raw: String) -> AnnotationQuoteSnapshot? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, let data = trimmed.data(using: .utf8) else { return nil }
        if let snapshot = try? JSONDecoder().decode(EditorSelectionSnapshot.self, from: data) {
            let text = snapshot.selectedText.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !text.isEmpty else { return nil }
            return AnnotationQuoteSnapshot(
                selectedText: text,
                prefixText: snapshot.prefixText?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                suffixText: snapshot.suffixText?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
            )
        }
        guard
            let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let selected = obj["selectedText"] as? String
        else { return nil }
        let t = selected.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty else { return nil }
        let prefix = (obj["prefixText"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
        let suffix = (obj["suffixText"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
        return AnnotationQuoteSnapshot(
            selectedText: t,
            prefixText: prefix,
            suffixText: suffix
        )
    }
}

private struct EditorSelectionSnapshot: Codable {
    var selectedText: String
    var prefixText: String?
    var suffixText: String?
}
