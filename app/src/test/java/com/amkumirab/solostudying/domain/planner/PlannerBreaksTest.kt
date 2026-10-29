package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PlannerBreaksTest {
    private val monday = LocalDate.of(2026, 10, 26)
    private val bosses = listOf(BossEntity(id = 1, name = "Physics", difficulty = "Hard", requiredMinutes = 300))
    private fun config(end: Int = 660, effort: Int = 120, length: Int = 60): PlannerConfig {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, end))) })
        return PlannerConfig(monday, listOf(PlannerCourse(1, effort, monday, 5, 0)),
            windows.weekdayMinutes, length, studyWindows = windows, breakMinutes = 10)
    }

    @Test fun `two hour window includes a break without crediting it or trailing padding`() {
        val plan = buildStudyPlan(config())
        assertEquals(listOf(60, 50), plan.blocks.map { it.minutes })
        assertEquals(listOf(540, 610), plan.blocks.map { it.startMinute })
        assertEquals(mapOf(1 to 10), plan.shortfalls)
        assertEquals(0L, blockProgress(plan, bosses).sumOf { it.creditedSeconds })
        val one = buildStudyPlan(config(effort = 60))
        assertEquals(listOf(StudyBlock(1, 1, monday, 60, 540)), one.blocks)
        assertTrue(one.shortfalls.isEmpty())
    }

    @Test fun `natural gaps count and adjacent windows cannot bypass a break`() {
        val input = config(effort = 60, length = 30)
        fun windows(secondStart: Int) = WeeklyAvailability(List(7) {
            StudyDayAvailability(true, listOf(StudyTimeWindow(540, 570), StudyTimeWindow(secondStart, secondStart + 30)))
        })
        val separate = buildStudyPlan(input.copy(weekdayMinutes = List(7) { 60 }, studyWindows = windows(600)))
        assertEquals(listOf(540, 600), separate.blocks.map { it.startMinute })
        assertTrue(separate.shortfalls.isEmpty())
        val adjacent = buildStudyPlan(input.copy(weekdayMinutes = List(7) { 60 }, studyWindows = windows(570)))
        assertEquals(listOf(540, 580), adjacent.blocks.map { it.startMinute })
        assertEquals(10, adjacent.shortfalls[1])
    }

    @Test fun `manual moves additions and growth cannot consume reserved break time`() {
        val input = config(effort = 180, length = 60)
        val plan = StudyPlan(input, listOf(StudyBlock(4, 1, monday, 30, 540), StudyBlock(8, 1, monday, 30, 580)))
        assertNull(moveTimedStudyBlock(plan, 8, monday, 570, monday, 0))
        assertNotNull(moveTimedStudyBlock(plan, 8, monday, 610, monday, 0))
        assertNull(previewAddStudySession(plan, bosses, monday, 1, monday, 610, 20).plan)
        assertNotNull(previewAddStudySession(plan, bosses, monday, 1, monday, 620, 20).plan)
        assertNull(previewStudyDuration(plan, bosses, monday, 4, 31).plan)
        assertTrue(runCatching { plan.copy(blocks = plan.blocks.map { if (it.id == 8) it.copy(startMinute = 575) else it }) }.isFailure)
    }

    @Test fun `locked slots stay exact and incompatible spacing is rejected atomically`() {
        val input = config(end = 720, effort = 150, length = 30)
        val lock = StudyBlock(99, 1, monday, 30, 600, locked = true)
        val source = StudyPlan(input, listOf(lock))
        val rebuilt = rebuildWithStudyLocks(input, source, bosses, monday).plan!!
        assertEquals(lock, rebuilt.blocks.single { it.locked })
        assertEquals(rebuilt, rebuildWithStudyLocks(input, source, bosses, monday).plan)
        assertTrue(rebuilt.blocks.filterNot { it.locked }.all { it.startMinute!! + it.minutes <= 590 || it.startMinute >= 640 })
        val close = StudyPlan(input.copy(breakMinutes = 0), listOf(lock, StudyBlock(100, 1, monday, 30, 630, locked = true)))
        val rejected = rebuildWithStudyLocks(input, close, bosses, monday)
        assertNull(rejected.plan)
        assertNotNull(rejected.error)
        assertEquals(630, close.blocks.last().startMinute)
    }

    @Test fun `zero is compatible and invalid or untimed gaps are refused`() {
        val input = config().copy(breakMinutes = 0)
        assertEquals(listOf(540, 600), buildStudyPlan(input).blocks.map { it.startMinute })
        listOf(-1, 31, Int.MAX_VALUE).forEach { invalid -> assertTrue(runCatching { input.copy(breakMinutes = invalid) }.isFailure) }
        assertTrue(runCatching { input.copy(studyWindows = null, breakMinutes = 10) }.isFailure)
        val midnight = config().copy(studyWindows = WeeklyAvailability(List(7) {
            StudyDayAvailability(true, listOf(StudyTimeWindow(1320, 1440)))
        }))
        assertEquals(1440, buildStudyPlan(midnight).blocks.last().let { it.startMinute!! + it.minutes })
    }

    @Test fun `generated multi course plans respect deadlines weekly caps and exact accounting`() {
        repeat(60) { seed ->
            val input = config(end = 720, length = listOf(15, 25, 45, 60)[seed % 4]).copy(
                courses = listOf(PlannerCourse(1, 90 + seed, monday.plusDays(5), 5, 0),
                    PlannerCourse(2, 75 + seed, monday.plusDays(12), 2, 0)),
                weeklyGoalMinutes = 180, weeklyScheduling = WeeklySchedulingPolicy(mapOf(1 to 150, 2 to 150)),
                breakMinutes = seed % 31)
            val plan = buildStudyPlan(input)
            assertEquals("Seed $seed", plan, buildStudyPlan(input))
            assertTrue("Feasible case $seed: ${plan.shortfalls}", plan.shortfalls.isEmpty())
            input.courses.forEach { course ->
                assertEquals(course.remainingMinutes, plan.blocks.filter { it.bossId == course.bossId }.sumOf { it.minutes } + (plan.shortfalls[course.bossId] ?: 0))
            }
            plan.blocks.groupBy { it.date }.values.forEach { blocks ->
                assertTrue(blocks.sortedBy { it.startMinute }.zipWithNext().all { (a, b) -> a.startMinute!! + a.minutes + input.breakMinutes <= b.startMinute!! })
            }
            assertTrue(plan.blocks.groupBy { plannerWeekStart(it.date) }.values.all { blocks -> blocks.sumOf { it.minutes } <= 180 })
        }
    }

    @Test fun `capacity reports exclude break overhead and day postponement keeps spacing and credit`() {
        val input = config()
        assertEquals(110, analyzePlannerTimeBudget(input).availableMinutes)
        assertEquals(110, recommendWeeklyHours(input, emptyList(), monday).minutesByCourse[1])
        val twoDays = input.copy(courses = listOf(input.courses.single().copy(finishBy = monday.plusDays(1))))
        val source = StudyPlan(twoDays, listOf(StudyBlock(4, 1, monday, 30, 540), StudyBlock(8, 1, monday.plusDays(1), 30, 540)))
        val moved = postponeTodayStudy(source, bosses, monday).plan!!
        assertEquals(580, moved.blocks.first().startMinute)
        assertEquals(monday.plusDays(1), moved.blocks.first().date)
        assertEquals(source.config, moved.config)
        val recorded = bosses.map { it.copy(timeSpentSeconds = 15) }
        val added = previewAddStudySession(source, recorded, monday, 1, monday, 580, 15).plan!!
        assertEquals(blockProgress(source, recorded).map { it.creditedSeconds }, blockProgress(added, recorded).take(2).map { it.creditedSeconds })
        assertEquals(0L, blockProgress(added, recorded).last().creditedSeconds)
    }

    @Test fun `extra course boundaries carry leftover work to a later valid day`() {
        val input = config(end = 600, effort = 30, length = 60).copy(courses = listOf(
            PlannerCourse(1, 30, monday.plusDays(1), 5, 0), PlannerCourse(2, 30, monday.plusDays(1), 5, 0)))
        val plan = buildStudyPlan(input)
        assertTrue(plan.shortfalls.isEmpty())
        assertEquals(60, plan.blocks.sumOf { it.minutes })
        assertTrue(plan.blocks.all { it.date <= monday.plusDays(1) })
    }

    @Test fun `weekly course limit remains a real shortage even with spare clock time`() {
        val input = config(end = 720, effort = 121).copy(weeklyGoalMinutes = 180,
            weeklyScheduling = WeeklySchedulingPolicy(mapOf(1 to 120)))
        val plan = buildStudyPlan(input)
        assertEquals(120, plan.blocks.sumOf { it.minutes })
        assertEquals(mapOf(1 to 1), plan.shortfalls)
    }
}
