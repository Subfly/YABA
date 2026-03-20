package dev.subfly.yabacore.webview

import androidx.compose.runtime.Stable

@Stable
data class WebPdfConverterInput(
    val pdfUrl: String,
    val renderScale: Float = 1.2f,
)

@Stable
data class WebPdfTextSection(
    val sectionKey: String,
    val text: String,
)

@Stable
data class WebPdfConverterResult(
    val title: String?,
    val pageCount: Int,
    val firstPagePngDataUrl: String?,
    val sections: List<WebPdfTextSection>,
)
