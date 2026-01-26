package dev.subfly.yabacore.database.mappers

import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.preload.model.PreloadCollection
import dev.subfly.yabacore.model.utils.YabaColor
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
        order = 0,
    )

internal fun PreloadCollection.toTagEntity(now: Instant): TagEntity =
    TagEntity(
        id = id,
        label = label,
        icon = icon,
        color = YabaColor.fromCode(color),
        createdAt = now.toEpochMilliseconds(),
        editedAt = now.toEpochMilliseconds(),
        order = 0,
    )
