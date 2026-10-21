package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class WeightedWeeklyPlanTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private fun course(id: Int, minutes: Int = 600, days: Long = 41, priority: Int = 3) =
        PlannerCourse(id, minutes, monday.plusDays(days), priority, 120)
    private fun config(courses: List<PlannerCourse>, goal: Int = 240, days: List<Int> = List(7) { 60 }) =
        PlannerConfig(monday, courses, days, 30, goal).withWeeklyScheduling(emptyList())
    private fun week(date: LocalDate) = date.minusDays(date.dayOfWeek.value - 1L)

    @Test fun `weekly shares become exact actual sessions and spare capacity stays free`() {
        val input = config(listOf(course(1, priority = 1), course(2, priority = 5)))
        val plan = buildStudyPlan(input)
        val first = plan.blocks.filter { it.date < monday.plusWeeks(1) }.groupBy { it.bossId }.mapValues { it.value.sumOf { b -> b.minutes } }
        assertEquals(mapOf(1 to 40, 2 to 200), first)
        assertTrue(plan.blocks.groupBy { week(it.date) }.all { it.value.sumOf { b -> b.minutes } <= 240 })
        assertTrue(plan.blocks.filter { week(it.date) == monday }.map { it.date }.distinct().size > 1)
        assertEquals(plan, buildStudyPlan(input))
        assertEquals(plan.blocks.indices.map { it + 1 }, plan.blocks.map { it.id })
    }

    @Test fun `due work is reserved before discretionary weighted shares`() {
        val plan = buildStudyPlan(config(listOf(course(1, 90, 0, 1), course(2, 1000, 20, 5)), days = List(7) { 120 }))
        assertEquals(90, plan.blocks.filter { it.bossId == 1 && it.date == monday }.sumOf { it.minutes })
        assertFalse(plan.shortfalls.containsKey(1))
    }

    @Test fun `future shared capacity reserves work this week to protect next weeks deadline`() {
        val plan = buildStudyPlan(config(listOf(course(1, 180, 8, 1), course(2, 180, 8, 1), course(3, 600, 27, 5))))
        assertFalse(plan.shortfalls.containsKey(1))
        assertFalse(plan.shortfalls.containsKey(2))
        assertEquals(240, plan.blocks.filter { it.date < monday.plusWeeks(1) && it.bossId in 1..2 }.sumOf { it.minutes })
    }

    @Test fun `preferred ceilings persist for each week and fractional effort is conserved`() {
        val input = config(listOf(course(1, 91), course(2, 400))).withWeeklyScheduling(listOf(PlannerSubject(1, "Physics", preferredWeeklyMinutes = 31)))
        val plan = buildStudyPlan(input)
        assertTrue(plan.blocks.filter { it.bossId == 1 }.groupBy { week(it.date) }.all { it.value.sumOf { b -> b.minutes } <= 31 })
        assertEquals(91, plan.blocks.filter { it.bossId == 1 }.sumOf { it.minutes })
        assertTrue(plan.shortfalls.isEmpty())
        assertTrue(plan.blocks.all { it.minutes in 1..30 })
    }

    @Test fun `zero goal creates an honest empty plan and partial weeks use only available days`() {
        val empty = buildStudyPlan(config(listOf(course(1, 61)), goal = 0))
        assertTrue(empty.blocks.isEmpty())
        assertEquals(mapOf(1 to 61), empty.shortfalls)
        val start = monday.plusDays(5)
        val partial = buildStudyPlan(config(listOf(course(1, 61, 6)), goal = 200).copy(start = start))
        assertEquals(61, partial.blocks.sumOf { it.minutes })
        assertTrue(partial.blocks.all { it.date >= start })
    }

    @Test fun `earlier deadline is protected when later work is infeasible`() {
        val plan = buildStudyPlan(config(listOf(course(1, 180, 8, 1), course(2, 180, 8, 1), course(3, 1000, 20, 5))))
        assertFalse(plan.shortfalls.containsKey(1))
        assertFalse(plan.shortfalls.containsKey(2))
        assertTrue(plan.shortfalls.containsKey(3))
    }

    @Test fun `moving a block respects both the destination weeks goal and its course limit`() {
        val input = config(listOf(course(1), course(2)), goal = 60, days = List(7) { 120 }).copy(sessionMinutes = 60)
        val plan = StudyPlan(input, listOf(StudyBlock(7, 1, monday, 30), StudyBlock(4, 2, monday.plusWeeks(1), 60)))
        assertNull(moveStudyBlock(plan, 7, monday.plusWeeks(1).plusDays(1), monday, 0))
        assertNotNull(moveStudyBlock(plan, 7, monday.plusDays(1), monday, 0))
        val capped = input.withWeeklyScheduling(listOf(PlannerSubject(1, "Physics", preferredWeeklyMinutes = 30)))
        val byCourse = StudyPlan(capped, listOf(StudyBlock(7, 1, monday, 30), StudyBlock(4, 1, monday.plusWeeks(1), 30)))
        assertNull(moveStudyBlock(byCourse, 7, monday.plusWeeks(1).plusDays(1), monday, 0))
    }

    @Test fun `postponement skips full weeks without losing credit and same-week moves do not double count`() {
        val input = config(listOf(course(1), course(2)), goal = 60).copy(sessionMinutes = 60)
        val sunday = monday.plusDays(6)
        val plan = StudyPlan(input, listOf(StudyBlock(7, 1, sunday, 30), StudyBlock(4, 2, monday.plusWeeks(1), 60)))
        val bosses = listOf(com.amkumirab.solostudying.data.entity.BossEntity(id = 1, name = "Physics", difficulty = "Hard", requiredMinutes = 600, timeSpentSeconds = 130))
        val postponed = postponeTodayStudy(plan, bosses, sunday)
        assertEquals(monday.plusWeeks(2), postponed.moves.single().date)
        assertEquals(blockProgress(plan, bosses).map { it.creditedSeconds }, blockProgress(postponed.plan!!, bosses).map { it.creditedSeconds })
        val one = StudyPlan(input.copy(weeklyGoalMinutes = 30), listOf(StudyBlock(7, 1, monday, 30)))
        assertEquals(monday.plusDays(1), postponeTodayStudy(one, bosses, monday).moves.single().date)
    }

    @Test fun `a hundred hour course finishes within two months when weekly capacity permits`() {
        val plan = buildStudyPlan(config(listOf(course(1, 6000, 59)), goal = 750, days = listOf(150, 150, 150, 150, 150, 0, 0)))
        assertEquals(6000, plan.blocks.sumOf { it.minutes })
        assertTrue(plan.shortfalls.isEmpty())
        assertTrue(plan.blocks.groupBy { week(it.date) }.all { it.value.sumOf { b -> b.minutes } <= 750 })
    }

    @Test fun `generated schedules conserve every minute and respect all saved constraints`() {
        val random = kotlin.random.Random(619)
        repeat(150) { scenario ->
            val courses = (1..random.nextInt(1, 9)).map { id -> course(id, random.nextInt(1, 3000), random.nextLong(0, 70), random.nextInt(1, 6)) }
            val days = List(7) { random.nextInt(0, 121) }.let { if (it.all { n -> n == 0 }) List(7) { 60 } else it }
            val subjects = courses.filter { random.nextBoolean() }.map { PlannerSubject(it.bossId, "Course ${it.bossId}", preferredWeeklyMinutes = random.nextInt(1, 601)) }
            val input = config(courses, random.nextInt(0, 841), days).copy(start = monday.plusDays(random.nextLong(0, courses.minOf { java.time.temporal.ChronoUnit.DAYS.between(monday, it.finishBy) } + 1)))
                .withWeeklyScheduling(subjects)
            val plan = buildStudyPlan(input)
            assertEquals("scenario $scenario", plan, buildStudyPlan(input))
            assertEquals(plan.blocks, buildStudyPlan(input.copy(courses = courses.reversed())).blocks)
            courses.forEach { c -> assertEquals(c.remainingMinutes, plan.blocks.filter { it.bossId == c.bossId }.sumOf { it.minutes } + (plan.shortfalls[c.bossId] ?: 0)) }
            plan.blocks.groupBy { week(it.date) }.forEach { (_, values) ->
                assertTrue(values.sumOf { it.minutes } <= input.weeklyGoalMinutes!!)
                values.groupBy { it.bossId }.forEach { (id, blocks) -> assertTrue(blocks.sumOf { it.minutes } <= (input.weeklyScheduling!!.courseMinuteLimits[id] ?: Int.MAX_VALUE)) }
            }
            assertTrue(plan.blocks.all { it.minutes in 1..input.sessionMinutes && it.date >= input.start })
            assertTrue(plan.blocks.groupBy { it.date }.all { (date, blocks) -> blocks.sumOf { it.minutes } <= input.capacity(date) })
        }
    }
}
