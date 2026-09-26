package com.smartsleep.app.domain

import kotlin.math.roundToLong

/**
 * Pure, side-effect-free implementation of the wake-time suggestion algorithm described by
 * the product spec:
 *
 *  1. sleepStart is recorded when the user taps "Start Sleep".
 *  2. estimatedFallAsleep = sleepStart + onsetDelay.
 *  3. Candidate wake moments = estimatedFallAsleep + n * cycleLength, for n = 1, 2, 3, ...
 *  4. Keep only candidates that fall inside [wakeWindowStart, wakeWindowEnd].
 *  5. Pick the "best" candidate inside the window (see [pickBestCandidate]).
 *
 * The 90-minute cycle length is treated purely as a configurable rough estimate — never as an
 * exact physiological measurement. All time values are epoch millis (UTC) so callers are
 * responsible for resolving the user's wake-window clock times to the correct absolute instants
 * (see [CalculatorInput.wakeWindowStartMillis] / [wakeWindowEndMillis]) before calling this.
 */
object SleepCycleCalculator {

    data class CalculatorInput(
        val sleepStartMillis: Long,
        val onsetDelayMinutes: Double,
        val cycleLengthMinutes: Double,
        val wakeWindowStartMillis: Long,
        val wakeWindowEndMillis: Long,
        // 0f = prefer earliest candidate in window, 1f = prefer latest. 0.5 = middle.
        val preferredWindowPosition: Float = 0.7f,
        val maxCyclesToConsider: Int = 12
    )

    data class CalculatorResult(
        val estimatedFallAsleepMillis: Long,
        val candidates: List<Long>,
        val suggestedWakeMillis: Long
    )

    fun calculate(input: CalculatorInput): CalculatorResult {
        require(input.wakeWindowEndMillis > input.wakeWindowStartMillis) {
            "wakeWindowEndMillis must be after wakeWindowStartMillis"
        }
        val onsetDelayMillis = (input.onsetDelayMinutes * 60_000.0).roundToLong()
        val cycleMillis = (input.cycleLengthMinutes * 60_000.0).roundToLong()
        val fallAsleep = input.sleepStartMillis + onsetDelayMillis

        val candidates = mutableListOf<Long>()
        for (n in 1..input.maxCyclesToConsider) {
            val candidate = fallAsleep + n * cycleMillis
            if (candidate in input.wakeWindowStartMillis..input.wakeWindowEndMillis) {
                candidates.add(candidate)
            }
            if (candidate > input.wakeWindowEndMillis) break
        }

        val suggested = pickBestCandidate(
            candidates = candidates,
            windowStart = input.wakeWindowStartMillis,
            windowEnd = input.wakeWindowEndMillis,
            preferredPosition = input.preferredWindowPosition
        )

        return CalculatorResult(
            estimatedFallAsleepMillis = fallAsleep,
            candidates = candidates,
            suggestedWakeMillis = suggested
        )
    }

    /**
     * Chooses the candidate closest to a target point inside the window, where the target point
     * is interpolated between windowStart and windowEnd using [preferredPosition]
     * (0 = windowStart, 1 = windowEnd). Defaults to leaning late (0.7) since a later full cycle
     * inside the user's own bounds generally means more total sleep, while still respecting the
     * user's outer limit. Falls back to windowEnd itself if no full cycle lands in the window,
     * since we must still wake the user by their latest bound.
     */
    private fun pickBestCandidate(
        candidates: List<Long>,
        windowStart: Long,
        windowEnd: Long,
        preferredPosition: Float
    ): Long {
        if (candidates.isEmpty()) return windowEnd
        val clampedPos = preferredPosition.coerceIn(0f, 1f)
        val target = windowStart + ((windowEnd - windowStart) * clampedPos).toLong()
        return candidates.minByOrNull { kotlin.math.abs(it - target) } ?: windowEnd
    }
}
