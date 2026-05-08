package com.bushop.sg.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bushop.sg.data.model.BusService
import com.bushop.sg.data.model.BusStop
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bushop_prefs")

class BusStopStorage(private val context: Context) {

    private val gson = Gson()
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "bushop_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fall back to regular SharedPreferences if encryption fails
        // (e.g. Keystore changed after OTA update)
        android.util.Log.w("BusStopStorage", "Encrypted prefs failed, using plaintext fallback", e)
        context.getSharedPreferences("bushop_encrypted_fallback", Context.MODE_PRIVATE)
    }
    
    private val busStopsKey = stringPreferencesKey("saved_bus_stops")

    val savedBusStops: Flow<List<BusStop>> = context.dataStore.data.map { prefs ->
        val json = prefs[busStopsKey] ?: "[]"
        try {
            val type = object : TypeToken<List<BusStop>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
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
        return context.dataStore.data.map { prefs ->
            val result = mutableMapOf<String, List<BusService>>()
            prefs.asMap().forEach { (key, value) ->
                if (key.name.startsWith("services_")) {
                    val code = key.name.removePrefix("services_")
                    try {
                        val type = object : TypeToken<List<BusService>>() {}.type
                        val services: List<BusService> = gson.fromJson(value as String, type) ?: emptyList()
                        result[code] = services
                    } catch (e: Exception) {
                        result[code] = emptyList()
                    }
                }
            }
            result
        }
    }

    suspend fun saveBusServices(code: String, services: List<BusService>) {
        val key = stringPreferencesKey("services_$code")
        context.dataStore.edit { prefs ->
            prefs[key] = gson.toJson(services)
        }
    }

    suspend fun saveAutoRefreshInterval(seconds: Int) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            encryptedPrefs.edit().putInt("auto_refresh_interval", seconds).apply()
        }
    }

    fun getAutoRefreshInterval(): Int {
        return encryptedPrefs.getInt("auto_refresh_interval", 30)
    }

    // ── Theme mode ──

    val themeMode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[intPreferencesKey("theme_mode")] ?: 0
    }

    suspend fun saveThemeMode(mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[intPreferencesKey("theme_mode")] = mode
        }
    }

    // ── Collapsed stops ──

    val collapsedStopCodes: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val json = prefs[stringPreferencesKey("collapsed_stops")] ?: "[]"
        try {
            gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
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