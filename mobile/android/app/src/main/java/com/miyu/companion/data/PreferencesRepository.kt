package com.miyu.companion.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "miyu_prefs")

class PreferencesRepository(private val context: Context) {
    companion object {
        val AGE_KEY = intPreferencesKey("age")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val GUARDIAN_KEY = booleanPreferencesKey("guardian")
        val OVERLAY_X = floatPreferencesKey("overlay_x")
        val OVERLAY_Y = floatPreferencesKey("overlay_y")
        val OVERLAY_SIZE = floatPreferencesKey("overlay_size")
        val OVERLAY_ALPHA = floatPreferencesKey("overlay_alpha")
        val OVERLAY_SNAP = booleanPreferencesKey("overlay_snap")
        val OVERLAY_MINI = booleanPreferencesKey("overlay_mini")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val RESPECT_METER = intPreferencesKey("respect")
        val CURRENT_TOY = stringPreferencesKey("current_toy")
        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val LAST_TOY_TIME = longPreferencesKey("last_toy_time")
    }

    val miyuState: Flow<MiyuState> = context.dataStore.data.map { prefs ->
        MiyuState(
            age = prefs[AGE_KEY],
            language = prefs[LANGUAGE_KEY] ?: "ar",
            toy = prefs[CURRENT_TOY],
            respectMeter = prefs[RESPECT_METER] ?: 100,
            guardianConfirmed = prefs[GUARDIAN_KEY] ?: false,
            overlayEnabled = prefs[OVERLAY_ENABLED] ?: false,
            focusMinutes = prefs[FOCUS_MINUTES] ?: 25
        )
    }

    val floatingPosition: Flow<FloatingPosition> = context.dataStore.data.map { prefs ->
        FloatingPosition(
            x = prefs[OVERLAY_X] ?: 0.85f,
            y = prefs[OVERLAY_Y] ?: 0.3f,
            size = prefs[OVERLAY_SIZE] ?: 1f,
            alpha = prefs[OVERLAY_ALPHA] ?: 1f,
            snapToEdge = prefs[OVERLAY_SNAP] ?: true,
            isMini = prefs[OVERLAY_MINI] ?: false,
            isHidden = false
        )
    }

    suspend fun savePosition(pos: FloatingPosition) {
        context.dataStore.edit { prefs ->
            prefs[OVERLAY_X] = pos.x
            prefs[OVERLAY_Y] = pos.y
            prefs[OVERLAY_SIZE] = pos.size
            prefs[OVERLAY_ALPHA] = pos.alpha
            prefs[OVERLAY_SNAP] = pos.snapToEdge
            prefs[OVERLAY_MINI] = pos.isMini
        }
    }

    suspend fun saveAgeProfile(age: Int, language: String, guardian: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AGE_KEY] = age
            prefs[LANGUAGE_KEY] = language
            prefs[GUARDIAN_KEY] = guardian
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[OVERLAY_ENABLED] = enabled }
    }

    suspend fun setCurrentToy(toyKey: String?) {
        context.dataStore.edit { prefs ->
            if (toyKey == null) prefs.remove(CURRENT_TOY)
            else prefs[CURRENT_TOY] = toyKey
            prefs[LAST_TOY_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun updateRespect(delta: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[RESPECT_METER] ?: 100
            prefs[RESPECT_METER] = (current + delta).coerceIn(0, 100)
        }
    }
}
