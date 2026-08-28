package com.mangareader.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room [TypeConverter]s shared by every database in the app (reader's
 * `:translate:core` tables today; a future parser/library database
 * tomorrow). Kept dependency-free of any manga-domain type — only generic
 * JSON/primitive-collection conversions live here so this module never
 * needs to know what a "TextBlock" or a "Manga" is.
 */
object CommonConverters {

    val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @TypeConverter
    @JvmStatic
    fun stringListToJson(value: List<String>?): String? =
        value?.let { json.encodeToString(it) }

    @TypeConverter
    @JvmStatic
    fun jsonToStringList(value: String?): List<String>? =
        value?.let { json.decodeFromString<List<String>>(it) }

    @TypeConverter
    @JvmStatic
    fun floatArrayToCsv(value: FloatArray?): String? =
        value?.joinToString(",")

    @TypeConverter
    @JvmStatic
    fun csvToFloatArray(value: String?): FloatArray? =
        value?.takeIf { it.isNotBlank() }?.split(",")?.map { it.toFloat() }?.toFloatArray()

    @TypeConverter
    @JvmStatic
    fun stringIntMapToJson(value: Map<String, Int>?): String? =
        value?.let { json.encodeToString(it) }

    @TypeConverter
    @JvmStatic
    fun jsonToStringIntMap(value: String?): Map<String, Int>? =
        value?.let { json.decodeFromString<Map<String, Int>>(it) }
}
