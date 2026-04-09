package dev.subfly.yaba.core.webview

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Builds JSON strings for `window.YabaCanvasBridge.applyCanvasInline`, matching
 * [Extensions/yaba-web-components/src/bridge/canvas-inline.ts] `ApplyCanvasInlinePayload`.
 */
object CanvasInlineApplyJson {
    fun setUrl(url: String?): String =
        buildJsonObject {
            put("op", "setUrl")
            if (url == null) put("url", JsonNull) else put("url", url)
        }.toString()

    fun setMention(
        text: String,
        bookmarkId: String,
        bookmarkKindCode: Int,
        bookmarkLabel: String,
    ): String =
        buildJsonObject {
            put("op", "setMention")
            put("text", text)
            put("bookmarkId", bookmarkId)
            put("bookmarkKindCode", JsonPrimitive(bookmarkKindCode))
            put("bookmarkLabel", bookmarkLabel)
        }.toString()

    fun insertTextWithUrl(displayText: String, url: String): String =
        buildJsonObject {
            put("op", "insertTextWithUrl")
            put("displayText", displayText)
            put("url", url)
        }.toString()

    fun insertTextWithMention(
        displayText: String,
        bookmarkId: String,
        bookmarkKindCode: Int,
        bookmarkLabel: String,
    ): String =
        buildJsonObject {
            put("op", "insertTextWithMention")
            put("displayText", displayText)
            put("bookmarkId", bookmarkId)
            put("bookmarkKindCode", JsonPrimitive(bookmarkKindCode))
            put("bookmarkLabel", bookmarkLabel)
        }.toString()

    fun clearLink(elementId: String): String =
        buildJsonObject {
            put("op", "clearLink")
            put("elementId", elementId)
        }.toString()

    fun setUrlOnElement(elementId: String, url: String?): String =
        buildJsonObject {
            put("op", "setUrlOnElement")
            put("elementId", elementId)
            if (url == null) put("url", JsonNull) else put("url", url)
        }.toString()

    fun setMentionOnElement(
        elementId: String,
        text: String,
        bookmarkId: String,
        bookmarkKindCode: Int,
        bookmarkLabel: String,
    ): String =
        buildJsonObject {
            put("op", "setMentionOnElement")
            put("elementId", elementId)
            put("text", text)
            put("bookmarkId", bookmarkId)
            put("bookmarkKindCode", JsonPrimitive(bookmarkKindCode))
            put("bookmarkLabel", bookmarkLabel)
        }.toString()

}
