package com.smartsleep.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smartsleep.app.domain.model.AppLanguage
import com.smartsleep.app.domain.model.AppSettings
import com.smartsleep.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "smart_sleep_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val CYCLE_MINUTES = doublePreferencesKey("cycle_minutes")
        val BASE_ONSET_DELAY = intPreferencesKey("base_onset_delay")
        val ADAPTIVE_ONSET_DELAY = doublePreferencesKey("adaptive_onset_delay")
        val PREFERRED_POSITION = floatPreferencesKey("preferred_window_position")
        val ALARM_SOUND_URI = stringPreferencesKey("alarm_sound_uri")
        val VOLUME_PERCENT = intPreferencesKey("volume_percent")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SNOOZE_ENABLED = booleanPreferencesKey("snooze_enabled")
        val SNOOZE_DURATION = intPreferencesKey("snooze_duration")
        val SMART_WAKE_ENABLED = booleanPreferencesKey("smart_wake_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_24H = booleanPreferencesKey("use_24h")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            sleepCycleMinutes = prefs[Keys.CYCLE_MINUTES] ?: 90.0,
            baseOnsetDelayMinutes = prefs[Keys.BASE_ONSET_DELAY] ?: 15,
            adaptiveOnsetDelayMinutes = prefs[Keys.ADAPTIVE_ONSET_DELAY] ?: 15.0,
            preferredWindowPosition = prefs[Keys.PREFERRED_POSITION] ?: 0.7f,
            alarmSoundUri = prefs[Keys.ALARM_SOUND_URI],
            volumePercent = prefs[Keys.VOLUME_PERCENT] ?: 80,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            snoozeEnabled = prefs[Keys.SNOOZE_ENABLED] ?: true,
            snoozeDurationMinutes = prefs[Keys.SNOOZE_DURATION] ?: 9,
            smartWakeEnabled = prefs[Keys.SMART_WAKE_ENABLED] ?: false,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            use24HourFormat = prefs[Keys.USE_24H] ?: true,
            language = prefs[Keys.LANGUAGE]?.let { tag -> AppLanguage.entries.find { it.tag == tag } }
                ?: AppLanguage.PERSIAN
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        var current: AppSettings? = null
        context.dataStore.edit { prefs ->
            val snapshot = AppSettings(
                sleepCycleMinutes = prefs[Keys.CYCLE_MINUTES] ?: 90.0,
                baseOnsetDelayMinutes = prefs[Keys.BASE_ONSET_DELAY] ?: 15,
                adaptiveOnsetDelayMinutes = prefs[Keys.ADAPTIVE_ONSET_DELAY] ?: 15.0,
                preferredWindowPosition = prefs[Keys.PREFERRED_POSITION] ?: 0.7f,
                alarmSoundUri = prefs[Keys.ALARM_SOUND_URI],
                volumePercent = prefs[Keys.VOLUME_PERCENT] ?: 80,
                vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
                snoozeEnabled = prefs[Keys.SNOOZE_ENABLED] ?: true,
                snoozeDurationMinutes = prefs[Keys.SNOOZE_DURATION] ?: 9,
                smartWakeEnabled = prefs[Keys.SMART_WAKE_ENABLED] ?: false,
                themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.DARK,
                use24HourFormat = prefs[Keys.USE_24H] ?: true,
                language = prefs[Keys.LANGUAGE]?.let { tag -> AppLanguage.entries.find { it.tag == tag } }
                    ?: AppLanguage.PERSIAN
            )
            val updated = transform(snapshot)
            prefs[Keys.CYCLE_MINUTES] = updated.sleepCycleMinutes
            prefs[Keys.BASE_ONSET_DELAY] = updated.baseOnsetDelayMinutes
            prefs[Keys.ADAPTIVE_ONSET_DELAY] = updated.adaptiveOnsetDelayMinutes
            prefs[Keys.PREFERRED_POSITION] = updated.preferredWindowPosition
            updated.alarmSoundUri?.let { prefs[Keys.ALARM_SOUND_URI] = it }
            prefs[Keys.VOLUME_PERCENT] = updated.volumePercent
            prefs[Keys.VIBRATION_ENABLED] = updated.vibrationEnabled
            prefs[Keys.SNOOZE_ENABLED] = updated.snoozeEnabled
            prefs[Keys.SNOOZE_DURATION] = updated.snoozeDurationMinutes
            prefs[Keys.SMART_WAKE_ENABLED] = updated.smartWakeEnabled
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.USE_24H] = updated.use24HourFormat
            prefs[Keys.LANGUAGE] = updated.language.tag
            current = updated
        }
    }
}
