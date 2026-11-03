package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class StudyPlanComparisonTest {
    private val monday = LocalDate.of(2026, 11, 2)
    private val windows = defaultWeeklyAvailability(List(7) { 180 })
    private val config = PlannerConfig(monday, listOf(PlannerCourse(1, 180, monday.plusDays(13), 3, 0),
        PlannerCourse(2, 60, monday.plusDays(13), 2, 0)), List(7) { 180 }, 60, studyWindows = windows)

    @Test fun `identical slots ignore regenerated ids and list order but retain metadata meaning`() {
        val old = StudyPlan(config, listOf(StudyBlock(1, 1, monday, 30, 1020),
            StudyBlock(2, 1, monday.plusDays(1), 30, 1020, manual = true)))
        val renumbered = old.copy(blocks = old.blocks.reversed().map { it.copy(id = it.id + 100) })
        val comparison = compareStudyPlans(old, renumbered)
        assertEquals(2, comparison.changes.count { it.kind == StudyPlanChangeKind.Kept })
        val automated = renumbered.copy(blocks = renumbered.blocks.map { it.copy(manual = false) })
        val changed = compareStudyPlans(old, automated)
        assertEquals(1, changed.changes.count { it.kind == StudyPlanChangeKind.Replanned })
        assertEquals(1, changed.manualSlotsReplaced)
    }

    @Test fun `cross week changes preserve both sides and show exact weekly course deltas`() {
        val old = StudyPlan(config, listOf(StudyBlock(1, 1, monday, 30, 1020),
            StudyBlock(2, 1, monday.plusDays(1), 30, 1020), StudyBlock(3, 2, monday, 30, 1080)))
        val next = StudyPlan(config, listOf(StudyBlock(50, 1, monday, 30, 1020),
            StudyBlock(51, 1, monday.plusWeeks(1), 60, 1020), StudyBlock(52, 1, monday.plusDays(2), 30, 1020)))
        val comparison = compareStudyPlans(old, next)
        assertEquals(1, comparison.changes.count { it.kind == StudyPlanChangeKind.Kept })
        assertEquals(1, comparison.changes.count { it.kind == StudyPlanChangeKind.Replanned })
        assertEquals(1, comparison.changes.count { it.kind == StudyPlanChangeKind.Added })
        assertEquals(1, comparison.changes.count { it.kind == StudyPlanChangeKind.Removed })
        assertEquals(listOf(1 to (60 to 60), 2 to (30 to 0)), comparison.week(monday).courses.map {
            it.bossId to (it.acceptedMinutes to it.proposedMinutes) })
        assertEquals(60, comparison.week(monday.plusWeeks(1)).proposed.plannedMinutes)
        assertEquals(old, comparison.accepted)
        assertEquals(next, comparison.proposed)
    }

    @Test fun `duplicate date only slots match one to one without inventing clock times`() {
        val old = StudyPlan(config.copy(studyWindows = null), listOf(StudyBlock(1, 1, monday, 30), StudyBlock(2, 1, monday, 30)))
        val next = old.copy(blocks = listOf(StudyBlock(99, 1, monday, 30)))
        val comparison = compareStudyPlans(old, next)
        assertEquals(1, comparison.changes.count { it.kind == StudyPlanChangeKind.Kept })
        assertEquals(1, comparison.changes.count { it.kind == StudyPlanChangeKind.Removed })
        assertTrue(comparison.changes.all { it.before?.startMinute == null && it.after?.startMinute == null })
    }

    @Test fun `proposal uses new settings and recorded progress without mutating accepted locks`() {
        val old = StudyPlan(config, listOf(StudyBlock(1, 1, monday, 60, 1020),
            StudyBlock(2, 1, monday.plusDays(1), 60, 1020, locked = true)))
        val bosses = listOf(BossEntity(id = 1, name = "Physics", difficulty = "Hard", requiredMinutes = 180, timeSpentSeconds = 900),
            BossEntity(id = 2, name = "Systems", difficulty = "Hard", requiredMinutes = 60))
        val snapshot = PlannerSnapshot(old, listOf(PlannerSubject(1, "Physics", 5)), windows)
        val result = previewStudyRebuild(snapshot, bosses, monday)
        assertNull(result.error)
        val rebuilt = result.plan!!
        assertEquals(225, rebuilt.blocks.sumOf { it.minutes })
        assertEquals(900L, rebuilt.config.courses.first().baselineSeconds)
        assertEquals(5, rebuilt.config.courses.first().priority)
        assertEquals(old.blocks.single { it.locked }, rebuilt.blocks.single { it.locked })
        assertEquals(old, snapshot.plan)
        val conflicting = windows.copy(days = windows.days.map { it.copy(windows = listOf(StudyTimeWindow(540, 720))) })
        assertNull(previewStudyRebuild(snapshot.copy(availability = conflicting), bosses, monday).plan)
    }

    @Test fun `rest complete and expired inputs return clear errors without proposals`() {
        val old = buildStudyPlan(config)
        val boss = BossEntity(id = 1, name = "Physics", difficulty = "Hard", requiredMinutes = 180)
        val snapshot = PlannerSnapshot(old, availability = windows.copy(days = windows.days.map { it.copy(enabled = false) }))
        assertTrue(previewStudyRebuild(snapshot, listOf(boss), monday).error!!.contains("Enable at least one"))
        assertTrue(previewStudyRebuild(PlannerSnapshot(old), emptyList(), monday).error!!.contains("complete"))
        assertTrue(previewStudyRebuild(PlannerSnapshot(old), listOf(boss), monday.plusDays(20)).error!!.contains("finish date"))
    }

    @Test fun `generated comparisons account for every old and new slot once and are repeatable`() {
        val random = kotlin.random.Random(307)
        repeat(80) {
            val input = config.copy(courses = config.courses.map { course ->
                course.copy(remainingMinutes = random.nextInt(1, 481), priority = random.nextInt(1, 6))
            }, sessionMinutes = listOf(25, 45, 60)[random.nextInt(3)])
            val old = buildStudyPlan(input)
            val next = buildStudyPlan(input.copy(weekdayMinutes = List(7) { 60 },
                courses = input.courses.map { it.copy(priority = 6 - it.priority) }))
            val result = compareStudyPlans(old, next)
            assertEquals(result, compareStudyPlans(old.copy(blocks = old.blocks.reversed()), next.copy(blocks = next.blocks.reversed()))
                .copy(accepted = old, proposed = next))
            assertEquals(old.blocks.sortedBy { it.id }, result.changes.mapNotNull { it.before }.sortedBy { it.id })
            assertEquals(next.blocks.sortedBy { it.id }, result.changes.mapNotNull { it.after }.sortedBy { it.id })
            result.changes.filter { it.kind == StudyPlanChangeKind.Kept }.forEach { change ->
                assertEquals(change.before!!.copy(id = change.after!!.id), change.after)
            }
            repeat(2) { week ->
                val report = result.week(monday.plusWeeks(week.toLong()))
                assertEquals(report.accepted.plannedMinutes, report.courses.sumOf { it.acceptedMinutes })
                assertEquals(report.proposed.plannedMinutes, report.courses.sumOf { it.proposedMinutes })
            }
        }
    }
}
