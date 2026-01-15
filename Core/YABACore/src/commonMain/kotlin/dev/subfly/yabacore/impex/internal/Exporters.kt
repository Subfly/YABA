package dev.subfly.yabacore.impex.internal

import dev.subfly.yabacore.impex.model.CodableBookmark
import dev.subfly.yabacore.impex.model.CodableCollection
import dev.subfly.yabacore.impex.model.ExportFormat
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi


@OptIn(ExperimentalUuidApi::class)
internal object Exporters {
    fun export(snapshot: ExportSnapshot, format: ExportFormat): ByteArray {
        val (collections, bookmarks) = buildCodable(snapshot)
        return when (format) {
            ExportFormat.JSON -> exportJson(collections, bookmarks)
            ExportFormat.CSV -> exportCsv(bookmarks)
            ExportFormat.MARKDOWN -> exportMarkdown(collections, bookmarks)
            ExportFormat.HTML -> exportHtml(collections, bookmarks)
        }
    }

    private fun buildCodable(
        snapshot: ExportSnapshot,
    ): Pair<List<CodableCollection>, List<CodableBookmark>> {
        val bookmarksByFolder = snapshot.bookmarks.groupBy { it.folderId }
        val childFolders = snapshot.folders.groupBy { it.parentId }
        val tagLinks = snapshot.tagLinks.groupBy { it.tagId }

        val folderCollections = snapshot.folders.map { folder ->
            val bookmarkIds = bookmarksByFolder[folder.id].orEmpty().map { it.id }
            val children = childFolders[folder.id].orEmpty().map { it.id.toString() }
            folder.toCodable(bookmarkIds).copy(children = children)
        }

        val tagCollections = snapshot.tags.map { tag ->
            val bookmarkIds = tagLinks[tag.id].orEmpty().map { it.bookmarkId }
            tag.toCodable(bookmarkIds)
        }

        val bookmarkCodables = snapshot.bookmarks.map { it.toCodable() }

        val collections = folderCollections + tagCollections
        return collections to bookmarkCodables
    }

    private fun exportJson(
        collections: List<CodableCollection>,
        bookmarks: List<CodableBookmark>,
    ): ByteArray = buildCodableContent(
        folders = collections,
        bookmarks = bookmarks,
    ).let { content ->
        Json.encodeToString(content).encodeToByteArray()
    }

    private fun exportCsv(bookmarks: List<CodableBookmark>): ByteArray {
        val header = CsvUtils.expectedHeader.joinToString(",")
        val rows = bookmarks.joinToString("\n") { bookmark ->
            val values = listOf(
                bookmark.bookmarkId ?: "",
                bookmark.label ?: bookmark.link,
                bookmark.description ?: "",
                bookmark.link,
                bookmark.domain ?: "",
                bookmark.createdAt ?: "",
                bookmark.editedAt ?: "",
                bookmark.imageUrl ?: "",
                bookmark.iconUrl ?: "",
                bookmark.videoUrl ?: "",
                (bookmark.type ?: 1).toString(),
                (bookmark.version ?: 0).toString(),
            )
            values.joinToString(",") { CsvUtils.escape(it) }
        }
        return listOf(header, rows)
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .encodeToByteArray()
    }

    private fun exportMarkdown(
        collections: List<CodableCollection>,
        bookmarks: List<CodableBookmark>,
    ): ByteArray {
        val bookmarkMap = bookmarks.associateBy { it.bookmarkId ?: it.link }
        val builder = StringBuilder()
        builder.appendLine("# 📚 YABA").appendLine()

        collections.filter { it.type == 1 }.forEach { collection ->
            val collectionBookmarks = collection.bookmarks.mapNotNull { bookmarkMap[it] }
            if (collectionBookmarks.isEmpty()) return@forEach

            builder.appendLine("## 📁 ${collection.label}").appendLine()
            collectionBookmarks.forEach { bookmark ->
                val title = bookmark.label ?: bookmark.link
                builder.appendLine("### 🔖 $title").appendLine()
                builder.appendLine("🔗 ${bookmark.link}").appendLine()
                bookmark.description?.takeIf { it.isNotEmpty() }?.let {
                    builder.appendLine("📝 $it").appendLine()
                }
            }
        }

        val bookmarkedIds = collections.flatMap { it.bookmarks }.toSet()
        val withoutCollection = bookmarks.filter { !bookmarkedIds.contains(it.bookmarkId) }
        if (withoutCollection.isNotEmpty()) {
            builder.appendLine("## 📂 Uncategorized").appendLine()
            withoutCollection.forEach { bookmark ->
                val title = bookmark.label ?: bookmark.link
                builder.appendLine("### 🔖 $title").appendLine()
                builder.appendLine("🔗 ${bookmark.link}").appendLine()
                bookmark.description?.takeIf { it.isNotEmpty() }?.let {
                    builder.appendLine("📝 $it").appendLine()
                }
            }
        }

        return builder.toString().encodeToByteArray()
    }

    private fun exportHtml(
        collections: List<CodableCollection>,
        bookmarks: List<CodableBookmark>,
    ): ByteArray {
        val bookmarkMap = bookmarks.associateBy { it.bookmarkId ?: it.link }
        val folderMap = collections.filter { it.type == 1 }
            .associateBy { it.collectionId }
        val roots = folderMap.values.filter { it.parent == null }
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
        sb.appendLine("<!-- This is an automatically generated file.")
        sb.appendLine("     It will be read and overwritten.")
        sb.appendLine("     DO NOT EDIT! -->")
        sb.appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
        sb.appendLine("<TITLE>Bookmarks</TITLE>")
        sb.appendLine("<H1>Bookmarks</H1>")
        sb.appendLine()
        sb.appendLine("<DL><p>")

        fun appendFolder(collection: CodableCollection, indent: Int) {
            val prefix = "    ".repeat(indent)
            sb.appendLine("$prefix<DT><H3>${escapeHtml(collection.label)}</H3>")
            sb.appendLine("$prefix<DL><p>")
            collection.bookmarks.mapNotNull { bookmarkMap[it] }.forEach { bookmark ->
                val title = escapeHtml(bookmark.label ?: bookmark.link)
                sb.appendLine("$prefix    <DT><A HREF=\"${bookmark.link}\">$title</A>")
            }
            collection.children.mapNotNull { folderMap[it] }.forEach { child ->
                appendFolder(child, indent + 1)
            }
            sb.appendLine("$prefix</DL><p>")
        }

        roots.forEach { appendFolder(it, 1) }

        val bookmarkedIds = collections.flatMap { it.bookmarks }.toSet()
        val withoutCollection = bookmarks.filter { !bookmarkedIds.contains(it.bookmarkId) }
        if (withoutCollection.isNotEmpty()) {
            sb.appendLine("    <DT><H3>Uncategorized</H3>")
            sb.appendLine("    <DL><p>")
            withoutCollection.forEach { bookmark ->
                val title = escapeHtml(bookmark.label ?: bookmark.link)
                sb.appendLine("        <DT><A HREF=\"${bookmark.link}\">$title</A>")
            }
            sb.appendLine("    </DL><p>")
        }

        sb.appendLine("</DL><p>")
        return sb.toString().encodeToByteArray()
    }

    private fun escapeHtml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
