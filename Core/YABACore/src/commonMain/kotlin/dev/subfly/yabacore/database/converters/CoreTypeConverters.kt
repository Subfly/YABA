package dev.subfly.yabacore.database.converters

import androidx.room3.TypeConverter
import dev.subfly.yabacore.model.highlight.HighlightType
import dev.subfly.yabacore.model.utils.BookmarkKind
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
    fun highlightTypeToString(value: HighlightType?): String? = value?.name

    @TypeConverter
    fun stringToHighlightType(value: String?): HighlightType? =
        value?.let { runCatching { HighlightType.valueOf(it) }.getOrNull() }
}
