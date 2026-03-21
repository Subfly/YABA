package dev.subfly.yabacore.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Subtype metadata for note bookmarks.
 *
 * [markdownRelativePath] is relative to the app working directory (same convention as other bookmarks).
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
    /** Canonical note body on disk (e.g. `bookmarks/<id>/note/body.md`). */
    val markdownRelativePath: String,
    /**
     * Stable [ReadableVersionEntity.id] used for highlight anchors.
     * The mirrored markdown lives at [CoreConstants.FileSystem.Linkmark.readableVersionPath].
     */
    val readableVersionId: String,
)
