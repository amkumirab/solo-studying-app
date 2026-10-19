package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class WeeklyStudyAllocationTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private fun config(courses: List<PlannerCourse>, capacity: List<Int> = listOf(60, 60, 60, 60, 60, 0, 0), goal: Int = 240) =
        PlannerConfig(monday, courses, capacity, 30, goal)
    private fun course(id: Int, priority: Int = 3, deadline: LocalDate = monday.plusDays(6)) =
        PlannerCourse(id, 600, deadline, priority, 0)

    @Test fun `higher priority receives more of a feasible weekly target`() {
        val allocation = recommendWeeklyHours(config(listOf(course(1, 1), course(2, 5))), emptyList(), monday)
        assertEquals(300, allocation.availableMinutes)
        assertEquals(240, allocation.allocatedMinutes)
        assertEquals(mapOf(1 to 40, 2 to 200), allocation.minutesByCourse)
        assertEquals(0, allocation.unallocatedMinutes)
    }

    @Test fun `nearer deadline increases weight but cannot exceed predeadline capacity`() {
        val input = config(listOf(course(1, deadline = monday.plusDays(1)), course(2)), goal = 180)
        val allocation = recommendWeeklyHours(input, emptyList(), monday)
        assertTrue(allocation.minutesByCourse.getValue(1) > allocation.minutesByCourse.getValue(2))
        assertTrue(allocation.minutesByCourse.getValue(1) <= 120)
        assertEquals(180, allocation.allocatedMinutes)
    }

    @Test fun `preferred weekly hours cap one subject and release the rest`() {
        val input = config(listOf(course(1, 5), course(2, 1)))
        val subjects = listOf(PlannerSubject(1, "Physics", preferredWeeklyMinutes = 60))
        val allocation = recommendWeeklyHours(input, subjects, monday)
        assertEquals(mapOf(1 to 60, 2 to 180), allocation.minutesByCourse)
    }

    @Test fun `capacity and subject limits expose remaining target without rounding loss`() {
        val constrained = recommendWeeklyHours(config(listOf(course(1)), goal = 420), emptyList(), monday)
        assertEquals(300, constrained.allocatedMinutes)
        assertEquals(120, constrained.unallocatedMinutes)
        val limited = recommendWeeklyHours(config(listOf(course(1)), goal = 240),
            listOf(PlannerSubject(1, "Physics", preferredWeeklyMinutes = 90)), monday)
        assertEquals(90, limited.allocatedMinutes)
        assertEquals(150, limited.unallocatedMinutes)
    }

    @Test fun `zero goal and equal-weight ties have stable exact results`() {
        val courses = listOf(course(3), course(1), course(2))
        assertEquals(0, recommendWeeklyHours(config(courses, goal = 0), emptyList(), monday).allocatedMinutes)
        val input = config(courses, goal = 61)
        val first = recommendWeeklyHours(input, emptyList(), monday)
        assertEquals(mapOf(1 to 21, 2 to 20, 3 to 20), first.minutesByCourse)
        assertEquals(first, recommendWeeklyHours(input, emptyList(), monday))
    }

    @Test fun `invalid weekly goals are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { config(listOf(course(1)), goal = -1) }
        assertThrows(IllegalArgumentException::class.java) { config(listOf(course(1)), goal = 3361) }
    }

    @Test fun `shared early deadline cannot spend capacity from later days`() {
        val input = config(listOf(course(1, deadline = monday), course(2, deadline = monday), course(3)), goal = 240)
        val allocation = recommendWeeklyHours(input, emptyList(), monday)
        assertEquals(60, allocation.minutesByCourse.getValue(1) + allocation.minutesByCourse.getValue(2))
        assertEquals(180, allocation.minutesByCourse.getValue(3))
        assertEquals(240, allocation.allocatedMinutes)
    }

    @Test fun `partial first week excludes past days and remaining effort bounds recommendations`() {
        val input = config(listOf(course(1).copy(remainingMinutes = 91)), goal = 240).copy(start = monday.plusDays(2))
        val allocation = recommendWeeklyHours(input, emptyList(), monday)
        assertEquals(180, allocation.availableMinutes)
        assertEquals(91, allocation.allocatedMinutes)
        assertEquals(149, allocation.unallocatedMinutes)
    }

    @Test fun `generated recommendations conserve totals and respect every deadline prefix`() {
        val random = kotlin.random.Random(28)
        repeat(100) {
            val capacity = List(7) { random.nextInt(0, 121) }
            if (capacity.all { it == 0 }) return@repeat
            val courses = (1..random.nextInt(2, 8)).map { id ->
                course(id, random.nextInt(1, 6), monday.plusDays(random.nextLong(0, 14))).copy(remainingMinutes = random.nextInt(1, 601))
            }
            val goal = random.nextInt(0, 901)
            val input = config(courses, capacity, goal)
            val allocation = recommendWeeklyHours(input, emptyList(), monday)
            assertEquals(goal, allocation.allocatedMinutes + allocation.unallocatedMinutes)
            assertTrue(allocation.allocatedMinutes <= minOf(goal, capacity.sum()))
            courses.forEach { assertTrue((allocation.minutesByCourse[it.bossId] ?: 0) in 0..it.remainingMinutes) }
            (0..6).forEach { index ->
                val due = courses.filter { it.finishBy <= monday.plusDays(index.toLong()) }.sumOf { allocation.minutesByCourse[it.bossId] ?: 0 }
                assertTrue(due <= capacity.take(index + 1).sum())
            }
            assertEquals(allocation, recommendWeeklyHours(input.copy(courses = courses.reversed()), emptyList(), monday))
        }
    }
}
