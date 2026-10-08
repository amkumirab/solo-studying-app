package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TodayStudyPlannerTest {
    private val today = LocalDate.of(2026, 10, 8)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 6000)
    private fun plan(deadline: LocalDate = today.plusDays(3), capacity: Int = 60) = StudyPlan(
        PlannerConfig(today, listOf(PlannerCourse(1, 75, deadline, 2, 100)), List(7) { capacity }, 25),
        listOf(StudyBlock(1, 1, today, 25), StudyBlock(2, 1, today, 25), StudyBlock(3, 1, today.plusDays(1), 25)),
    )

    @Test fun `today shows exact credit and excludes future blocks`() {
        val result = todayStudyBlocks(plan(), listOf(boss.copy(timeSpentSeconds = 110)), today)
        assertEquals(listOf(1, 2), result.map { it.block.id })
        assertEquals(10L, result.first().creditedSeconds)
        assertEquals(1490L, result.first().remainingSeconds)
    }

    @Test fun `postponement preserves partial credit ids and existing future work`() {
        val original = plan()
        val goals = listOf(boss.copy(timeSpentSeconds = 110))
        val preview = postponeTodayStudy(original, goals, today)
        assertTrue(preview.unmovedMinutes.isEmpty())
        val moved = preview.plan!!
        assertEquals(original.config, moved.config)
        assertEquals(original.blocks.last(), moved.blocks.last())
        assertEquals(listOf(1, 2), preview.moves.map { it.id })
        assertTrue(preview.moves.all { it.date > today })
        assertEquals(blockProgress(original, goals).map { it.creditedSeconds }, blockProgress(moved, goals).map { it.creditedSeconds })
        assertTrue(moved.blocks.groupBy { it.date }.all { (date, blocks) -> blocks.sumOf { it.minutes } <= moved.config.capacity(date) })
        assertEquals(today, original.blocks.first().date)
    }

    @Test fun `insufficient capacity never applies a partial move`() {
        val preview = postponeTodayStudy(plan(today.plusDays(1), 50), listOf(boss), today)
        assertNull(preview.plan)
        assertEquals(25, preview.unmovedMinutes[1])
    }

    @Test fun `deadline today cannot move and leaves all work unchanged`() {
        val original = StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 25, today, 2, 0)), List(7) { 60 }, 25), listOf(StudyBlock(1, 1, today, 25)))
        val preview = postponeTodayStudy(original, listOf(boss), today)
        assertNull(preview.plan)
        assertEquals(25, preview.unmovedMinutes[1])
    }

    @Test fun `rest days are skipped and completed or missing courses never move`() {
        val original = plan().let { it.copy(config = it.config.copy(weekdayMinutes = listOf(60, 60, 60, 60, 0, 60, 60)), blocks = it.blocks.dropLast(1)) }
        val preview = postponeTodayStudy(original, listOf(boss), today)
        assertTrue(preview.moves.all { it.date == today.plusDays(2) })
        assertTrue(postponeTodayStudy(original, listOf(boss.copy(isCompleted = true)), today).moves.isEmpty())
        assertTrue(postponeTodayStudy(original, emptyList(), today).moves.isEmpty())
    }

    @Test fun `earlier deadline takes scarce next day capacity before a later course`() {
        val other = boss.copy(id = 2, name = "Physics")
        val original = StudyPlan(PlannerConfig(today, listOf(
            PlannerCourse(1, 25, today.plusDays(2), 3, 0), PlannerCourse(2, 25, today.plusDays(1), 1, 0),
        ), listOf(25, 25, 25, 50, 25, 25, 25), 25), listOf(StudyBlock(1, 1, today, 25), StudyBlock(2, 2, today, 25)))
        val preview = postponeTodayStudy(original, listOf(boss, other), today)
        assertTrue(preview.unmovedMinutes.isEmpty())
        assertEquals(today.plusDays(1), preview.plan!!.blocks.first { it.bossId == 2 }.date)
        assertEquals(today.plusDays(2), preview.plan.blocks.first { it.bossId == 1 }.date)
    }
}
