package com.smartsleep.app.domain

import com.smartsleep.app.data.local.SleepSessionEntity
import com.smartsleep.app.domain.model.AppSettings
import com.smartsleep.app.domain.model.WakeRating
import kotlin.math.max
import kotlin.math.min

/**
 * Nudges two learned settings after each rated session:
 *  - adaptiveOnsetDelayMinutes: how long the user actually seems to take to fall asleep.
 *  - preferredWindowPosition: whether the user tends to feel better waking earlier or later
 *    within their own chosen window.
 *
 * This is a simple, transparent heuristic (small fixed-step moving adjustment) — not a claim of
 * clinical sleep-stage inference. It only ever nudges by a small step per session so it can't
 * swing wildly from one data point, and every value stays within sane bounds.
 */
object AdaptiveLearningEngine {

    private const val ONSET_STEP_MINUTES = 1.0
    private const val POSITION_STEP = 0.04f

    fun updateAfterSession(
        current: AppSettings,
        session: SleepSessionEntity,
        rating: WakeRating,
        wokeBeforeAlarm: Boolean
    ): AppSettings {
        var onset = current.adaptiveOnsetDelayMinutes
        var position = current.preferredWindowPosition

        // If the user woke on their own before the alarm and felt good, they likely didn't need
        // the full window — lean the suggestion a bit earlier next time.
        if (wokeBeforeAlarm && rating.score >= 1) {
            position = (position - POSITION_STEP).coerceIn(0f, 1f)
        }

        // Good ratings reinforce the current position slightly toward where we already were;
        // bad ratings push away from it (toward the opposite edge of the window).
        when {
            rating == WakeRating.VERY_BAD || rating == WakeRating.BAD -> {
                position = if (position >= 0.5f) {
                    (position - POSITION_STEP).coerceIn(0f, 1f)
                } else {
                    (position + POSITION_STEP).coerceIn(0f, 1f)
                }
                // A bad rating also suggests the onset-delay estimate may be off; widen it a touch
                // toward the population-typical range (10-20 min) rather than drifting endlessly.
                onset = when {
                    onset < 10.0 -> min(onset + ONSET_STEP_MINUTES, 20.0)
                    onset > 20.0 -> max(onset - ONSET_STEP_MINUTES, 10.0)
                    else -> onset
                }
            }
            rating == WakeRating.VERY_GOOD || rating == WakeRating.GOOD -> {
                // Reinforce: nudge slightly further in the same direction, capped.
                position = if (position >= 0.5f) {
                    (position + POSITION_STEP / 2).coerceIn(0f, 1f)
                } else {
                    (position - POSITION_STEP / 2).coerceIn(0f, 1f)
                }
            }
            else -> Unit // OK rating: no change
        }

        return current.copy(
            adaptiveOnsetDelayMinutes = onset.coerceIn(5.0, 30.0),
            preferredWindowPosition = position
        )
    }
}
