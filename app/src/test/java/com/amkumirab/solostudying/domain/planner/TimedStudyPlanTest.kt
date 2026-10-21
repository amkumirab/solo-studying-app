package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TimedStudyPlanTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private fun windows(vararg slots: StudyTimeWindow) = WeeklyAvailability(List(7) { StudyDayAvailability(true, slots.toList()) })
    private fun config(minutes: Int = 91, availability: WeeklyAvailability = windows(StudyTimeWindow(1020, 1050), StudyTimeWindow(1140, 1200))) =
        PlannerConfig(monday, listOf(PlannerCourse(1, minutes, monday.plusDays(6), 5, 120)), availability.weekdayMinutes, 45,
            studyWindows = availability)

    @Test fun `generated times fit separate windows with exact duration and deterministic ids`() {
        val input = config()
        val plan = buildStudyPlan(input)
        assertEquals(91, plan.blocks.sumOf { it.minutes })
        assertEquals(plan, buildStudyPlan(input))
        assertEquals(plan.blocks.indices.map { it + 1 }, plan.blocks.map { it.id })
        assertTrue(plan.blocks.all { b -> input.studyWindows!!.days[b.date.dayOfWeek.value - 1].windows.any { b.startMinute!! >= it.startMinute && b.startMinute + b.minutes <= it.endMinute } })
        plan.blocks.groupBy { it.date }.values.forEach { blocks -> assertTrue(blocks.sortedBy { it.startMinute }.zipWithNext().all { (a, b) -> a.startMinute!! + a.minutes <= b.startMinute!! }) }
    }

    @Test fun `boundary splitting conserves a 45 minute block in two half hour windows`() {
        val plan = buildStudyPlan(config(45, windows(StudyTimeWindow(1020, 1050), StudyTimeWindow(1140, 1170))).copy(courses = listOf(PlannerCourse(1, 45, monday, 5, 120))))
        assertEquals(listOf(30, 15), plan.blocks.map { it.minutes })
        assertEquals(listOf(1020, 1140), plan.blocks.map { it.startMinute })
        assertEquals(45, plan.blocks.sumOf { it.minutes })
    }

    @Test fun `minute precision and midnight endpoint are retained`() {
        val plan = buildStudyPlan(config(31, windows(StudyTimeWindow(1380, 1440))).copy(sessionMinutes = 25,
            courses = listOf(PlannerCourse(1, 31, monday, 5, 120))))
        assertEquals(listOf(25, 6), plan.blocks.map { it.minutes })
        assertEquals(listOf(1380, 1405), plan.blocks.map { it.startMinute })
        assertEquals("24:00", plannerClockLabel(1440))
        assertEquals(1051, parsePlannerClock("17:31"))
        assertNull(parsePlannerClock("24:00"))
        assertNull(parsePlannerClock("17:60"))
    }

    @Test fun `validation rejects mixed untimed overlapping and outside window blocks`() {
        val input = config()
        listOf(listOf(StudyBlock(1, 1, monday, 25)),
            listOf(StudyBlock(1, 1, monday, 25, 1020), StudyBlock(2, 1, monday, 20, 1040)),
            listOf(StudyBlock(1, 1, monday, 45, 1020))).forEach { blocks ->
            assertTrue(runCatching { StudyPlan(input, blocks) }.isFailure)
        }
        assertTrue(runCatching { StudyPlan(input.copy(studyWindows = null), listOf(StudyBlock(1, 1, monday, 25, 1020))) }.isFailure)
    }

    @Test fun `date moves find a free complete slot without changing credit ids`() {
        val input = config(90, windows(StudyTimeWindow(1020, 1080)))
        val plan = StudyPlan(input, listOf(StudyBlock(7, 1, monday, 30, 1020), StudyBlock(19, 1, monday.plusDays(1), 30, 1020)))
        val moved = moveStudyBlock(plan, 7, monday.plusDays(1), monday, 0)!!
        assertEquals(1050, moved.blocks.first().startMinute)
        assertEquals(listOf(7, 19), moved.blocks.map { it.id })
        assertEquals(plan.config, moved.config)
    }

    @Test fun `postponement respects occupied slots and preserves original order and partial credit`() {
        val input = config(90, windows(StudyTimeWindow(1020, 1080)))
        val plan = StudyPlan(input, listOf(StudyBlock(7, 1, monday, 30, 1020), StudyBlock(19, 1, monday.plusDays(1), 30, 1020)))
        val bosses = listOf(com.amkumirab.solostudying.data.entity.BossEntity(id = 1, name = "Physics", difficulty = "Hard", requiredMinutes = 90, timeSpentSeconds = 180))
        val result = postponeTodayStudy(plan, bosses, monday).plan!!
        assertEquals(1050, result.blocks.first().startMinute)
        assertEquals(blockProgress(plan, bosses).map { it.creditedSeconds }, blockProgress(result, bosses).map { it.creditedSeconds })
    }

    @Test fun `daily budget helpers reject exceeding frozen windows without throwing`() {
        val input = config(120, windows(StudyTimeWindow(1020, 1050))).copy(courses = listOf(PlannerCourse(1, 120, monday, 3, 0)))
        assertNull(adjustPlannerDailyTime(input, 15))
        assertNull(requiredPlannerDailyTime(input))
        assertEquals(List(7) { 15 }, adjustPlannerDailyTime(input, -15)!!.weekdayMinutes)
    }

    @Test fun `generated weighted timed plans retain every minute and all capacity bounds`() {
        val random = kotlin.random.Random(701)
        repeat(100) {
            val availability = WeeklyAvailability(List(7) {
                StudyDayAvailability(true, listOf(StudyTimeWindow(600, 630), StudyTimeWindow(1020, 1110)))
            })
            val courses = (1..random.nextInt(1, 9)).map { id -> PlannerCourse(id, random.nextInt(1, 1500), monday.plusDays(random.nextLong(0, 60)), random.nextInt(1, 6), 0) }
            val input = PlannerConfig(monday, courses, List(7) { random.nextInt(1, 121) }, listOf(15, 25, 45, 90).random(random), random.nextInt(0, 841), studyWindows = availability)
                .withWeeklyScheduling(courses.filter { random.nextBoolean() }.map { PlannerSubject(it.bossId, "Subject ${it.bossId}", preferredWeeklyMinutes = random.nextInt(1, 401)) })
            val plan = buildStudyPlan(input)
            val untimed = buildStudyPlan(input.copy(studyWindows = null))
            assertEquals(untimed.shortfalls, plan.shortfalls)
            assertEquals(untimed.blocks.sumOf { it.minutes }, plan.blocks.sumOf { it.minutes })
            assertEquals(plan, buildStudyPlan(input))
            assertTrue(plan.blocks.all { it.startMinute != null })
        }
    }

    @Test fun `maximum horizon and course count fit the storage block bound`() {
        val availability = windows(*(0 until 16).map { StudyTimeWindow(it * 60, it * 60 + 30) }.toTypedArray())
        val input = PlannerConfig(monday, (1..20).map { PlannerCourse(it, 60000, monday.plusDays(365), it % 5 + 1, 0) },
            List(7) { 480 }, 25, 3360, studyWindows = availability).withWeeklyScheduling(emptyList())
        val plan = buildStudyPlan(input)
        assertEquals(366 * 480, plan.blocks.sumOf { it.minutes })
        assertTrue(plan.blocks.size <= 18000)
        assertEquals(input.courses.sumOf { it.remainingMinutes }, plan.blocks.sumOf { it.minutes } + plan.shortfalls.values.sum())
    }
}
