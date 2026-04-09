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
}
