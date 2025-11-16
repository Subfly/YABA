package dev.subfly.yabacore.database.converters

import androidx.room.TypeConverter
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
object CoreTypeConverters {
    @TypeConverter
    fun uuidToString(value: Uuid?): String? = value?.toString()

    @TypeConverter
    fun stringToUuid(value: String?): Uuid? =
        value?.let { runCatching { Uuid.parse(it) }.getOrNull() }

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun bookmarkKindToInt(value: BookmarkKind?): Int? = value?.code

    @TypeConverter
    fun intToBookmarkKind(value: Int?): BookmarkKind? = value?.let { BookmarkKind.fromCode(it) }

    @TypeConverter
    fun linkTypeToInt(value: LinkType?): Int? = value?.code

    @TypeConverter
    fun intToLinkType(value: Int?): LinkType? = value?.let { LinkType.fromCode(it) }

    @TypeConverter
    fun yabaColorToInt(value: YabaColor?): Int? = value?.code

    @TypeConverter
    fun intToYabaColor(value: Int?): YabaColor? = value?.let { YabaColor.fromCode(it) }
}
