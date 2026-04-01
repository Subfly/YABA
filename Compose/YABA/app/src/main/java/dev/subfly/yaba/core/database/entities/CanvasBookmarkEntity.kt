package dev.subfly.yaba.core.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Subtype metadata for canvmark bookmarks.
 *
 * [sceneRelativePath] stores canonical Excalidraw scene JSON under bookmarks/<id>/canvas/.
 */
@Entity(
    tableName = "canvas_bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookmarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookmarkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookmarkId"], unique = true),
    ],
)
data class CanvasBookmarkEntity(
    @PrimaryKey val bookmarkId: String,
    val sceneRelativePath: String,
)
