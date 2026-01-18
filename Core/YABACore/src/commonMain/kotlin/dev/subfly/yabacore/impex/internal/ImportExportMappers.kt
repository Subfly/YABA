package dev.subfly.yabacore.impex.internal

import dev.subfly.yabacore.common.IdGenerator
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
    folderId: String,
    now: Instant,
    resolvedId: String? = null,
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
        description = description,
        createdAt = created,
        editedAt = edited,
        viewCount = 0,
        isPrivate = false,
        isPinned = false,
        localImagePath = null,
        localIconPath = null,
        url = link,
        domain = domainValue,
        linkType = linkType,
        videoUrl = videoUrl,
    )
}

internal fun LinkBookmarkDomainModel.toCodable(): CodableBookmark = CodableBookmark(
    bookmarkId = id,
    label = label,
    description = description,
    link = url,
    domain = domain,
    createdAt = createdAt.toIsoString(),
    editedAt = editedAt.toIsoString(),
    imageUrl = null,
    iconUrl = null,
    videoUrl = videoUrl,
    readableHTML = null,
    type = linkType.code,
    version = 0,
    imageData = null,
    iconData = null,
)

internal fun FolderDomainModel.toCodable(bookmarkIds: List<String>): CodableCollection =
    CodableCollection(
        collectionId = id,
        label = label,
        icon = icon,
        createdAt = createdAt.toIsoString(),
        editedAt = editedAt.toIsoString(),
        color = color.code,
        type = 1, // folder
        bookmarks = bookmarkIds,
        version = 0,
        parent = parentId,
        children = emptyList(), // children filled by exporter using tree
        order = order,
    )

internal fun TagDomainModel.toCodable(bookmarkIds: List<String>): CodableCollection =
    CodableCollection(
        collectionId = id,
        label = label,
        icon = icon,
        createdAt = createdAt.toIsoString(),
        editedAt = editedAt.toIsoString(),
        color = color.code,
        type = 2, // tag
        bookmarks = bookmarkIds,
        version = 0,
        parent = null,
        children = emptyList(),
        order = order,
    )

internal fun buildCodableContent(
    folders: List<CodableCollection>,
    bookmarks: List<CodableBookmark>,
): CodableContent = CodableContent(
    id = IdGenerator.newId(),
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
