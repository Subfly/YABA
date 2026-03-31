package dev.subfly.yaba.core.webview

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

    const val EPUB_BRIDGE_READY: String =
        "(function(){ try { return !!(window.YabaEpubBridge && window.YabaEpubBridge.isReady && window.YabaEpubBridge.isReady()); } catch(e){ return false; } })();"

    const val EPUB_BRIDGE_READY_LOOSE: String =
        "(function(){ try { return !!(window.YabaEpubBridge && window.YabaEpubBridge.isReady); } catch(e){ return false; } })();"
}
