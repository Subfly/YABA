package dev.subfly.yaba.core.webview

/**
 * URIs for yaba-web-components bundled under `app/src/main/assets/files/web-components/`.
 * Use with [android.webkit.WebView] via `file:///android_asset/...` URLs.
 */
object WebComponentUris {
    private const val ASSET_PREFIX = "file:///android_asset/"
    private const val WEB_COMPONENTS_BASE = "files/web-components"

    fun getViewerUri(): String = "${ASSET_PREFIX}${WEB_COMPONENTS_BASE}/viewer.html"

    fun getEditorUri(): String = "${ASSET_PREFIX}${WEB_COMPONENTS_BASE}/editor.html"

    fun getConverterUri(): String = "${ASSET_PREFIX}${WEB_COMPONENTS_BASE}/converter.html"

    fun getPdfViewerUri(): String = "${ASSET_PREFIX}${WEB_COMPONENTS_BASE}/pdf-viewer.html"

    fun getEpubViewerUri(): String = "${ASSET_PREFIX}${WEB_COMPONENTS_BASE}/epub-viewer.html"

    /**
     * Base URL for loading web-component assets (chunks, CSS, etc.).
     */
    fun getWebComponentsBaseUrl(): String = "${ASSET_PREFIX}${WEB_COMPONENTS_BASE}/"
}
