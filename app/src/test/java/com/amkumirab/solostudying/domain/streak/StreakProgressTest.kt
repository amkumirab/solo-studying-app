package com.amkumirab.solostudying.domain.streak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakProgressTest {

    @Test
    fun `consecutive study dates advance across daylight saving changes`() {
        val springForward = calculateStreakProgress(
            currentStreak = 4,
            longestStreak = 6,
            lastStudyDate = "2026-03-28",
            today = LocalDate.of(2026, 3, 29),
        )
        val fallBack = calculateStreakProgress(
            currentStreak = 5,
            longestStreak = 5,
            lastStudyDate = "2026-10-24",
            today = LocalDate.of(2026, 10, 25),
        )

        assertEquals(5, springForward.current)
        assertEquals(6, springForward.longest)
        assertTrue(springForward.advanced)
        assertEquals(6, fallBack.current)
        assertEquals(6, fallBack.longest)
        assertTrue(fallBack.advanced)
    }

    @Test
    fun `same date does not advance or repeat a streak reward`() {
        val progress = calculateStreakProgress(
            currentStreak = 7,
            longestStreak = 9,
            lastStudyDate = "2026-09-29",
            today = LocalDate.of(2026, 9, 29),
        )

        assertEquals(7, progress.current)
        assertEquals(9, progress.longest)
        assertFalse(progress.advanced)
    }

    @Test
    fun `missed or invalid dates restart the streak safely`() {
        val missedDay = calculateStreakProgress(
            currentStreak = 12,
            longestStreak = 12,
            lastStudyDate = "2026-09-26",
            today = LocalDate.of(2026, 9, 29),
        )
        val invalidDate = calculateStreakProgress(
            currentStreak = 4,
            longestStreak = 8,
            lastStudyDate = "not-a-date",
            today = LocalDate.of(2026, 9, 29),
        )

        assertEquals(1, missedDay.current)
        assertEquals(12, missedDay.longest)
        assertTrue(missedDay.advanced)
        assertEquals(1, invalidDate.current)
        assertEquals(8, invalidDate.longest)
        assertTrue(invalidDate.advanced)
    }
}
