package dev.subfly.yaba.core.components.webview

import dev.subfly.yaba.core.webview.InlineLinkTapEvent
import dev.subfly.yaba.core.webview.InlineMentionTapEvent
import dev.subfly.yaba.core.webview.MathTapEvent
import dev.subfly.yaba.core.webview.YabaWebHostEvent
import org.json.JSONObject

internal fun createNativeHostMessageHandler(
    expectedBridgeFeature: String?,
    onBridgeReady: () -> Unit,
    onHostEvent: (YabaWebHostEvent) -> Unit,
    onAnnotationTap: (String) -> Unit,
    onMathTap: (MathTapEvent) -> Unit,
    onInlineLinkTap: (InlineLinkTapEvent) -> Unit,
    onInlineMentionTap: (InlineMentionTapEvent) -> Unit,
): (String) -> Unit =
    handler@{ json ->
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return@handler
        when (root.optString("type")) {
            "bridgeReady" -> {
                if (root.optString("feature") == expectedBridgeFeature) {
                    onBridgeReady()
                }
            }
            "converterJob" -> YabaConverterJobBridge.onConverterJobMessage(root)
            "editorPdfExport" -> YabaEditorPdfExportJobBridge.onEditorPdfExportMessage(root)
            else ->
                YabaNativeHostMessageParser.parse(
                    json = json,
                    onAnnotationTap = onAnnotationTap,
                    onMathTap = onMathTap,
                    onInlineLinkTap = onInlineLinkTap,
                    onInlineMentionTap = onInlineMentionTap,
                )?.let(onHostEvent)
        }
    }
