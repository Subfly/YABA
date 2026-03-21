package dev.subfly.yabacore.webview

import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.utils.ReaderPreferences

/** Which web shell to load and what data to drive it with. */
sealed class YabaWebFeature {
    data class ReadableViewer(
        val html: String,
        val assetsBaseUrl: String?,
        val readerPreferences: ReaderPreferences,
        val platform: YabaWebPlatform,
        val appearance: YabaWebAppearance,
        val highlights: List<HighlightUiModel>,
    ) : YabaWebFeature()

    data class Editor(
        val initialDocumentJson: String,
        val assetsBaseUrl: String?,
        val highlights: List<HighlightUiModel> = emptyList(),
    ) : YabaWebFeature()

    data class HtmlConverter(
        val input: WebConverterInput?,
    ) : YabaWebFeature()

    data class PdfExtractor(
        val input: WebPdfConverterInput?,
    ) : YabaWebFeature()

    data class PdfViewer(
        val pdfUrl: String,
        val platform: YabaWebPlatform,
        val appearance: YabaWebAppearance,
        val highlights: List<HighlightUiModel>,
    ) : YabaWebFeature()
}
