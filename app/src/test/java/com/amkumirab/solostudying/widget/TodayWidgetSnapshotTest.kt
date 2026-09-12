package com.amkumirab.solostudying.widget

import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.quickstart.QuickStartSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TodayWidgetSnapshotTest {
    private val today = LocalDate.of(2026, 9, 11)
    private val zoneId = ZoneId.of("Europe/Rome")

    @Test
    fun `widget asks a new user to finish setup`() {
        val snapshot = snapshot(profile = null)

        assertFalse(snapshot.isReady)
        assertEquals(0, snapshot.progressPercent)
        assertEquals("Begin your journey", snapshot.actionTitle)
        assertEquals(25, snapshot.quickStartMinutes)
    }

    @Test
    fun `widget presents today's progress and highest priority action`() {
        val sessionTime = today.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val snapshot = snapshot(
            profile = readyProfile(currentStreak = 6),
            sessions = listOf(session(durationSeconds = 20 * 60L, timestamp = sessionTime)),
            quests = listOf(
                quest(id = 1, title = "Review formulas", priority = 1),
                quest(id = 2, title = "Solve wave problems", priority = 2),
            ),
            quickStart = QuickStartSelection(durationMinutes = 45, skillId = 3),
        )

        assertTrue(snapshot.isReady)
        assertEquals(33, snapshot.progressPercent)
        assertEquals("20 / 60 MIN TODAY", snapshot.progressLabel)
        assertEquals("6-DAY STREAK", snapshot.streakLabel)
        assertEquals("Solve wave problems", snapshot.actionTitle)
        assertEquals("30 min · Today's daily quest", snapshot.actionDetail)
        assertEquals(45, snapshot.quickStartMinutes)
    }

    @Test
    fun `widget celebrates a completed target when no work remains`() {
        val sessionTime = today.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli()
        val snapshot = snapshot(
            profile = readyProfile(),
            sessions = listOf(session(durationSeconds = 75 * 60L, timestamp = sessionTime)),
        )

        assertEquals(100, snapshot.progressPercent)
        assertEquals("75 / 60 MIN TODAY", snapshot.progressLabel)
        assertEquals("Daily target complete", snapshot.actionTitle)
        assertEquals("Mission cleared — keep your momentum", snapshot.actionDetail)
    }

    private fun snapshot(
        profile: UserProfileEntity?,
        sessions: List<StudySessionEntity> = emptyList(),
        quests: List<DailyQuestEntity> = emptyList(),
        quickStart: QuickStartSelection = QuickStartSelection(),
    ) = buildTodayWidgetSnapshot(
        profile = profile,
        sessions = sessions,
        quests = quests,
        bosses = emptyList(),
        steps = emptyList(),
        quickStart = quickStart,
        today = today,
        zoneId = zoneId,
    )

    private fun readyProfile(currentStreak: Int = 0) = UserProfileEntity(
        hasCompletedOnboarding = true,
        hasCompletedTutorial = true,
        currentStreak = currentStreak,
        scheduleDays = "Mon,Tue,Wed,Thu,Fri,Sat,Sun",
        scheduleMinutesPerDay = 60,
        scheduleWeekdayMinutes = "60,60,60,60,60,60,60",
    )

    private fun quest(id: Int, title: String, priority: Int) = DailyQuestEntity(
        id = id,
        title = title,
        durationMinutes = 30,
        scheduledDate = today.toString(),
        priority = priority,
    )

    private fun session(durationSeconds: Long, timestamp: Long) = StudySessionEntity(
        bossId = null,
        bossName = "Focus",
        durationSeconds = durationSeconds,
        xpEarned = 0,
        goldEarned = 0,
        timestamp = timestamp,
        wasCompleted = true,
        isFreeStudy = true,
    )
}
