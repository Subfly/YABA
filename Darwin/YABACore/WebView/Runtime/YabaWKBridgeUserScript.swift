//
//  YabaWKBridgeUserScript.swift
//  YABACore
//
//  Injects `window.YabaNativeHost.postMessage(json)` → `webkit.messageHandlers.yabaNativeHost`.
//

import WebKit

public enum YabaWKBridgeUserScript {
    /// Must match `YabaNativeHostRouterDarwin.nativeHostScriptMessageName`.
    public static let messageHandlerName = "yabaNativeHost"

    /// Document-start injection so bundled shells can use the same contract as Android `JavascriptInterface`.
    public static func nativeHostBridgeScript() -> WKUserScript {
        let source = """
        (function() {
          if (window.YabaNativeHost && window.YabaNativeHost.__yabaDarwinInstalled) { return; }
          var bridge = {
            postMessage: function(json) {
              try {
                if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.\(messageHandlerName)) {
                  window.webkit.messageHandlers.\(messageHandlerName).postMessage(json);
                }
              } catch (e) {}
            }
          };
          bridge.__yabaDarwinInstalled = true;
          window.YabaNativeHost = bridge;
          window.YabaAndroidHost = bridge;
        })();
        """
        return WKUserScript(source: source, injectionTime: .atDocumentStart, forMainFrameOnly: false)
    }
}
