package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PlannerWeekOverviewTest {
    private val monday = LocalDate.of(2026, 11, 2)
    private fun config(goal: Int? = 120) = PlannerConfig(monday,
        listOf(PlannerCourse(1, 600, monday.plusDays(13), 5, 0)), List(7) { 60 }, 60, goal)
    private fun plan(goal: Int? = 120) = StudyPlan(config(goal), listOf(
        StudyBlock(1, 1, monday, 60), StudyBlock(2, 1, monday.plusDays(1), 30),
        StudyBlock(3, 1, monday.plusWeeks(1), 60),
    ))

    @Test fun `accepted weekly totals and target gap exclude next week without changing inputs`() {
        val source = plan()
        val overview = plannerWeekOverview(source, monday)
        assertEquals(90, overview.plannedMinutes)
        assertEquals(420, overview.availableBudgetMinutes)
        assertEquals(330, overview.unallocatedBudgetMinutes)
        assertEquals(30, overview.remainingGoalMinutes)
        assertEquals(0, overview.aboveGoalMinutes)
        assertEquals(75, overview.percentPlanned)
        assertEquals(0.75f, overview.progress!!, 0f)
        assertEquals(WeekPlanningStatus.UnfilledGoal, overview.status)
        assertEquals(overview, plannerWeekOverview(source.copy(blocks = source.blocks.reversed()), monday))
        assertEquals(source, plan())
    }

    @Test fun `partial weeks exclude dates outside the plan but do not prorate the goal`() {
        val start = monday.plusDays(2)
        val end = monday.plusDays(4)
        val input = config(240).copy(start = start, courses = listOf(PlannerCourse(1, 300, end, 3, 0)))
        val overview = plannerWeekOverview(StudyPlan(input, emptyList()), monday)
        assertEquals(3, overview.activeDays)
        assertTrue(overview.isPartialWeek)
        assertEquals(180, overview.availableBudgetMinutes)
        assertEquals(240, overview.goalMinutes)
        assertEquals(60, overview.budgetShortfallMinutes)
        assertEquals(WeekPlanningStatus.BudgetShortfall, overview.status)
    }

    @Test fun `empty unavailable and outside-plan weeks are explicit and zero-safe`() {
        val input = config().copy(weekdayMinutes = listOf(0, 0, 0, 0, 0, 0, 60),
            courses = listOf(PlannerCourse(1, 60, monday, 3, 0)))
        val source = StudyPlan(input, emptyList())
        val unavailable = plannerWeekOverview(source, monday)
        assertEquals(0, unavailable.availableBudgetMinutes)
        assertEquals(120, unavailable.budgetShortfallMinutes)
        assertEquals(0, unavailable.percentPlanned)
        assertEquals(WeekPlanningStatus.BudgetShortfall, unavailable.status)
        listOf(monday.minusWeeks(1), monday.plusWeeks(1)).forEach { week ->
            val outside = plannerWeekOverview(source, week)
            assertEquals(WeekPlanningStatus.OutsidePlan, outside.status)
            assertEquals(0, outside.activeDays)
            assertEquals(0, outside.availableBudgetMinutes)
            assertNull(outside.goalMinutes)
            assertNull(outside.progress)
            assertNull(outside.percentPlanned)
            assertFalse(outside.isPartialWeek)
        }
        assertTrue(runCatching { plannerWeekOverview(source, monday.plusDays(1)) }.isFailure)
    }

    @Test fun `no weekly target and zero target never pretend to be positive completion`() {
        val noGoal = plannerWeekOverview(plan(null), monday)
        assertEquals(WeekPlanningStatus.NoGoal, noGoal.status)
        assertNull(noGoal.progress)
        assertNull(noGoal.remainingGoalMinutes)
        val zero = plannerWeekOverview(StudyPlan(config(0), emptyList()), monday)
        assertEquals(WeekPlanningStatus.ZeroGoal, zero.status)
        assertNull(zero.progress)
        assertNull(zero.percentPlanned)
        assertEquals(0, zero.remainingGoalMinutes)
        val legacy = plannerWeekOverview(plan(0), monday)
        assertEquals(WeekPlanningStatus.AboveGoal, legacy.status)
        assertEquals(90, legacy.aboveGoalMinutes)
        assertNull(legacy.progress)
    }

    @Test fun `legacy over-goal time is separate from capped progress and a fully planned goal`() {
        val above = plannerWeekOverview(plan(60), monday)
        assertEquals(30, above.aboveGoalMinutes)
        assertEquals(0, above.remainingGoalMinutes)
        assertEquals(100, above.percentPlanned)
        assertEquals(1f, above.progress!!, 0f)
        assertEquals(WeekPlanningStatus.AboveGoal, above.status)
        assertEquals(WeekPlanningStatus.FullyPlanned, plannerWeekOverview(plan(90), monday).status)
    }

    @Test fun `minimum breaks are separate from focus budgets and manually locked work`() {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 660))) })
        val input = config().copy(weekdayMinutes = List(7) { 90 }, studyWindows = windows, breakMinutes = 10)
        val blocks = listOf(StudyBlock(1, 1, monday, 45, 540, locked = true),
            StudyBlock(2, 1, monday, 45, 595, durationEdited = true, manual = true))
        val source = StudyPlan(input, blocks)
        val overview = plannerWeekOverview(source, monday)
        assertEquals(90, overview.plannedMinutes)
        assertEquals(630, overview.availableBudgetMinutes)
        assertEquals(540, overview.unallocatedBudgetMinutes)
        assertEquals(10, overview.reservedBreakMinutes)
        assertEquals(blocks, source.blocks)
    }

    @Test fun `a break-induced unmet target is not falsely diagnosed as a focus-budget shortage`() {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 660))) })
        val input = config().copy(courses = listOf(PlannerCourse(1, 120, monday, 5, 0)),
            weekdayMinutes = List(7) { 120 }, studyWindows = windows, breakMinutes = 10)
        val overview = plannerWeekOverview(buildStudyPlan(input), monday)
        assertEquals(110, overview.plannedMinutes)
        assertEquals(120, overview.availableBudgetMinutes)
        assertEquals(10, overview.remainingGoalMinutes)
        assertEquals(0, overview.budgetShortfallMinutes)
        assertEquals(WeekPlanningStatus.UnfilledGoal, overview.status)
    }

    @Test fun `generated plans conserve budget and target metrics at minute precision`() {
        repeat(80) { seed ->
            val goal = seed * 7
            val source = buildStudyPlan(config(goal))
            val overview = plannerWeekOverview(source, monday)
            assertEquals(overview.availableBudgetMinutes, overview.plannedMinutes + overview.unallocatedBudgetMinutes)
            assertEquals((goal - overview.plannedMinutes).coerceAtLeast(0), overview.remainingGoalMinutes)
            assertEquals((overview.plannedMinutes - goal).coerceAtLeast(0), overview.aboveGoalMinutes)
            overview.progress?.let { assertTrue(it.isFinite() && it in 0f..1f) }
            assertEquals(overview, plannerWeekOverview(source, monday))
        }
    }
}
