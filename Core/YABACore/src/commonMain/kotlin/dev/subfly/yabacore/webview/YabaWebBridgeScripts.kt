package dev.subfly.yabacore.webview

/**
 * JS snippets for bridge readiness checks (must match yaba-web-components).
 */
object YabaWebBridgeScripts {
    const val EDITOR_BRIDGE_READY: String =
        "(function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady && window.YabaEditorBridge.isReady()); } catch(e){ return false; } })();"

    const val EDITOR_BRIDGE_READY_LOOSE: String =
        "(function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady); } catch(e){ return false; } })();"

    const val CONVERTER_BRIDGE_DEFINED: String =
        "(function(){ try { return typeof window.YabaConverterBridge !== 'undefined'; } catch(e){ return false; } })();"

    const val PDF_BRIDGE_READY: String =
        "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady && window.YabaPdfBridge.isReady()); } catch(e){ return false; } })();"

    const val PDF_BRIDGE_READY_LOOSE: String =
        "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();"

    const val ANNOTATION_TAP_SCHEME_PREFIX = "yaba://annotation-tap?"

    /** Editor math node tap — intercepted in the host WebView client (same pattern as highlight tap). */
    const val MATH_TAP_SCHEME_PREFIX = "yaba://math-tap?"
}
