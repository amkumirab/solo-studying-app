package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class StudyPlannerTest {
    private val monday = LocalDate.of(2026, 10, 5)
    private fun course(id: Int = 1, minutes: Int = 120, days: Long = 6, priority: Int = 2) =
        PlannerCourse(id, minutes, monday.plusDays(days), priority, 0)
    private fun config(courses: List<PlannerCourse> = listOf(course()), capacity: List<Int> = List(7) { 60 }) =
        PlannerConfig(monday, courses, capacity, 25)

    @Test fun `plan conserves effort and respects budgets deadlines and rest days`() {
        val input = config(listOf(course(1, 130, 3), course(2, 180, 6)), listOf(60, 0, 90, 60, 0, 100, 0))
        val plan = buildStudyPlan(input)
        assertTrue(plan.shortfalls.isEmpty())
        assertEquals(310, plan.blocks.sumOf { it.minutes })
        plan.blocks.groupBy { it.date }.forEach { (date, blocks) ->
            assertTrue(blocks.sumOf { it.minutes } <= input.weekdayMinutes[date.dayOfWeek.value - 1])
        }
        assertTrue(plan.blocks.all { block -> block.date <= input.courses.first { it.bossId == block.bossId }.finishBy })
        assertTrue(plan.blocks.all { it.minutes in 1..25 })
    }

    @Test fun `small course is spread instead of filling the first day`() {
        val plan = buildStudyPlan(config())
        assertEquals(120, plan.blocks.sumOf { it.minutes })
        assertTrue(plan.blocks.map { it.date }.distinct().size > 1)
        assertEquals(plan, buildStudyPlan(config()))
    }

    @Test fun `spreading work prefers full focus blocks instead of many tiny sessions`() {
        val plan = buildStudyPlan(config())
        assertEquals(listOf(25, 25, 25, 25, 20), plan.blocks.map { it.minutes })
        assertEquals(5, plan.blocks.map { it.date }.distinct().size)
    }

    @Test fun `earlier deadline wins over priority and shortages are honest`() {
        val plan = buildStudyPlan(config(listOf(course(1, 100, 0, 1), course(2, 100, 1, 3))))
        assertEquals(1, plan.blocks.first().bossId)
        assertEquals(mapOf(1 to 40, 2 to 40), plan.shortfalls)
    }

    @Test fun `higher priority wins identical deadlines`() {
        val plan = buildStudyPlan(config(listOf(course(1, 60, 0, 1), course(2, 60, 0, 3))))
        assertEquals(2, plan.blocks.first().bossId)
        assertEquals(mapOf(1 to 60), plan.shortfalls)
    }

    @Test fun `recorded progress is allocated once and a partial block stays partial`() {
        val input = config(listOf(course(minutes = 60, days = 0)))
        val plan = buildStudyPlan(input)
        val boss = BossEntity(id = 1, name = "OS", difficulty = "Hard", requiredMinutes = 6000, timeSpentSeconds = 26 * 60 + 15)
        val progress = blockProgress(plan, listOf(boss))
        assertEquals(25 * 60L, progress[0].creditedSeconds)
        assertEquals(75L, progress[1].creditedSeconds)
        assertEquals(0L, progress[2].creditedSeconds)
        assertEquals(34, remainingPlannerCourses(plan, listOf(boss)).single().remainingMinutes)
        assertTrue(blockProgress(plan, listOf(boss.copy(isCompleted = true))).all { it.remainingSeconds == 0L })
        assertTrue(blockProgress(plan, emptyList()).all { it.missingCourse })
    }

    @Test fun `baseline excludes old study and negative progress never creates credit`() {
        val input = config(listOf(course(minutes = 60, days = 0).copy(baselineSeconds = 300)))
        val plan = buildStudyPlan(input)
        val boss = BossEntity(id = 1, name = "OS", difficulty = "Hard", requiredMinutes = 6000, timeSpentSeconds = 300)
        assertEquals(0L, blockProgress(plan, listOf(boss)).sumOf { it.creditedSeconds })
        assertEquals(0L, blockProgress(plan, listOf(boss.copy(timeSpentSeconds = 0))).sumOf { it.creditedSeconds })
    }

    @Test fun `move rejects rest days full days deadlines and studied blocks`() {
        val plan = buildStudyPlan(config(listOf(course(minutes = 80)), listOf(60, 0, 90, 0, 0, 0, 0)))
        val block = plan.blocks.first()
        assertNull(moveStudyBlock(plan, block.id, monday.plusDays(1), monday, 0))
        assertNull(moveStudyBlock(plan, block.id, monday.plusDays(7), monday, 0))
        assertNull(moveStudyBlock(plan, block.id, monday.plusDays(2), monday, 1))
        val moved = moveStudyBlock(plan, block.id, monday.plusDays(2), monday, 0)
        assertNotNull(moved)
        assertEquals(monday.plusDays(2), moved!!.blocks.first { it.id == block.id }.date)
        assertEquals(plan.blocks.sumOf { it.minutes }, moved.blocks.sumOf { it.minutes })
    }

    @Test fun `invalid inputs are bounded`() {
        listOf<() -> Unit>(
            { config(capacity = List(7) { 0 }) },
            { config(capacity = List(7) { 481 }) },
            { config(listOf(course(), course())) },
            { config(listOf(course(days = -1))) },
            { config(listOf(course(days = 366))) },
            { course(minutes = 0) },
            { config().copy(sessionMinutes = 0) },
        ).forEach { invalid -> assertThrows(IllegalArgumentException::class.java) { invalid() } }
    }

    @Test fun `five courses and one hundred hours are supported`() {
        val plan = buildStudyPlan(config((1..5).map { course(it, 1200, 59) }, List(7) { 120 }))
        assertTrue(plan.shortfalls.isEmpty())
        assertEquals(6000, plan.blocks.sumOf { it.minutes })
        assertEquals(5, plan.blocks.map { it.bossId }.distinct().size)
    }

    @Test fun `generated feasible deadline prefixes always remain feasible`() {
        val random = Random(42)
        repeat(500) {
            val capacity = List(7) { random.nextInt(0, 121) }
            if (capacity.all { it == 0 }) return@repeat
            val courses = (1..random.nextInt(2, 7)).map { course(it, random.nextInt(1, 301), random.nextLong(0, 14), random.nextInt(1, 4)) }
            val input = config(courses, capacity)
            val feasible = (0L..13L).all { day ->
                val due = courses.filter { it.finishBy <= monday.plusDays(day) }.sumOf { it.remainingMinutes }
                val available = (0L..day).sumOf { input.capacity(monday.plusDays(it)) }
                due <= available
            }
            val plan = buildStudyPlan(input)
            if (feasible) assertTrue("Feasible input had shortages: $input / ${plan.shortfalls}", plan.shortfalls.isEmpty())
            assertEquals(courses.sumOf { it.remainingMinutes }, plan.blocks.sumOf { it.minutes } + plan.shortfalls.values.sum())
        }
    }

    @Test fun `moving an untouched block does not reassign existing credit`() {
        val plan = buildStudyPlan(config(listOf(course(minutes = 60))))
        val boss = BossEntity(id = 1, name = "OS", difficulty = "Hard", requiredMinutes = 6000, timeSpentSeconds = 300)
        val before = blockProgress(plan, listOf(boss)).associate { it.block.id to it.creditedSeconds }
        val second = plan.blocks[1]
        val moved = moveStudyBlock(plan, second.id, monday, monday, 0)!!
        assertEquals(before, blockProgress(moved, listOf(boss)).associate { it.block.id to it.creditedSeconds })
    }

    @Test fun `moving into a full day is rejected`() {
        val plan = buildStudyPlan(config(listOf(course(minutes = 120, days = 1))))
        assertNull(moveStudyBlock(plan, plan.blocks.first().id, monday.plusDays(1), monday, 0))
    }

    @Test fun `hours accept decimals but reject overflow nonfinite and oversized values`() {
        assertEquals(6000, plannerHoursToMinutes("100"))
        assertEquals(90, plannerHoursToMinutes("1,5"))
        assertEquals(1, plannerHoursToMinutes("0.01"))
        assertEquals(60000, plannerHoursToMinutes("1000"))
        listOf("0", "-1", "NaN", "Infinity", "1e9", "1000.01", "1.234", "99999999999999999").forEach { assertNull(plannerHoursToMinutes(it)) }
    }

    @Test fun `maximum horizon remains bounded and never invents capacity`() {
        val input = config((1..20).map { course(it, 60000, 365) }, List(7) { 480 }).copy(sessionMinutes = 15)
        val plan = buildStudyPlan(input)
        assertTrue(plan.blocks.size <= 18000)
        assertEquals(366 * 480, plan.blocks.sumOf { it.minutes })
        assertEquals(20 * 60000 - 366 * 480, plan.shortfalls.values.sum())
    }
}
