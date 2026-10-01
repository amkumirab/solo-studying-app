package com.amkumirab.solostudying.domain.dungeon

import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.DungeonEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DungeonStudyPlanTest {
    private val today = LocalDate.of(2026, 10, 1)
    private fun dungeon() = DungeonEntity(
        name = "Operating Systems", description = "Course", targetMinutes = 6_000,
        planStartDate = today.toString(), planDeadlineDate = "2026-11-30",
    )
    private fun boss(name: String = "Operating Systems", seconds: Long = 0) = BossEntity(
        name = "Processes", difficulty = "Medium", requiredMinutes = 6_000,
        dungeonName = name, timeSpentSeconds = seconds,
    )

    @Test fun `one hundred hours across two months gives an inclusive daily estimate`() {
        val plan = calculateDungeonStudyPlan(dungeon(), emptyList(), today)!!
        assertEquals(61, plan.studyDaysRemaining)
        assertEquals(99, plan.minutesPerStudyDay)
        assertEquals(6_000, plan.remainingMinutes)
        assertEquals(DungeonPlanStatus.OnTrack, plan.status)
    }

    @Test fun `all matching bosses including completed ones count but other dungeons do not`() {
        val bosses = listOf(boss(seconds = 3_600), boss(seconds = 7_200).copy(isCompleted = true), boss("Other", 99_000))
        val plan = calculateDungeonStudyPlan(dungeon(), bosses, today)!!
        assertEquals(10_800L, plan.completedSeconds)
        assertEquals(5_820, plan.remainingMinutes)
        assertEquals(0.03f, plan.progress, 0.0001f)
    }

    @Test fun `weekdays omit rest days including a weekend deadline`() {
        val plan = calculateDungeonStudyPlan(dungeon().copy(planDeadlineDate = "2026-10-11", studyWeekdaysMask = 31), emptyList(), today)!!
        assertEquals(7, plan.studyDaysRemaining)
        assertEquals(858, plan.minutesPerStudyDay)
    }

    @Test fun `behind schedule is based on elapsed selected study days`() {
        val plan = calculateDungeonStudyPlan(dungeon(), listOf(boss(seconds = 20 * 3_600)), today.plusDays(30))!!
        assertEquals(DungeonPlanStatus.BehindSchedule, plan.status)
        assertEquals(155, plan.minutesPerStudyDay)
    }

    @Test fun `a rest day with no remaining study days has no daily estimate`() {
        val plan = calculateDungeonStudyPlan(dungeon().copy(planDeadlineDate = "2026-10-04", studyWeekdaysMask = 31), emptyList(), LocalDate.of(2026, 10, 3))!!
        assertEquals(DungeonPlanStatus.NoStudyDays, plan.status)
        assertNull(plan.minutesPerStudyDay)
    }

    @Test fun `due today and overdue remain distinct`() {
        assertEquals(DungeonPlanStatus.DueToday, calculateDungeonStudyPlan(dungeon(), emptyList(), LocalDate.of(2026, 11, 30))!!.status)
        val late = calculateDungeonStudyPlan(dungeon(), emptyList(), LocalDate.of(2026, 12, 1))!!
        assertEquals(DungeonPlanStatus.Overdue, late.status)
        assertNull(late.minutesPerStudyDay)
    }

    @Test fun `completed progress is capped and cannot overflow`() {
        val plan = calculateDungeonStudyPlan(dungeon(), listOf(boss(seconds = Long.MAX_VALUE), boss(seconds = Long.MAX_VALUE)), today.plusMonths(3))!!
        assertEquals(DungeonPlanStatus.Completed, plan.status)
        assertEquals(1f, plan.progress)
        assertEquals(0, plan.remainingMinutes)
        assertEquals(0, plan.minutesPerStudyDay)
        assertEquals(360_000L, plan.completedSeconds)
    }

    @Test fun `negative progress is ignored and a partial minute rounds up`() {
        val plan = calculateDungeonStudyPlan(dungeon(), listOf(boss(seconds = -500), boss(seconds = 59)), today)!!
        assertEquals(59L, plan.completedSeconds)
        assertEquals(6_000, plan.remainingMinutes)
    }

    @Test fun `missing malformed and incomplete plans are not displayed`() {
        assertNull(calculateDungeonStudyPlan(dungeon().copy(targetMinutes = null), emptyList(), today))
        assertNull(calculateDungeonStudyPlan(dungeon().copy(planDeadlineDate = "bad"), emptyList(), today))
        assertNull(calculateDungeonStudyPlan(dungeon().copy(studyWeekdaysMask = 0), emptyList(), today))
        assertNull(calculateDungeonStudyPlan(dungeon().copy(planStartDate = "2027-01-01"), emptyList(), today))
    }

    @Test fun `form accepts fractional hours and trims names`() {
        val result = parseDungeonStudyPlanInput(" Operating Systems ", "100.5", "2026-11-30", 127, today)!!
        assertEquals("Operating Systems", result.dungeonName)
        assertEquals(6_030, result.targetMinutes)
        assertEquals(90, parseDungeonStudyPlanInput("OS", "1,5", "2026-11-30", 31, today)!!.targetMinutes)
    }

    @Test fun `form rejects invalid or unsafe inputs`() {
        for (hours in listOf("", "0", "-1", "NaN", "Infinity", "1e4", "10001", "1.234", "999999999999")) {
            assertNull("Accepted $hours", parseDungeonStudyPlanInput("OS", hours, "2026-11-30", 127, today))
        }
        assertNull(parseDungeonStudyPlanInput("All", "100", "2026-11-30", 127, today))
        assertNull(parseDungeonStudyPlanInput(" ", "100", "2026-11-30", 127, today))
        assertNull(parseDungeonStudyPlanInput("OS", "100", "2026-02-30", 127, today))
        assertNull(parseDungeonStudyPlanInput("OS", "100", "2026-09-30", 127, today))
        assertNull(parseDungeonStudyPlanInput("OS", "100", "2036-10-02", 127, today))
        assertNull(parseDungeonStudyPlanInput("OS", "100", "2026-11-30", 0, today))
        assertNull(parseDungeonStudyPlanInput("OS", "100", "2026-11-30", 128, today))
    }
}
