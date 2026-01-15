@file:OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)

package dev.subfly.yabacore.impex.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
internal data class CodableContent(
    val id: String? = null,
    val exportedFrom: String? = null,
    val collections: List<CodableCollection>? = null,
    val bookmarks: List<CodableBookmark> = emptyList(),
)

@Serializable
internal data class CodableCollection(
    val collectionId: String,
    val label: String,
    val icon: String,
    val createdAt: String,
    val editedAt: String,
    val color: Int,
    val type: Int,
    val bookmarks: List<String> = emptyList(),
    val version: Int = 0,
    val parent: String? = null,
    val children: List<String> = emptyList(),
    val order: Int = 0,
)

@Serializable
internal data class CodableBookmark(
    val bookmarkId: String? = null,
    val label: String? = null,
    @SerialName("bookmarkDescription")
    val description: String? = null,
    val link: String,
    val domain: String? = null,
    val createdAt: String? = null,
    val editedAt: String? = null,
    val imageUrl: String? = null,
    val iconUrl: String? = null,
    val videoUrl: String? = null,
    val readableHTML: String? = null,
    val type: Int? = null,
    val version: Int? = null,
    @Serializable(with = Base64ByteArraySerializer::class)
    val imageData: ByteArray? = null,
    @Serializable(with = Base64ByteArraySerializer::class)
    val iconData: ByteArray? = null,
)

/**
 * CSV mapping helpers mirror Darwin's enum names to keep sheet-mapping UX aligned.
 */
enum class MappableCsvHeader {
    URL,
    LABEL,
    DESCRIPTION,
    CREATED_AT,
}

/**
 * Export formats supported by the shared library.
 */
enum class ExportFormat {
    JSON,
    CSV,
    MARKDOWN,
    HTML,
}

/**
 * Small summary of what got imported, useful for UI to surface counts.
 */
data class ImportSummary(
    val folders: Int,
    val tags: Int,
    val bookmarks: Int,
)

/**
 * Base64 serializer so we stay wire-compatible with Swift's `Data` encoding.
 */
internal object Base64ByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Base64ByteArray", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(Base64.Default.encode(value))
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val raw = decoder.decodeString()
        return runCatching { Base64.Default.decode(raw) }.getOrElse { ByteArray(0) }
    }
}

/**
 * Simple pair tying a tag to a bookmark. Extracted here to avoid leaking DAO models.
 */
internal data class TagLink(
    val tagId: Uuid,
    val bookmarkId: Uuid,
)

