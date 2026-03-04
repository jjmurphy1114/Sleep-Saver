package com.example.sleepsaver.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.snapshotDataStore by preferencesDataStore(name = "context_snapshot")

data class PersistedContextSnapshot(
    val lightLux: Float?,
    val isFaceDown: Boolean,
    val isCharging: Boolean,
    val lastUpdatedMillis: Long
)

class ContextSnapshotStore(private val context: Context) {
    private object Keys {
        val lightLux = floatPreferencesKey("light_lux")
        val faceDown = booleanPreferencesKey("face_down")
        val charging = booleanPreferencesKey("charging")
        val updated = longPreferencesKey("updated")
    }

    suspend fun update(lightLux: Float?, isFaceDown: Boolean, isCharging: Boolean, now: Long) {
        context.snapshotDataStore.edit { prefs ->
            if (lightLux != null) {
                prefs[Keys.lightLux] = lightLux
            }
            prefs[Keys.faceDown] = isFaceDown
            prefs[Keys.charging] = isCharging
            prefs[Keys.updated] = now
        }
    }

    suspend fun read(): PersistedContextSnapshot {
        val prefs = context.snapshotDataStore.data.first()
        return PersistedContextSnapshot(
            lightLux = prefs[Keys.lightLux],
            isFaceDown = prefs[Keys.faceDown] ?: false,
            isCharging = prefs[Keys.charging] ?: false,
            lastUpdatedMillis = prefs[Keys.updated] ?: 0L
        )
    }
}

