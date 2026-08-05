package com.fueru.app.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStringList(list: List<String>?): String? = list?.joinToString("|")

    @TypeConverter
    fun toStringList(raw: String?): List<String>? = when {
        raw == null -> null
        raw.isEmpty() -> emptyList()
        else -> raw.split("|")
    }
}
