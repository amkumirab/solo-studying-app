package com.amkumirab.solostudying.domain.today

import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.BossStepEntity
import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TodayPlanTest {

    private val today = LocalDate.of(2026, 9, 6)
    private val zoneId = ZoneId.of("Europe/Rome")
    private val profile = UserProfileEntity(
        scheduleDays = "Mon,Tue,Wed,Thu,Fri,Sat,Sun",
        scheduleMinutesPerDay = 60,
        scheduleWeekdayMinutes = "60,60,60,60,60,60,60",
    )

    @Test
    fun `step for goal due today is the next action`() {
        val boss = boss(
            id = 4,
            name = "Physics Exam",
            deadline = today.toString(),
        )
        val step = step(id = 9, bossId = boss.id, title = "Review wave propagation")
        val carriedQuest = quest(
            id = 6,
            title = "Read lecture notes",
            scheduledDate = today.minusDays(1).toString(),
            priority = 2,
        )

        val plan = buildTodayPlan(
            profile = profile,
            sessions = emptyList(),
            quests = listOf(carriedQuest),
            bosses = listOf(boss),
            steps = listOf(step),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(TodayPlanItemType.BossStep, plan.items.first().type)
        assertEquals(step.id, plan.items.first().sourceId)
        assertEquals("Physics Exam · due today", plan.items.first().context)
        assertEquals(TodayPlanUrgency.Urgent, plan.items.first().urgency)
    }

    @Test
    fun `carried quest comes before an ordinary study step`() {
        val boss = boss(id = 12, name = "Programming Course")
        val step = step(id = 15, bossId = boss.id, title = "Practice collections")
        val carriedQuest = quest(
            id = 18,
            title = "Finish yesterday's exercises",
            scheduledDate = today.minusDays(1).toString(),
        )

        val plan = buildTodayPlan(
            profile = profile,
            sessions = emptyList(),
            quests = listOf(carriedQuest),
            bosses = listOf(boss),
            steps = listOf(step),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(TodayPlanItemType.DailyQuest, plan.items[0].type)
        assertEquals(carriedQuest.id, plan.items[0].sourceId)
        assertEquals(TodayPlanItemType.BossStep, plan.items[1].type)
        assertEquals(TodayPlanItemType.QuickFocus, plan.items[2].type)
    }

    @Test
    fun `daily progress only includes sessions in the local calendar day`() {
        val insideToday = today.atTime(9, 30).atZone(zoneId).toInstant().toEpochMilli()
        val beforeToday = today.minusDays(1).atTime(23, 59).atZone(zoneId).toInstant().toEpochMilli()
        val afterToday = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val plan = buildTodayPlan(
            profile = profile,
            sessions = listOf(
                session(durationSeconds = 25 * 60L, timestamp = insideToday),
                session(durationSeconds = 50 * 60L, timestamp = beforeToday),
                session(durationSeconds = 10 * 60L, timestamp = afterToday),
            ),
            quests = emptyList(),
            bosses = emptyList(),
            steps = emptyList(),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(25 * 60L, plan.studiedSeconds)
        assertEquals(35, plan.remainingMinutes)
        assertEquals(25f / 60f, plan.progress, 0.001f)
        assertFalse(plan.isDailyTargetComplete)
    }

    @Test
    fun `completed work is excluded and remaining target offers a short focus block`() {
        val completedBoss = boss(id = 30, name = "Completed Goal").copy(isCompleted = true)
        val completedStep = step(id = 31, bossId = completedBoss.id, title = "Finished step")
            .copy(isCompleted = true)
        val completedQuest = quest(
            id = 32,
            title = "Finished quest",
            scheduledDate = today.toString(),
        ).copy(isCompleted = true)

        val plan = buildTodayPlan(
            profile = profile.copy(scheduleWeekdayMinutes = "30,30,30,30,30,30,30"),
            sessions = emptyList(),
            quests = listOf(completedQuest),
            bosses = listOf(completedBoss),
            steps = listOf(completedStep),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(1, plan.items.size)
        assertEquals(TodayPlanItemType.QuickFocus, plan.items.single().type)
        assertEquals(25, plan.items.single().durationMinutes)
        assertEquals(30, plan.remainingMinutes)
    }

    @Test
    fun `zero minute schedule is treated as a completed rest day`() {
        val plan = buildTodayPlan(
            profile = profile.copy(scheduleWeekdayMinutes = "0,0,0,0,0,0,0"),
            sessions = emptyList(),
            quests = emptyList(),
            bosses = emptyList(),
            steps = emptyList(),
            today = today,
            zoneId = zoneId,
        )

        assertTrue(plan.isRestDay)
        assertTrue(plan.isDailyTargetComplete)
        assertEquals(0, plan.remainingMinutes)
        assertTrue(plan.items.isEmpty())
    }

    private fun boss(
        id: Int,
        name: String,
        deadline: String? = null,
    ) = BossEntity(
        id = id,
        name = name,
        difficulty = "Medium",
        requiredMinutes = 120,
        createdAt = today.minusDays(5).atStartOfDay(zoneId).toInstant().toEpochMilli(),
        deadlineDate = deadline,
    )

    private fun step(
        id: Int,
        bossId: Int,
        title: String,
    ) = BossStepEntity(
        id = id,
        bossId = bossId,
        title = title,
        estimatedMinutes = 35,
        sortOrder = 0,
    )

    private fun quest(
        id: Int,
        title: String,
        scheduledDate: String,
        priority: Int = 1,
    ) = DailyQuestEntity(
        id = id,
        title = title,
        durationMinutes = 30,
        scheduledDate = scheduledDate,
        priority = priority,
    )

    private fun session(
        durationSeconds: Long,
        timestamp: Long,
    ) = StudySessionEntity(
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
