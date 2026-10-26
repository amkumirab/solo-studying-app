package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CustomStudySessionTest {
    private val today = LocalDate.of(2026, 10, 26)
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
    private val config = PlannerConfig(today, listOf(PlannerCourse(1, 180, today.plusDays(13), 5, 120)), List(7) { 120 }, 30, studyWindows = windows)
    private val goals = listOf(BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 182, timeSpentSeconds = 120))
    private fun plan() = StudyPlan(config, listOf(StudyBlock(7, 1, today, 30, 1020), StudyBlock(2, 1, today.plusDays(1), 30, 1020)))

    @Test fun `custom session consumes only unscheduled work and appends a new credit ID`() {
        val original = plan()
        val proposal = previewAddStudySession(original, goals, today, 1, today, 1050, 45)
        assertNull(proposal.error)
        val added = proposal.plan!!
        assertEquals(original.config, added.config)
        assertEquals(original.blocks, added.blocks.dropLast(1))
        val block = added.blocks.last()
        assertEquals(8, block.id)
        assertEquals(45, block.minutes)
        assertEquals(1050, block.startMinute)
        assertTrue(block.manual)
        assertFalse(block.locked)
        assertFalse(block.durationEdited)
        assertEquals(75, added.shortfalls[1])
        assertTrue(blockProgress(added, goals).all { it.creditedSeconds == 0L })
        assertEquals(original, plan())
    }

    @Test fun `invalid bounds dates courses and unavailable goals reject without changing slots`() {
        val original = plan()
        listOf(Int.MIN_VALUE, 0, 121, Int.MAX_VALUE).forEach {
            assertNull(previewAddStudySession(original, goals, today, 1, today, 1050, it).plan)
        }
        listOf(-1, 1440, Int.MAX_VALUE).forEach {
            assertNull(previewAddStudySession(original, goals, today, 1, today, it, 30).plan)
        }
        listOf(today.minusDays(1), today.plusDays(14)).forEach {
            assertNull(previewAddStudySession(original, goals, today, 1, it, 1050, 30).plan)
        }
        assertNull(previewAddStudySession(original, goals, today, 99, today, 1050, 30).plan)
        assertNull(previewAddStudySession(original, emptyList(), today, 1, today, 1050, 30).plan)
        assertNull(previewAddStudySession(original, goals.map { it.copy(isCompleted = true) }, today, 1, today, 1050, 30).plan)
        val dated = buildStudyPlan(config.copy(studyWindows = null))
        assertNull(previewAddStudySession(dated, goals, today, 1, today, 1050, 30).plan)
        assertNull(previewAddStudySession(original.copy(blocks = listOf(original.blocks.first().copy(id = Int.MAX_VALUE))), goals, today, 1, today, 1050, 30).plan)
    }

    @Test fun `overlap window daily weekly and course limits are enforced without borrowing`() {
        val original = plan()
        assertNull(previewAddStudySession(original, goals, today, 1, today, 1040, 30).plan)
        assertNull(previewAddStudySession(original, goals, today, 1, today, 1130, 30).plan)
        val daily = original.copy(config = config.copy(weekdayMinutes = List(7) { 30 }))
        assertNull(previewAddStudySession(daily, goals, today, 1, today, 1050, 1).plan)
        val weekly = original.copy(config = config.copy(weeklyGoalMinutes = 60, weeklyScheduling = WeeklySchedulingPolicy(emptyMap())))
        assertNull(previewAddStudySession(weekly, goals, today, 1, today, 1050, 1).plan)
        val limited = original.copy(config = config.copy(weeklyGoalMinutes = 120, weeklyScheduling = WeeklySchedulingPolicy(mapOf(1 to 60))))
        assertNull(previewAddStudySession(limited, goals, today, 1, today, 1050, 1).plan)
        val full = original.copy(config = config.copy(courses = listOf(config.courses.single().copy(remainingMinutes = 60))))
        assertNull(previewAddStudySession(full, goals, today, 1, today, 1050, 1).plan)
        assertEquals(original.blocks, plan().blocks)
    }

    @Test fun `recorded credit remains on original IDs and is never absorbed by new slots`() {
        val original = plan()
        val partial = goals.map { it.copy(timeSpentSeconds = 121) }
        val changed = previewAddStudySession(original, partial, today, 1, today, 1050, 30).plan!!
        assertEquals(1L, blockProgress(changed, partial).first { it.block.id == 2 }.creditedSeconds)
        assertEquals(0L, blockProgress(changed, partial).first { it.block.id == 8 }.creditedSeconds)
        val extraCredit = goals.map { it.copy(timeSpentSeconds = 120 + 60 * 60 + 1) }
        assertNull(previewAddStudySession(original, extraCredit, today, 1, today, 1050, 30).plan)
    }

    @Test fun `removal returns minutes to unscheduled work and retains surviving ledger order`() {
        val original = plan()
        val partial = goals.map { it.copy(timeSpentSeconds = 121) }
        val removed = previewRemoveStudySession(original, partial, today, 7).plan!!
        assertEquals(original.config, removed.config)
        assertEquals(listOf(original.blocks[1]), removed.blocks)
        assertEquals(150, removed.shortfalls[1])
        assertEquals(1L, blockProgress(removed, partial).single().creditedSeconds)
        val empty = previewRemoveStudySession(plan().copy(blocks = listOf(plan().blocks.first())), goals, today, 7).plan!!
        assertTrue(empty.blocks.isEmpty())
        assertEquals(180, empty.shortfalls[1])
    }

    @Test fun `locked past missing and studied sessions cannot be removed`() {
        val original = plan()
        assertNull(previewRemoveStudySession(original, goals, today, 99).plan)
        assertNull(previewRemoveStudySession(original, emptyList(), today, 7).plan)
        assertNull(previewRemoveStudySession(original, goals, today.plusDays(1), 7).plan)
        assertNull(previewRemoveStudySession(original, goals.map { it.copy(timeSpentSeconds = 121) }, today, 2).plan)
        assertNull(previewRemoveStudySession(original, goals.map { it.copy(isCompleted = true) }, today, 7).plan)
        val locked = original.copy(blocks = original.blocks.map { it.copy(locked = true) })
        assertNull(previewRemoveStudySession(locked, goals, today, 7).plan)
        val dated = buildStudyPlan(config.copy(studyWindows = null))
        assertNull(previewRemoveStudySession(dated, goals, today, dated.blocks.first().id).plan)
    }

    @Test fun `manual slots can move resize lock and survive an explicit locked rebuild`() {
        val late = config.copy(studyWindows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1320, 1440))) }))
        val original = StudyPlan(late, emptyList())
        val added = previewAddStudySession(original, goals, today, 1, today, 1320, 120).plan!!
        assertEquals("22:00–24:00", added.blocks.single().clockRange())
        val moved = moveTimedStudyBlock(added, 1, today.plusDays(1), 1320, today, 0)!!
        assertTrue(moved.blocks.single().manual)
        val resized = previewStudyDuration(moved, goals, today, 1, 90).plan!!
        assertTrue(resized.blocks.single().manual)
        assertTrue(resized.blocks.single().durationEdited)
        val locked = setStudyBlockLocked(added, 1, true, goals, today)!!
        val rebuilt = rebuildWithStudyLocks(late.copy(sessionMinutes = 15), locked, goals, today).plan!!
        assertEquals(locked.blocks.single(), rebuilt.blocks.first { it.id == 1 })
        val unlockedRebuild = rebuildWithStudyLocks(late, added, goals, today).plan!!
        assertTrue(unlockedRebuild.blocks.none { it.manual })
    }

    @Test fun `generated additions and removals preserve accounting and all other blocks`() {
        repeat(120) { seed ->
            val original = plan()
            val minutes = seed + 1
            val added = previewAddStudySession(original, goals, today, 1, today.plusDays(2), 1020, minutes).plan!!
            assertEquals(180, added.blocks.sumOf { it.minutes } + (added.shortfalls[1] ?: 0))
            assertEquals(original, previewRemoveStudySession(added, goals, today, 8).plan)
            assertEquals(original.blocks, added.blocks.dropLast(1))
        }
    }
}
