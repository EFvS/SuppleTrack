package com.efvs.suppletrack.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings_datastore")

object SettingsKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val LANGUAGE = stringPreferencesKey("language")
    val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
    val TEXT_SIZE = floatPreferencesKey("text_size")
    val COLOR_BLIND_MODE = booleanPreferencesKey("color_blind_mode")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
}

class SettingsDataStore(private val context: Context) {
    val settingsFlow: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            darkMode = prefs[SettingsKeys.DARK_MODE] ?: false,
            language = prefs[SettingsKeys.LANGUAGE] ?: "en",
            pinEnabled = prefs[SettingsKeys.PIN_ENABLED] ?: false,
            textSize = prefs[SettingsKeys.TEXT_SIZE] ?: 1.0f,
            colorBlindMode = prefs[SettingsKeys.COLOR_BLIND_MODE] ?: false,
            notificationsEnabled = prefs[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[SettingsKeys.DARK_MODE] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[SettingsKeys.LANGUAGE] = lang }
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SettingsKeys.PIN_ENABLED] = enabled }
    }

    suspend fun setTextSize(size: Float) {
        context.dataStore.edit { it[SettingsKeys.TEXT_SIZE] = size }
    }

    suspend fun setColorBlindMode(enabled: Boolean) {
        context.dataStore.edit { it[SettingsKeys.COLOR_BLIND_MODE] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled }
    }
}