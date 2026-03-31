package dev.subfly.yaba.core.components.webview

import dev.subfly.yaba.core.webview.WebConverterAsset
import dev.subfly.yaba.core.webview.WebConverterResult
import dev.subfly.yaba.core.webview.WebEpubConverterResult
import dev.subfly.yaba.core.webview.WebLinkMetadata
import dev.subfly.yaba.core.webview.WebPdfConverterResult
import dev.subfly.yaba.core.webview.WebPdfTextSection
import dev.subfly.yaba.core.webview.normalizeBridgeOptionalString
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONArray
import org.json.JSONObject

internal object YabaConverterJobBridge {

    private val htmlJobs = ConcurrentHashMap<String, CompletableDeferred<Result<WebConverterResult>>>()
    private val pdfJobs = ConcurrentHashMap<String, CompletableDeferred<Result<WebPdfConverterResult>>>()
    private val epubJobs = ConcurrentHashMap<String, CompletableDeferred<Result<WebEpubConverterResult>>>()

    fun registerHtmlJob(jobId: String, deferred: CompletableDeferred<Result<WebConverterResult>>) {
        htmlJobs[jobId] = deferred
    }

    fun registerPdfJob(jobId: String, deferred: CompletableDeferred<Result<WebPdfConverterResult>>) {
        pdfJobs[jobId] = deferred
    }

    fun registerEpubJob(jobId: String, deferred: CompletableDeferred<Result<WebEpubConverterResult>>) {
        epubJobs[jobId] = deferred
    }

    fun removeHtmlJob(jobId: String) {
        htmlJobs.remove(jobId)
    }

    fun removePdfJob(jobId: String) {
        pdfJobs.remove(jobId)
    }

    fun removeEpubJob(jobId: String) {
        epubJobs.remove(jobId)
    }

    fun onConverterJobMessage(root: JSONObject) {
        val jobId = root.optString("jobId", "")
        if (jobId.isBlank()) return
        val kind = root.optString("kind", "")
        val status = root.optString("status", "")
        when (kind) {
            "html" -> {
                val d = htmlJobs[jobId] ?: return
                when (status) {
                    "done" -> {
                        val outputJson = root.optString("outputJson", "")
                        d.complete(parseHtmlOutput(outputJson))
                    }
                    "error" ->
                        d.complete(
                            Result.failure(
                                IllegalStateException(
                                    root.optString("error", "HTML conversion failed"),
                                ),
                            ),
                        )
                }
            }
            "pdf" -> {
                val d = pdfJobs[jobId] ?: return
                when (status) {
                    "done" -> {
                        val outputJson = root.optString("outputJson", "")
                        d.complete(parsePdfOutput(outputJson))
                    }
                    "error" ->
                        d.complete(
                            Result.failure(
                                IllegalStateException(
                                    root.optString("error", "PDF extraction failed"),
                                ),
                            ),
                        )
                }
            }
            "epub" -> {
                val d = epubJobs[jobId] ?: return
                when (status) {
                    "done" -> {
                        val outputJson = root.optString("outputJson", "")
                        d.complete(parseEpubOutput(outputJson))
                    }
                    "error" ->
                        d.complete(
                            Result.failure(
                                IllegalStateException(
                                    root.optString("error", "EPUB extraction failed"),
                                ),
                            ),
                        )
                }
            }
        }
    }

    private fun parseHtmlOutput(outputJson: String): Result<WebConverterResult> =
        runCatching {
            val json = JSONObject(outputJson)
            val documentJson = json.optString("documentJson", "")
            val assetsArray = json.optJSONArray("assets") ?: JSONArray()
            val assets = mutableListOf<WebConverterAsset>()
            for (i in 0 until assetsArray.length()) {
                val item = assetsArray.optJSONObject(i) ?: continue
                assets.add(
                    WebConverterAsset(
                        placeholder = item.optString("placeholder", ""),
                        url = item.optString("url", ""),
                        alt = item.optString("alt").takeIf { it.isNotEmpty() },
                    ),
                )
            }
            val linkMetaJson = json.optJSONObject("linkMetadata") ?: JSONObject()
            val linkMetadata =
                WebLinkMetadata(
                    cleanedUrl =
                        linkMetaJson.optString("cleanedUrl", "").normalizeBridgeOptionalString()
                            ?: "",
                    title = linkMetaJson.optString("title").normalizeBridgeOptionalString(),
                    description =
                        linkMetaJson.optString("description").normalizeBridgeOptionalString(),
                    author = linkMetaJson.optString("author").normalizeBridgeOptionalString(),
                    date = linkMetaJson.optString("date").normalizeBridgeOptionalString(),
                    audio = linkMetaJson.optString("audio").normalizeBridgeOptionalString(),
                    video = linkMetaJson.optString("video").normalizeBridgeOptionalString(),
                    image = linkMetaJson.optString("image").normalizeBridgeOptionalString(),
                    logo = linkMetaJson.optString("logo").normalizeBridgeOptionalString(),
                )
            WebConverterResult(
                documentJson = documentJson,
                assets = assets,
                linkMetadata = linkMetadata,
            )
        }

    private fun parsePdfOutput(outputJson: String): Result<WebPdfConverterResult> =
        runCatching {
            val output = JSONObject(outputJson)
            val sectionsJson = output.optJSONArray("sections") ?: JSONArray()
            val sections = buildList {
                for (index in 0 until sectionsJson.length()) {
                    val section = sectionsJson.optJSONObject(index) ?: continue
                    add(
                        WebPdfTextSection(
                            sectionKey = section.optString("sectionKey", ""),
                            text = section.optString("text", ""),
                        ),
                    )
                }
            }
            WebPdfConverterResult(
                title = output.optString("title").normalizeBridgeOptionalString(),
                author = output.optString("author").normalizeBridgeOptionalString(),
                subject = output.optString("subject").normalizeBridgeOptionalString(),
                creationDate =
                    output.optString("creationDate").normalizeBridgeOptionalString(),
                pageCount = output.optInt("pageCount", 0),
                firstPagePngDataUrl =
                    output.optString("firstPagePngDataUrl").normalizeBridgeOptionalString(),
                sections = sections,
            )
        }

    private fun parseEpubOutput(outputJson: String): Result<WebEpubConverterResult> =
        runCatching {
            val output = JSONObject(outputJson)
            WebEpubConverterResult(
                coverPngDataUrl =
                    output.optString("coverPngDataUrl").normalizeBridgeOptionalString(),
                title = output.optString("title").normalizeBridgeOptionalString(),
                author = output.optString("author").normalizeBridgeOptionalString(),
                description =
                    output.optString("description").normalizeBridgeOptionalString(),
                pubdate = output.optString("pubdate").normalizeBridgeOptionalString(),
            )
        }
}
