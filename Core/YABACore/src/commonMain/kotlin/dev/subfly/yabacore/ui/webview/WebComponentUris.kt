package dev.subfly.yabacore.ui.webview

import dev.subfly.yabacore.yabacore.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * URIs for yaba-web-components shipped as raw resources.
 * Used by YabaWebView to load viewer.html and converter.html.
 */
@OptIn(ExperimentalResourceApi::class)
object WebComponentUris {
    private const val WEB_COMPONENTS_BASE = "files/web-components"

    fun getViewerUri(): String = Res.getUri("$WEB_COMPONENTS_BASE/viewer.html")

    fun getEditorUri(): String = Res.getUri("$WEB_COMPONENTS_BASE/editor.html")

    fun getConverterUri(): String = Res.getUri("$WEB_COMPONENTS_BASE/converter.html")

    /**
     * Base URL for loading web-component assets (chunks, CSS, etc.).
     * Use this as the base URL when loading the HTML so relative paths resolve.
     */
    fun getWebComponentsBaseUrl(): String = Res.getUri("$WEB_COMPONENTS_BASE/")
}
