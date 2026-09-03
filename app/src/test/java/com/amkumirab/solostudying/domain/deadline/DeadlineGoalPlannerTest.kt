package com.amkumirab.solostudying.domain.deadline

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeadlineGoalPlannerTest {

    private val monday = LocalDate.of(2026, 9, 7)
    private val friday = LocalDate.of(2026, 9, 11)

    @Test
    fun `remaining work is divided across configured study days`() {
        val plan = calculateDeadlineGoalPlan(
            boss = deadlineBoss(created = monday, deadline = friday, targetMinutes = 300, spentMinutes = 60),
            scheduleDays = "Mon,Wed,Fri",
            today = monday,
            zoneId = ZoneOffset.UTC,
        )

        assertNotNull(plan)
        assertEquals(3, plan?.studyDaysRemaining)
        assertEquals(240, plan?.remainingMinutes)
        assertEquals(80, plan?.recommendedMinutesPerStudyDay)
        assertEquals(4, plan?.daysRemaining)
        assertEquals(DeadlineGoalStatus.OnTrack, plan?.status)
    }

    @Test
    fun `progress behind the elapsed plan is reported`() {
        val plan = calculateDeadlineGoalPlan(
            boss = deadlineBoss(created = monday, deadline = friday, targetMinutes = 300),
            scheduleDays = "Mon,Wed,Fri",
            today = monday.plusDays(2),
            zoneId = ZoneOffset.UTC,
        )

        assertEquals(DeadlineGoalStatus.BehindSchedule, plan?.status)
        assertEquals(2, plan?.studyDaysRemaining)
        assertEquals(150, plan?.recommendedMinutesPerStudyDay)
    }

    @Test
    fun `sufficient progress remains on track`() {
        val plan = calculateDeadlineGoalPlan(
            boss = deadlineBoss(created = monday, deadline = friday, targetMinutes = 300, spentMinutes = 120),
            scheduleDays = "Mon,Wed,Fri",
            today = monday.plusDays(2),
            zoneId = ZoneOffset.UTC,
        )

        assertEquals(DeadlineGoalStatus.OnTrack, plan?.status)
    }

    @Test
    fun `due and overdue goals receive urgent states`() {
        val dueToday = calculateDeadlineGoalPlan(
            boss = deadlineBoss(created = monday, deadline = friday, targetMinutes = 90),
            scheduleDays = "Mon,Wed,Fri",
            today = friday,
            zoneId = ZoneOffset.UTC,
        )
        val overdue = calculateDeadlineGoalPlan(
            boss = deadlineBoss(created = monday, deadline = friday, targetMinutes = 90),
            scheduleDays = "Mon,Wed,Fri",
            today = friday.plusDays(1),
            zoneId = ZoneOffset.UTC,
        )

        assertEquals(DeadlineGoalStatus.DueToday, dueToday?.status)
        assertEquals(90, dueToday?.recommendedMinutesPerStudyDay)
        assertEquals(DeadlineGoalStatus.Overdue, overdue?.status)
        assertEquals(0, overdue?.recommendedMinutesPerStudyDay)
    }

    @Test
    fun `missing or invalid deadlines are ignored`() {
        assertNull(
            calculateDeadlineGoalPlan(
                boss = deadlineBoss(created = monday, deadline = friday).copy(deadlineDate = null),
                scheduleDays = "",
                today = monday,
            ),
        )
        assertNull(
            calculateDeadlineGoalPlan(
                boss = deadlineBoss(created = monday, deadline = friday).copy(deadlineDate = "not-a-date"),
                scheduleDays = "",
                today = monday,
            ),
        )
    }

    private fun deadlineBoss(
        created: LocalDate,
        deadline: LocalDate,
        targetMinutes: Int = 300,
        spentMinutes: Int = 0,
    ) = BossEntity(
        id = 7,
        name = "Physics Exam",
        difficulty = "Hard",
        requiredMinutes = targetMinutes,
        timeSpentSeconds = spentMinutes * 60L,
        createdAt = created.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        deadlineDate = deadline.toString(),
    )
}
