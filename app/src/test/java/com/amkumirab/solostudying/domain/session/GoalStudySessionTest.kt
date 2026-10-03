package com.amkumirab.solostudying.domain.session

import com.amkumirab.solostudying.data.entity.BossEntity
import org.junit.Assert.*
import org.junit.Test

class GoalStudySessionTest {
    private val boss = BossEntity(name = "Operating Systems", difficulty = "Hard", requiredMinutes = 6000)

    @Test fun `session length is separate from the goal budget`() {
        assertEquals(1500L, goalSessionSeconds(boss, 25))
        assertFalse(goalCompletedBySession(boss, 1500))
        assertEquals(10L, goalSessionSeconds(boss.copy(timeSpentSeconds = 359990), 60))
        assertTrue(goalCompletedBySession(boss, 360000))
    }

    @Test fun `replay and deliverable sessions preserve accumulated time`() {
        assertEquals(2700L, goalSessionSeconds(boss.copy(isCompleted = true, timeSpentSeconds = 360000), 45))
        assertFalse(goalCompletedBySession(boss.copy(isCompleted = true), 362700))
        assertEquals(2700L, goalSessionSeconds(boss.copy(isRealBoss = true, timeSpentSeconds = 360000), 45))
        assertFalse(goalCompletedBySession(boss.copy(isRealBoss = true), 362700))
    }

    @Test fun `recommendation caps a large daily target to one focus block`() {
        assertEquals(25, suggestedGoalSessionMinutes(null))
        assertEquals(25, suggestedGoalSessionMinutes(0))
        assertEquals(45, suggestedGoalSessionMinutes(45))
        assertEquals(60, suggestedGoalSessionMinutes(236))
    }

    @Test fun `invalid durations are rejected`() {
        listOf(-1, 0, 481, Int.MAX_VALUE).forEach { minutes ->
            assertThrows(IllegalArgumentException::class.java) { goalSessionSeconds(boss, minutes) }
        }
    }
}
