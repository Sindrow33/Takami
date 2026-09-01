package com.mangareader.translate.core

import com.mangareader.translate.api.TextBlock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * (De)serializes `List<TextBlock>` to the single JSON string stored per
 * (pageKey, dstLang) row (§4/§8). Isolated in its own object so the Room
 * entity layer never needs a `TypeConverter` registered for `TextBlock`
 * itself — we store/load the JSON string directly as a plain column.
 */
object TextBlockJson {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(blocks: List<TextBlock>): String = json.encodeToString(blocks)

    fun decode(raw: String): List<TextBlock> = json.decodeFromString(raw)
}
