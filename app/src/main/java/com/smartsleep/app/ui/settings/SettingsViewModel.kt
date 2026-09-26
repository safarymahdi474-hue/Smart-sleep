package com.smartsleep.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsleep.app.data.repository.SleepRepository
import com.smartsleep.app.domain.model.AppLanguage
import com.smartsleep.app.domain.model.AppSettings
import com.smartsleep.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SleepRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setCycleMinutes(v: Double) = update { it.copy(sleepCycleMinutes = v) }
    fun setBaseOnsetDelay(v: Int) = update { it.copy(baseOnsetDelayMinutes = v, adaptiveOnsetDelayMinutes = v.toDouble()) }
    fun setAlarmSoundUri(uri: String?) = update { it.copy(alarmSoundUri = uri) }
    fun setVolume(v: Int) = update { it.copy(volumePercent = v) }
    fun setVibration(v: Boolean) = update { it.copy(vibrationEnabled = v) }
    fun setSnoozeEnabled(v: Boolean) = update { it.copy(snoozeEnabled = v) }
    fun setSnoozeDuration(v: Int) = update { it.copy(snoozeDurationMinutes = v) }
    fun setSmartWake(v: Boolean) = update { it.copy(smartWakeEnabled = v) }
    fun setDarkTheme(dark: Boolean) = update { it.copy(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) }
    fun setUse24Hour(v: Boolean) = update { it.copy(use24HourFormat = v) }
    fun setLanguage(lang: AppLanguage) = update { it.copy(language = lang) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }
}
