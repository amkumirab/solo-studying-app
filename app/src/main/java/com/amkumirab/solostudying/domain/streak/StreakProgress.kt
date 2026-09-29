package com.amkumirab.solostudying.domain.streak

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class StreakProgress(
    val current: Int,
    val longest: Int,
    val advanced: Boolean,
)

internal fun calculateStreakProgress(
    currentStreak: Int,
    longestStreak: Int,
    lastStudyDate: String?,
    today: LocalDate,
): StreakProgress {
    val safeCurrent = currentStreak.coerceAtLeast(0)
    val previousDate = lastStudyDate
        ?.let { date -> runCatching { LocalDate.parse(date) }.getOrNull() }
    val daysSincePreviousStudy = previousDate?.let { ChronoUnit.DAYS.between(it, today) }

    val advanced = daysSincePreviousStudy == null || daysSincePreviousStudy > 0L
    val updatedCurrent = when {
        daysSincePreviousStudy == null -> 1
        daysSincePreviousStudy <= 0L -> safeCurrent
        daysSincePreviousStudy == 1L -> safeCurrent + 1
        else -> 1
    }

    return StreakProgress(
        current = updatedCurrent,
        longest = maxOf(longestStreak, updatedCurrent),
        advanced = advanced,
    )
}
