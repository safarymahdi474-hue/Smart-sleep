package com.smartsleep.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartsleep.app.R
import com.smartsleep.app.data.local.SleepSessionEntity
import com.smartsleep.app.domain.model.AppSettings
import com.smartsleep.app.util.TimeUtils
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: HomeViewModel, settings: AppSettings) {
    val activeSession by viewModel.activeSession.collectAsState()
    val window by viewModel.window.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (activeSession == null) {
            SetupContent(
                window = window,
                use24Hour = settings.use24HourFormat,
                onEarliestChange = viewModel::setEarliest,
                onLatestChange = viewModel::setLatest,
                onStart = { viewModel.startSleep {} }
            )
        } else {
            ActiveSleepContent(
                session = activeSession!!,
                use24Hour = settings.use24HourFormat,
                smartWakeEnabled = settings.smartWakeEnabled,
                onCancel = { viewModel.cancelSleep() }
            )
        }
    }
}

@Composable
private fun SetupContent(
    window: WakeWindowUi,
    use24Hour: Boolean,
    onEarliestChange: (Int, Int) -> Unit,
    onLatestChange: (Int, Int) -> Unit,
    onStart: () -> Unit
) {
    var showEarliestPicker by remember { mutableStateOf(false) }
    var showLatestPicker by remember { mutableStateOf(false) }

    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.home_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(32.dp))

    TimeRow(
        label = stringResource(R.string.earliest_wake_label),
        hour = window.earliestHour,
        minute = window.earliestMinute,
        use24Hour = use24Hour,
        onClick = { showEarliestPicker = true }
    )
    Spacer(Modifier.height(12.dp))
    TimeRow(
        label = stringResource(R.string.latest_wake_label),
        hour = window.latestHour,
        minute = window.latestMinute,
        use24Hour = use24Hour,
        onClick = { showLatestPicker = true }
    )

    Spacer(Modifier.height(40.dp))
    Button(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(Icons.Filled.NightsStay, contentDescription = null)
        Spacer(Modifier.height(0.dp))
        Text("  " + stringResource(R.string.start_sleep_button))
    }

    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.cycle_disclaimer),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (showEarliestPicker) {
        TimePickerDialog(
            initialHour = window.earliestHour,
            initialMinute = window.earliestMinute,
            use24Hour = use24Hour,
            onDismiss = { showEarliestPicker = false },
            onConfirm = { h, m -> onEarliestChange(h, m); showEarliestPicker = false }
        )
    }
    if (showLatestPicker) {
        TimePickerDialog(
            initialHour = window.latestHour,
            initialMinute = window.latestMinute,
            use24Hour = use24Hour,
            onDismiss = { showLatestPicker = false },
            onConfirm = { h, m -> onLatestChange(h, m); showLatestPicker = false }
        )
    }
}

@Composable
private fun TimeRow(label: String, hour: Int, minute: Int, use24Hour: Boolean, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            val fakeMillis = timeOfDayToTodayMillis(hour, minute)
            Text(
                TimeUtils.formatClock(fakeMillis, use24Hour),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = use24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.skip)) }
        },
        text = { TimePicker(state = state) }
    )
}

@Composable
private fun ActiveSleepContent(
    session: SleepSessionEntity,
    use24Hour: Boolean,
    smartWakeEnabled: Boolean,
    onCancel: () -> Unit
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    Spacer(Modifier.height(24.dp))
    Text(
        stringResource(R.string.sleep_active_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(24.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            InfoLine(stringResource(R.string.sleep_start_time_label), TimeUtils.formatClock(session.sleepStartEpochMillis, use24Hour))
            InfoLine(stringResource(R.string.estimated_fall_asleep_label), TimeUtils.formatClock(session.estimatedFallAsleepEpochMillis, use24Hour))
            InfoLine(
                stringResource(R.string.wake_window_label),
                "${TimeUtils.formatClock(session.wakeWindowStartEpochMillis, use24Hour)} – ${TimeUtils.formatClock(session.wakeWindowEndEpochMillis, use24Hour)}"
            )
            InfoLine(stringResource(R.string.suggested_wake_label), TimeUtils.formatClock(session.suggestedWakeEpochMillis, use24Hour), emphasize = true)
            InfoLine(stringResource(R.string.time_remaining_label), TimeUtils.formatRemaining(session.suggestedWakeEpochMillis, now))
        }
    }

    if (smartWakeEnabled) {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.smart_wake_active_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(32.dp))
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.cancel_sleep_button))
    }
}

@Composable
private fun InfoLine(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun timeOfDayToTodayMillis(hour: Int, minute: Int): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
    cal.set(java.util.Calendar.MINUTE, minute)
    cal.set(java.util.Calendar.SECOND, 0)
    return cal.timeInMillis
}
