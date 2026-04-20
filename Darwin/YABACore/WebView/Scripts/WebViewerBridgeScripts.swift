//
//  WebViewerBridgeScripts.swift
//  YABACore
//
//  JavaScript bodies for `window.YabaEditorBridge` in `viewer.html` (Milkdown Crepe).
//

import Foundation

/// Scripts evaluated after `bridgeReady` for the readable viewer — aligned with `yaba-web-components` bridge types.
public enum WebViewerBridgeScripts {
    /// Markdown document (`setDocumentJson` legacy name) plus optional `assetsBaseUrl` for `../assets/` resolution.
    public static func setDocumentJson(markdown: String, assetsBaseUrl: String?) -> String {
        let mdLiteral: String
        if let data = try? JSONSerialization.data(withJSONObject: markdown, options: []),
           let s = String(data: data, encoding: .utf8)
        {
            mdLiteral = s
        } else {
            mdLiteral = "\"\""
        }
        let options: String
        if let assetsBaseUrl {
            if let d = try? JSONSerialization.data(
                withJSONObject: ["assetsBaseUrl": assetsBaseUrl],
                options: []
            ),
                let o = String(data: d, encoding: .utf8)
            {
                options = ", \(o)"
            } else {
                options = ", {}"
            }
        } else {
            options = ""
        }
        return """
        (function(){
          try {
            var b = window.YabaEditorBridge;
            if (!b || !b.setDocumentJson) { return "no_bridge"; }
            var md = JSON.parse(\(mdLiteral));
            b.setDocumentJson(md\(options));
            return "ok";
          } catch(e) { return String(e); }
        })();
        """
    }

    public static func setReaderPreferences(_ prefs: ReaderPreferences) -> String {
        let obj: [String: String] = [
            "theme": prefs.theme.rawValue,
            "fontSize": prefs.fontSize.rawValue,
            "lineHeight": prefs.lineHeight.rawValue,
        ]
        guard let data = try? JSONSerialization.data(withJSONObject: obj, options: []),
              let json = String(data: data, encoding: .utf8)
        else {
            return #"(() => "bad_json")()"#
        }
        return """
        (function(){
          try {
            var b = window.YabaEditorBridge;
            if (!b || !b.setReaderPreferences) { return "no_bridge"; }
            b.setReaderPreferences(\(json));
            return "ok";
          } catch(e) { return String(e); }
        })();
        """
    }

    /// `AnnotationForRendering[]` JSON: `[{"id":"…","colorRole":"…"}]`.
    public static func setAnnotations(jsonArrayBody: String) -> String {
        let literal = jsonArrayBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "[]" : jsonArrayBody
        return """
        (function(){
          try {
            var b = window.YabaEditorBridge;
            if (!b || !b.setAnnotations) { return "no_bridge"; }
            b.setAnnotations(\(literal));
            return "ok";
          } catch(e) { return String(e); }
        })();
        """
    }

    public static func setWebChromeInsets(topPx: CGFloat) -> String {
        let t = Int(round(topPx))
        return """
        (function(){
          try {
            var b = window.YabaEditorBridge;
            if (!b || !b.setWebChromeInsets) { return "no_bridge"; }
            b.setWebChromeInsets(\(t));
            return "ok";
          } catch(e) { return String(e); }
        })();
        """
    }

    public static func navigateToTocItem(id: String, extrasJson: String?) -> String {
        let idJs = Self.javaScriptStringLiteral(id)
        let extrasJs: String
        if let extrasJson, !extrasJson.isEmpty {
            extrasJs = Self.javaScriptStringLiteral(extrasJson)
        } else {
            extrasJs = "null"
        }
        return """
        (function(){
          try {
            var b = window.YabaEditorBridge;
            if (!b || !b.navigateToTocItem) { return "no_bridge"; }
            b.navigateToTocItem(\(idJs), \(extrasJs));
            return "ok";
          } catch(e) { return String(e); }
        })();
        """
    }

    private static func javaScriptStringLiteral(_ s: String) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: s, options: []),
              let out = String(data: data, encoding: .utf8)
        else {
            return "\"\""
        }
        return out
    }

    public static func exportMarkdown() -> String {
        """
        (function(){
          try {
            var b = window.YabaEditorBridge;
            if (!b || !b.exportMarkdown) { return ""; }
            return b.exportMarkdown() || "";
          } catch(e) { return ""; }
        })();
        """
    }

    public static func startPdfExportJob(jobId: String) -> String {
        let idLit: String
        if let d = try? JSONSerialization.data(withJSONObject: jobId, options: []),
           let s = String(data: d, encoding: .utf8)
        {
            idLit = s
        } else {
            idLit = "\"\""
        }
        return """
        (function(){
          try {
            var b = window.YabaEditorBridge;
            if (!b || !b.startPdfExportJob) { return "no_bridge"; }
            b.startPdfExportJob(JSON.parse(\(idLit)));
            return "ok";
          } catch(e) { return String(e); }
        })();
        """
    }

    public static func scrollToAnnotation(annotationId: String) -> String {
        let idLit: String
        if let d = try? JSONSerialization.data(withJSONObject: annotationId, options: []),
           let s = String(data: d, encoding: .utf8)
        {
            idLit = s
        } else {
            idLit = "\"\""
        }
        return """
        (function(){
          try {
            var b = window.YabaEditorBridge;
            if (!b || !b.scrollToAnnotation) { return "no_bridge"; }
            b.scrollToAnnotation(JSON.parse(\(idLit)));
            return "ok";
          } catch(e) { return String(e); }
        })();
        """
    }
}
