package dev.subfly.yabacore.webview

import androidx.compose.runtime.Stable

@Stable
data class WebEpubConverterInput(
    val epubDataUrl: String,
)

@Stable
data class WebEpubConverterResult(
    val coverPngDataUrl: String?,
)
