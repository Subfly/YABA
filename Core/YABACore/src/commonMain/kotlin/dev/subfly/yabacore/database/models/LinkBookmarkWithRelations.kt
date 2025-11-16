package dev.subfly.yabacore.database.models

import androidx.room.Embedded
import androidx.room.Relation
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity

data class LinkBookmarkWithRelations(
    @Embedded val bookmark: BookmarkEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "bookmarkId",
    )
    val link: LinkBookmarkEntity,
)
