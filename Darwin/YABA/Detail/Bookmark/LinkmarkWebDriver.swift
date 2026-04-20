//
//  Created by Ali Taha on 20.04.2026.
//

import Foundation

/// Retains access to the active readable `WKWebViewRuntime` for bridge commands (export, etc.).
@MainActor
final class LinkmarkWebDriver {
    weak var runtime: WKWebViewRuntime?

    func exportMarkdown() async -> String {
        guard let runtime else { return "" }
        let script = WebViewerBridgeScripts.exportMarkdown()
        return (try? await runtime.evaluateJavaScriptStringResult(script)) ?? ""
    }

    func startPdfExportJob(jobId: String) async {
        guard let runtime else { return }
        let script = WebViewerBridgeScripts.startPdfExportJob(jobId: jobId)
        _ = try? await runtime.evaluateJavaScriptStringResult(script)
    }
}
