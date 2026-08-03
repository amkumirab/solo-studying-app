package com.amkumirab.solostudying

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.ui.viewmodel.BattleViewModel
import com.amkumirab.solostudying.ui.viewmodel.StatusViewModel
import com.amkumirab.solostudying.ui.viewmodel.TutorialViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CoreGameplayValidationTest {

    private companion object {
        const val BATTLE_PREFERENCES = "solo_studying_battle_prefs"
    }

    private lateinit var db: SoloStudyingDatabase
    private lateinit var repository: SoloStudyingRepository
    private lateinit var context: Context
    private lateinit var statusViewModel: StatusViewModel
    private lateinit var battleViewModel: BattleViewModel

    private suspend fun <T> waitForCondition(
        timeoutMs: Long = 3000,
        condition: suspend () -> T?
    ): T {
        val start = System.currentTimeMillis()
        var result = condition()
        while (result == null && System.currentTimeMillis() - start < timeoutMs) {
            kotlinx.coroutines.delay(50)
            ShadowLooper.idleMainLooper()
            result = condition()
        }
        return result ?: throw AssertionError("Condition not met within $timeoutMs ms")
    }

    @Before
    fun setup() {
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            context.getSharedPreferences(BATTLE_PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
            db = Room.inMemoryDatabaseBuilder(context, SoloStudyingDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            repository = SoloStudyingRepository(db.soloStudyingDao())

            // Initialize the shared profile before ViewModels start their asynchronous setup.
            repository.insertOrUpdateProfile(UserProfileEntity())
            waitForCondition {
                repository.getProfileSync()
            }

            statusViewModel = StatusViewModel(repository, context)
            battleViewModel = BattleViewModel(repository, context)
        }
    }

    @After
    fun tearDown() {
        if (::battleViewModel.isInitialized) {
            battleViewModel.pauseBattle()
        }
        if (::context.isInitialized) {
            context.getSharedPreferences(BATTLE_PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
        if (::db.isInitialized) {
            db.close()
        }
    }

    @Test
    fun testOnboardingPersistence() {
        runBlocking {
            // Complete detailed onboarding
            statusViewModel.finishOnboarding(
                name = "Test Champion",
                hunterClass = "Academic Sage",
                mainGoal = "Master a Language",
                learningPath = "Balanced Mystic Path",
                scheduleDays = "Tue,Thu,Sat",
                scheduleMinutes = 60,
                scheduleFlexibility = "High",
                weekdayMinutes = "60,60,60,60,60,60,60"
            )
            
            // Wait until onboarding is completed in the database
            val profile = waitForCondition {
                val p = repository.getProfileSync()
                if (p != null && p.hasCompletedOnboarding) p else null
            }

            assertEquals("Test Champion", profile.name)
            assertEquals("Academic Sage", profile.hunterClass)
            assertEquals("Master a Language", profile.mainGoal)
            assertEquals("Balanced Mystic Path", profile.learningPath)
            assertEquals("Tue,Thu,Sat", profile.scheduleDays)
            assertEquals(60, profile.scheduleMinutesPerDay)
            assertEquals("High", profile.scheduleFlexibility)
            assertEquals("60,60,60,60,60,60,60", profile.scheduleWeekdayMinutes)
            assertTrue(profile.hasCompletedOnboarding)
        }
    }

    @Test
    fun testCosmeticArchetypes() {
        // Create profiles with different classes and complete study
        // Verify they yield exact identical base calculations
        val shadowProfile = UserProfileEntity(hunterClass = "Shadow Monarch")
        val sageProfile = UserProfileEntity(hunterClass = "Academic Sage")

        assertEquals(0, shadowProfile.xp)
        assertEquals(100, shadowProfile.gold)
        assertEquals(0, sageProfile.xp)
        assertEquals(100, sageProfile.gold)
    }

    @Test
    fun testLevelProgressionAndXpGoldCalculation() {
        runBlocking {
            // Start with clean slate
            val profile = UserProfileEntity(
                level = 1,
                xp = 50,
                gold = 100,
                currentStreak = 0,
                lastStudyDate = null
            )
            repository.insertOrUpdateProfile(profile)
            
            waitForCondition {
                val p = repository.getProfileSync()
                if (p != null && p.xp == 50) p else null
            }

            // Add a boss
            val boss = BossEntity(
                id = 100,
                name = "Calculus Midterm",
                difficulty = "Medium", // Medium yields 150 XP, 80 Gold
                requiredMinutes = 30
            )
            repository.insertBoss(boss)
            
            waitForCondition {
                repository.getBossById(100)
            }

            // Start battle and complete active boss
            battleViewModel.selectAndStartBattle(boss)

            waitForCondition {
                if (battleViewModel.isBattleActive) true else null
            }

            // Simulating complete session directly
            // Medium Boss gives 150 XP, 80 Gold.
            // Starting at 50 XP, Level 1.
            // Level up threshold for Lvl 1 is 1 * 150 = 150 XP.
            // Adding 150 XP makes it 200 XP -> Level ups to Lvl 2, leaving 50 XP.
            // Gold: 100 base + 80 reward + 50 Level up bonus = 230 Gold.
            battleViewModel.simulateStudySeconds(30 * 60) // Completes boss

            // Wait until level progression is updated in the database
            val updatedProfile = waitForCondition {
                val p = repository.getProfileSync()
                if (p != null && p.level == 2) p else null
            }

            assertEquals(2, updatedProfile.level)
            assertEquals(50, updatedProfile.xp)
            assertEquals(230, updatedProfile.gold)
        }
    }

    @Test
    fun testBossHpReduction() {
        runBlocking {
            // Add a boss with 45 minutes required
            val boss = BossEntity(
                id = 200,
                name = "Organic Chemistry exam",
                difficulty = "Hard",
                requiredMinutes = 45
            )
            repository.insertBoss(boss)
            
            waitForCondition {
                repository.getBossById(200)
            }

            // Select and start battle
            battleViewModel.selectAndStartBattle(boss)

            waitForCondition {
                if (battleViewModel.isBattleActive) true else null
            }

            // Simulate partial focus study: 15 minutes (900 seconds)
            battleViewModel.simulateStudySeconds(15 * 60)

            // Wait until boss hp is reduced in the database
            val updatedBoss = waitForCondition {
                val b = repository.getBossById(200)
                if (b != null && b.timeSpentSeconds == 900L) b else null
            }

            assertEquals(900L, updatedBoss.timeSpentSeconds)
            assertFalse(updatedBoss.isCompleted)
        }
    }

    @Test
    fun testRedDungeonTriggerLogic() {
        runBlocking {
            // Setup profile study on a previous date
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -3) // Missed 3 days of studying
            val missedDaysDate = sdf.format(cal.time)

            val profile = UserProfileEntity(
                currentStreak = 5,
                lastStudyDate = missedDaysDate,
                gold = 100,
                redDungeonDays = 0
            )
            repository.insertOrUpdateProfile(profile)
            
            waitForCondition {
                val p = repository.getProfileSync()
                if (p != null && p.lastStudyDate == missedDaysDate) p else null
            }

            // Instantiate new StatusViewModel to trigger streak check on startup
            val testStatusVM = StatusViewModel(repository, context)

            // Wait until the streak resets in the database
            val updatedProfile = waitForCondition {
                val p = repository.getProfileSync()
                if (p != null && p.currentStreak == 0 && p.redDungeonDays == 3) p else null
            }

            assertEquals(0, updatedProfile.currentStreak)
            assertEquals(75, updatedProfile.gold) // Lost 25 Gold
            // Penalty cap for redDungeonDays is 3
            assertEquals(3, updatedProfile.redDungeonDays)
        }
    }

    @Test
    fun testTutorialCompleteFlowAndFirstMissionWizard() {
        runBlocking {
            // Instantiate TutorialViewModel
            val tutorialViewModel = TutorialViewModel(repository, context)

            // Verify initial state
            val initialState = tutorialViewModel.uiState.first()
            assertEquals(1, initialState.currentStep)
            assertFalse(initialState.isCompleted)

            // Let's modify the step 11 wizard inputs
            tutorialViewModel.updateDungeonName("Sorcery Academy")
            tutorialViewModel.updateBossName("Summoning Exam")
            tutorialViewModel.updateEstimatedHours(15)
            tutorialViewModel.updateSelectedSkill("Deep Learning Sage")
            tutorialViewModel.updateScheduleMinutes(90)

            // Complete step 11 -> creates first mission in db and rewards user
            tutorialViewModel.createFirstMissionAndCelebrate()

            // Verify step has advanced to celebration
            val stateAfterMission = waitForCondition {
                val s = tutorialViewModel.uiState.value
                if (s.currentStep == 12) s else null
            }

            // Let's check if the first Dungeon, Boss, and Skill are inserted
            val dungeons = repository.allDungeons.first()
            val bosses = repository.allBosses.first()
            val skills = repository.allSkills.first()

            val addedDungeon = dungeons.find { it.name == "Sorcery Academy" }
            assertNotNull("Dungeon should be created", addedDungeon)
            assertEquals("Newborn Hunter", addedDungeon?.unlockedTitle)

            val addedBoss = bosses.find { it.name == "Summoning Exam" }
            assertNotNull("Boss should be created", addedBoss)
            assertEquals(15 * 60, addedBoss?.requiredMinutes)

            // Let's verify the updated user profile rewards and schedule
            val profile = waitForCondition {
                val p = repository.getProfileSync()
                if (p != null && p.gold == 150) p else null // Base 100 + 50 reward
            }

            assertEquals(150, profile.gold)
            assertEquals(100, profile.xp) // Base 0 + 100 reward
            assertTrue(profile.hasCompletedOnboarding)
            assertEquals(90, profile.scheduleMinutesPerDay)
            assertTrue(profile.scheduleDays.contains("Mon"))
            assertTrue(profile.scheduleDays.contains("Wed"))
            assertTrue(profile.scheduleDays.contains("Fri"))

            // Move to final step completion
            tutorialViewModel.completeTutorial()

            val finalState = waitForCondition {
                val s = tutorialViewModel.uiState.value
                if (s.isCompleted) s else null
            }

            val finalProfile = waitForCondition {
                val p = repository.getProfileSync()
                if (p != null && p.hasCompletedTutorial) p else null
            }
            assertTrue(finalProfile.hasCompletedTutorial)
        }
    }
}
