package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class SessionLocksTest {
    private val today = LocalDate.of(2026, 10, 19)
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1080))) })
    private fun config() = PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(6), 5, 0)),
        List(7) { 60 }, 30, 120, WeeklySchedulingPolicy(mapOf(1 to 120)), windows)
    private val bosses = listOf(BossEntity(id = 1, name = "Physics", difficulty = "Hard", requiredMinutes = 120))
    private fun plan() = StudyPlan(config(), listOf(StudyBlock(7, 1, today, 30, 1035), StudyBlock(20, 1, today.plusDays(1), 30, 1020)))

    @Test fun `locking and unlocking change only the flag and preserve credit`() {
        val original = plan()
        val locked = setStudyBlockLocked(original, 20, true, bosses, today)!!
        assertEquals(original.blocks[1].copy(locked = true), locked.blocks[1])
        assertEquals(original.blocks[0], locked.blocks[0])
        assertEquals(blockProgress(original, bosses).map { it.creditedSeconds }, blockProgress(locked, bosses).map { it.creditedSeconds })
        assertEquals(original, setStudyBlockLocked(locked, 20, false, bosses, today))
        assertNull(setStudyBlockLocked(original, 7, true, bosses.map { it.copy(timeSpentSeconds = 1) }, today))
        assertNull(setStudyBlockLocked(original, 7, true, bosses, today.plusDays(1)))
        assertNull(setStudyBlockLocked(original, 999, true, bosses, today))
        assertNull(setStudyBlockLocked(original, 7, true, emptyList(), today))
        val dated = buildStudyPlan(config().copy(studyWindows = null))
        assertNull(setStudyBlockLocked(dated, dated.blocks.first().id, true, bosses, today))
    }

    @Test fun `locked slots reject moves and postpone never silently moves them`() {
        val locked = setStudyBlockLocked(plan(), 7, true, bosses, today)!!
        assertNull(moveStudyBlock(locked, 7, today.plusDays(1), today, 0))
        assertNull(moveTimedStudyBlock(locked, 7, today, 1020, today, 0))
        val postponed = postponeTodayStudy(locked, bosses, today)
        assertNull(postponed.plan)
        assertEquals(30, postponed.unmovedMinutes[1])
        assertTrue(postponed.moves.none { it.id == 7 })
    }

    @Test fun `rebuild preserves exact lock and fills both sides without double counting`() {
        val old = setStudyBlockLocked(plan(), 7, true, bosses, today)!!
        val result = rebuildWithStudyLocks(config(), old, bosses, today)
        assertNull(result.error)
        val rebuilt = result.plan!!
        assertEquals(old.blocks.first(), rebuilt.blocks.single { it.locked })
        assertEquals(120, rebuilt.blocks.sumOf { it.minutes })
        assertTrue(rebuilt.shortfalls.isEmpty())
        assertEquals(rebuilt, rebuildWithStudyLocks(config(), old, bosses, today).plan)
        assertEquals(rebuilt.blocks.size, rebuilt.blocks.distinctBy { it.id }.size)
        assertEquals(120, recommendWeeklyHours(rebuilt.config, emptyList(), today).allocatedMinutes)
    }

    @Test fun `invalid windows dates effort goals and credits report conflicts without mutating source`() {
        val old = setStudyBlockLocked(plan(), 7, true, bosses, today)!!
        val different = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 600))) })
        val candidates = listOf(config().copy(studyWindows = different), config().copy(weekdayMinutes = List(7) { 15 }),
            config().copy(sessionMinutes = 15), config().copy(weeklyGoalMinutes = 0),
            config().copy(courses = listOf(config().courses.single().copy(remainingMinutes = 15))),
            config().copy(weeklyScheduling = WeeklySchedulingPolicy(mapOf(1 to 15))))
        candidates.forEach { next ->
            val rejected = rebuildWithStudyLocks(next, old, bosses, today)
            assertNull(rejected.plan)
            assertNotNull(rejected.error)
        }
        assertNull(rebuildWithStudyLocks(config(), old, bosses.map { it.copy(timeSpentSeconds = 1) }, today).plan)
        assertNull(rebuildWithStudyLocks(config(), old, bosses, today.plusDays(1)).plan)
        assertTrue(old.blocks.first().locked)
    }

    @Test fun `future locked reservations respect course weekly limits and deadline work`() {
        val next = config().copy(courses = listOf(PlannerCourse(1, 90, today.plusDays(13), 1, 0),
            PlannerCourse(2, 60, today.plusDays(1), 5, 0)), weeklyGoalMinutes = 90,
            weeklyScheduling = WeeklySchedulingPolicy(mapOf(1 to 60, 2 to 60)))
        val old = StudyPlan(next, listOf(StudyBlock(100, 1, today.plusDays(7), 30, 1020, locked = true)))
        val goals = bosses + BossEntity(id = 2, name = "Math", difficulty = "Hard", requiredMinutes = 60)
        val rebuilt = rebuildWithStudyLocks(next, old, goals, today).plan!!
        assertTrue(rebuilt.shortfalls.isEmpty())
        assertEquals(60, rebuilt.blocks.filter { it.bossId == 2 }.sumOf { it.minutes })
        assertEquals(old.blocks.single(), rebuilt.blocks.single { it.locked })
    }

    @Test fun `generated feasible plans remain valid with multiple frozen slots`() {
        repeat(80) { seed ->
            val day = today.plusDays((seed % 7).toLong())
            val next = config().copy(start = day, courses = listOf(
                PlannerCourse(1, 60 + seed, day.plusDays(6), seed % 5 + 1, 0),
                PlannerCourse(2, 90 + seed, day.plusDays(13), (seed + 2) % 5 + 1, 0)),
                sessionMinutes = listOf(15, 25, 45, 60)[seed % 4], weeklyGoalMinutes = 180 + seed,
                weeklyScheduling = WeeklySchedulingPolicy(mapOf(1 to 150, 2 to 150)))
            val generated = buildStudyPlan(next)
            assertTrue("Original case $seed", generated.shortfalls.isEmpty())
            val old = generated.copy(blocks = generated.blocks.mapIndexed { index, block -> block.copy(locked = index % 3 == 0) })
            val goals = bosses + BossEntity(id = 2, name = "Math", difficulty = "Hard", requiredMinutes = 240)
            val result = rebuildWithStudyLocks(next, old, goals, day)
            assertNull("Case $seed: ${result.error}", result.error)
            val rebuilt = result.plan!!
            assertTrue("Case $seed: ${rebuilt.shortfalls}", rebuilt.shortfalls.isEmpty())
            assertEquals(old.blocks.filter { it.locked }, rebuilt.blocks.filter { it.locked })
            assertEquals(next.courses.sumOf { it.remainingMinutes }, rebuilt.blocks.sumOf { it.minutes })
        }
    }

    @Test fun `reserved full days and maximum imported IDs do not lose minutes or overflow`() {
        val next = config().copy(courses = listOf(config().courses.single().copy(remainingMinutes = 90)))
        val old = StudyPlan(next, listOf(StudyBlock(Int.MAX_VALUE, 1, today, 30, 1020, true),
            StudyBlock(1, 1, today, 30, 1050, true)))
        val rebuilt = rebuildWithStudyLocks(next, old, bosses, today).plan!!
        assertEquals(90, rebuilt.blocks.sumOf { it.minutes })
        assertEquals(old.blocks, rebuilt.blocks.filter { it.locked })
        assertTrue(rebuilt.blocks.all { it.id > 0 })
        assertTrue(rebuilt.blocks.filterNot { it.locked }.all { it.date > today })
        val allLocked = old.copy(config = next.copy(courses = listOf(next.courses.single().copy(remainingMinutes = 60))))
        assertEquals(allLocked, rebuildWithStudyLocks(allLocked.config, allLocked, bosses, today).plan)
    }

    @Test fun `rebuild rebases unlocked credit while retaining an unstudied lock`() {
        val old = StudyPlan(config(), listOf(StudyBlock(1, 1, today, 30, 1020), StudyBlock(50, 1, today.plusDays(1), 30, 1020, true)))
        val goals = bosses.map { it.copy(timeSpentSeconds = 60) }
        val next = config().copy(courses = remainingPlannerCourses(old, goals))
        val rebuilt = rebuildWithStudyLocks(next, old, goals, today).plan!!
        assertEquals(119, rebuilt.blocks.sumOf { it.minutes })
        assertEquals(60L, rebuilt.config.courses.single().baselineSeconds)
        assertEquals(old.blocks.last(), rebuilt.blocks.single { it.locked })
        assertTrue(blockProgress(rebuilt, goals).all { it.creditedSeconds == 0L })
        assertEquals(60L, goals.single().timeSpentSeconds)
    }

    @Test fun `removing a locked course from answers conflicts but no-lock rebuild stays unchanged`() {
        val old = setStudyBlockLocked(plan(), 7, true, bosses, today)!!
        val unrelated = config().copy(courses = listOf(PlannerCourse(2, 60, today.plusDays(6), 5, 0)), weeklyScheduling = null)
        assertNull(rebuildWithStudyLocks(unrelated, old, bosses, today).plan)
        assertEquals(buildStudyPlan(config()), rebuildWithStudyLocks(config(), plan(), bosses, today).plan)
        assertEquals(buildStudyPlan(config()), rebuildWithStudyLocks(config(), null, bosses, today).plan)
    }
}
