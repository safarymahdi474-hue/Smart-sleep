package com.smartsleep.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One sleep session, from "Start Sleep" to being marked complete (alarm dismissed + optionally
 * rated). While the session is still in progress, actualWakeEpochMillis / ratingScore /
 * wokeBeforeAlarm are null.
 */
@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sleepStartEpochMillis: Long,
    val estimatedFallAsleepEpochMillis: Long,
    val wakeWindowStartEpochMillis: Long,
    val wakeWindowEndEpochMillis: Long,
    val suggestedWakeEpochMillis: Long,
    val onsetDelayUsedMinutes: Double,
    val cycleMinutesUsed: Double,
    val smartWakeEnabled: Boolean,
    val actualWakeEpochMillis: Long? = null,
    val ratingScore: Int? = null, // maps to WakeRating.score
    val wokeBeforeAlarm: Boolean? = null,
    val isActive: Boolean = true
)
