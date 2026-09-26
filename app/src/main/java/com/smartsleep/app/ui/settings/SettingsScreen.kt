package com.smartsleep.app.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartsleep.app.R
import com.smartsleep.app.domain.model.AppLanguage
import com.smartsleep.app.util.PermissionUtils

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(24.dp))

        LabeledSlider(
            label = stringResource(R.string.settings_sleep_cycle_length),
            value = settings.sleepCycleMinutes.toFloat(),
            valueRange = 70f..110f,
            valueText = stringResource(R.string.minutes_format, settings.sleepCycleMinutes.toInt()),
            onValueChange = { viewModel.setCycleMinutes(it.toDouble()) }
        )
        LabeledSlider(
            label = stringResource(R.string.settings_onset_delay),
            value = settings.baseOnsetDelayMinutes.toFloat(),
            valueRange = 5f..30f,
            valueText = stringResource(R.string.minutes_format, settings.baseOnsetDelayMinutes),
            onValueChange = { viewModel.setBaseOnsetDelay(it.toInt()) }
        )

        Divider(Modifier.padding(vertical = 16.dp))

        LabeledSlider(
            label = stringResource(R.string.settings_volume),
            value = settings.volumePercent.toFloat(),
            valueRange = 0f..100f,
            valueText = "${settings.volumePercent}%",
            onValueChange = { viewModel.setVolume(it.toInt()) }
        )
        SwitchRow(
            label = stringResource(R.string.settings_vibration),
            checked = settings.vibrationEnabled,
            onCheckedChange = viewModel::setVibration
        )
        SwitchRow(
            label = stringResource(R.string.settings_snooze_enabled),
            checked = settings.snoozeEnabled,
            onCheckedChange = viewModel::setSnoozeEnabled
        )
        if (settings.snoozeEnabled) {
            LabeledSlider(
                label = stringResource(R.string.settings_snooze_duration),
                value = settings.snoozeDurationMinutes.toFloat(),
                valueRange = 1f..20f,
                valueText = stringResource(R.string.minutes_format, settings.snoozeDurationMinutes),
                onValueChange = { viewModel.setSnoozeDuration(it.toInt()) }
            )
        }

        Divider(Modifier.padding(vertical = 16.dp))

        SwitchRow(
            label = stringResource(R.string.settings_smart_wake),
            checked = settings.smartWakeEnabled,
            onCheckedChange = viewModel::setSmartWake
        )
        Text(
            stringResource(R.string.settings_smart_wake_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider(Modifier.padding(vertical = 16.dp))

        SwitchRow(
            label = stringResource(R.string.settings_dark_mode),
            checked = settings.themeMode.name == "DARK",
            onCheckedChange = viewModel::setDarkTheme
        )
        SwitchRow(
            label = stringResource(R.string.settings_time_format),
            checked = settings.use24HourFormat,
            onCheckedChange = viewModel::setUse24Hour
        )

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row {
            LanguageChip(
                label = "فارسی",
                selected = settings.language == AppLanguage.PERSIAN,
                onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) }
            )
            Spacer(Modifier.height(0.dp))
            LanguageChip(
                label = "English",
                selected = settings.language == AppLanguage.ENGLISH,
                onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
            )
        }

        Divider(Modifier.padding(vertical = 16.dp))

        if (!PermissionUtils.isIgnoringBatteryOptimizations(context)) {
            OutlinedButton(
                onClick = { requestBatteryExemption(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_battery_optimization))
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, valueText: String, onValueChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(end = 8.dp)
    )
}

private fun requestBatteryExemption(context: Context) {
    runCatching {
        context.startActivity(PermissionUtils.requestIgnoreBatteryOptimizationsIntent(context))
    }
}
