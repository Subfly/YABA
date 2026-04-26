package dev.subfly.yaba.core.database.converters

import androidx.room3.TypeConverter
import dev.subfly.yaba.core.model.annotation.AnnotationType
import org.json.JSONArray
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.model.utils.DocmarkType
import dev.subfly.yaba.core.model.utils.YabaColor
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
    fun annotationTypeToString(value: AnnotationType?): String? = value?.name

    @TypeConverter
    fun stringToAnnotationType(value: String?): AnnotationType? =
        value?.let { runCatching { AnnotationType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun docmarkTypeToString(value: DocmarkType?): String? = value?.name

    @TypeConverter
    fun stringToDocmarkType(value: String?): DocmarkType? =
        value?.let { runCatching { DocmarkType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun stringListToJson(value: List<String>?): String? =
        value?.let { JSONArray(it).toString() }

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(value)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.getString(i))
                }
            }
        }.getOrDefault(emptyList())
    }
}
