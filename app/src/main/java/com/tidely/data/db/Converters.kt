package com.tidely.data.db

import androidx.room.TypeConverter
import com.tidely.data.model.TideType
import java.util.Date

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromTideType(value: TideType): String {
        return value.name
    }

    @TypeConverter
    fun toTideType(value: String): TideType {
        return TideType.valueOf(value)
    }
}
