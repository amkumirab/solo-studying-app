package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PlannerTimeBudgetTest {
    private val monday = LocalDate.of(2026, 10, 5)
    private fun course(id: Int, minutes: Int, days: Long) = PlannerCourse(id, minutes, monday.plusDays(days), 2, 0)
    private fun config(courses: List<PlannerCourse>, budgets: List<Int> = List(7) { 60 }) =
        PlannerConfig(monday, courses, budgets, 25)

    @Test fun `one hundred hours over forty study days needs one hundred fifty minutes daily`() {
        val input = config(listOf(course(1, 6000, 55)), listOf(150, 150, 150, 150, 150, 0, 0))
        val report = analyzePlannerTimeBudget(input)
        assertEquals(40, report.studyDays)
        assertEquals(6000, report.requiredMinutes)
        assertEquals(150, report.requiredDailyMinutes)
        assertEquals(0, report.balanceMinutes)
        assertEquals(600, analyzePlannerTimeBudget(adjustPlannerDailyTime(input, 15)!!).balanceMinutes)
        assertEquals(-600, analyzePlannerTimeBudget(adjustPlannerDailyTime(input, -15)!!).balanceMinutes)
    }

    @Test fun `early deadline shortage is visible even with a final surplus`() {
        val report = analyzePlannerTimeBudget(config(listOf(course(1, 120, 0), course(2, 60, 6))))
        assertEquals(240, report.balanceMinutes)
        assertEquals(120, report.requiredDailyMinutes)
        assertEquals(-60, report.deadlines.first().balanceMinutes)
        assertFalse(report.fitsAllDeadlines)
    }

    @Test fun `courses share capacity and identical deadlines combine effort`() {
        val report = analyzePlannerTimeBudget(config(listOf(course(1, 61, 1), course(2, 60, 1))))
        assertEquals(61, report.requiredDailyMinutes)
        assertEquals(1, report.deadlines.size)
        assertEquals(listOf(1, 2), report.deadlines.single().courseIds)
        assertEquals(-1, report.balanceMinutes)
    }

    @Test fun `deadline with no study days has no valid uniform recommendation`() {
        val input = config(listOf(course(1, 60, 0), course(2, 60, 6)), listOf(0, 120, 0, 0, 0, 0, 0))
        val report = analyzePlannerTimeBudget(input)
        assertNull(report.requiredDailyMinutes)
        assertEquals(0, report.deadlines.first().studyDays)
        assertFalse(report.fitsAllDeadlines)
        assertNull(requiredPlannerDailyTime(input))
    }

    @Test fun `same day deadline includes today and uneven budgets stay exact`() {
        val input = config(listOf(course(1, 61, 0), course(2, 60, 2)), listOf(61, 0, 100, 0, 0, 0, 0))
        val report = analyzePlannerTimeBudget(input)
        assertEquals(2, report.studyDays)
        assertEquals(61, report.requiredDailyMinutes)
        assertEquals(40, report.balanceMinutes)
        assertTrue(report.fitsAllDeadlines)
        assertEquals(listOf(61, 0, 61, 0, 0, 0, 0), requiredPlannerDailyTime(input)!!.weekdayMinutes)
    }

    @Test fun `adjustment preserves rest days and does not clamp unequal budgets`() {
        val input = config(listOf(course(1, 60, 6)), listOf(60, 0, 120, 0, 30, 0, 480))
        assertNull(adjustPlannerDailyTime(input, 15))
        assertNull(adjustPlannerDailyTime(input, -30))
        val adjusted = adjustPlannerDailyTime(input, -15)!!
        assertEquals(listOf(45, 0, 105, 0, 15, 0, 465), adjusted.weekdayMinutes)
        assertEquals(input.courses, adjusted.courses)
        assertEquals(480, input.weekdayMinutes.last())
    }

    @Test fun `only exact fifteen minute steps within bounds are accepted`() {
        val input = config(listOf(course(1, 60, 6)), List(7) { 15 })
        assertNull(adjustPlannerDailyTime(input, -15))
        assertNull(adjustPlannerDailyTime(input, 1))
        assertNull(adjustPlannerDailyTime(input, Int.MAX_VALUE))
        assertEquals(input, adjustPlannerDailyTime(input, 0))
        assertEquals(List(7) { 480 }, adjustPlannerDailyTime(input, 465)!!.weekdayMinutes)
    }

    @Test fun `required budget above daily limit is reported without silently capping`() {
        val input = config(listOf(course(1, 60000, 0)))
        assertEquals(60000, analyzePlannerTimeBudget(input).requiredDailyMinutes)
        assertNull(requiredPlannerDailyTime(input))
    }

    @Test fun `maximum horizon and course effort do not overflow`() {
        val report = analyzePlannerTimeBudget(config((1..20).map { course(it, 60000, 365) }, List(7) { 480 }))
        assertEquals(1200000, report.requiredMinutes)
        assertEquals(366 * 480, report.availableMinutes)
        assertEquals(3279, report.requiredDailyMinutes)
    }

    @Test fun `uniform recommendation is feasible and one fewer minute misses a deadline`() {
        val random = Random(51)
        repeat(200) {
            val courses = (1..random.nextInt(2, 7)).map { course(it, random.nextInt(1, 180), random.nextLong(0, 21)) }
            val input = config(courses, List(7) { if (random.nextBoolean()) 60 else 0 }.let {
                if (it.all { minutes -> minutes == 0 }) List(7) { 60 } else it
            })
            val recommendation = requiredPlannerDailyTime(input) ?: return@repeat
            assertTrue(buildStudyPlan(recommendation).shortfalls.isEmpty())
            val required = analyzePlannerTimeBudget(input).requiredDailyMinutes!!
            if (required > 1) {
                val reduced = recommendation.copy(weekdayMinutes = recommendation.weekdayMinutes.map { if (it == 0) 0 else it - 1 })
                assertFalse(buildStudyPlan(reduced).shortfalls.isEmpty())
            }
        }
    }
}
