package com.bushop.sg.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BusStopEntry(
    val code: String,
    val name: String,
    val road: String = ""
) {
    val displayName: String get() = if (name.isNotBlank()) "$name, $road" else code
}

class BusStopIndex(private val context: Context) {

    @Volatile
    private var stops: Map<String, BusStopEntry> = emptyMap()
    @Volatile
    private var stopsByName: Map<String, List<BusStopEntry>> = emptyMap()

    init { /* empty — parsing is done in load() */ }

    /** Load and parse the JSON on [Dispatchers.IO]. Safe to call multiple times. */
    suspend fun load() {
        withContext(Dispatchers.IO) {
            val json = try {
                context.assets.open("bus_stops.json")
                    .bufferedReader()
                    .use { it.readText() }
            } catch (e: Exception) {
                "{}"
            }
            val type = object : TypeToken<Map<String, List<Any>>>() {}.type
            val raw: Map<String, List<Any>> = try {
                Gson().fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
            val parsed = raw.mapNotNull { (code, data) ->
                if (data.size >= 3) {
                    val name = data[2].toString().trim()
                    val road = data.getOrNull(3)?.toString()?.trim() ?: ""
                    if (name.isNotBlank()) BusStopEntry(code, name, road) else null
                } else null
            }
            stops = parsed.associateBy { it.code }
            stopsByName = parsed.groupBy { it.name.lowercase() }
        }
    }

    /** Returns empty results if index hasn't loaded yet. */
    fun search(query: String): List<BusStopEntry> {
        val q = query.trim()
        if (q.length < 2 || stops.isEmpty()) return emptyList()
        val results = linkedSetOf<BusStopEntry>()
        results.addAll(stops.values.filter { it.code.startsWith(q) }.take(5))
        if (results.size < 20) {
            val parts = q.lowercase().split(" ")
            results.addAll(stops.values.filter { entry ->
                val lower = entry.name.lowercase()
                parts.all { it in lower } || entry.road.lowercase().contains(q.lowercase())
            }.take(20 - results.size))
        }
        return results.take(20)
    }

    /** Returns null if index hasn't loaded yet. */
    fun findByCode(code: String): BusStopEntry? = stops[code]
}
