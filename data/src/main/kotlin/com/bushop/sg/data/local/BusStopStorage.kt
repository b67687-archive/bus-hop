package com.bushop.sg.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bushop.sg.domain.model.BusService
import com.bushop.sg.domain.model.BusStop
import com.bushop.sg.domain.model.ColorSchemeOption
import com.bushop.sg.domain.model.DuplicateStopException
import com.bushop.sg.domain.model.ThemeMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bushop_prefs")

class BusStopStorage(
    private val context: Context,
) {
    companion object {
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val gson = Gson()
    private val busStopsKey = stringPreferencesKey("saved_bus_stops")

    val savedBusStops: Flow<List<BusStop>> =
        context.dataStore.data
            .map { prefs -> parseBusStopList(prefs[busStopsKey]) }
            .distinctUntilChanged()

    private fun parseBusStopList(json: String?): List<BusStop> {
        val text = json ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BusStop>>() {}.type
            gson.fromJson<List<BusStop>>(text, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addBusStop(stop: BusStop): Result<Unit> {
        var result = Result.success(Unit)
        context.dataStore.edit { prefs ->
            val json = prefs[busStopsKey] ?: "[]"
            val type = object : TypeToken<MutableList<BusStop>>() {}.type
            val stops: MutableList<BusStop> =
                try {
                    gson.fromJson(json, type) ?: mutableListOf()
                } catch (e: Exception) {
                    result = Result.failure(Exception("Failed to read saved stops"))
                    return@edit
                }

            if (stops.any { it.code == stop.code }) {
                result = Result.failure(DuplicateStopException("Bus stop already exists"))
            } else {
                stops.add(stop)
                prefs[busStopsKey] = gson.toJson(stops)
            }
        }
        return result
    }

    suspend fun reorderStops(reordered: List<BusStop>) {
        context.dataStore.edit { prefs ->
            prefs[busStopsKey] = gson.toJson(reordered)
        }
    }

    suspend fun removeBusStop(code: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[busStopsKey] ?: "[]"
            val type = object : TypeToken<MutableList<BusStop>>() {}.type
            val stops: MutableList<BusStop> =
                try {
                    gson.fromJson(json, type) ?: mutableListOf()
                } catch (e: Exception) {
                    return@edit // Abort — don't overwrite with empty list
                }

            stops.removeAll { it.code == code }
            prefs[busStopsKey] = gson.toJson(stops)

            val collapsedKey = stringPreferencesKey("collapsed_stops_set")
            val updatedCollapsed =
                parseStringSet(prefs[collapsedKey]).toMutableSet().apply {
                    remove(code)
                }
            prefs[collapsedKey] = gson.toJson(updatedCollapsed.toList())

            val pinnedServicesKey = stringPreferencesKey("pinned_services")
            val updatedPinnedServices =
                parsePinnedServices(prefs[pinnedServicesKey])
                    .filterNot { it.startsWith("$code:") }
                    .toSet()
            prefs[pinnedServicesKey] = gson.toJson(updatedPinnedServices.toList())

            prefs.remove(stringPreferencesKey("services_$code"))
            prefs.remove(stringPreferencesKey("services_${code}_ts"))
        }
    }

    /** Flow of [code -> List<BusService>] for stops with cached data (non-stale). */
    fun getBusServicesFlow(): Flow<Map<String, List<BusService>>> {
        val ttlMs = CACHE_TTL_MS
        return context.dataStore.data
            .map { prefs -> parseServicesMap(prefs, ttlMs) }
            .distinctUntilChanged()
    }

    /** Flow of [code -> cachedAt timestamp] for every stop with saved services. */
    val cachedTimestamps: Flow<Map<String, Long>> =
        context.dataStore.data
            .map { prefs ->
                val result = mutableMapOf<String, Long>()
                prefs.asMap().forEach { (key, value) ->
                    if (key.name.startsWith("services_") && key.name.endsWith("_ts")) {
                        val code = key.name.removePrefix("services_").removeSuffix("_ts")
                        result[code] = (value as String).toLongOrNull() ?: 0L
                    }
                }
                result
            }.distinctUntilChanged()

    private fun parseServicesMap(
        prefs: Preferences,
        ttlMs: Long,
    ): Map<String, List<BusService>> {
        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, List<BusService>>()
        prefs.asMap().forEach { (key, value) ->
            if (key.name.startsWith("services_") && !key.name.endsWith("_ts")) {
                val code = key.name.removePrefix("services_")
                val tsKey = stringPreferencesKey("services_${code}_ts")
                val ts = prefs[tsKey]?.toLongOrNull() ?: 0L
                if (now - ts > ttlMs) return@forEach
                try {
                    val type = object : TypeToken<List<BusService>>() {}.type
                    val services: List<BusService> = gson.fromJson(value as String, type) ?: emptyList()
                    result[code] = services
                } catch (e: Exception) {
                    result[code] = emptyList()
                }
            }
        }
        return result
    }

    suspend fun saveBusServices(
        code: String,
        services: List<BusService>,
    ) {
        val servicesKey = stringPreferencesKey("services_$code")
        val timestampKey = stringPreferencesKey("services_${code}_ts")
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            prefs[servicesKey] = gson.toJson(services)
            prefs[timestampKey] = now.toString()
        }
    }

    // ── Auto-refresh interval ──

    val autoRefreshInterval: Flow<Int> =
        context.dataStore.data
            .map { prefs -> prefs[intPreferencesKey("auto_refresh_interval")] ?: 30 }
            .distinctUntilChanged()

    suspend fun getAutoRefreshIntervalOnce(): Int = autoRefreshInterval.first()

    suspend fun saveAutoRefreshInterval(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[intPreferencesKey("auto_refresh_interval")] = seconds
        }
    }

    // ── Theme mode ──

    val themeModeFlow: Flow<ThemeMode> =
        context.dataStore.data
            .map { prefs ->
                val raw = prefs[stringPreferencesKey("theme_mode_str")] ?: "system"
                when (raw) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
            }.distinctUntilChanged()

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            val raw =
                when (mode) {
                    ThemeMode.SYSTEM -> "system"
                    ThemeMode.LIGHT -> "light"
                    ThemeMode.DARK -> "dark"
                }
            prefs[stringPreferencesKey("theme_mode_str")] = raw
        }
    }

    // ── Colour scheme ──

    val colorSchemeOptionFlow: Flow<ColorSchemeOption> =
        context.dataStore.data
            .map { prefs ->
                ColorSchemeOption.fromRawValue(
                    prefs[stringPreferencesKey("color_scheme_option")],
                )
            }.distinctUntilChanged()

    suspend fun saveColorSchemeOption(option: ColorSchemeOption) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("color_scheme_option")] = option.rawValue
        }
    }

    // ── Collapsed stops (type-safe Set) ──

    val collapsedStopsFlow: Flow<Set<String>> =
        context.dataStore.data
            .map { prefs -> parseStringSet(prefs[stringPreferencesKey("collapsed_stops_set")]) }
            .distinctUntilChanged()

    suspend fun saveCollapsedStops(stops: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("collapsed_stops_set")] = gson.toJson(stops.toList())
        }
    }

    private fun parseStringSet(json: String?): Set<String> {
        val text = json ?: return emptySet()
        return try {
            gson.fromJson(text, object : TypeToken<Set<String>>() {}.type) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // ── Pinned services (per-service within a stop) ──

    val pinnedServices: Flow<Set<String>> =
        context.dataStore.data
            .map { prefs -> parsePinnedServices(prefs[stringPreferencesKey("pinned_services")]) }
            .distinctUntilChanged()

    private fun parsePinnedServices(json: String?): Set<String> {
        val text = json ?: return emptySet()
        return try {
            gson.fromJson(text, object : TypeToken<Set<String>>() {}.type) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun savePinnedServices(pinned: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("pinned_services")] = gson.toJson(pinned.toList())
        }
    }
}
