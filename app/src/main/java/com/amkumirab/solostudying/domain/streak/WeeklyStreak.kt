package com.amkumirab.solostudying.domain.streak

import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.domain.today.dailyTargetMinutes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class WeeklyStreakDayState {
    Completed,
    Missed,
    Today,
    Upcoming,
    Rest,
}

data class WeeklyStreakDay(
    val date: LocalDate,
    val dayLabel: String,
    val studiedMinutes: Int,
    val targetMinutes: Int,
    val state: WeeklyStreakDayState,
)

data class WeeklyStreakSnapshot(
    val isReady: Boolean,
    val streakDays: Int,
    val message: String,
    val days: List<WeeklyStreakDay>,
)

fun buildWeeklyStreakSnapshot(
    profile: UserProfileEntity?,
    sessions: List<StudySessionEntity>,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): WeeklyStreakSnapshot {
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    if (profile == null || !profile.hasCompletedOnboarding) {
        return WeeklyStreakSnapshot(
            isReady = false,
            streakDays = 0,
            message = "Open the app to begin your first study week",
            days = (0L..6L).map { offset ->
                val date = weekStart.plusDays(offset)
                WeeklyStreakDay(
                    date = date,
                    dayLabel = date.dayOfWeek.name.take(1),
                    studiedMinutes = 0,
                    targetMinutes = 0,
                    state = if (date == today) WeeklyStreakDayState.Today else WeeklyStreakDayState.Upcoming,
                )
            },
        )
    }

    val studiedSecondsByDate = sessions
        .groupBy { session ->
            Instant.ofEpochMilli(session.timestamp).atZone(zoneId).toLocalDate()
        }
        .mapValues { (_, sessionsForDate) ->
            sessionsForDate.sumOf { session -> session.durationSeconds.coerceAtLeast(0L) }
        }

    val days = (0L..6L).map { offset ->
        val date = weekStart.plusDays(offset)
        val targetMinutes = dailyTargetMinutes(profile, date)
        val studiedSeconds = studiedSecondsByDate[date] ?: 0L
        val targetSeconds = targetMinutes * 60L
        val state = when {
            targetMinutes == 0 -> WeeklyStreakDayState.Rest
            date > today -> WeeklyStreakDayState.Upcoming
            studiedSeconds >= targetSeconds -> WeeklyStreakDayState.Completed
            date == today -> WeeklyStreakDayState.Today
            else -> WeeklyStreakDayState.Missed
        }
        WeeklyStreakDay(
            date = date,
            dayLabel = date.dayOfWeek.name.take(1),
            studiedMinutes = (studiedSeconds / 60L).toInt(),
            targetMinutes = targetMinutes,
            state = state,
        )
    }

    val todayState = days.first { it.date == today }
    val remainingMinutes = (todayState.targetMinutes - todayState.studiedMinutes).coerceAtLeast(0)
    val message = when (todayState.state) {
        WeeklyStreakDayState.Completed -> "Today's mission is complete"
        WeeklyStreakDayState.Rest -> "Recovery day — return stronger tomorrow"
        WeeklyStreakDayState.Today -> if (profile.currentStreak > 0) {
            "$remainingMinutes min to protect your streak"
        } else {
            "$remainingMinutes min to begin your streak"
        }
        WeeklyStreakDayState.Missed,
        WeeklyStreakDayState.Upcoming,
        -> "Your next mission is waiting"
    }

    return WeeklyStreakSnapshot(
        isReady = true,
        streakDays = profile.currentStreak.coerceAtLeast(0),
        message = message,
        days = days,
    )
}
