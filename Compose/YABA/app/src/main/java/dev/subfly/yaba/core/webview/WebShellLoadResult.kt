package dev.subfly.yaba.core.webview

/**
 * One-shot outcome for initial web shell content (editor, viewer, converter, PDF/EPUB).
 * Emitted at most once per load cycle from web components via [YabaWebHostEvent.InitialContentLoad].
 */
enum class WebShellLoadResult {
    /** Initial content applied and ready for display (or conversion succeeded for converter flows). */
    Loaded,

    /** Initial load or conversion failed; native UI may show unavailable state. */
    Error,
}
