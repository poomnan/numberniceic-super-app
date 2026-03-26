package com.numberniceic.utils

import android.content.Context
import com.numberniceic.data.persons.OutfitDayPalettesResponse
import com.numberniceic.data.persons.OutfitDayPalettesUpsertRequest
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import org.json.JSONArray
import org.json.JSONObject

data class OutfitApiColorEntry(
    val colorName: String,
    val sourceColorName: String,
    val defaultHex: String,
    val activeHex: String,
    val isOverridden: Boolean,
    val dayNumber: Int?,
    val dayName: String?
)

object OutfitApiColorMappingManager {
    private const val PREFS_NAME = "outfit_api_color_mapping_prefs"
    private const val KEY_OVERRIDES = "overrides_json"
    private const val KEY_DAY_PALETTE_OVERRIDES = "day_palette_overrides_json"

    val defaultMap: LinkedHashMap<String, String> = linkedMapOf(
        "สีแดง" to "#FF0000",
        "สีแดงเลือดนก" to "#8B0000",
        "แดงอ่อน" to "#FF6B6B",
        "สีขาว" to "#FFFFFF",
        "สีเหลืองอ่อน" to "#FFF59D",
        "สีชมพู" to "#FF00FF",
        "สีชมพูอ่อน" to "#FF66FF",
        "สีชมพูเข้ม" to "#C000C0",
        "สีเขียวเข้ม" to "#2E7D32",
        "สีเขียวอ่อน" to "#90EE90",
        "สีเหลืองเข้ม" to "#FBC02D",
        "สีแสด" to "#FF8C00",
        "สีส้มเข้ม" to "#F57C00",
        "สีฟ้า" to "#03A9F4",
        "สีฟ้าอ่อน" to "#81D4FA",
        "สีน้ำเงิน" to "#1976D2",
        "สีดำ" to "#000000",
        "สีม่วง" to "#6A1B9A",
        "สีน้ำตาลเข้ม" to "#5D4037",
        "สีเทา" to "#757575",
        "สีเทาอ่อน" to "#BDBDBD",
        "สีเทาอ่อนไล่ตามเฉด" to "#E0E0E0"
    )
    private val dayColorSlots: Map<Int, List<String>> = mapOf(
        1 to listOf("สีแดง", "สีแดงเลือดนก", "แดงอ่อน"),
        2 to listOf("สีขาว", "สีเหลืองอ่อน", "สีเหลืองอ่อน"),
        3 to listOf("สีชมพู", "สีชมพูอ่อน", "สีชมพูเข้ม"),
        4 to listOf("สีเขียวเข้ม", "สีเขียวอ่อน", "สีเขียวอ่อน"),
        5 to listOf("สีเหลืองเข้ม", "สีแสด", "สีส้มเข้ม"),
        6 to listOf("สีฟ้า", "สีฟ้าอ่อน", "สีน้ำเงิน"),
        7 to listOf("สีดำ", "สีม่วง", "สีน้ำตาลเข้ม"),
        8 to listOf("สีเทา", "สีเทาอ่อน", "สีเทาอ่อนไล่ตามเฉด")
    )
    private val numberToDayName: Map<Int, String> = mapOf(
        1 to "อาทิตย์",
        2 to "จันทร์",
        3 to "อังคาร",
        4 to "พุธ",
        5 to "พฤหัสบดี",
        6 to "ศุกร์",
        7 to "เสาร์",
        8 to "พุธ กลางคืน"
    )
    private val legacyDayDefault4Shades: Map<Int, List<String>> = mapOf(
        1 to listOf("#FF0000", "#CC0000", "#FF3333", "#990000"),
        2 to listOf("#FFFFFF", "#FFFFCC", "#FFFF99", "#FFFFC2"),
        3 to listOf("#FF00FF", "#FF66FF", "#CC00CC", "#FF007F"),
        4 to listOf("#009900", "#006600", "#00CC00", "#336600"),
        5 to listOf("#FFFF00", "#FF8000", "#FFFF33", "#CC6600"),
        6 to listOf("#00FFFF", "#0080FF", "#0000FF", "#66B2FF"),
        7 to listOf("#000000", "#6600CC", "#9933FF", "#4C0099"),
        8 to listOf("#663300", "#808080", "#C0C0C0", "#E0E0E0")
    )

    fun resolveHex(context: Context, thaiColorName: String): String? {
        val key = thaiColorName.trim()
        if (key.isEmpty()) return null
        val overrides = loadOverrides(context)
        return overrides[key] ?: defaultMap[key]
    }

    fun getEntries(context: Context): List<OutfitApiColorEntry> {
        val overrides = loadOverrides(context)
        val entries = mutableListOf<OutfitApiColorEntry>()
        dayColorSlots.toSortedMap().forEach { (dayNumber, slots) ->
            val dayName = numberToDayName[dayNumber]
            val totalByName = slots.groupingBy { it }.eachCount()
            val runningByName = mutableMapOf<String, Int>()
            slots.forEach { sourceName ->
                val defaultHex = defaultMap[sourceName] ?: return@forEach
                val active = overrides[sourceName] ?: defaultHex
                val index = (runningByName[sourceName] ?: 0) + 1
                runningByName[sourceName] = index
                val displayName = if ((totalByName[sourceName] ?: 0) > 1 && index > 1) {
                    "$sourceName (ซ้ำ $index)"
                } else {
                    sourceName
                }
                entries.add(
                    OutfitApiColorEntry(
                        colorName = displayName,
                        sourceColorName = sourceName,
                        defaultHex = defaultHex,
                        activeHex = active,
                        isOverridden = overrides.containsKey(sourceName),
                        dayNumber = dayNumber,
                        dayName = dayName
                    )
                )
            }
        }
        return entries
    }

    fun saveOverride(context: Context, thaiColorName: String, hexColor: String) {
        val colorName = normalizeSourceColorName(thaiColorName)
        val normalized = normalizeHex(hexColor)
        val map = loadOverrides(context).toMutableMap()
        map[colorName] = normalized
        persistOverrides(context, map)
    }

    fun clearOverride(context: Context, thaiColorName: String) {
        val colorName = normalizeSourceColorName(thaiColorName)
        val map = loadOverrides(context).toMutableMap()
        map.remove(colorName)
        persistOverrides(context, map)
    }

    fun clearAllOverrides(context: Context) {
        persistOverrides(context, emptyMap())
    }

    fun getDayPalette(context: Context, dayNumber: Int): List<String> {
        val overrides = loadDayPaletteOverrides(context)
        val overridden = overrides[dayNumber]
        if (!overridden.isNullOrEmpty()) {
            return normalizePaletteSize(overridden)
        }
        return buildDefaultDayPalette(context, dayNumber)
    }

    fun saveDayPalette(context: Context, dayNumber: Int, shades: List<String>) {
        val normalized = normalizePaletteSize(shades)
        val current = loadDayPaletteOverrides(context).toMutableMap()
        current[dayNumber] = normalized
        persistDayPaletteOverrides(context, current)
    }

    fun clearDayPalette(context: Context, dayNumber: Int) {
        val current = loadDayPaletteOverrides(context).toMutableMap()
        current.remove(dayNumber)
        persistDayPaletteOverrides(context, current)
    }

    fun clearAllDayPalettes(context: Context) {
        persistDayPaletteOverrides(context, emptyMap())
    }

    fun syncDayPalettesFromServer(context: Context): Boolean {
        return try {
            val api = RetrofitClient.instance.create(ApiService::class.java)
            val response = api.getOutfitDayPalettes().execute()
            if (!response.isSuccessful) return false
            val body = response.body() ?: return false
            applyServerPalettes(context, body)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun pushDayPalettesToServer(context: Context): Boolean {
        return try {
            val payload = loadDayPaletteOverrides(context).entries.associate { entry ->
                entry.key.toString() to normalizePaletteSize(entry.value)
            }
            val api = RetrofitClient.instance.create(ApiService::class.java)
            val response = api.upsertOutfitDayPalettes(
                OutfitDayPalettesUpsertRequest(dayPalettes = payload)
            ).execute()
            if (!response.isSuccessful) return false
            response.body()?.let { applyServerPalettes(context, it) }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun normalizeHex(value: String): String {
        var text = value.trim().uppercase()
        if (!text.startsWith("#")) {
            text = "#$text"
        }
        return text
    }

    private fun normalizeSourceColorName(value: String): String {
        return value.trim().replace(Regex("\\s*\\(ซ้ำ\\s*\\d+\\)$"), "")
    }

    private fun buildDefaultDayPalette(context: Context, dayNumber: Int): List<String> {
        val legacyFour = legacyDayDefault4Shades[dayNumber]
        if (!legacyFour.isNullOrEmpty()) {
            val shades = legacyFour.map { normalizeHex(it) }.toMutableList()
            shades.add(shades.getOrElse(3) { shades.lastOrNull() ?: "#FFFFFF" })
            return normalizePaletteSize(shades)
        }
        val sourceSlots = dayColorSlots[dayNumber].orEmpty()
        if (sourceSlots.isEmpty()) {
            return listOf("#FFFFFF", "#FFFFFF", "#FFFFFF", "#FFFFFF", "#FFFFFF")
        }
        val three = sourceSlots.take(3).map { source ->
            resolveHex(context, source) ?: defaultMap[source] ?: "#FFFFFF"
        }
        val c1 = three.getOrNull(0) ?: "#FFFFFF"
        val c2 = three.getOrNull(1) ?: c1
        val c3 = three.getOrNull(2) ?: c2
        return listOf(c1, c2, c3, c2, c2).map { normalizeHex(it) }
    }

    private fun normalizePaletteSize(shades: List<String>): List<String> {
        val normalized = shades.map { normalizeHex(it) }.toMutableList()
        while (normalized.size < 5) {
            normalized.add(normalized.lastOrNull() ?: "#FFFFFF")
        }
        if (normalized.size > 5) {
            return normalized.take(5)
        }
        return normalized
    }

    private fun loadOverrides(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_OVERRIDES, "{}") ?: "{}"
        val obj = runCatching { JSONObject(json) }.getOrElse { JSONObject() }
        val out = linkedMapOf<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            out[key] = obj.optString(key)
        }
        return out
    }

    private fun applyServerPalettes(context: Context, response: OutfitDayPalettesResponse) {
        val parsed = linkedMapOf<Int, List<String>>()
        response.dayPalettes.orEmpty().forEach { (k, v) ->
            val dayNumber = k.toIntOrNull() ?: return@forEach
            if (dayNumber in 1..8) {
                parsed[dayNumber] = normalizePaletteSize(v)
            }
        }
        if (parsed.isNotEmpty()) {
            persistDayPaletteOverrides(context, parsed)
        }
    }

    private fun loadDayPaletteOverrides(context: Context): Map<Int, List<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DAY_PALETTE_OVERRIDES, "{}") ?: "{}"
        val obj = runCatching { JSONObject(json) }.getOrElse { JSONObject() }
        val out = linkedMapOf<Int, List<String>>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val dayNumber = key.toIntOrNull() ?: continue
            val arr = obj.optJSONArray(key) ?: continue
            val shades = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val shade = arr.optString(i).trim()
                if (shade.isNotBlank()) shades.add(normalizeHex(shade))
            }
            if (shades.isNotEmpty()) {
                out[dayNumber] = normalizePaletteSize(shades)
            }
        }
        return out
    }

    private fun persistOverrides(context: Context, map: Map<String, String>) {
        val obj = JSONObject()
        for ((k, v) in map) {
            obj.put(k, v)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_OVERRIDES, obj.toString())
            .apply()
    }

    private fun persistDayPaletteOverrides(context: Context, map: Map<Int, List<String>>) {
        val obj = JSONObject()
        map.toSortedMap().forEach { (dayNumber, shades) ->
            val arr = JSONArray()
            normalizePaletteSize(shades).forEach { arr.put(it) }
            obj.put(dayNumber.toString(), arr)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DAY_PALETTE_OVERRIDES, obj.toString())
            .apply()
    }
}
