package com.smartsleep.app.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartsleep.app.R
import com.smartsleep.app.data.local.SleepSessionEntity
import com.smartsleep.app.util.TimeUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, use24Hour: Boolean) {
    val history by viewModel.history.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(20.dp))

        if (history.isEmpty()) {
            Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        WeeklyRegularityChart(history)
        Spacer(Modifier.height(24.dp))

        history.forEach { session -> HistoryRow(session, use24Hour) }
    }
}

@Composable
private fun WeeklyRegularityChart(history: List<SleepSessionEntity>) {
    val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
    val weekSessions = history.filter { it.sleepStartEpochMillis >= weekAgo }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.weekly_chart_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (weekSessions.isNotEmpty()) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val minMinute = 0f
                        val maxMinute = 24 * 60f
                        val dayWidth = size.width / 7f
                        weekSessions.forEach { session ->
                            val cal = Calendar.getInstance().apply { timeInMillis = session.sleepStartEpochMillis }
                            val dayIndex = cal.get(Calendar.DAY_OF_WEEK) - 1
                            val startMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                            val wakeMillis = session.actualWakeEpochMillis ?: session.suggestedWakeEpochMillis
                            val wakeCal = Calendar.getInstance().apply { timeInMillis = wakeMillis }
                            val wakeMinuteOfDay = wakeCal.get(Calendar.HOUR_OF_DAY) * 60 + wakeCal.get(Calendar.MINUTE)

                            val x = dayWidth * dayIndex + dayWidth / 2f
                            val yStart = size.height * (1f - (startMinuteOfDay - minMinute) / (maxMinute - minMinute))
                            val yEnd = size.height * (1f - (wakeMinuteOfDay - minMinute) / (maxMinute - minMinute))

                            drawLine(
                                color = primaryColor,
                                start = Offset(x, yStart.coerceIn(0f, size.height)),
                                end = Offset(x, yEnd.coerceIn(0f, size.height)),
                                strokeWidth = 10f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(session: SleepSessionEntity, use24Hour: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                TimeUtils.formatDate(session.sleepStartEpochMillis),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${TimeUtils.formatClock(session.sleepStartEpochMillis, use24Hour)} → ${
                        TimeUtils.formatClock(session.actualWakeEpochMillis ?: session.suggestedWakeEpochMillis, use24Hour)
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    ratingLabel(session.ratingScore),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.history_planned_duration) + ": " +
                        TimeUtils.formatDurationMinutes(session.sleepStartEpochMillis, session.suggestedWakeEpochMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun ratingLabel(score: Int?): String = when (score) {
    2 -> "🙂 +2"
    1 -> "🙂 +1"
    0 -> "😐 0"
    -1 -> "🙁 -1"
    -2 -> "🙁 -2"
    else -> "—"
}
