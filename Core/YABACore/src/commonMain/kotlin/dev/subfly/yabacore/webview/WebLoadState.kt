package dev.subfly.yabacore.webview

/**
 * Load lifecycle for the WebView shell (page + optional progress).
 */
sealed interface WebLoadState {
    /** No document loaded or load was reset. */
    data object Idle : WebLoadState

    /** Main frame is loading. */
    data class Loading(val progressFraction: Float? = null) : WebLoadState

    /** Main frame finished loading; JS bridge may still be initializing. */
    data object PageFinished : WebLoadState

    /** Shell and reader/converter bridge are ready for commands. */
    data object BridgeReady : WebLoadState

    /** WebView render process died; host should recover or show error. */
    data object RendererCrashed : WebLoadState
}
