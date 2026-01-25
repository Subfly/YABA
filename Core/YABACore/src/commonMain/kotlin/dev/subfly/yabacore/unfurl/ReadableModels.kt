package dev.subfly.yabacore.unfurl

import dev.subfly.yabacore.model.utils.ReadableAssetRole
import dev.subfly.yabacore.model.utils.ReadableAssetRoleSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Immutable readable document snapshot extracted from HTML.
 *
 * Once created, a snapshot is never modified. New saves create new versions.
 * The document uses a closed set of block and inline types for deterministic
 * parsing and rendering.
 */
@Serializable
data class ReadableDocumentSnapshot(
    /** Schema version for AST structure compatibility */
    val schemaVersion: Int = 1,
    /** Content version within the bookmark (v1, v2, ...) */
    val contentVersion: Int,
    /** Original source URL */
    val sourceUrl: String,
    /** Extracted document title */
    val title: String? = null,
    /** Extracted author name */
    val author: String? = null,
    /** Creation timestamp in epoch milliseconds */
    val createdAt: Long,
    /** Ordered list of content blocks */
    val blocks: List<ReadableBlock>,
)

/**
 * Closed set of block types for readable content.
 *
 * Each block has a deterministic ID assigned during extraction (b0, b1, ...).
 */
@Serializable
sealed interface ReadableBlock {
    val id: String

    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        override val id: String,
        val inlines: List<ReadableInline>,
    ) : ReadableBlock

    @Serializable
    @SerialName("heading")
    data class Heading(
        override val id: String,
        @Serializable(with = ReadableHeadingLevelSerializer::class)
        val level: ReadableHeadingLevel,
        val inlines: List<ReadableInline>,
    ) : ReadableBlock

    @Serializable
    @SerialName("image")
    data class Image(
        override val id: String,
        /** Reference to asset in /content/assets/<assetId>.<ext> */
        val assetId: String,
        /** Image role: "hero", "inline" */
        @Serializable(with = ReadableAssetRoleSerializer::class)
        val role: ReadableAssetRole,
        /** Optional caption text */
        val caption: String? = null,
    ) : ReadableBlock

    @Serializable
    @SerialName("code")
    data class Code(
        override val id: String,
        /** Programming language if detected */
        val language: String? = null,
        /** Raw code text */
        val text: String,
    ) : ReadableBlock

    @Serializable
    @SerialName("list")
    data class ListBlock(
        override val id: String,
        /** True for ordered lists, false for unordered */
        val ordered: Boolean,
        /** List items, each containing blocks */
        val items: List<ListItem>,
    ) : ReadableBlock

    @Serializable
    @SerialName("quote")
    data class Quote(
        override val id: String,
        /** Nested blocks within the blockquote */
        val children: List<ReadableBlock>,
    ) : ReadableBlock

    @Serializable
    @SerialName("divider")
    data class Divider(
        override val id: String,
    ) : ReadableBlock
}

/**
 * A list item containing blocks.
 */
@Serializable
data class ListItem(
    val blocks: List<ReadableBlock>,
)

@Serializable
enum class ReadableHeadingLevel {
    H1,
    H2,
    H3,
    H4,
    H5,
    H6;

    companion object {
        fun fromInt(level: Int): ReadableHeadingLevel = when (level) {
            1 -> H1
            2 -> H2
            3 -> H3
            4 -> H4
            5 -> H5
            6 -> H6
            else -> H3
        }
    }
}

/**
 * Closed set of inline types for readable content.
 *
 * Only text nodes contain actual characters; other inlines wrap children.
 */
@Serializable
sealed interface ReadableInline {

    @Serializable
    @SerialName("text")
    data class Text(
        val content: String,
    ) : ReadableInline

    @Serializable
    @SerialName("bold")
    data class Bold(
        val children: List<ReadableInline>,
    ) : ReadableInline

    @Serializable
    @SerialName("italic")
    data class Italic(
        val children: List<ReadableInline>,
    ) : ReadableInline

    @Serializable
    @SerialName("underline")
    data class Underline(
        val children: List<ReadableInline>,
    ) : ReadableInline

    @Serializable
    @SerialName("strikethrough")
    data class Strikethrough(
        val children: List<ReadableInline>,
    ) : ReadableInline

    @Serializable
    @SerialName("code")
    data class Code(
        val content: String,
    ) : ReadableInline

    @Serializable
    @SerialName("link")
    data class Link(
        val href: String,
        val children: List<ReadableInline>,
    ) : ReadableInline

    @Serializable
    @SerialName("color")
    data class Color(
        /** Semantic color role: default, muted, accent, info, warning, success, error */
        val role: String,
        val children: List<ReadableInline>,
    ) : ReadableInline
}

/**
 * Result of readable content extraction, ready for persistence.
 *
 * This is returned by the Unfurler and contains both the document AST
 * and the downloaded asset bytes.
 */
data class ReadableUnfurl(
    val document: ReadableDocumentSnapshot,
    val assets: List<ReadableAsset>,
)

/**
 * An asset (image) extracted from the document with its bytes.
 */
data class ReadableAsset(
    /** UUID for the asset */
    val assetId: String,
    /** File extension (e.g., "jpg", "png", "webp") */
    val extension: String,
    /** Raw image bytes */
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ReadableAsset
        return assetId == other.assetId
    }

    override fun hashCode(): Int = assetId.hashCode()
}
