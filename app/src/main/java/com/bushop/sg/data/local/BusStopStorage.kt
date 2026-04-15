package com.bushop.sg.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bushop.sg.data.model.BusStop
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bushop_prefs")

class BusStopStorage(private val context: Context) {

    private val gson = Gson()
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

    suspend fun addBusStop(stop: BusStop) {
        context.dataStore.edit { prefs ->
            val json = prefs[busStopsKey] ?: "[]"
            val type = object : TypeToken<MutableList<BusStop>>() {}.type
            val stops: MutableList<BusStop> = try {
                gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            if (stops.none { it.code == stop.code }) {
                stops.add(stop)
                prefs[busStopsKey] = gson.toJson(stops)
            }
        }
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
}