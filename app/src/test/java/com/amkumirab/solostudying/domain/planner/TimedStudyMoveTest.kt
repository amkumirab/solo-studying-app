package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TimedStudyMoveTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
    private val config = PlannerConfig(monday, listOf(PlannerCourse(1, 300, monday.plusDays(13), 5, 120), PlannerCourse(2, 300, monday.plusDays(13), 3, 240)),
        List(7) { 120 }, 60, 90, studyWindows = windows).withWeeklyScheduling(listOf(PlannerSubject(1, "Physics", preferredWeeklyMinutes = 60)))
    private val plan = StudyPlan(config, listOf(StudyBlock(7, 1, monday, 30, 1020), StudyBlock(19, 2, monday, 30, 1050)))

    @Test fun `explicit move accepts adjacency and its own released slot but rejects overlaps`() {
        assertNull(moveTimedStudyBlock(plan, 7, monday, 1045, monday, 0))
        val moved = moveTimedStudyBlock(plan, 7, monday, 1080, monday, 0)!!
        assertEquals(listOf(7, 19), moved.blocks.map { it.id })
        assertEquals(plan.blocks.last(), moved.blocks.last())
        assertEquals(1080, moved.blocks.first().startMinute)
        assertEquals(plan.config, moved.config)
        assertEquals(plan, moveTimedStudyBlock(plan, 7, monday, 1020, monday, 0))
    }

    @Test fun `invalid time dates ids and credited sessions are rejected without throwing`() {
        listOf(-1, 1440, Int.MAX_VALUE, 1130, 0).forEach { assertNull(moveTimedStudyBlock(plan, 7, monday, it, monday, 0)) }
        assertNull(moveTimedStudyBlock(plan, 7, monday.minusDays(1), 1020, monday, 0))
        assertNull(moveTimedStudyBlock(plan, 7, monday.plusDays(14), 1020, monday, 0))
        assertNull(moveTimedStudyBlock(plan, 999, monday, 1020, monday, 0))
        assertNull(moveTimedStudyBlock(plan, 7, monday, 1080, monday, 1))
        assertEquals(1020, plan.blocks.first().startMinute)
    }

    @Test fun `explicit move cannot bypass overall weekly or frozen subject limits`() {
        val full = plan.copy(blocks = plan.blocks + StudyBlock(23, 1, monday.plusWeeks(1), 60, 1020))
        assertNull(moveTimedStudyBlock(full, 7, monday.plusWeeks(1).plusDays(1), 1020, monday, 0))
        val totalFull = plan.copy(blocks = plan.blocks + listOf(StudyBlock(23, 2, monday.plusWeeks(1), 60, 1020), StudyBlock(24, 2, monday.plusWeeks(1).plusDays(1), 30, 1020)))
        assertNull(moveTimedStudyBlock(totalFull, 7, monday.plusWeeks(1).plusDays(2), 1020, monday, 0))
    }

    @Test fun `postponement fails atomically when future windows cannot hold an accepted slot`() {
        val narrow = WeeklyAvailability(List(7) { index -> StudyDayAvailability(true,
            if (index == 0) listOf(StudyTimeWindow(1020, 1080)) else listOf(StudyTimeWindow(1020, 1050), StudyTimeWindow(1140, 1170))) })
        val input = config.copy(courses = listOf(config.courses.first().copy(finishBy = monday.plusDays(6))), weeklyScheduling = null, studyWindows = narrow,
            weekdayMinutes = narrow.weekdayMinutes)
        val source = StudyPlan(input, listOf(StudyBlock(7, 1, monday, 45, 1020)))
        val bosses = listOf(com.amkumirab.solostudying.data.entity.BossEntity(id = 1, name = "Physics", difficulty = "Hard", requiredMinutes = 300))
        val result = postponeTodayStudy(source, bosses, monday)
        assertNull(result.plan)
        assertEquals(mapOf(1 to 45), result.unmovedMinutes)
        assertEquals(listOf(StudyBlock(7, 1, monday, 45, 1020)), source.blocks)
    }
}
