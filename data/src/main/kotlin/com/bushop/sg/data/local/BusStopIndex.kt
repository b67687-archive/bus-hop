package com.bushop.sg.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

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
        }
        _isReady.value = true
    }

    /** Levenshtein distance for typo-tolerant matching (max 2 edits checked). */
    private fun levenshtein(s1: String, s2: String, limit: Int = 2): Int {
        if (kotlin.math.abs(s1.length - s2.length) > limit) return limit + 1
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
            if (dp[i].min() > limit) return limit + 1  // early exit
        }
        return dp[s1.length][s2.length]
    }

    /** Tokenise a string into lowercase search tokens. */
    private fun tokenise(text: String): List<String> =
        text.lowercase().split(Regex("""[\s,./()&]+""")).filter { it.isNotEmpty() }

    /** Returns empty results if index hasn't loaded yet. */
    fun search(query: String): List<BusStopEntry> {
        val q = query.trim().lowercase()
        if (q.length < 1 || stops.isEmpty()) return emptyList()

        data class Scored(val entry: BusStopEntry, val score: Int)

        // Always search names; codes get a small bonus for backtracking
        val queryTokens = q.split(Regex("""\s+""")).filter { it.isNotEmpty() }

        val results = stops.values.mapNotNull { entry ->
            val nameLower = entry.name.lowercase()
            val nameTokens = tokenise(entry.name)
            val roadTokens = tokenise(entry.road)
            val codeLower = entry.code.lowercase()
            var score = 0
            var matchedAny = false

            for (qt in queryTokens) {
                var best = 0

                // ── Name matching (primary) ──
                for (nt in nameTokens) {
                    val s = when {
                        nt == qt -> 1000
                        nt.startsWith(qt) -> 800
                        qt.startsWith(nt) -> 600
                        nt.contains(qt) -> 400
                        qt.contains(nt) -> 300
                        qt.length >= 3 && levenshtein(nt, qt) <= 1 -> 250
                        else -> 0
                    }
                    if (s > best) best = s
                }

                // ── Road matching (secondary — always lower than name) ──
                if (best == 0) {
                    for (rt in roadTokens) {
                        val s = when {
                            rt.startsWith(qt) -> 180
                            rt.contains(qt) -> 80
                            else -> 0
                        }
                        if (s > best) best = s
                    }
                }

                // ── Code match (tiny bonus, never primary) ──
                if (best == 0 && qt.length >= 2) {
                    if (codeLower.startsWith(qt)) best = 40
                    else if (codeLower.contains(qt)) best = 15
                }

                if (best > 0) {
                    matchedAny = true
                    score += best
                }
            }

            // Bonus: all tokens matched in name
            if (queryTokens.isNotEmpty() && queryTokens.all { qt ->
                    nameTokens.any { nt -> nt.startsWith(qt) || nt.contains(qt) || qt.contains(nt) }
                }) {
                score += 500
            }

            if (matchedAny) Scored(entry, score) else null
        }

        return results
            .sortedByDescending { it.score }
            .take(20)
            .map { it.entry }
    }

    /** Returns null if index hasn't loaded yet. */
    fun findByCode(code: String): BusStopEntry? = stops[code]
}
