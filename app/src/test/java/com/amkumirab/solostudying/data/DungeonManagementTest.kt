package com.amkumirab.solostudying.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.BossStepEntity
import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.domain.dungeon.DungeonGoalEdit
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DungeonManagementTest {
    private lateinit var database: SoloStudyingDatabase
    private lateinit var repository: SoloStudyingRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), SoloStudyingDatabase::class.java,
        ).build()
        repository = SoloStudyingRepository(database)
    }

    @After fun tearDown() = database.close()

    @Test fun `deleting a goal leaves the original study steps available for recovery`() = runBlocking {
        val goal = goalWithProgress()
        val stepId = repository.insertBossStep(BossStepEntity(bossId = goal.id, title = "Processes",
            estimatedMinutes = 25, sortOrder = 0, isCompleted = true)).toInt()
        repository.insertSession(StudySessionEntity(bossId = goal.id, bossName = goal.name, durationSeconds = 1_500,
            xpEarned = 25, goldEarned = 10, wasCompleted = true, note = "Reviewed scheduling"))
        val history = repository.allSessions.first()

        repository.deleteBoss(goal)

        assertTrue(repository.allBosses.first().isEmpty())
        assertEquals(goal.id, repository.getBossStepById(stepId)?.bossId)
        assertEquals(goal.id, repository.archivedBosses.first().single().id)
        assertNull(repository.getBossById(goal.id))
        assertTrue(repository.restoreBoss(goal.id))
        assertFalse(repository.restoreBoss(goal.id))
        assertEquals(goal, repository.allBosses.first().single())
        assertTrue(repository.archivedBosses.first().isEmpty())
        assertEquals(history, repository.allSessions.first())
    }

    @Test fun `editing the estimate preserves elapsed time and rejects invalid targets`() = runBlocking {
        val goal = goalWithProgress()
        val edit = DungeonGoalEdit(" OS exam ", "Hard", 240, LocalDate.of(2026, 12, 15))
        assertFalse(repository.editBoss(goal.id, edit.copy(minutes = 60)))
        assertTrue(repository.editBoss(goal.id, edit))
        val edited = checkNotNull(repository.getBossById(goal.id))
        assertEquals(goal.copy(name = "OS exam", difficulty = "Hard", requiredMinutes = 240,
            deadlineDate = "2026-12-15"), edited)
        repository.updateBossProgress(goal.copy(timeSpentSeconds = 3_650))
        assertEquals(edited.copy(timeSpentSeconds = 3_650), repository.getBossById(goal.id))
    }

    @Test fun `second gate adds new minutes without replacing the original completion`() = runBlocking {
        val original = goalWithProgress(completed = true)
        assertFalse(repository.editBoss(original.id, DungeonGoalEdit(original.name, original.difficulty, 240, null)))
        assertFalse(repository.openSecondGate(original.id, 0))
        val attempts = List(2) { async { repository.openSecondGate(original.id, 60) } }.awaitAll()
        assertEquals(1, attempts.count { it })
        val gate = repository.allBosses.first().single { it.secondGateOfId == original.id }
        assertEquals(60, gate.requiredMinutes)
        assertEquals(0L, gate.timeSpentSeconds)
        assertFalse(gate.isCompleted)
        assertEquals(original, repository.getBossById(original.id))
        assertFalse(repository.openSecondGate(gate.id, 30))
    }

    @Test fun `metadata stays editable at the target while an exam awaits manual completion`() = runBlocking {
        val goal = goalWithProgress()
        val ready = goal.copy(timeSpentSeconds = 10_800, isRealBoss = true)
        repository.updateBoss(ready)
        assertTrue(repository.editBoss(goal.id, DungeonGoalEdit("OS exam", goal.difficulty, 180, null)))
        assertEquals(ready.copy(name = "OS exam"), repository.getBossById(goal.id))
    }

    @Test fun `archiving the original hides its gate without changing independent gate deletion`() = runBlocking {
        val original = goalWithProgress(completed = true)
        assertTrue(repository.openSecondGate(original.id, 60))
        val gate = repository.allBosses.first().single { it.secondGateOfId == original.id }
        repository.updateBossProgress(gate.copy(timeSpentSeconds = 900))
        repository.deleteBoss(original)
        assertTrue(repository.allBosses.first().isEmpty())
        assertNull(repository.getBossById(gate.id))
        assertTrue(repository.restoreBoss(original.id))
        assertEquals(900L, repository.getBossById(gate.id)?.timeSpentSeconds)
        repository.deleteBoss(gate)
        repository.deleteBoss(original)
        assertFalse(repository.restoreBoss(gate.id))
        assertTrue(repository.restoreBoss(original.id))
        assertFalse(repository.openSecondGate(original.id, 60))
        assertTrue(repository.restoreBoss(gate.id))
        assertEquals(900L, repository.getBossById(gate.id)?.timeSpentSeconds)
    }

    private suspend fun goalWithProgress(completed: Boolean = false): BossEntity {
        val goal = BossEntity(name = "Operating Systems", difficulty = "Medium", requiredMinutes = 180,
            timeSpentSeconds = if (completed) 10_800 else 3_621, isCompleted = completed)
        return goal.copy(id = repository.insertBoss(goal).toInt())
    }
}
