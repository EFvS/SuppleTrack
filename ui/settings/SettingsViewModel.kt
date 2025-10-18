package com.efvs.suppletrack.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.efvs.suppletrack.data.settings.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dataStore = SettingsDataStore(application)

    val settings: StateFlow<SettingsState> = dataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    fun toggleDarkMode() {
        viewModelScope.launch {
            dataStore.setDarkMode(!settings.value.darkMode)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            dataStore.setLanguage(lang)
        }
    }

    fun togglePin() {
        viewModelScope.launch {
            dataStore.setPinEnabled(!settings.value.pinEnabled)
        }
    }

    fun setTextSize(scale: Float) {
        viewModelScope.launch {
            dataStore.setTextSize(scale)
        }
    }

    fun toggleColorBlindMode() {
        viewModelScope.launch {
            dataStore.setColorBlindMode(!settings.value.colorBlindMode)
        }
    }

    fun toggleNotifications() {
        viewModelScope.launch {
            dataStore.setNotifications(!settings.value.notificationsEnabled)
        }
    }
}