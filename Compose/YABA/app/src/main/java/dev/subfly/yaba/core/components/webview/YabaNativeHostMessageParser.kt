package dev.subfly.yaba.core.components.webview

import dev.subfly.yaba.core.webview.EditorFormattingState
import dev.subfly.yaba.core.webview.InlineLinkTapEvent
import dev.subfly.yaba.core.webview.InlineMentionTapEvent
import dev.subfly.yaba.core.webview.MathTapEvent
import dev.subfly.yaba.core.webview.CanvasHostMetrics
import dev.subfly.yaba.core.webview.CanvasHostStyleState
import dev.subfly.yaba.core.webview.Toc
import dev.subfly.yaba.core.webview.TocItem
import dev.subfly.yaba.core.webview.WebShellLoadResult
import dev.subfly.yaba.core.webview.YabaWebHostEvent
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses JSON from [YabaAndroidHostJsBridge.postMessage] into [YabaWebHostEvent] or tap side-effects.
 * Must stay aligned with `Extensions/yaba-web-components/src/bridge/yaba-native-host.ts`.
 */
internal object YabaNativeHostMessageParser {

    fun parse(
        json: String,
        onAnnotationTap: ((String) -> Unit)?,
        onMathTap: ((MathTapEvent) -> Unit)?,
        onInlineLinkTap: ((InlineLinkTapEvent) -> Unit)?,
        onInlineMentionTap: ((InlineMentionTapEvent) -> Unit)?,
    ): YabaWebHostEvent? {
        val root =
            runCatching { JSONObject(json) }.getOrNull()
                ?: return null
        return when (val type = root.optString("type")) {
            "shellLoad" -> parseShellLoad(root)
            "toc" -> parseToc(root)
            "noteAutosaveIdle" -> YabaWebHostEvent.NoteEditorIdleForAutosave
            "canvasAutosaveIdle" -> YabaWebHostEvent.CanvasIdleForAutosave
            "readerMetrics" -> parseReaderMetrics(root)
            "canvasMetrics" -> parseCanvasMetrics(root)
            "canvasStyleState" -> parseCanvasStyleState(root)
            "annotationTap" -> {
                val id = root.optString("id", "")
                if (id.isNotBlank()) onAnnotationTap?.invoke(id)
                null
            }
            "mathTap" -> {
                val kind = root.optString("kind", "")
                val pos = root.optInt("pos", -1)
                val latex = root.optString("latex", "")
                if (pos >= 0) {
                    onMathTap?.invoke(
                        MathTapEvent(
                            isBlock = kind == "block",
                            documentPos = pos,
                            latex = latex,
                        ),
                    )
                }
                null
            }
            "inlineLinkTap" -> {
                val pos = root.optInt("pos", -1)
                val text = root.optString("text", "")
                val url = root.optString("url", "")
                if (pos >= 0 && url.isNotBlank()) {
                    onInlineLinkTap?.invoke(
                        InlineLinkTapEvent(
                            documentPos = pos,
                            text = text,
                            url = url,
                        ),
                    )
                }
                null
            }
            "inlineMentionTap" -> {
                val pos = root.optInt("pos", -1)
                val text = root.optString("text", "")
                val bookmarkId = root.optString("bookmarkId", "")
                val bookmarkKindCode = root.optInt("bookmarkKindCode", 0)
                val bookmarkLabel = root.optString("bookmarkLabel", "")
                if (pos >= 0 && bookmarkId.isNotBlank()) {
                    onInlineMentionTap?.invoke(
                        InlineMentionTapEvent(
                            documentPos = pos,
                            text = text,
                            bookmarkId = bookmarkId,
                            bookmarkKindCode = bookmarkKindCode,
                            bookmarkLabel = bookmarkLabel,
                        ),
                    )
                }
                null
            }
            "converterJob" -> null
            "bridgeReady" -> null
            else -> null
        }
    }

    private fun parseShellLoad(root: JSONObject): YabaWebHostEvent.InitialContentLoad {
        val result = root.optString("result", "")
        val shell =
            when (result) {
                "loaded" -> WebShellLoadResult.Loaded
                "error" -> WebShellLoadResult.Error
                else -> WebShellLoadResult.Error
            }
        return YabaWebHostEvent.InitialContentLoad(shell)
    }

    private fun parseToc(root: JSONObject): YabaWebHostEvent.TableOfContentsChanged {
        if (root.isNull("toc")) {
            return YabaWebHostEvent.TableOfContentsChanged(toc = null)
        }
        val tocObj = root.optJSONObject("toc") ?: return YabaWebHostEvent.TableOfContentsChanged(Toc())
        val itemsArr = tocObj.optJSONArray("items") ?: return YabaWebHostEvent.TableOfContentsChanged(Toc())
        return YabaWebHostEvent.TableOfContentsChanged(Toc(parseTocItems(itemsArr)))
    }

    private fun parseTocItems(arr: JSONArray): List<TocItem> {
        val out = ArrayList<TocItem>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val children = parseTocItems(o.optJSONArray("children") ?: JSONArray())
            val extras = o.optString("extrasJson", "").takeIf { it.isNotBlank() }
            out.add(
                TocItem(
                    id = o.optString("id", ""),
                    title = o.optString("title", ""),
                    level = o.optInt("level", 1),
                    children = children,
                    extrasJson = extras,
                ),
            )
        }
        return out
    }

    private fun parseReaderMetrics(root: JSONObject): YabaWebHostEvent.ReaderMetrics {
        val can = root.optBoolean("canCreateAnnotation")
        val page = root.optInt("currentPage", 1).coerceAtLeast(1)
        val count = root.optInt("pageCount", 1).coerceAtLeast(1)
        val formatting =
            if (root.has("formatting")) {
                parseEditorFormatting(root.optJSONObject("formatting") ?: JSONObject())
            } else {
                null
            }
        return YabaWebHostEvent.ReaderMetrics(
            canCreateAnnotation = can,
            currentPage = page,
            pageCount = count,
            editorFormatting = formatting,
        )
    }

    private fun parseCanvasMetrics(root: JSONObject): YabaWebHostEvent.CanvasMetrics =
        YabaWebHostEvent.CanvasMetrics(
            CanvasHostMetrics(
                activeTool = root.optString("activeTool", "selection"),
                hasSelection = root.optBoolean("hasSelection"),
                canUndo = root.optBoolean("canUndo"),
                canRedo = root.optBoolean("canRedo"),
                gridModeEnabled = root.optBoolean("gridModeEnabled", true),
                objectsSnapModeEnabled = root.optBoolean("objectsSnapModeEnabled", true),
            ),
        )

    private fun parseCanvasStyleState(root: JSONObject): YabaWebHostEvent.CanvasStyleState =
        YabaWebHostEvent.CanvasStyleState(
            CanvasHostStyleState(
                hasSelection = root.optBoolean("hasSelection"),
                selectionCount = root.optInt("selectionCount"),
                selectionElementTypes = parseStringArray(root, "selectionElementTypes"),
                primaryElementType = root.optString("primaryElementType", ""),
                elementTypeMixed = root.optBoolean("elementTypeMixed"),
                availableOptionGroups = parseStringArray(root, "availableOptionGroups"),
                strokeYabaCode = root.optInt("strokeYabaCode"),
                backgroundYabaCode = root.optInt("backgroundYabaCode"),
                strokeWidthKey = root.optString("strokeWidthKey", "thin"),
                strokeStyle = root.optString("strokeStyle", "solid"),
                roughnessKey = root.optString("roughnessKey", "architect"),
                edgeKey = root.optString("edgeKey", "sharp"),
                fontSizeKey = root.optString("fontSizeKey", "M"),
                opacityStep = root.optInt("opacityStep", 10).coerceIn(0, 10),
                mixedStroke = root.optBoolean("mixedStroke"),
                mixedBackground = root.optBoolean("mixedBackground"),
                mixedStrokeWidth = root.optBoolean("mixedStrokeWidth"),
                mixedStrokeStyle = root.optBoolean("mixedStrokeStyle"),
                mixedRoughness = root.optBoolean("mixedRoughness"),
                mixedEdge = root.optBoolean("mixedEdge"),
                mixedFontSize = root.optBoolean("mixedFontSize"),
                mixedOpacity = root.optBoolean("mixedOpacity"),
                arrowTypeKey = root.optString("arrowTypeKey", "sharp"),
                mixedArrowType = root.optBoolean("mixedArrowType"),
                startArrowheadKey = root.optString("startArrowheadKey", "none"),
                endArrowheadKey = root.optString("endArrowheadKey", "none"),
                mixedStartArrowhead = root.optBoolean("mixedStartArrowhead"),
                mixedEndArrowhead = root.optBoolean("mixedEndArrowhead"),
                availableStartArrowheads = parseStringArray(root, "availableStartArrowheads"),
                availableEndArrowheads = parseStringArray(root, "availableEndArrowheads"),
                fillStyleKey = root.optString("fillStyleKey", "solid"),
                mixedFillStyle = root.optBoolean("mixedFillStyle"),
            ),
        )

    private fun parseStringArray(root: JSONObject, key: String): List<String> {
        val arr: JSONArray = root.optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val s = arr.optString(i, "").trim()
                if (s.isNotEmpty()) add(s)
            }
        }
    }

    private fun parseEditorFormatting(json: JSONObject): EditorFormattingState =
        EditorFormattingState(
            headingLevel = json.optInt("headingLevel"),
            bold = json.optBoolean("bold"),
            italic = json.optBoolean("italic"),
            underline = json.optBoolean("underline"),
            strikethrough = json.optBoolean("strikethrough"),
            subscript = json.optBoolean("subscript"),
            superscript = json.optBoolean("superscript"),
            code = json.optBoolean("code"),
            codeBlock = json.optBoolean("codeBlock"),
            blockquote = json.optBoolean("blockquote"),
            bulletList = json.optBoolean("bulletList"),
            orderedList = json.optBoolean("orderedList"),
            taskList = json.optBoolean("taskList"),
            inlineMath = json.optBoolean("inlineMath"),
            blockMath = json.optBoolean("blockMath"),
            canUndo = json.optBoolean("canUndo"),
            canRedo = json.optBoolean("canRedo"),
            canIndent = json.optBoolean("canIndent"),
            canOutdent = json.optBoolean("canOutdent"),
            inTable = json.optBoolean("inTable"),
            canAddRowBefore = json.optBoolean("canAddRowBefore"),
            canAddRowAfter = json.optBoolean("canAddRowAfter"),
            canDeleteRow = json.optBoolean("canDeleteRow"),
            canAddColumnBefore = json.optBoolean("canAddColumnBefore"),
            canAddColumnAfter = json.optBoolean("canAddColumnAfter"),
            canDeleteColumn = json.optBoolean("canDeleteColumn"),
            textHighlight = json.optBoolean("textHighlight"),
        )
}
