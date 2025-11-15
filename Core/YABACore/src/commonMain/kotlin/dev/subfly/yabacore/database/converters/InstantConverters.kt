package dev.subfly.yabacore.database.converters

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlin.jvm.JvmStatic

object InstantConverters {
    @TypeConverter
    @JvmStatic
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    @JvmStatic
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }
}
