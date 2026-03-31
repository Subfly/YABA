package dev.subfly.yaba.core.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Subtype metadata for note bookmarks.
 *
 * [documentRelativePath] is relative to the app working directory (same convention as other bookmarks).
 */
@Entity(
    tableName = "note_bookmarks",
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
data class NoteBookmarkEntity(
    @PrimaryKey val bookmarkId: String,
    /** Canonical note body on disk (document JSON, e.g. `bookmarks/<id>/note/body.json`). */
    val documentRelativePath: String,
    /**
     * Stable [ReadableVersionEntity.id] used for highlight anchors.
     * TODO: FIX PATH
     * The mirrored document lives at [CoreConstants.FileSystem.Linkmark.readableVersionJsonPath].
     */
    val readableVersionId: String,
)
