package dev.subfly.yabacore.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.subfly.yabacore.model.annotation.AnnotationType
import dev.subfly.yabacore.model.utils.YabaColor

/**
 * Room entity for persisted annotations (read-it-later, PDF).
 *
 * - [READABLE]: identity is stored in TipTap JSON via `yabaAnnotation` marks; DB holds metadata only.
 * - [PDF]: positional data is in [extrasJson] ([dev.subfly.yabacore.model.annotation.PdfAnnotationExtras]).
 */
@Entity(
    tableName = "annotations",
    foreignKeys = [
        ForeignKey(
            entity = BookmarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookmarkId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ReadableVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["readableVersionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookmarkId"]),
        Index(value = ["readableVersionId"]),
        Index(value = ["bookmarkId", "readableVersionId"]),
        Index(value = ["editedAt"]),
    ],
)
data class AnnotationEntity(
    @PrimaryKey val id: String,
    val bookmarkId: String,
    val readableVersionId: String,
    val type: AnnotationType,
    val colorRole: YabaColor,
    val note: String?,
    val quoteText: String?,
    val extrasJson: String?,
    val createdAt: Long,
    val editedAt: Long,
)
