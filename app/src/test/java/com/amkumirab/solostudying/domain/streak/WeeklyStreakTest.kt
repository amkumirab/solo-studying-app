package com.amkumirab.solostudying.domain.streak

import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WeeklyStreakTest {
    private val zoneId = ZoneId.of("Europe/Rome")
    private val today = LocalDate.of(2026, 9, 16)
    private val profile = UserProfileEntity(
        hasCompletedOnboarding = true,
        currentStreak = 4,
        scheduleMinutesPerDay = 60,
        scheduleWeekdayMinutes = "60,60,60,60,60,0,0",
    )

    @Test
    fun `week marks completed missed current upcoming and rest days`() {
        val snapshot = buildWeeklyStreakSnapshot(
            profile = profile,
            sessions = listOf(
                session(LocalDate.of(2026, 9, 14), 60),
                session(LocalDate.of(2026, 9, 15), 30),
                session(today, 20),
            ),
            today = today,
            zoneId = zoneId,
        )

        assertTrue(snapshot.isReady)
        assertEquals(4, snapshot.streakDays)
        assertEquals("40 min to protect your streak", snapshot.message)
        assertEquals(
            listOf(
                WeeklyStreakDayState.Completed,
                WeeklyStreakDayState.Missed,
                WeeklyStreakDayState.Today,
                WeeklyStreakDayState.Upcoming,
                WeeklyStreakDayState.Upcoming,
                WeeklyStreakDayState.Rest,
                WeeklyStreakDayState.Rest,
            ),
            snapshot.days.map(WeeklyStreakDay::state),
        )
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), snapshot.days.map { it.dayLabel })
    }

    @Test
    fun `today is completed only after reaching its configured target`() {
        val snapshot = buildWeeklyStreakSnapshot(
            profile = profile,
            sessions = listOf(session(today, 60)),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(WeeklyStreakDayState.Completed, snapshot.days[2].state)
        assertEquals("Today's mission is complete", snapshot.message)
    }

    @Test
    fun `session dates use the user's local time zone`() {
        val lateTuesdayUtc = LocalDate.of(2026, 9, 15)
            .atTime(22, 30)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val snapshot = buildWeeklyStreakSnapshot(
            profile = profile.copy(scheduleWeekdayMinutes = "0,0,1,0,0,0,0"),
            sessions = listOf(session(timestamp = lateTuesdayUtc, minutes = 1)),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(WeeklyStreakDayState.Completed, snapshot.days[2].state)
        assertEquals(1, snapshot.days[2].studiedMinutes)
    }

    @Test
    fun `setup state keeps all seven calendar days`() {
        val snapshot = buildWeeklyStreakSnapshot(
            profile = null,
            sessions = emptyList(),
            today = today,
            zoneId = zoneId,
        )

        assertFalse(snapshot.isReady)
        assertEquals(7, snapshot.days.size)
        assertEquals(WeeklyStreakDayState.Today, snapshot.days[2].state)
        assertEquals("Open the app to begin your first study week", snapshot.message)
    }

    private fun session(date: LocalDate, minutes: Int) = session(
        timestamp = date.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
        minutes = minutes,
    )

    private fun session(timestamp: Long, minutes: Int) = StudySessionEntity(
        bossId = null,
        bossName = "Focus",
        durationSeconds = minutes * 60L,
        xpEarned = 0,
        goldEarned = 0,
        timestamp = timestamp,
        wasCompleted = true,
        isFreeStudy = true,
    )
}
