package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class WeeklyPlannerReportTest {
    private val monday = LocalDate.of(2026, 10, 26)
    private val bosses = (1..5).map { BossEntity(id = it, name = "Course $it", difficulty = "Hard", requiredMinutes = 120) }
    private fun config() = PlannerConfig(monday, (1..5).map { PlannerCourse(it, 120, monday.plusDays(13), it, 0) }, List(7) { 180 }, 60)
    private fun plan() = StudyPlan(config(), (1..5).map { StudyBlock(it, it, monday.plusDays((it - 1).toLong()), it * 10) } +
        StudyBlock(9, 1, monday.plusDays(7), 60))

    @Test fun `weekly rows conserve exact course and priority totals without counting next week`() {
        val source = plan()
        val report = weeklyPlannerReport(source, bosses, monday)
        assertEquals((0L..6L).map(monday::plusDays), report.days.map { it.date })
        assertEquals(listOf(10, 20, 30, 40, 50, 0, 0), report.days.map { it.plannedMinutes })
        assertEquals(150, report.plannedMinutes)
        assertEquals(report.plannedMinutes, report.courses.sumOf { it.plannedMinutes })
        assertEquals(report.plannedMinutes, report.priorities.sumOf { it.plannedMinutes })
        assertEquals(listOf(30, 30, 90), report.priorities.map { it.plannedMinutes })
        assertEquals(listOf(PlannerPriorityBand.Low, PlannerPriorityBand.Normal, PlannerPriorityBand.High), report.priorities.map { it.band })
        assertEquals(listOf(monday.plusDays(4)), report.busiestDays)
        assertEquals(0L, report.creditedSeconds)
        assertEquals(source, plan())
        assertEquals(report, weeklyPlannerReport(source.copy(blocks = source.blocks.reversed()), bosses, monday))
    }

    @Test fun `credit is assigned across the full ID ledger before filtering the week`() {
        val input = config().copy(courses = listOf(config().courses.first()))
        val source = StudyPlan(input, listOf(StudyBlock(9, 1, monday, 60), StudyBlock(1, 1, monday.plusDays(7), 60)))
        val studied = bosses.map { if (it.id == 1) it.copy(timeSpentSeconds = 1801) else it }
        assertEquals(0L, weeklyPlannerReport(source, studied, monday).creditedSeconds)
        assertEquals(1801L, weeklyPlannerReport(source, studied, monday.plusDays(7)).creditedSeconds)
        assertEquals(source.blocks.map { it.id }, listOf(9, 1))
    }

    @Test fun `breaks stay separate from planned focus and fractional minute credit stays exact`() {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 660))) })
        val input = config().copy(courses = listOf(config().courses.first().copy(finishBy = monday)),
            weekdayMinutes = List(7) { 120 }, studyWindows = windows, breakMinutes = 10)
        val source = buildStudyPlan(input)
        val report = weeklyPlannerReport(source, bosses.map { if (it.id == 1) it.copy(timeSpentSeconds = 45) else it }, monday)
        assertEquals(110, report.plannedMinutes)
        assertEquals(10, report.breakMinutes)
        assertEquals(45L, report.creditedSeconds)
        assertEquals(45L, report.courses.single().creditedSeconds)
        assertEquals(2, report.days.first().sessionCount)
        assertEquals(10, source.shortfalls[1])
    }

    @Test fun `empty weeks retain seven zero days and every accepted course without bogus highlights`() {
        listOf(plan(), StudyPlan(config(), emptyList())).forEach { source ->
            val report = weeklyPlannerReport(source, bosses, monday.minusWeeks(1))
            assertEquals(7, report.days.size)
            assertTrue(report.days.all { it.plannedMinutes == 0 && it.sessionCount == 0 && it.breakMinutes == 0 })
            assertEquals(5, report.courses.size)
            assertTrue(report.courses.all { it.plannedMinutes == 0 && it.creditedSeconds == 0L })
            assertEquals(0, report.plannedMinutes)
            assertTrue(report.busiestDays.isEmpty())
        }
        assertTrue(runCatching { weeklyPlannerReport(plan(), bosses, monday.plusDays(1)) }.isFailure)
    }

    @Test fun `missing and completed goals follow existing credit rules without changing planned totals`() {
        val source = plan()
        val report = weeklyPlannerReport(source, bosses.filter { it.id != 1 }.map { it.copy(isCompleted = true) }, monday)
        assertEquals(150, report.plannedMinutes)
        assertTrue(report.courses.single { it.bossId == 1 }.missingCourse)
        assertEquals(0L, report.courses.single { it.bossId == 1 }.creditedSeconds)
        assertEquals(140 * 60L, report.creditedSeconds)
        assertEquals(listOf(1, 2, 3, 4, 5), report.courses.sortedBy { it.bossId }.map { it.priority })
    }

    @Test fun `baselines clamp negative and excessive credit and preserve exact totals over generated cases`() {
        repeat(80) { seed ->
            val input = config().copy(courses = config().courses.map { it.copy(baselineSeconds = 100) })
            val source = plan().copy(config = input)
            val studied = bosses.map { it.copy(timeSpentSeconds = seed * 123L - 50) }
            val report = weeklyPlannerReport(source, studied, monday)
            assertTrue(report.creditedSeconds in 0L..report.plannedMinutes * 60L)
            assertEquals(report.creditedSeconds, report.courses.sumOf { it.creditedSeconds })
            assertEquals(report.plannedMinutes, report.days.sumOf { it.plannedMinutes })
            assertEquals(report.plannedMinutes, report.priorities.sumOf { it.plannedMinutes })
            assertEquals(report, weeklyPlannerReport(source, studied, monday))
        }
    }
}
