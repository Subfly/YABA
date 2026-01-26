package dev.subfly.yabacore.database.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.entities.TagEntity

/**
 * Generic bookmark row + its common relations used for list/grid previews.
 *
 * - Folder is always expected to exist (FK).
 * - Tags are optional (0..N).
 * - Subtype entity (e.g., LinkBookmarkEntity) may be null depending on bookmark kind.
 */
data class BookmarkWithRelations(
    @Embedded val bookmark: BookmarkEntity,
    @Relation(
        parentColumn = "folderId",
        entityColumn = "id",
    )
    val folder: FolderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TagBookmarkCrossRef::class,
            parentColumn = "bookmarkId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
)
