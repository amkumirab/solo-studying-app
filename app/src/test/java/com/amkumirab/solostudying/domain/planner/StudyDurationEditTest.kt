package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class StudyDurationEditTest {
    private val today = LocalDate.of(2026, 10, 19)
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
    private val config = PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(13), 5, 120)), List(7) { 120 }, 30, studyWindows = windows)
    private val goals = listOf(BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 122, timeSpentSeconds = 120))
    private fun plan() = StudyPlan(config, listOf(StudyBlock(7, 1, today, 30, 1020), StudyBlock(20, 1, today.plusDays(1), 30, 1020),
        StudyBlock(99, 1, today.plusDays(2), 30, 1020), StudyBlock(100, 1, today.plusDays(3), 30, 1020)))

    @Test fun `growth takes furthest eligible time without changing total effort credit or IDs`() {
        val original = plan()
        val result = previewStudyDuration(original, goals, today, 7, 45)
        assertNull(result.error)
        val changed = result.plan!!
        assertEquals(original.config, changed.config)
        assertEquals(original.blocks.first().copy(minutes = 45, durationEdited = true), changed.blocks.first())
        assertEquals(original.blocks.last().copy(minutes = 15, durationEdited = true), changed.blocks.last())
        assertEquals(original.blocks.subList(1, 3), changed.blocks.subList(1, 3))
        assertEquals(120, changed.blocks.sumOf { it.minutes })
        assertTrue(changed.shortfalls.isEmpty())
        assertEquals(blockProgress(original, goals).map { it.creditedSeconds }, blockProgress(changed, goals).map { it.creditedSeconds })
        assertEquals(original, plan())
    }

    @Test fun `growth uses unscheduled work before borrowing and may remove empty donors`() {
        val original = plan().copy(config = config.copy(courses = listOf(config.courses.single().copy(remainingMinutes = 135))))
        val changed = previewStudyDuration(original, goals, today, 7, 90).plan!!
        assertEquals(listOf(7, 20, 99), changed.blocks.map { it.id })
        assertEquals(15, changed.blocks.last().minutes)
        assertEquals(135, changed.blocks.sumOf { it.minutes })
        assertEquals(1020, changed.blocks.last().startMinute)
    }

    @Test fun `shrinking returns time to unscheduled work without credit or baseline changes`() {
        val original = plan()
        val changed = previewStudyDuration(original, goals, today, 7, 15).plan!!
        assertEquals(15, changed.shortfalls[1])
        assertEquals(original.blocks.drop(1), changed.blocks.drop(1))
        assertEquals(original.config, changed.config)
        assertEquals(0L, blockProgress(changed, goals).sumOf { it.creditedSeconds })
        assertEquals(original, previewStudyDuration(original, goals, today, 7, 30).plan)
    }

    @Test fun `invalid durations past studied locked missing and date-only sessions cannot change`() {
        val original = plan()
        listOf(Int.MIN_VALUE, -1, 0, 121, Int.MAX_VALUE).forEach {
            assertNull(previewStudyDuration(original, goals, today, 7, it).plan)
        }
        assertNull(previewStudyDuration(original, goals, today, 999, 45).plan)
        assertNull(previewStudyDuration(original, emptyList(), today, 7, 45).plan)
        assertNull(previewStudyDuration(original, goals.map { it.copy(timeSpentSeconds = 121) }, today, 7, 45).plan)
        assertNull(previewStudyDuration(original, goals, today.plusDays(1), 7, 45).plan)
        val locked = setStudyBlockLocked(original, 7, true, goals, today)!!
        assertNull(previewStudyDuration(locked, goals, today, 7, 45).plan)
        val dated = buildStudyPlan(config.copy(studyWindows = null))
        assertNull(previewStudyDuration(dated, goals, today, dated.blocks.first().id, 15).plan)
    }

    @Test fun `donors cannot include locked sessions same-day slots or another course`() {
        val otherConfig = config.copy(courses = config.courses + PlannerCourse(2, 30, today.plusDays(13), 1, 0))
        val original = StudyPlan(otherConfig, listOf(StudyBlock(7, 1, today, 30, 1020),
            StudyBlock(20, 1, today, 30, 1110), StudyBlock(99, 1, today.plusDays(1), 30, 1020, locked = true),
            StudyBlock(100, 2, today.plusDays(2), 30, 1020)))
        val changed = previewStudyDuration(original, goals, today, 7, 60).plan!!
        assertEquals(original.blocks.drop(1), changed.blocks.drop(1))
        assertNull(previewStudyDuration(original, goals, today, 7, 90).plan)
    }

    @Test fun `overlaps windows and daily or weekly caps reject the whole transfer`() {
        val original = plan()
        val collision = original.copy(blocks = original.blocks + StudyBlock(101, 1, today, 15, 1050),
            config = config.copy(courses = listOf(config.courses.single().copy(remainingMinutes = 135))))
        assertNull(previewStudyDuration(collision, goals, today, 7, 45).plan)
        val narrow = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1050))) })
        val narrowPlan = original.copy(config = config.copy(weekdayMinutes = List(7) { 30 }, studyWindows = narrow))
        assertNull(previewStudyDuration(narrowPlan, goals, today, 7, 45).plan)
        val daily = original.copy(config = config.copy(weekdayMinutes = List(7) { 30 }))
        assertNull(previewStudyDuration(daily, goals, today, 7, 45).plan)
        val weekly = original.copy(config = config.copy(weeklyGoalMinutes = 120, weeklyScheduling = WeeklySchedulingPolicy(mapOf(1 to 120))))
        assertNotNull(previewStudyDuration(weekly, goals, today, 7, 45).plan)
        val secondWeek = weekly.copy(blocks = weekly.blocks.map { if (it.id == 100) it.copy(date = today.plusDays(7)) else it })
        val limited = secondWeek.copy(config = secondWeek.config.copy(weeklyGoalMinutes = 90))
        val transferred = previewStudyDuration(limited, goals, today, 7, 45).plan!!
        assertEquals(15, transferred.blocks.first { it.id == 99 }.minutes)
        assertEquals(30, transferred.blocks.first { it.id == 100 }.minutes)
        val fixedWeek = limited.copy(blocks = limited.blocks.map { if (it.id == 20 || it.id == 99) it.copy(locked = true) else it })
        assertNull(previewStudyDuration(fixedWeek, goals, today, 7, 45).plan)
    }

    @Test fun `generated edits preserve exact accounting and every locked or credited slot`() {
        repeat(150) { seed ->
            val total = 120 + seed % 31
            val original = plan().copy(config = config.copy(courses = listOf(config.courses.single().copy(remainingMinutes = total))),
                blocks = plan().blocks.map { if (seed % 3 == 0 && it.id == 100) it.copy(locked = true) else it })
            val minutes = seed % 120 + 1
            val available = 120 + total - 120 - if (seed % 3 == 0) 30 else 0
            val proposal = previewStudyDuration(original, goals, today, 7, minutes)
            if (minutes > available) assertNull("Case $seed", proposal.plan)
            else {
                assertNull("Case $seed: ${proposal.error}", proposal.error)
                val changed = proposal.plan!!
                assertEquals(minutes, changed.blocks.first { it.id == 7 }.minutes)
                assertEquals(total, changed.blocks.sumOf { it.minutes } + (changed.shortfalls[1] ?: 0))
                assertEquals(original.config, changed.config)
                original.blocks.filter { it.locked }.forEach { fixed -> assertEquals(fixed, changed.blocks.first { it.id == fixed.id }) }
                assertTrue(blockProgress(changed, goals).all { it.creditedSeconds == 0L })
            }
        }
    }

    @Test fun `earlier partial credit survives resizing and removing later unstudied IDs`() {
        val original = StudyPlan(config, listOf(StudyBlock(7, 1, today, 30, 1050), StudyBlock(1, 1, today, 30, 1020),
            StudyBlock(20, 1, today.plusDays(1), 30, 1020), StudyBlock(99, 1, today.plusDays(2), 30, 1020)))
        val recorded = goals.map { it.copy(timeSpentSeconds = 180) }
        val changed = previewStudyDuration(original, recorded, today, 7, 75).plan!!
        assertEquals(listOf(7, 1, 20), changed.blocks.map { it.id })
        assertEquals(original.blocks[1], changed.blocks[1])
        assertEquals(60L, blockProgress(changed, recorded).single { it.block.id == 1 }.creditedSeconds)
        assertEquals(15, changed.blocks.last().minutes)
        assertEquals(180L, recorded.single().timeSpentSeconds)
    }

    @Test fun `maximum edited slot can move postpone lock and rebuild with a shorter preferred length`() {
        val late = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1320, 1440))) })
        val original = plan().copy(config = config.copy(studyWindows = late), blocks = plan().blocks.map { it.copy(startMinute = 1320) })
        val changed = previewStudyDuration(original, goals, today, 7, 120).plan!!
        assertEquals("22:00–24:00", changed.blocks.single().clockRange())
        val moved = moveTimedStudyBlock(changed, 7, today.plusDays(1), 1320, today, 0)!!
        assertTrue(moved.blocks.single().durationEdited)
        val postponed = postponeTodayStudy(changed, goals, today).plan!!
        assertEquals(120, postponed.blocks.single().minutes)
        assertTrue(postponed.blocks.single().durationEdited)
        val locked = setStudyBlockLocked(changed, 7, true, goals, today)!!
        val rebuilt = rebuildWithStudyLocks(locked.config.copy(sessionMinutes = 15), locked, goals, today).plan!!
        assertEquals(locked.blocks, rebuilt.blocks)
    }
}
