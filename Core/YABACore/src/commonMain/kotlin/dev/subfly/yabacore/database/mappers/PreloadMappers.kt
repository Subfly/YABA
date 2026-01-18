package dev.subfly.yabacore.database.mappers

import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.preload.model.PreloadCollection
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Instant

internal fun PreloadCollection.toFolderDomain(now: Instant): FolderDomainModel =
    FolderDomainModel(
        id = id,
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
        id = id,
        label = label,
        icon = icon,
        color = YabaColor.fromCode(color),
        createdAt = now,
        editedAt = now,
        order = 0,
    )
