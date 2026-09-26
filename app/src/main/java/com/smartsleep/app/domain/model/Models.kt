package com.smartsleep.app.domain.model

/**
 * How the user rated a completed sleep session. Purely subjective / self-reported —
 * the app never claims to measure sleep quality medically.
 */
enum class WakeRating(val score: Int) {
    VERY_GOOD(2),
    GOOD(1),
    OK(0),
    BAD(-1),
    VERY_BAD(-2);

    companion object {
        fun fromScoreOrNull(score: Int?): WakeRating? = entries.find { it.score == score }
    }
}

enum class ThemeMode { DARK, LIGHT, SYSTEM }

enum class AppLanguage(val tag: String) { PERSIAN("fa"), ENGLISH("en") }

/**
 * All user-adjustable settings, plus the two values the app quietly adapts over time
 * (adaptiveOnsetDelayMinutes and preferredWindowPosition). Nothing here is a medical claim.
 */
data class AppSettings(
    val sleepCycleMinutes: Double = 90.0,
    val baseOnsetDelayMinutes: Int = 15,
    // Learned adjustment applied on top of baseOnsetDelayMinutes, updated slowly over time.
    val adaptiveOnsetDelayMinutes: Double = 15.0,
    // 0f = prefer the earliest good moment in the wake window, 1f = prefer the latest.
    // Learned slowly from ratings; starts neutral.
    val preferredWindowPosition: Float = 0.7f,
    val alarmSoundUri: String? = null, // null = system default alarm sound
    val volumePercent: Int = 80,
    val vibrationEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 9,
    val smartWakeEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val use24HourFormat: Boolean = true,
    val language: AppLanguage = AppLanguage.PERSIAN
)

/**
 * A currently running (not-yet-woken) sleep session. Persisted so it survives process death,
 * device restarts, etc.
 */
data class ActiveSleepSession(
    val id: Long,
    val sleepStartEpochMillis: Long,
    val estimatedFallAsleepEpochMillis: Long,
    val wakeWindowStartEpochMillis: Long,
    val wakeWindowEndEpochMillis: Long,
    val suggestedWakeEpochMillis: Long,
    val onsetDelayUsedMinutes: Double,
    val cycleMinutesUsed: Double,
    val smartWakeEnabled: Boolean
)
