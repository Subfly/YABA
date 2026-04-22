package dev.subfly.yaba.core.webview

import dev.subfly.yaba.core.model.ui.AnnotationUiModel
import dev.subfly.yaba.core.model.utils.ReaderPreferences

/** Which web shell to load and what data to drive it with. */
sealed class YabaWebFeature {
    data class ReadableViewer(
        val initialDocumentJson: String,
        val assetsBaseUrl: String?,
        val readerPreferences: ReaderPreferences,
        val platform: YabaWebPlatform,
        val appearance: YabaWebAppearance,
        val annotations: List<AnnotationUiModel>,
    ) : YabaWebFeature()

    data class Editor(
        val initialDocumentJson: String,
        val assetsBaseUrl: String?,
        val placeholderText: String? = null,
        val platform: YabaWebPlatform = YabaWebPlatform.Android,
        val appearance: YabaWebAppearance = YabaWebAppearance.Auto,
        val readerPreferences: ReaderPreferences = ReaderPreferences(),
        /**
         * Increments only when bootstrapping the editor from disk for this session. Hosts apply
         * [initialDocumentJson] when this changes — not when the JSON string is refreshed after save
         * (same generation) so the WebView is not reset.
         */
        val documentLoadGeneration: Int = 0,
    ) : YabaWebFeature()

    data class Canvas(
        val initialSceneJson: String,
        val platform: YabaWebPlatform = YabaWebPlatform.Android,
        val appearance: YabaWebAppearance = YabaWebAppearance.Auto,
        /**
         * Increments only when bootstrapping the canvas from disk. Hosts apply [initialSceneJson]
         * when this changes, not when JSON is refreshed after save.
         */
        val sceneLoadGeneration: Int = 0,
    ) : YabaWebFeature()

    data class HtmlConverter(
        val input: WebConverterInput?,
    ) : YabaWebFeature()

    data class PdfExtractor(
        val input: WebPdfConverterInput?,
    ) : YabaWebFeature()

    data class EpubExtractor(
        val input: WebEpubConverterInput?,
    ) : YabaWebFeature()

    data class PdfViewer(
        val pdfUrl: String,
        val platform: YabaWebPlatform,
        val appearance: YabaWebAppearance,
        val annotations: List<AnnotationUiModel>,
    ) : YabaWebFeature()

    data class EpubViewer(
        val epubUrl: String,
        val readerPreferences: ReaderPreferences,
        val platform: YabaWebPlatform,
        val appearance: YabaWebAppearance,
        val annotations: List<AnnotationUiModel>,
    ) : YabaWebFeature()
}
