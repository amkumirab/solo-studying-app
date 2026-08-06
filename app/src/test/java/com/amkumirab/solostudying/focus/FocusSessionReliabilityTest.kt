package com.amkumirab.solostudying.focus

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
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
        repository = SoloStudyingRepository(database.soloStudyingDao())
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
