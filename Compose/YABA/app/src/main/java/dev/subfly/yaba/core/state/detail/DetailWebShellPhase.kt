package dev.subfly.yaba.core.state.detail

/**
 * Unified phase for detail screens that host a WebView shell (note editor, canvas, document reader).
 * Computed via [computeDetailWebShellPhase] from each screen’s loading flags, payload presence,
 * and web-shell error state.
 */
enum class DetailWebShellPhase {
    /** Resolving bookmark data: no WebView payload yet (spinner only). */
    Loading,

    /**
     * Payload exists and the WebView must mount, but the shell has not reported ready yet
     * (`isLoading` still true). Compose the WebView and show a loading overlay until [Ready].
     */
    Bootstrapping,

    /** Web shell reported ready; show content without blocking overlay; chrome may show. */
    Ready,

    /** Missing payload after load settled, or the web shell failed to load. */
    Unavailable,
}

/**
 * @param isLoading When true with a payload, phase is [DetailWebShellPhase.Bootstrapping] (WebView
 *   must mount). When true without a payload, phase is [DetailWebShellPhase.Loading]. Combine
 *   multiple blocking flags when needed (e.g. link detail uses `isLoading || isUpdatingReadable`).
 */
fun computeDetailWebShellPhase(
    isLoading: Boolean,
    hasWebPayload: Boolean,
    webContentLoadFailed: Boolean,
): DetailWebShellPhase =
    when {
        webContentLoadFailed -> DetailWebShellPhase.Unavailable
        !isLoading && !hasWebPayload -> DetailWebShellPhase.Unavailable
        !isLoading && hasWebPayload -> DetailWebShellPhase.Ready
        isLoading && hasWebPayload -> DetailWebShellPhase.Bootstrapping
        else -> DetailWebShellPhase.Loading
    }
