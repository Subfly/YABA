package dev.subfly.yaba.core.webview

object YabaCanvasBridgeScripts {
    fun setSceneJsonScript(sceneJson: String): String {
        val escaped = escapeForJsSingleQuotedString(sceneJson)
        return "window.YabaCanvasBridge?.setSceneJson?.('$escaped');"
    }

    const val GET_SCENE_JSON_SCRIPT: String = "window.YabaCanvasBridge?.getSceneJson?.() ?? '';"

    fun setActiveToolScript(tool: String): String {
        val escaped = escapeForJsSingleQuotedString(tool)
        return "window.YabaCanvasBridge?.setActiveTool?.('$escaped');"
    }

    const val UNDO_SCRIPT: String = "window.YabaCanvasBridge?.undo?.();"
    const val REDO_SCRIPT: String = "window.YabaCanvasBridge?.redo?.();"
    const val DELETE_SELECTED_SCRIPT: String = "window.YabaCanvasBridge?.deleteSelected?.();"

    fun insertImageFromDataUrlScript(dataUrl: String): String {
        val escaped = escapeForJsSingleQuotedString(dataUrl)
        return "window.YabaCanvasBridge?.insertImageFromDataUrl?.('$escaped');"
    }

    fun applySelectionStyleScript(json: String): String {
        val escaped = escapeForJsSingleQuotedString(json)
        return "window.YabaCanvasBridge?.applySelectionStyle?.('$escaped');"
    }

    fun canvasLayerScript(action: String): String {
        val escaped = escapeForJsSingleQuotedString(action)
        return "window.YabaCanvasBridge?.canvasLayer?.('$escaped');"
    }

    const val TOGGLE_GRID_MODE_SCRIPT: String = "window.YabaCanvasBridge?.toggleGridMode?.();"

    const val TOGGLE_OBJECTS_SNAP_MODE_SCRIPT: String =
        "window.YabaCanvasBridge?.toggleObjectsSnapMode?.();"

    fun applyCanvasInlineScript(json: String): String {
        val escaped = escapeForJsSingleQuotedString(json)
        return "window.YabaCanvasBridge?.applyCanvasInline?.('$escaped');"
    }

    const val GET_CANVAS_SELECTION_LINK_CONTEXT_SCRIPT: String =
        "window.YabaCanvasBridge?.getCanvasSelectionLinkContext?.() ?? '{}';"

    /**
     * Starts async export; result is read via [EXPORT_CANVAS_IMAGE_POLL_SCRIPT].
     * Android [WebView.evaluateJavascript] does not reliably await returned Promises, so we poll
     * [window.__yabaCanvasExport] instead of returning the Promise from a single evaluation.
     */
    fun exportCanvasImageKickoffScript(requestJson: String): String {
        val escaped = escapeForJsSingleQuotedString(requestJson)
        return """
            (function(){
              try {
                delete window.__yabaCanvasExport;
                window.__yabaCanvasExport = { status: 'pending', value: null };
                var b = window.YabaCanvasBridge;
                if (!b || !b.exportImage) {
                  window.__yabaCanvasExport = { status: 'ready', value: JSON.stringify({ok:false,error:'no_export'}) };
                  return;
                }
                var p = b.exportImage('$escaped');
                if (p && typeof p.then === 'function') {
                  p.then(function(v) {
                    window.__yabaCanvasExport = { status: 'ready', value: v };
                  }).catch(function(e) {
                    window.__yabaCanvasExport = { status: 'ready', value: JSON.stringify({ok:false,error:String(e)}) };
                  });
                } else {
                  window.__yabaCanvasExport = { status: 'ready', value: JSON.stringify({ok:false,error:'not_a_promise'}) };
                }
              } catch (e) {
                window.__yabaCanvasExport = { status: 'ready', value: JSON.stringify({ok:false,error:String(e)}) };
              }
            })();
        """.trimIndent()
    }

    const val EXPORT_CANVAS_IMAGE_POLL_SCRIPT: String =
        """
            (function(){
              try {
                var w = window.__yabaCanvasExport;
                if (!w) return '';
                if (w.status === 'pending') return '';
                if (w.status === 'ready') {
                  var v = w.value;
                  delete window.__yabaCanvasExport;
                  return (typeof v === 'string') ? v : '';
                }
              } catch (e) {}
              return '';
            })();
        """
}
