package dev.subfly.yaba.core.database.mappers

import dev.subfly.yaba.core.database.entities.FolderEntity
import dev.subfly.yaba.core.database.entities.TagEntity
import dev.subfly.yaba.core.database.preload.model.PreloadCollection
import dev.subfly.yaba.core.model.utils.YabaColor
import kotlin.time.Instant

internal fun PreloadCollection.toFolderEntity(now: Instant): FolderEntity =
    FolderEntity(
        id = id,
        parentId = null,
        label = label,
        description = null,
        icon = icon,
        color = YabaColor.fromCode(color),
        createdAt = now.toEpochMilliseconds(),
        editedAt = now.toEpochMilliseconds(),
    )

internal fun PreloadCollection.toTagEntity(now: Instant): TagEntity =
    TagEntity(
        id = id,
        label = label,
        icon = icon,
        color = YabaColor.fromCode(color),
        createdAt = now.toEpochMilliseconds(),
        editedAt = now.toEpochMilliseconds(),
    )
