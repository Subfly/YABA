package dev.subfly.yaba.core.webview

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
    val author: String?,
    val subject: String?,
    val creationDate: String?,
    val pageCount: Int,
    val firstPagePngDataUrl: String?,
    val sections: List<WebPdfTextSection>,
)
