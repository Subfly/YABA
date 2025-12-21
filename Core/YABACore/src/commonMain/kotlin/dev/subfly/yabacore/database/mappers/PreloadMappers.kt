@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.mappers

import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.preload.model.PreloadCollection
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun PreloadCollection.toFolderDomain(now: Instant): FolderDomainModel =
    FolderDomainModel(
        id = Uuid.parse(id),
        parentId = null,
        label = label,
        description = null,
        icon = icon,
        color = YabaColor.fromCode(color),
        createdAt = now,
        editedAt = now,
        order = 0,
    )

internal fun PreloadCollection.toTagDomain(now: Instant): TagDomainModel =
    TagDomainModel(
        id = Uuid.parse(id),
        label = label,
        icon = icon,
        color = YabaColor.fromCode(color),
        createdAt = now,
        editedAt = now,
        order = 0,
    )
