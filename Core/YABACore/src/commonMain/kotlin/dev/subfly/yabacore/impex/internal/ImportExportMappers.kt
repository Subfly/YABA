@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.impex.internal

import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.impex.model.CodableBookmark
import dev.subfly.yabacore.impex.model.CodableCollection
import dev.subfly.yabacore.impex.model.CodableContent
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun CodableCollection.toFolderDomain(
    idResolver: IdResolver,
    now: Instant,
): FolderDomainModel = FolderDomainModel(
    id = idResolver.resolve(collectionId),
    parentId = parent?.let { idResolver.resolve(it) },
    label = label,
    description = null,
    icon = icon,
    color = YabaColor.fromCode(color),
    createdAt = parseInstant(createdAt) ?: now,
    editedAt = parseInstant(editedAt) ?: now,
    order = order,
)

internal fun CodableCollection.toTagDomain(
    idResolver: IdResolver,
    now: Instant,
): TagDomainModel = TagDomainModel(
    id = idResolver.resolve(collectionId),
    label = label,
    icon = icon,
    color = YabaColor.fromCode(color),
    createdAt = parseInstant(createdAt) ?: now,
    editedAt = parseInstant(editedAt) ?: now,
    order = order,
)

internal fun CodableBookmark.toDomain(
    idResolver: IdResolver,
    folderId: Uuid,
    now: Instant,
    resolvedId: Uuid? = null,
): LinkBookmarkDomainModel {
    val created = parseInstant(createdAt) ?: now
    val edited = parseInstant(editedAt) ?: created
    val resolvedIdValue = resolvedId ?: bookmarkId?.let { idResolver.resolve(it) }
    ?: idResolver.resolve(null)
    val linkType = LinkType.fromCode(type ?: LinkType.NONE.code)
    val labelValue = label?.takeIf { it.isNotBlank() } ?: link
    val domainValue = domain?.takeIf { it.isNotBlank() } ?: extractDomain(link)

    return LinkBookmarkDomainModel(
        id = resolvedIdValue,
        folderId = folderId,
        kind = BookmarkKind.LINK,
        label = labelValue,
        createdAt = created,
        editedAt = edited,
        viewCount = 0,
        isPrivate = false,
        isPinned = false,
        description = description,
        url = link,
        domain = domainValue,
        linkType = linkType,
        previewImageUrl = imageUrl,
        previewIconUrl = iconUrl,
        videoUrl = videoUrl,
    )
}

internal fun LinkBookmarkDomainModel.toCodable(): CodableBookmark = CodableBookmark(
    bookmarkId = id.toString(),
    label = label,
    description = description,
    link = url,
    domain = domain,
    createdAt = createdAt.toIsoString(),
    editedAt = editedAt.toIsoString(),
    imageUrl = previewImageUrl,
    iconUrl = previewIconUrl,
    videoUrl = videoUrl,
    readableHTML = null,
    type = linkType.code,
    version = 0,
    imageData = null,
    iconData = null,
)

internal fun FolderDomainModel.toCodable(bookmarkIds: List<Uuid>): CodableCollection =
    CodableCollection(
        collectionId = id.toString(),
        label = label,
        icon = icon,
        createdAt = createdAt.toIsoString(),
        editedAt = editedAt.toIsoString(),
        color = color.code,
        type = 1, // folder
        bookmarks = bookmarkIds.map { it.toString() },
        version = 0,
        parent = parentId?.toString(),
        children = emptyList(), // children filled by exporter using tree
        order = order,
    )

internal fun TagDomainModel.toCodable(bookmarkIds: List<Uuid>): CodableCollection =
    CodableCollection(
        collectionId = id.toString(),
        label = label,
        icon = icon,
        createdAt = createdAt.toIsoString(),
        editedAt = editedAt.toIsoString(),
        color = color.code,
        type = 2, // tag
        bookmarks = bookmarkIds.map { it.toString() },
        version = 0,
        parent = null,
        children = emptyList(),
        order = order,
    )

internal fun buildCodableContent(
    folders: List<CodableCollection>,
    bookmarks: List<CodableBookmark>,
): CodableContent = CodableContent(
    id = Uuid.random().toString(),
    exportedFrom = "YABA",
    collections = folders,
    bookmarks = bookmarks,
)

internal fun parseInstant(raw: String?): Instant? =
    raw?.let {
        runCatching { Instant.parse(it) }.getOrNull()
    }

internal fun Instant.toIsoString(): String = toString()

internal fun extractDomain(url: String): String {
    val withoutProtocol = url.substringAfter("://", url)
    val candidate = withoutProtocol.substringBefore("/")
    return candidate.substringBefore("?").substringBefore("#")
}
