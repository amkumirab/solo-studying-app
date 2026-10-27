package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class StudySessionReassignmentTest {
    private val today = LocalDate.of(2026, 10, 27)
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
    private val config = PlannerConfig(today, listOf(
        PlannerCourse(1, 180, today.plusDays(13), 5, 120),
        PlannerCourse(2, 180, today.plusDays(13), 3, 60),
    ), List(7) { 120 }, 30, studyWindows = windows)
    private val goals = listOf(
        BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 182, timeSpentSeconds = 120),
        BossEntity(id = 2, name = "Physics", difficulty = "Hard", requiredMinutes = 181, timeSpentSeconds = 60),
    )
    private fun plan() = StudyPlan(config, listOf(
        StudyBlock(2, 1, today, 30, 1020, durationEdited = true),
        StudyBlock(7, 2, today.plusDays(1), 30, 1020, locked = true),
        StudyBlock(4, 1, today.plusDays(2), 30, 1020, manual = true),
    ))
    private fun change(plan: StudyPlan = plan(), bosses: List<BossEntity> = goals, date: LocalDate = today, id: Int = 2, target: Int = 2) =
        previewReassignStudySession(plan, bosses, date, id, target)

    @Test fun `reassignment keeps the exact slot and returns only planned minutes to the source`() {
        val original = plan()
        val result = change()
        assertNull(result.error)
        val changed = result.plan!!
        assertEquals(original.config, changed.config)
        assertEquals(original.blocks.drop(1), changed.blocks.dropLast(1))
        assertEquals(original.blocks.first().copy(id = 8, bossId = 2, manual = true), changed.blocks.last())
        assertEquals(150, changed.shortfalls[1])
        assertEquals(120, changed.shortfalls[2])
        assertEquals(original.blocks.sumOf { it.minutes }, changed.blocks.sumOf { it.minutes })
        assertEquals(original, plan())
    }

    @Test fun `new credit ID protects destination history even when source ID came first`() {
        val recorded = goals.map { if (it.id == 2) it.copy(timeSpentSeconds = 61) else it }
        val changed = change(bosses = recorded).plan!!
        val before = blockProgress(plan(), recorded).associate { it.block.id to it.creditedSeconds }
        val after = blockProgress(changed, recorded).associate { it.block.id to it.creditedSeconds }
        assertEquals(1L, after[7])
        assertEquals(0L, after[8])
        assertEquals(before.filterKeys { it != 2 }, after.filterKeys { it != 8 })
        val excess = goals.map { if (it.id == 2) it.copy(timeSpentSeconds = 60 + 1801) else it }
        assertNull(change(bosses = excess).plan)
        assertTrue(change(bosses = excess).error!!.contains("recorded study credit"))
    }

    @Test fun `studied locked past missing completed and date only sessions are rejected`() {
        assertNull(change(id = 99).plan)
        assertNull(change(target = 99).plan)
        assertNull(change(date = today.plusDays(1)).plan)
        assertNull(change(plan = plan().copy(blocks = plan().blocks.map { it.copy(locked = true) })).plan)
        assertNull(change(bosses = goals.map { if (it.id == 1) it.copy(timeSpentSeconds = 121) else it }).plan)
        goals.forEach { goal ->
            assertNull(change(bosses = goals.filterNot { it.id == goal.id }).plan)
            assertNull(change(bosses = goals.map { if (it.id == goal.id) it.copy(isCompleted = true) else it }).plan)
        }
        val dated = StudyPlan(config.copy(studyWindows = null), listOf(StudyBlock(2, 1, today, 30)))
        assertNull(change(plan = dated).plan)
        assertEquals(plan(), change(target = 1).plan)
    }

    @Test fun `destination deadline weekly limit and unscheduled work apply without moving others`() {
        val original = plan()
        val late = original.copy(blocks = listOf(original.blocks.first().copy(date = today.plusDays(2))))
        val expired = late.copy(config = config.copy(courses = config.courses.map { if (it.bossId == 2) it.copy(finishBy = today.plusDays(1)) else it }))
        assertNull(change(plan = expired).plan)
        val capped = original.copy(config = config.copy(weeklyGoalMinutes = 120, weeklyScheduling = WeeklySchedulingPolicy(mapOf(2 to 30))))
        assertNull(change(plan = capped).plan)
        assertTrue(change(plan = capped).error!!.contains("weekly limit"))
        val noWork = original.copy(config = config.copy(courses = config.courses.map { if (it.bossId == 2) it.copy(remainingMinutes = 59) else it }))
        assertNull(change(plan = noWork).plan)
        assertTrue(change(plan = noWork).error!!.contains("unscheduled work"))
        val noIds = original.copy(blocks = original.blocks.map { if (it.id == 7) it.copy(id = Int.MAX_VALUE) else it })
        assertNull(change(plan = noIds).plan)
    }

    @Test fun `all supported durations including midnight preserve accounting and neighboring slots`() {
        val midnight = config.copy(studyWindows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1320, 1440))) }))
        repeat(120) { index ->
            val minutes = index + 1
            val original = StudyPlan(midnight, listOf(StudyBlock(1, 1, today, minutes, 1440 - minutes, manual = true)))
            val changed = change(plan = original, id = 1).plan!!
            assertEquals(original.blocks.single().copy(id = 2, bossId = 2), changed.blocks.single())
            config.courses.forEach { course ->
                assertEquals(course.remainingMinutes, changed.blocks.filter { it.bossId == course.bossId }.sumOf { it.minutes } + (changed.shortfalls[course.bossId] ?: 0))
            }
            assertTrue(blockProgress(changed, goals).all { it.creditedSeconds == 0L })
        }
    }

    @Test fun `replacement works at full ledger capacity without dropping any other slot`() {
        val large = config.copy(courses = config.courses.map { it.copy(remainingMinutes = 18001, finishBy = today.plusDays(180)) })
        val blocks = List(18000) { index -> StudyBlock(index + 1, 1, today.plusDays((index / 120).toLong()), 1, 1020 + index % 120) }
        val original = StudyPlan(large, blocks)
        val changed = change(plan = original, id = 1).plan!!
        assertEquals(18000, changed.blocks.size)
        assertEquals(blocks.drop(1), changed.blocks.dropLast(1))
        assertEquals(18001, changed.blocks.last().id)
    }
}
