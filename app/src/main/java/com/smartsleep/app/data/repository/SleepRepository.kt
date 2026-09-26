package com.smartsleep.app.data.repository

import android.content.Context
import com.smartsleep.app.alarm.AlarmScheduler
import com.smartsleep.app.data.local.SettingsDataStore
import com.smartsleep.app.data.local.SleepDatabase
import com.smartsleep.app.data.local.SleepSessionEntity
import com.smartsleep.app.domain.AdaptiveLearningEngine
import com.smartsleep.app.domain.SleepCycleCalculator
import com.smartsleep.app.domain.model.AppSettings
import com.smartsleep.app.domain.model.WakeRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class SleepRepository(private val context: Context) {

    private val dao = SleepDatabase.getInstance(context).sleepSessionDao()
    private val settingsStore = SettingsDataStore(context)
    private val scheduler = AlarmScheduler(context)

    val settingsFlow: Flow<AppSettings> = settingsStore.settingsFlow
    val activeSessionFlow: Flow<SleepSessionEntity?> = dao.observeActiveSession()
    val historyFlow: Flow<List<SleepSessionEntity>> = dao.observeHistory()

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsStore.update(transform)
    }

    /**
     * Starts a new sleep session: resolves the user's earliest/latest wake clock times to the
     * correct absolute instants (tomorrow, or later today if the window hasn't passed yet),
     * runs the calculator, persists the session, and arms the real alarm plus (optionally) the
     * Smart Wake monitor start.
     */
    suspend fun startSleepSession(
        earliestWakeHour: Int,
        earliestWakeMinute: Int,
        latestWakeHour: Int,
        latestWakeMinute: Int
    ): SleepSessionEntity {
        val settings = settingsFlow.first()
        val now = System.currentTimeMillis()

        val windowStart = resolveNextOccurrence(now, earliestWakeHour, earliestWakeMinute)
        var windowEnd = resolveNextOccurrence(now, latestWakeHour, latestWakeMinute)
        // If the "latest" time resolved to today but before windowStart, push to the next day
        // (handles windows crossing midnight, e.g. 23:50 - 00:30).
        if (windowEnd <= windowStart) {
            windowEnd += 24L * 60 * 60 * 1000
        }

        val result = SleepCycleCalculator.calculate(
            SleepCycleCalculator.CalculatorInput(
                sleepStartMillis = now,
                onsetDelayMinutes = settings.adaptiveOnsetDelayMinutes,
                cycleLengthMinutes = settings.sleepCycleMinutes,
                wakeWindowStartMillis = windowStart,
                wakeWindowEndMillis = windowEnd,
                preferredWindowPosition = settings.preferredWindowPosition
            )
        )

        val entity = SleepSessionEntity(
            sleepStartEpochMillis = now,
            estimatedFallAsleepEpochMillis = result.estimatedFallAsleepMillis,
            wakeWindowStartEpochMillis = windowStart,
            wakeWindowEndEpochMillis = windowEnd,
            suggestedWakeEpochMillis = result.suggestedWakeMillis,
            onsetDelayUsedMinutes = settings.adaptiveOnsetDelayMinutes,
            cycleMinutesUsed = settings.sleepCycleMinutes,
            smartWakeEnabled = settings.smartWakeEnabled,
            isActive = true
        )
        val id = dao.insert(entity)
        val saved = entity.copy(id = id)

        scheduler.scheduleWakeAlarm(id, result.suggestedWakeMillis)
        if (settings.smartWakeEnabled) {
            // Start monitoring a few minutes before the window opens so it has a head start.
            val monitorStart = (windowStart - 5 * 60_000L).coerceAtLeast(now + 60_000L)
            scheduler.scheduleSmartWakeMonitorStart(id, monitorStart)
        }
        return saved
    }

    suspend fun cancelActiveSleep() {
        val active = dao.getActiveSessionOnce() ?: return
        scheduler.cancelWakeAlarm()
        scheduler.cancelSmartWakeMonitorStart()
        dao.update(active.copy(isActive = false))
    }

    /** Called by Smart Wake when it detects a good moment to wake the user, or by the normal alarm. */
    suspend fun markSessionWoken(
        sessionId: Long,
        actualWakeMillis: Long = System.currentTimeMillis()
    ) {
        val session = dao.getById(sessionId) ?: return
        if (!session.isActive) return
        dao.update(session.copy(actualWakeEpochMillis = actualWakeMillis))
    }

    suspend fun recordRating(sessionId: Long, rating: WakeRating, wokeBeforeAlarm: Boolean) {
        val session = dao.getById(sessionId) ?: return
        val finished = session.copy(
            isActive = false,
            ratingScore = rating.score,
            wokeBeforeAlarm = wokeBeforeAlarm,
            actualWakeEpochMillis = session.actualWakeEpochMillis ?: System.currentTimeMillis()
        )
        dao.update(finished)

        val currentSettings = settingsFlow.first()
        val updatedSettings = AdaptiveLearningEngine.updateAfterSession(
            current = currentSettings,
            session = finished,
            rating = rating,
            wokeBeforeAlarm = wokeBeforeAlarm
        )
        updateSettings { updatedSettings }
    }

    suspend fun finishWithoutRating(sessionId: Long) {
        val session = dao.getById(sessionId) ?: return
        dao.update(
            session.copy(
                isActive = false,
                actualWakeEpochMillis = session.actualWakeEpochMillis ?: System.currentTimeMillis()
            )
        )
    }

    /** Re-arms the alarm for an in-progress session after boot / time-set / timezone-change. */
    suspend fun rescheduleActiveSessionIfNeeded() {
        val active = dao.getActiveSessionOnce() ?: return
        val now = System.currentTimeMillis()
        val fireAt = if (active.suggestedWakeEpochMillis > now) {
            active.suggestedWakeEpochMillis
        } else {
            // The scheduled time already passed while the device was off / clock changed under
            // us — fire almost immediately rather than losing the alarm silently.
            now + 5_000L
        }
        scheduler.scheduleWakeAlarm(active.id, fireAt)

        if (active.smartWakeEnabled && active.wakeWindowStartEpochMillis > now) {
            val monitorStart = (active.wakeWindowStartEpochMillis - 5 * 60_000L)
                .coerceAtLeast(now + 60_000L)
            scheduler.scheduleSmartWakeMonitorStart(active.id, monitorStart)
        }
    }

    private fun resolveNextOccurrence(nowMillis: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }
}
