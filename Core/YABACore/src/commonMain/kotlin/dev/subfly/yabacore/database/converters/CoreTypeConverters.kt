package dev.subfly.yabacore.database.converters

import androidx.room3.TypeConverter
import dev.subfly.yabacore.model.annotation.AnnotationType
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.DocmarkType
import dev.subfly.yabacore.model.utils.ReadableAssetRole
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Instant

object CoreTypeConverters {
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun bookmarkKindToInt(value: BookmarkKind?): Int? = value?.code

    @TypeConverter
    fun intToBookmarkKind(value: Int?): BookmarkKind? = value?.let { BookmarkKind.fromCode(it) }

    @TypeConverter
    fun yabaColorToInt(value: YabaColor?): Int? = value?.code

    @TypeConverter
    fun intToYabaColor(value: Int?): YabaColor? = value?.let { YabaColor.fromCode(it) }

    @TypeConverter
    fun readableAssetRoleToString(value: ReadableAssetRole?): String? = value?.name

    @TypeConverter
    fun stringToReadableAssetRole(value: String?): ReadableAssetRole? =
        value?.let { ReadableAssetRole.fromRaw(it) }

    @TypeConverter
    fun annotationTypeToString(value: AnnotationType?): String? = value?.name

    @TypeConverter
    fun stringToAnnotationType(value: String?): AnnotationType? =
        value?.let { runCatching { AnnotationType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun docmarkTypeToString(value: DocmarkType?): String? = value?.name

    @TypeConverter
    fun stringToDocmarkType(value: String?): DocmarkType? =
        value?.let { runCatching { DocmarkType.valueOf(it) }.getOrNull() }
}
