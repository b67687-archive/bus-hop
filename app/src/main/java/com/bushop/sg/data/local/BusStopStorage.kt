package com.bushop.sg.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bushop.sg.data.model.BusService
import com.bushop.sg.data.model.BusStop
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bushop_prefs")

class BusStopStorage(private val context: Context) {

    private val gson = Gson()
    private val busStopsKey = stringPreferencesKey("saved_bus_stops")

    val savedBusStops: Flow<List<BusStop>> = context.dataStore.data
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
            val stops: MutableList<BusStop> = try {
                gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
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

    suspend fun removeBusStop(code: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[busStopsKey] ?: "[]"
            val type = object : TypeToken<MutableList<BusStop>>() {}.type
            val stops: MutableList<BusStop> = try {
                gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            stops.removeAll { it.code == code }
            prefs[busStopsKey] = gson.toJson(stops)
        }
    }

    fun getBusServicesFlow(): Flow<Map<String, List<BusService>>> {
        val ttlMs = 24 * 60 * 60 * 1000L // 24 hours
        return context.dataStore.data
            .map { prefs -> parseServicesMap(prefs, ttlMs) }
            .distinctUntilChanged()
    }

    private fun parseServicesMap(prefs: Preferences, ttlMs: Long): Map<String, List<BusService>> {
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

    suspend fun saveBusServices(code: String, services: List<BusService>) {
        val servicesKey = stringPreferencesKey("services_$code")
        val timestampKey = stringPreferencesKey("services_${code}_ts")
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            prefs[servicesKey] = gson.toJson(services)
            prefs[timestampKey] = now.toString()
        }
    }

    /** Remove cached services for stops that are no longer saved.
     *  Called after [removeBusStop]. */
    suspend fun evictBusServices(code: String) {
        val servicesKey = stringPreferencesKey("services_$code")
        val timestampKey = stringPreferencesKey("services_${code}_ts")
        context.dataStore.edit { prefs ->
            prefs.remove(servicesKey)
            prefs.remove(timestampKey)
        }
    }

    // ── Auto-refresh interval ──

    val autoRefreshInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[intPreferencesKey("auto_refresh_interval")] ?: 30
    }

    suspend fun getAutoRefreshIntervalOnce(): Int {
        return autoRefreshInterval.first()
    }

    suspend fun saveAutoRefreshInterval(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[intPreferencesKey("auto_refresh_interval")] = seconds
        }
    }

    // ── Theme mode ──

    val themeMode: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[intPreferencesKey("theme_mode")] ?: 0 }
        .distinctUntilChanged()

    suspend fun saveThemeMode(mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[intPreferencesKey("theme_mode")] = mode
        }
    }

    // ── Collapsed stops ──

    val collapsedStopCodes: Flow<List<String>> = context.dataStore.data
        .map { prefs -> parseCollapsedCodes(prefs[stringPreferencesKey("collapsed_stops")]) }
        .distinctUntilChanged()

    private fun parseCollapsedCodes(json: String?): List<String> {
        val text = json ?: return emptyList()
        return try {
            gson.fromJson(text, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveCollapsedStops(codes: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("collapsed_stops")] = gson.toJson(codes)
        }
    }
}

class DuplicateStopException(message: String) : Exception(message)