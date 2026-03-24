package com.example.lenskiegid.weather

import android.content.Context

object WeatherCache {
    private const val PREFS = "weather_cache"
    private const val KEY_JSON = "last_json"
    private const val KEY_UPDATED = "last_updated_millis"

    var lastJson: String? = null
    var lastUpdatedMillis: Long = 0L

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        lastJson = prefs.getString(KEY_JSON, null)
        lastUpdatedMillis = prefs.getLong(KEY_UPDATED, 0L)
    }

    fun update(json: String) {
        lastJson = json
        lastUpdatedMillis = System.currentTimeMillis()
    }

    fun update(context: Context, json: String) {
        update(json)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JSON, json)
            .putLong(KEY_UPDATED, lastUpdatedMillis)
            .apply()
    }

    fun isFresh(maxAgeMillis: Long): Boolean {
        val age = System.currentTimeMillis() - lastUpdatedMillis
        return lastJson != null && age in 0..maxAgeMillis
    }

    fun getFreshJson(context: Context, maxAgeMillis: Long): String? {
        if (lastJson == null) init(context)
        return if (isFresh(maxAgeMillis)) lastJson else null
    }

    fun getAnyJson(context: Context): String? {
        if (lastJson == null) init(context)
        return lastJson
    }
}
