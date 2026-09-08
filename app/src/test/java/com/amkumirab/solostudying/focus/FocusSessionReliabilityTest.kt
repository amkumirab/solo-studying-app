package com.amkumirab.solostudying.focus

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.BossStepEntity
import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.domain.session.SessionEndState
import com.amkumirab.solostudying.ui.viewmodel.BattleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusSessionReliabilityTest {

    private lateinit var context: Context
    private lateinit var database: SoloStudyingDatabase
    private lateinit var repository: SoloStudyingRepository
    private lateinit var store: FocusSessionStore
    private var battleViewModel: BattleViewModel? = null

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.deleteSharedPreferences(FocusSessionStore.PREFERENCES_NAME)
        database = Room.inMemoryDatabaseBuilder(context, SoloStudyingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SoloStudyingRepository(database)
        store = FocusSessionStore(context)
        repository.insertOrUpdateProfile(UserProfileEntity())
    }

    @After
    fun tearDown() {
        battleViewModel?.takeIf { it.isBattleActive && !it.isBattlePaused }?.pauseBattle()
        store.clear()
        database.close()
        context.deleteSharedPreferences(FocusSessionStore.PREFERENCES_NAME)
    }

    @Test
    fun `focus session snapshot survives store recreation`() {
        val snapshot = runningSnapshot()

        store.write(snapshot)

        assertEquals(snapshot, FocusSessionStore(context).read())
        FocusSessionStore(context).clear()
        assertNull(FocusSessionStore(context).read())
    }

    @Test
    fun `running session applies real elapsed time after app returns`() {
        val snapshot = runningSnapshot(
            timeLeftSeconds = 480L,
            timeSpentSeconds = 60L,
            lastTickTimeMillis = 1_000_000L,
        )

        val restored = reconcileFocusSession(snapshot, nowMillis = 1_030_000L)

        assertEquals(450L, restored.timeLeftSeconds)
        assertEquals(90L, restored.timeSpentSeconds)
        assertEquals(1_030_000L, restored.lastTickTimeMillis)
    }

    @Test
    fun `paused session does not advance while app is closed`() {
        val snapshot = runningSnapshot(
            isPaused = true,
            timeLeftSeconds = 480L,
            timeSpentSeconds = 60L,
            lastTickTimeMillis = 1_000_000L,
        )

        assertEquals(snapshot, reconcileFocusSession(snapshot, nowMillis = 1_900_000L))
        assertEquals(snapshot, reconcileFocusSession(snapshot, nowMillis = 900_000L))
    }

    @Test
    fun `notification pause accounts for elapsed time before freezing session`() {
        val snapshot = runningSnapshot(
            timeLeftSeconds = 300L,
            timeSpentSeconds = 30L,
            lastTickTimeMillis = 1_000_000L,
        )

        val paused = applyFocusSessionControl(
            snapshot = snapshot,
            action = FocusSessionControlAction.Pause,
            nowMillis = 1_025_000L,
        )

        assertTrue(paused.isPaused)
        assertEquals(275L, paused.timeLeftSeconds)
        assertEquals(55L, paused.timeSpentSeconds)
        assertEquals(1_025_000L, paused.lastTickTimeMillis)
    }

    @Test
    fun `notification resume starts from current time without adding paused time`() {
        val snapshot = runningSnapshot(
            isPaused = true,
            timeLeftSeconds = 275L,
            timeSpentSeconds = 55L,
            lastTickTimeMillis = 1_025_000L,
        )

        val resumed = applyFocusSessionControl(
            snapshot = snapshot,
            action = FocusSessionControlAction.Resume,
            nowMillis = 1_900_000L,
        )

        assertFalse(resumed.isPaused)
        assertEquals(275L, resumed.timeLeftSeconds)
        assertEquals(55L, resumed.timeSpentSeconds)
        assertEquals(1_900_000L, resumed.lastTickTimeMillis)
    }

    @Test
    fun `finish request is persisted and consumed only once`() {
        assertFalse(store.requestFinish())
        store.write(runningSnapshot())

        assertTrue(store.requestFinish())
        assertTrue(store.consumeFinishRequest())
        assertFalse(store.consumeFinishRequest())
    }

    @Test
    fun `finish request completes a restored session once`() = runBlocking {
        store.write(
            runningSnapshot(
                isPaused = true,
                bossId = null,
                timeLeftSeconds = 40L,
                timeSpentSeconds = 20L,
            ),
        )
        assertTrue(store.requestFinish())

        val viewModel = BattleViewModel(repository, context, store) { 1_000_000L }
        battleViewModel = viewModel

        waitForCondition { if (!viewModel.isBattleActive) true else null }
        val sessions = waitForCondition {
            repository.allSessions.first().takeIf { it.size == 1 }
        }
        assertEquals(20L, sessions.single().durationSeconds)
        assertTrue(sessions.single().wasCompleted)
        assertFalse(store.consumeFinishRequest())
    }

    @Test
    fun `notification title uses the most specific session label`() {
        assertEquals(
            "Solve wave equations",
            runningSnapshot().copy(
                bossTitle = "Electromagnetics",
                bossStepTitle = "Solve wave equations",
            ).displayTitle(),
        )
        assertEquals(
            "Review calculus",
            runningSnapshot(bossId = null).copy(
                skillTitle = "Mathematics",
                dailyQuestTitle = "Review calculus",
            ).displayTitle(),
        )
        assertEquals("Free study", runningSnapshot(bossId = null).displayTitle())
    }

    @Test
    fun `battle view model restores boss progress using offline elapsed time`() = runBlocking {
        val boss = BossEntity(
            id = 42,
            name = "Reliability Trial",
            difficulty = "Medium",
            requiredMinutes = 10,
            timeSpentSeconds = 120L,
        )
        repository.insertBoss(boss)
        store.write(
            runningSnapshot(
                bossId = boss.id,
                initialBossTimeSpentSeconds = 120L,
                timeLeftSeconds = 480L,
                timeSpentSeconds = 60L,
                lastTickTimeMillis = 1_000_000L,
            ),
        )

        val nowMillis = 1_030_000L
        val viewModel = BattleViewModel(repository, context, store) { nowMillis }
        battleViewModel = viewModel

        waitForCondition {
            if (
                viewModel.isBattleActive &&
                viewModel.battleTimeLeftSeconds == 450L &&
                viewModel.battleTimeSpentSeconds == 90L
            ) {
                true
            } else {
                null
            }
        }

        val restoredBoss = waitForCondition {
            repository.getBossById(boss.id)?.takeIf { it.timeSpentSeconds == 210L }
        }
        assertEquals(210L, restoredBoss.timeSpentSeconds)
        assertFalse(viewModel.isBattlePaused)
    }

    @Test
    fun `repeated completion requests create one study session`() = runBlocking {
        var nowMillis = 2_000_000L
        val viewModel = BattleViewModel(repository, context, store) { nowMillis }
        battleViewModel = viewModel
        viewModel.selectAndStartFreeStudy(minutes = 1)

        waitForCondition { if (viewModel.isBattleActive) true else null }
        viewModel.simulateStudySeconds(10L)
        waitForCondition { if (viewModel.battleTimeSpentSeconds == 10L) true else null }

        nowMillis += 1_000L
        viewModel.completeActiveBoss()
        viewModel.completeActiveBoss()
        viewModel.completeActiveBoss()

        waitForCondition { if (!viewModel.isBattleActive) true else null }
        val sessions = waitForCondition {
            repository.allSessions.first().takeIf { it.size == 1 }
        }
        val profile = waitForCondition {
            repository.getProfileSync()?.takeIf { it.totalSessionCount == 1 }
        }

        assertEquals(1, sessions.size)
        assertEquals(1, profile.totalSessionCount)
        assertTrue(sessions.single().wasCompleted)
        val summary = viewModel.sessionSummary ?: error("Session summary was not created")
        assertEquals(sessions.single().id.toLong(), summary.sessionId)
        assertEquals(sessions.single().durationSeconds, summary.durationSeconds)
        assertEquals(sessions.single().xpEarned, summary.xpEarned)
        assertEquals(SessionEndState.Completed, summary.endState)

        viewModel.dismissSessionSummary()
        assertNull(viewModel.sessionSummary)
    }

    @Test
    fun `session history stores the same rewards added to the profile`() = runBlocking {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        repository.insertOrUpdateProfile(
            UserProfileEntity(
                currentStreak = 1,
                longestStreak = 1,
                lastStudyDate = today,
                redDungeonDays = 1,
                isRedDungeonBoostActive = true,
            ),
        )
        val boss = BossEntity(
            id = 73,
            name = "Reward Consistency Trial",
            difficulty = "Medium",
            requiredMinutes = 1,
        )
        repository.insertBoss(boss)
        val viewModel = BattleViewModel(repository, context, store) { 4_000_000L }
        battleViewModel = viewModel

        viewModel.selectAndStartBattle(boss)
        waitForCondition { if (viewModel.isBattleActive) true else null }
        viewModel.simulateStudySeconds(60L)
        waitForCondition { if (!viewModel.isBattleActive) true else null }

        val session = repository.allSessions.first().single()
        val profile = repository.getProfileSync() ?: error("Profile was not saved")

        assertEquals(270, session.xpEarned)
        assertEquals(122, session.goldEarned)
        assertEquals(session.xpEarned, profile.totalXpEarned)
        assertEquals(session.goldEarned, profile.totalGoldEarned - 100)
        assertEquals(222, profile.gold)
        val summary = viewModel.sessionSummary ?: error("Session summary was not created")
        assertEquals("Reward Consistency Trial", summary.subject)
        assertEquals(270, summary.xpEarned)
        assertEquals(122, summary.goldEarned)
        assertEquals(1, summary.previousLevel)
        assertEquals(2, summary.currentLevel)
        assertEquals(0f, summary.bossProgress?.progressBefore)
        assertEquals(1f, summary.bossProgress?.progressAfter)
    }

    @Test
    fun `suspended study creates a saved session summary`() = runBlocking {
        val viewModel = BattleViewModel(repository, context, store) { 6_000_000L }
        battleViewModel = viewModel
        viewModel.selectAndStartFreeStudy(minutes = 1)
        waitForCondition { if (viewModel.isBattleActive) true else null }

        viewModel.simulateStudySeconds(12L)
        waitForCondition { if (viewModel.battleTimeSpentSeconds == 12L) true else null }
        viewModel.suspendCurrentSession()

        val summary = viewModel.sessionSummary ?: error("Session summary was not created")
        val session = repository.allSessions.first().single()
        assertEquals(SessionEndState.Suspended, summary.endState)
        assertEquals("Astral Free Study", summary.subject)
        assertEquals(12L, summary.durationSeconds)
        assertEquals(session.id.toLong(), summary.sessionId)
        assertFalse(session.wasCompleted)
        assertFalse(viewModel.isBattleActive)
    }

    @Test
    fun `finishing the full quest timer completes the daily quest`() = runBlocking {
        val questId = repository.insertDailyQuest(
            DailyQuestEntity(
                title = "Review electromagnetics",
                durationMinutes = 1,
                scheduledDate = "2026-09-01",
            ),
        ).toInt()
        val quest = repository.getDailyQuestById(questId) ?: error("Quest was not saved")
        val viewModel = BattleViewModel(repository, context, store) { 7_000_000L }
        battleViewModel = viewModel

        viewModel.selectAndStartDailyQuest(quest)
        waitForCondition { if (viewModel.isBattleActive) true else null }
        viewModel.simulateStudySeconds(60L)
        waitForCondition { if (!viewModel.isBattleActive) true else null }

        val completed = repository.getDailyQuestById(questId) ?: error("Quest disappeared")
        assertTrue(completed.isCompleted)
        assertEquals("Review electromagnetics", viewModel.sessionSummary?.subject)
        assertEquals("Review electromagnetics", repository.allSessions.first().single().bossName)
    }

    @Test
    fun `finishing a quest early keeps it pending`() = runBlocking {
        val questId = repository.insertDailyQuest(
            DailyQuestEntity(
                title = "Read chapter four",
                durationMinutes = 1,
                scheduledDate = "2026-09-01",
            ),
        ).toInt()
        val quest = repository.getDailyQuestById(questId) ?: error("Quest was not saved")
        val viewModel = BattleViewModel(repository, context, store) { 8_000_000L }
        battleViewModel = viewModel

        viewModel.selectAndStartDailyQuest(quest)
        waitForCondition { if (viewModel.isBattleActive) true else null }
        viewModel.simulateStudySeconds(10L)
        waitForCondition { if (viewModel.battleTimeSpentSeconds == 10L) true else null }
        viewModel.completeActiveBoss()
        waitForCondition { if (!viewModel.isBattleActive) true else null }

        val pending = repository.getDailyQuestById(questId) ?: error("Quest disappeared")
        assertFalse(pending.isCompleted)
    }

    @Test
    fun `finishing a study step completes it and adds time to its boss`() = runBlocking {
        val bossId = repository.insertBoss(
            BossEntity(
                name = "Physics Exam",
                difficulty = "Hard",
                requiredMinutes = 120,
            ),
        ).toInt()
        val boss = repository.getBossById(bossId) ?: error("Boss was not saved")
        val stepId = repository.insertBossStep(
            BossStepEntity(
                bossId = bossId,
                title = "Review electromagnetic waves",
                estimatedMinutes = 1,
                sortOrder = 0,
            ),
        ).toInt()
        val step = repository.getBossStepById(stepId) ?: error("Step was not saved")
        val viewModel = BattleViewModel(repository, context, store) { 9_000_000L }
        battleViewModel = viewModel

        viewModel.selectAndStartBossStep(boss, step)
        waitForCondition { if (viewModel.isBattleActive) true else null }
        assertEquals(stepId, viewModel.activeBossStepId)
        assertEquals(60L, viewModel.battleTimeLeftSeconds)

        viewModel.simulateStudySeconds(60L)
        waitForCondition { if (!viewModel.isBattleActive) true else null }

        val completedStep = repository.getBossStepById(stepId) ?: error("Step disappeared")
        val updatedBoss = repository.getBossById(bossId) ?: error("Boss disappeared")
        assertTrue(completedStep.isCompleted)
        assertEquals(60L, updatedBoss.timeSpentSeconds)
        assertFalse(updatedBoss.isCompleted)
        assertEquals("Review electromagnetic waves", viewModel.sessionSummary?.subject)
        assertEquals(
            "Physics Exam: Review electromagnetic waves",
            repository.allSessions.first().single().bossName,
        )
    }

    @Test
    fun `ending a study step early leaves it pending`() = runBlocking {
        val bossId = repository.insertBoss(
            BossEntity(
                name = "Calculus Exam",
                difficulty = "Medium",
                requiredMinutes = 90,
            ),
        ).toInt()
        val boss = repository.getBossById(bossId) ?: error("Boss was not saved")
        val stepId = repository.insertBossStep(
            BossStepEntity(
                bossId = bossId,
                title = "Solve integration exercises",
                estimatedMinutes = 10,
                sortOrder = 0,
            ),
        ).toInt()
        val step = repository.getBossStepById(stepId) ?: error("Step was not saved")
        val viewModel = BattleViewModel(repository, context, store) { 10_000_000L }
        battleViewModel = viewModel

        viewModel.selectAndStartBossStep(boss, step)
        waitForCondition { if (viewModel.isBattleActive) true else null }
        viewModel.simulateStudySeconds(20L)
        waitForCondition { if (viewModel.battleTimeSpentSeconds == 20L) true else null }
        viewModel.endBossStepEarly()
        waitForCondition { if (!viewModel.isBattleActive) true else null }

        val pendingStep = repository.getBossStepById(stepId) ?: error("Step disappeared")
        assertFalse(pendingStep.isCompleted)
        assertEquals(20L, repository.getBossById(bossId)?.timeSpentSeconds)
        assertFalse(repository.allSessions.first().single().wasCompleted)
        assertEquals(SessionEndState.Suspended, viewModel.sessionSummary?.endState)
    }

    @Test
    fun `streak milestone reward is granted only once per day`() = runBlocking {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        repository.insertOrUpdateProfile(
            UserProfileEntity(
                currentStreak = 2,
                longestStreak = 2,
                lastStudyDate = yesterday,
            ),
        )
        val viewModel = BattleViewModel(repository, context, store) { 5_000_000L }
        battleViewModel = viewModel

        repeat(2) {
            viewModel.selectAndStartFreeStudy(minutes = 1)
            waitForCondition { if (viewModel.isBattleActive) true else null }
            viewModel.simulateStudySeconds(10L)
            waitForCondition { if (viewModel.battleTimeSpentSeconds == 10L) true else null }
            viewModel.completeActiveBoss()
            waitForCondition { if (!viewModel.isBattleActive) true else null }
            Unit
        }

        val sessions = repository.allSessions.first()
        val profile = repository.getProfileSync() ?: error("Profile was not saved")

        assertEquals(listOf(1, 51), sessions.map { it.xpEarned }.sorted())
        assertEquals(52, sessions.sumOf { it.xpEarned })
        assertEquals(52, profile.totalXpEarned)
        assertEquals(3, profile.currentStreak)
        assertEquals(2, profile.totalSessionCount)
    }

    @Test
    fun `pause callback runs after latest elapsed time is saved`() = runBlocking {
        var nowMillis = 3_000_000L
        val viewModel = BattleViewModel(repository, context, store) { nowMillis }
        battleViewModel = viewModel
        viewModel.selectAndStartFreeStudy(minutes = 1)
        waitForCondition { if (viewModel.isBattleActive) true else null }

        nowMillis += 5_000L
        var savedAtCallback: FocusSessionSnapshot? = null
        viewModel.pauseBattle {
            savedAtCallback = store.read()
        }

        val snapshot = waitForCondition { savedAtCallback }
        assertTrue(snapshot.isPaused)
        assertEquals(55L, snapshot.timeLeftSeconds)
        assertEquals(5L, snapshot.timeSpentSeconds)
    }

    private fun runningSnapshot(
        isPaused: Boolean = false,
        timeLeftSeconds: Long = 300L,
        timeSpentSeconds: Long = 30L,
        initialBossTimeSpentSeconds: Long = 0L,
        lastTickTimeMillis: Long = 1_000_000L,
        bossId: Int? = 42,
    ) = FocusSessionSnapshot(
        isActive = true,
        isFreeStudy = bossId == null,
        isPaused = isPaused,
        timeLeftSeconds = timeLeftSeconds,
        timeSpentSeconds = timeSpentSeconds,
        initialBossTimeSpentSeconds = initialBossTimeSpentSeconds,
        lastTickTimeMillis = lastTickTimeMillis,
        bossId = bossId,
        skillId = null,
    )

    private suspend fun <T> waitForCondition(
        timeoutMs: Long = 3_000L,
        condition: suspend () -> T?,
    ): T {
        val startedAt = System.currentTimeMillis()
        var result = condition()
        while (result == null && System.currentTimeMillis() - startedAt < timeoutMs) {
            delay(25L)
            ShadowLooper.idleMainLooper()
            result = condition()
        }
        return result ?: throw AssertionError("Condition not met within $timeoutMs ms")
    }
}
