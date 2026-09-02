package com.amkumirab.solostudying.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.RewardBalanceEntity
import com.amkumirab.solostudying.data.entity.RewardItemEntity
import com.amkumirab.solostudying.data.entity.SkillEntity
import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.data.repository.RewardPurchaseStatus
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionIntegrityTest {

    private lateinit var database: SoloStudyingDatabase
    private lateinit var repository: SoloStudyingRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoloStudyingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SoloStudyingRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `failed progression transaction rolls back every record`() = runBlocking {
        val profile = UserProfileEntity(gold = 100, xp = 20)
        val boss = BossEntity(
            id = 41,
            name = "Transaction Boss",
            difficulty = "Medium",
            requiredMinutes = 30,
        )
        val skill = SkillEntity(
            id = 17,
            name = "Transaction Skill",
            targetMinutes = 60,
        )
        repository.insertOrUpdateProfile(profile)
        repository.insertBoss(boss)
        repository.insertSkill(skill)

        val failure = runCatching {
            repository.runInTransaction {
                updateBoss(boss.copy(isCompleted = true, timeSpentSeconds = 1_800))
                updateSkill(skill.copy(spentSeconds = 1_800))
                insertSession(
                    StudySessionEntity(
                        bossId = boss.id,
                        bossName = boss.name,
                        durationSeconds = 1_800,
                        xpEarned = 150,
                        goldEarned = 80,
                        wasCompleted = true,
                    ),
                )
                insertOrUpdateProfile(profile.copy(gold = 180, xp = 170))
                error("Force rollback after all progression writes")
            }
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals(profile, repository.getProfileSync())
        assertEquals(boss, repository.getBossById(boss.id))
        assertEquals(skill, repository.getSkillById(skill.id))
        assertTrue(repository.allSessions.first().isEmpty())
    }

    @Test
    fun `concurrent reward purchases cannot overspend gold`() = runBlocking {
        repository.insertOrUpdateProfile(UserProfileEntity(gold = 100))
        val reward = RewardItemEntity(
            name = "Movie Night",
            description = "One movie",
            cost = 80,
            rewardType = "One-Time",
        )

        val results = coroutineScope {
            List(2) { async { repository.purchaseReward(reward) } }.awaitAll()
        }

        assertEquals(1, results.count { it == RewardPurchaseStatus.Purchased })
        assertEquals(1, results.count { it == RewardPurchaseStatus.InsufficientGold })
        assertEquals(20, repository.getProfileSync()?.gold)

        val balance = repository.getBalanceByName(reward.name)
        assertEquals(1, balance?.purchaseCount)
        assertEquals(1f, balance?.availableHours ?: 0f, 0f)
        assertFalse(results.contains(RewardPurchaseStatus.MissingProfile))
    }

    @Test
    fun `concurrent reward use cannot consume the same balance twice`() = runBlocking {
        repository.insertOrUpdateBalance(
            RewardBalanceEntity(
                rewardName = "Gaming Time",
                availableHours = 1f,
                purchaseCount = 1,
            ),
        )

        val results = coroutineScope {
            List(2) { async { repository.useReward("Gaming Time", 0.75f) } }.awaitAll()
        }

        assertEquals(1, results.count { it })
        assertEquals(1, results.count { !it })
        assertEquals(0.25f, repository.getBalanceByName("Gaming Time")?.availableHours ?: 0f, 0f)
    }

    @Test
    fun `session notes are normalized updated and removable`() = runBlocking {
        val sessionId = repository.insertSession(
            StudySessionEntity(
                bossId = null,
                bossName = "Physics Review",
                durationSeconds = 1_500,
                xpEarned = 40,
                goldEarned = 15,
                wasCompleted = true,
            ),
        )

        assertTrue(repository.updateSessionNote(sessionId, "  Review wave equations next time.  "))
        assertEquals(
            "Review wave equations next time.",
            repository.allSessions.first().single().note,
        )

        assertTrue(repository.updateSessionNote(sessionId, "   "))
        assertEquals(null, repository.allSessions.first().single().note)
        assertFalse(repository.updateSessionNote(-1L, "Invalid session"))
    }
}
