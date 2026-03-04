package com.example.sleepsaver.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.sleepsaver.domain.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "sleep_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val detectionEnabled = booleanPreferencesKey("detection_enabled")
        val lightThreshold = floatPreferencesKey("light_threshold")
        val windowStart = intPreferencesKey("window_start")
        val windowEnd = intPreferencesKey("window_end")
        val autoDnd = booleanPreferencesKey("auto_dnd")
        val darkMode = booleanPreferencesKey("dark_mode")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            sleepDetectionEnabled = prefs[Keys.detectionEnabled] ?: true,
            lightThresholdLux = prefs[Keys.lightThreshold] ?: 8f,
            sleepWindowStartHour = prefs[Keys.windowStart] ?: 22,
            sleepWindowEndHour = prefs[Keys.windowEnd] ?: 2,
            autoDndEnabled = prefs[Keys.autoDnd] ?: true,
            manualDarkMode = prefs[Keys.darkMode] ?: false
        )
    }

    suspend fun setDetectionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.detectionEnabled] = enabled }
    }

    suspend fun setLightThreshold(value: Float) {
        context.settingsDataStore.edit { it[Keys.lightThreshold] = value }
    }

    suspend fun setWindowStart(hour: Int) {
        context.settingsDataStore.edit { it[Keys.windowStart] = hour }
    }

    suspend fun setWindowEnd(hour: Int) {
        context.settingsDataStore.edit { it[Keys.windowEnd] = hour }
    }

    suspend fun setAutoDnd(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.autoDnd] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.darkMode] = enabled }
    }
}
