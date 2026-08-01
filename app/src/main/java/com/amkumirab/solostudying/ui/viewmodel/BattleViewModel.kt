package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.sound.RpgSoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BattleViewModel(
    private val repository: SoloStudyingRepository,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("solo_studying_battle_prefs", Context.MODE_PRIVATE)

    // --- Active Battle States ---
    var activeBoss by mutableStateOf<BossEntity?>(null)
        private set

    var isBattleActive by mutableStateOf(false)
        private set

    var isBattlePaused by mutableStateOf(false)
        private set

    var isFreeStudyActive by mutableStateOf(false)
        private set

    var selectedSkillToTrain by mutableStateOf<SkillEntity?>(null)

    var battleTimeLeftSeconds by mutableStateOf(0L)
        private set

    var battleTimeSpentSeconds by mutableStateOf(0L)
        private set

    private var initialBossTimeSpent: Long = 0
    private var lastTickTimeMillis: Long = 0
    private var timerJob: Job? = null

    // For level ups or streak updates that need to be triggered from BattleViewModel
    var showStreakResetToast by mutableStateOf<String?>(null)
    var showLevelUpToast by mutableStateOf<Pair<Int, Int>?>(null)
    var showPenaltyToast by mutableStateOf<String?>(null)

    init {
        restoreSavedFocusSession()
    }

    private fun saveFocusSessionState() {
        prefs.edit().apply {
            putBoolean("session_battle_active", isBattleActive)
            putBoolean("session_free_active", isFreeStudyActive)
            putBoolean("session_battle_paused", isBattlePaused)
            putLong("session_time_left", battleTimeLeftSeconds)
            putLong("session_time_spent", battleTimeSpentSeconds)
            putLong("session_boss_spent_initial", initialBossTimeSpent)
            putLong("session_last_tick", lastTickTimeMillis)
            putInt("session_boss_id", activeBoss?.id ?: -1)
            putInt("session_skill_id", selectedSkillToTrain?.id ?: -1)
            apply()
        }
    }

    private fun clearFocusSessionState() {
        prefs.edit().apply {
            remove("session_battle_active")
            remove("session_free_active")
            remove("session_battle_paused")
            remove("session_time_left")
            remove("session_time_spent")
            remove("session_boss_spent_initial")
            remove("session_last_tick")
            remove("session_boss_id")
            remove("session_skill_id")
            apply()
        }
    }

    private fun restoreSavedFocusSession() {
        viewModelScope.launch {
            val savedBattleActive = prefs.getBoolean("session_battle_active", false)
            val savedFreeActive = prefs.getBoolean("session_free_active", false)
            if (!savedBattleActive && !savedFreeActive) return@launch

            val savedPaused = prefs.getBoolean("session_battle_paused", false)
            val savedTimeLeftSec = prefs.getLong("session_time_left", 0L)
            val savedTimeSpentSec = prefs.getLong("session_time_spent", 0L)
            val savedBossSpentInitial = prefs.getLong("session_boss_spent_initial", 0L)
            val savedLastTickMillis = prefs.getLong("session_last_tick", 0L)
            val savedBossId = prefs.getInt("session_boss_id", -1)
            val savedSkillId = prefs.getInt("session_skill_id", -1)

            if (savedBossId != -1) {
                activeBoss = repository.getBossById(savedBossId)
            }
            if (savedSkillId != -1) {
                selectedSkillToTrain = repository.getSkillById(savedSkillId)
            }

            initialBossTimeSpent = savedBossSpentInitial
            isFreeStudyActive = savedFreeActive
            isBattleActive = true
            isBattlePaused = savedPaused

            // Compute offline elapsed focus seconds
            if (!savedPaused && savedLastTickMillis > 0) {
                val now = System.currentTimeMillis()
                val elapsedSeconds = (now - savedLastTickMillis) / 1000L
                if (elapsedSeconds > 0) {
                    val actualSub = minOf(elapsedSeconds, savedTimeLeftSec)
                    battleTimeLeftSeconds = savedTimeLeftSec - actualSub
                    battleTimeSpentSeconds = savedTimeSpentSec + actualSub
                    
                    if (actualSub > 0 && activeBoss != null) {
                        saveIncrementalBossProgress()
                    }
                } else {
                    battleTimeLeftSeconds = savedTimeLeftSec
                    battleTimeSpentSeconds = savedTimeSpentSec
                }
            } else {
                battleTimeLeftSeconds = savedTimeLeftSec
                battleTimeSpentSeconds = savedTimeSpentSec
            }

            if (battleTimeLeftSeconds > 0) {
                if (!isBattlePaused) {
                    startTimer()
                }
            } else {
                completeActiveBoss()
            }
        }
    }

    fun selectAndStartBattle(boss: BossEntity) {
        viewModelScope.launch {
            if (isBattleActive) {
                suspendCurrentSession(applyHeavyPenalty = false)
            }
            activeBoss = boss
            if (boss.isCompleted) {
                initialBossTimeSpent = 0L
                battleTimeLeftSeconds = boss.requiredMinutes * 60L
            } else {
                initialBossTimeSpent = boss.timeSpentSeconds
                val totalRequiredSeconds = boss.requiredMinutes * 60L
                battleTimeLeftSeconds = maxOf(0L, totalRequiredSeconds - boss.timeSpentSeconds)
            }
            battleTimeSpentSeconds = 0L
            isBattleActive = true
            isBattlePaused = false
            saveFocusSessionState()
            startTimer()
        }
    }

    fun selectAndStartFreeStudy(minutes: Int) {
        viewModelScope.launch {
            if (isBattleActive) {
                suspendCurrentSession(applyHeavyPenalty = false)
            }
            activeBoss = null
            battleTimeLeftSeconds = minutes * 60L
            battleTimeSpentSeconds = 0L
            initialBossTimeSpent = 0L
            isFreeStudyActive = true
            isBattleActive = true
            isBattlePaused = false
            saveFocusSessionState()
            startTimer()
        }
    }

    fun pauseBattle() {
        if (!isBattleActive || isBattlePaused) return
        isBattlePaused = true
        timerJob?.cancel()
        saveFocusSessionState()
        RpgSoundManager.playPauseStudySound()
    }

    fun resumeBattle() {
        if (!isBattleActive || !isBattlePaused) return
        isBattlePaused = false
        saveFocusSessionState()
        startTimer()
        RpgSoundManager.playResumeStudySound()
    }

    private fun startTimer() {
        timerJob?.cancel()
        lastTickTimeMillis = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (battleTimeLeftSeconds > 0) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val elapsedMillis = now - lastTickTimeMillis
                val elapsedSeconds = elapsedMillis / 1000
                if (elapsedSeconds > 0) {
                    val actualSub = minOf(elapsedSeconds, battleTimeLeftSeconds)
                    battleTimeLeftSeconds -= actualSub
                    battleTimeSpentSeconds += actualSub
                    lastTickTimeMillis += actualSub * 1000L
                    
                    if (battleTimeLeftSeconds in 1..5) {
                        RpgSoundManager.playWarningAlarmSound()
                    }

                    if (battleTimeSpentSeconds % 10 == 0L || elapsedSeconds >= 10L) {
                        saveIncrementalBossProgress()
                    }
                    saveFocusSessionState()
                }
            }
            completeActiveBoss()
        }
    }

    private suspend fun saveIncrementalBossProgress() {
        val boss = activeBoss ?: return
        val updatedBoss = boss.copy(
            timeSpentSeconds = initialBossTimeSpent + battleTimeSpentSeconds
        )
        activeBoss = updatedBoss
        repository.updateBoss(updatedBoss)
    }

    fun completeActiveBoss() {
        timerJob?.cancel()
        viewModelScope.launch {
            val finalDuration = battleTimeSpentSeconds
            val xpEarned: Int
            val goldEarned: Int
            val boss = activeBoss

            if (isFreeStudyActive) {
                val minutesStudied = finalDuration / 60f
                xpEarned = (minutesStudied * 1.5f).toInt().coerceAtLeast(1)
                goldEarned = (minutesStudied * 0.8f).toInt()

                repository.insertSession(
                    StudySessionEntity(
                        bossId = null,
                        bossName = "Astral Free Study",
                        durationSeconds = finalDuration,
                        xpEarned = xpEarned,
                        goldEarned = goldEarned,
                        wasCompleted = true,
                        isFreeStudy = true
                    )
                )
            } else if (boss != null) {
                val finishedBoss = boss.copy(
                    timeSpentSeconds = initialBossTimeSpent + finalDuration,
                    isCompleted = true
                )
                repository.updateBoss(finishedBoss)

                val baseRewards = getDifficultyRewards(boss.difficulty)
                xpEarned = baseRewards.xp
                goldEarned = baseRewards.gold

                repository.insertSession(
                    StudySessionEntity(
                        bossId = boss.id,
                        bossName = boss.name,
                        durationSeconds = finalDuration,
                        xpEarned = xpEarned,
                        goldEarned = goldEarned,
                        wasCompleted = true,
                        isFreeStudy = false
                    )
                )
            } else {
                xpEarned = 0
                goldEarned = 0
            }

            // Train selected skill if any
            val skill = selectedSkillToTrain
            if (skill != null && finalDuration > 0) {
                val updatedSpent = skill.spentSeconds + finalDuration
                val isNowUnlocked = updatedSpent >= skill.targetMinutes * 60L
                repository.updateSkill(
                    skill.copy(
                        spentSeconds = updatedSpent,
                        isUnlocked = skill.isUnlocked || isNowUnlocked
                    )
                )
                if (isNowUnlocked && !skill.isUnlocked) {
                    RpgSoundManager.playSkillUnlockSound()
                    showStreakResetToast = "SKILL MASTERED! You have unlocked passive trait [${skill.name.uppercase()}]!"
                }
            }

            updateProfileCompletingSession(
                durationSeconds = finalDuration,
                xpEarned = xpEarned,
                goldEarned = goldEarned,
                studyCompleted = true,
                isFreeStudy = isFreeStudyActive
            )

            // Reset state
            activeBoss = null
            isBattleActive = false
            isBattlePaused = false
            isFreeStudyActive = false
            selectedSkillToTrain = null
            battleTimeLeftSeconds = 0
            battleTimeSpentSeconds = 0
            clearFocusSessionState()
        }
    }

    suspend fun suspendCurrentSession(applyHeavyPenalty: Boolean = false) {
        timerJob?.cancel()
        val finalSpent = battleTimeSpentSeconds
        val boss = activeBoss

        if (isFreeStudyActive) {
            if (finalSpent > 5) {
                val minutesStudied = finalSpent / 60f
                val xpEarned = (minutesStudied * 0.8f).toInt().coerceAtLeast(1)
                val goldEarned = (minutesStudied * 0.4f).toInt()

                repository.insertSession(
                    StudySessionEntity(
                        bossId = null,
                        bossName = "Astral Free Study (Suspended)",
                        durationSeconds = finalSpent,
                        xpEarned = xpEarned,
                        goldEarned = goldEarned,
                        wasCompleted = false,
                        isFreeStudy = true
                    )
                )
                updateProfileCompletingSession(finalSpent, xpEarned, goldEarned, studyCompleted = false, isFreeStudy = true)
            }
        } else if (boss != null) {
            val updatedBoss = boss.copy(
                timeSpentSeconds = initialBossTimeSpent + finalSpent
            )
            repository.updateBoss(updatedBoss)

            if (finalSpent > 5) {
                val minutesStudied = finalSpent / 60f
                val xpEarned = (minutesStudied * 1.5f).toInt().coerceAtLeast(1)
                val goldEarned = (minutesStudied * 0.8f).toInt()

                repository.insertSession(
                    StudySessionEntity(
                        bossId = boss.id,
                        bossName = boss.name,
                        durationSeconds = finalSpent,
                        xpEarned = xpEarned,
                        goldEarned = goldEarned,
                        wasCompleted = false,
                        isFreeStudy = false
                    )
                )
                updateProfileCompletingSession(finalSpent, xpEarned, goldEarned, studyCompleted = false, isFreeStudy = false)
            }

            if (applyHeavyPenalty) {
                applyProcrastinationPenalty()
            }
        }

        // Train selected skill dynamically for partial time spent
        val skill = selectedSkillToTrain
        if (skill != null && finalSpent > 0) {
            val updatedSpent = skill.spentSeconds + finalSpent
            val isNowUnlocked = updatedSpent >= skill.targetMinutes * 60L
            repository.updateSkill(
                skill.copy(
                    spentSeconds = updatedSpent,
                    isUnlocked = skill.isUnlocked || isNowUnlocked
                )
            )
        }

        activeBoss = null
        isBattleActive = false
        isBattlePaused = false
        isFreeStudyActive = false
        selectedSkillToTrain = null
        battleTimeLeftSeconds = 0
        battleTimeSpentSeconds = 0
        clearFocusSessionState()
    }

    fun abandonActiveBoss(applyHeavyPenalty: Boolean = true) {
        viewModelScope.launch {
            suspendCurrentSession(applyHeavyPenalty)
        }
    }

    fun simulateStudySeconds(seconds: Long) {
        if (!isBattleActive) return
        viewModelScope.launch {
            if (seconds >= battleTimeLeftSeconds) {
                battleTimeSpentSeconds += battleTimeLeftSeconds
                battleTimeLeftSeconds = 0
                completeActiveBoss()
            } else {
                battleTimeLeftSeconds -= seconds
                battleTimeSpentSeconds += seconds
                if (!isFreeStudyActive) {
                    saveIncrementalBossProgress()
                }
            }
        }
    }

    private suspend fun applyProcrastinationPenalty() {
        val profile = repository.getProfileSync() ?: return
        val penaltyGold = 30
        val updatedGold = (profile.gold - penaltyGold).coerceAtLeast(0)

        var penaltyText = "Battle Fled! Procrastination penalty: Lost $penaltyGold Gold."
        val currentBalances = repository.allBalances.first()
        for (bal in currentBalances) {
            if (bal.availableHours > 0f) {
                val reduction = bal.availableHours * 0.20f
                val newHrs = (bal.availableHours - reduction).coerceAtLeast(0f)
                repository.insertOrUpdateBalance(bal.copy(availableHours = newHrs))
                penaltyText += " Slid ${bal.rewardName} balance by -20%."
            }
        }

        val updatedProfile = profile.copy(
            gold = updatedGold,
            currentStreak = 0
        )
        repository.insertOrUpdateProfile(updatedProfile)
        showPenaltyToast = penaltyText
    }

    private suspend fun updateProfileCompletingSession(
        durationSeconds: Long,
        xpEarned: Int,
        goldEarned: Int,
        studyCompleted: Boolean,
        isFreeStudy: Boolean = false
    ) {
        val profile = repository.getProfileSync() ?: UserProfileEntity()

        // Progressive Difficulty for Red Gates:
        // Level 1: -10%, Level 2: -20%, Level 3: -30%
        val redDungeonLevel = profile.redDungeonDays
        val penaltyMultiplier = when (redDungeonLevel) {
            1 -> 0.9f
            2 -> 0.8f
            3 -> 0.7f
            else -> 1.0f
        }

        val xpBoostMultiplier = if (profile.redDungeonDays > 0 && profile.isRedDungeonBoostActive) 2.0f else 1.0f
        val finalXp = (xpEarned * penaltyMultiplier * xpBoostMultiplier).toInt().coerceAtLeast(if (xpEarned > 0) 1 else 0)
        val finalGold = (goldEarned * penaltyMultiplier).toInt()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        var streakUpdated = profile.currentStreak
        var longestStreakUpdated = profile.longestStreak

        if (studyCompleted) {
            val lastStudyDate = profile.lastStudyDate
            if (lastStudyDate == null) {
                streakUpdated = 1
            } else if (lastStudyDate != todayStr) {
                val lastDate = sdf.parse(lastStudyDate)
                val today = sdf.parse(todayStr)
                if (lastDate != null && today != null) {
                    val diffDays = (today.time - lastDate.time) / (1000 * 60 * 60 * 24)
                    if (diffDays == 1L) {
                        streakUpdated++
                    } else if (diffDays > 1L) {
                        streakUpdated = 1
                    }
                }
            }
            if (streakUpdated > longestStreakUpdated) {
                longestStreakUpdated = streakUpdated
            }
        }

        var bonusGold = 0
        var bonusXp = 0
        if (studyCompleted && streakUpdated > 0) {
            if (streakUpdated == 3) {
                bonusXp = 50
                showStreakResetToast = "3-Day Streak Bonus! Received +$bonusXp XP"
            } else if (streakUpdated == 7) {
                bonusGold = 100
                showStreakResetToast = "7-Day Streak Master! Received +$bonusGold Gold"
            }
        }

        val totalGoldGained = finalGold + bonusGold
        val totalXpGained = finalXp + bonusXp

        var currentLvl = profile.level
        var currentXp = profile.xp + totalXpGained
        var levelUpsCount = 0

        while (currentXp >= (currentLvl * 150)) {
            currentXp -= (currentLvl * 150)
            currentLvl++
            levelUpsCount++
        }

        // Higher Red Dungeon levels require longer recovery: decrement by 1 level per successful study session
        val redDungeonDaysUpdated = if (studyCompleted) {
            (profile.redDungeonDays - 1).coerceAtLeast(0)
        } else {
            profile.redDungeonDays
        }
        val isRedDungeonBoostActiveUpdated = if (studyCompleted && redDungeonDaysUpdated == 0) false else profile.isRedDungeonBoostActive
        val totalRedDungeonsClearedUpdated = if (studyCompleted && profile.redDungeonDays > 0 && redDungeonDaysUpdated == 0) {
            profile.totalRedDungeonsCleared + 1
        } else {
            profile.totalRedDungeonsCleared
        }

        if (levelUpsCount > 0) {
            val levelUpGoldBonus = levelUpsCount * 50
            showLevelUpToast = Pair(profile.level, currentLvl)
            if (studyCompleted) {
                RpgSoundManager.playLevelUpSound()
            }

            repository.insertOrUpdateProfile(
                profile.copy(
                    level = currentLvl,
                    xp = currentXp,
                    gold = profile.gold + totalGoldGained + levelUpGoldBonus,
                    currentStreak = streakUpdated,
                    longestStreak = longestStreakUpdated,
                    lastStudyDate = todayStr,
                    totalStudyTimeSeconds = profile.totalStudyTimeSeconds + durationSeconds,
                    totalSessionCount = profile.totalSessionCount + 1,
                    totalBossesDefeated = profile.totalBossesDefeated + (if (studyCompleted && !isFreeStudy) 1 else 0),
                    totalGoldEarned = profile.totalGoldEarned + totalGoldGained + levelUpGoldBonus,
                    totalXpEarned = profile.totalXpEarned + totalXpGained,
                    totalFreeStudySeconds = profile.totalFreeStudySeconds + (if (isFreeStudy) durationSeconds else 0L),
                    redDungeonDays = redDungeonDaysUpdated,
                    isRedDungeonBoostActive = isRedDungeonBoostActiveUpdated,
                    totalRedDungeonsCleared = totalRedDungeonsClearedUpdated
                )
            )
        } else {
            if (studyCompleted) {
                RpgSoundManager.playConquerSound()
            }
            repository.insertOrUpdateProfile(
                profile.copy(
                    xp = currentXp,
                    gold = profile.gold + totalGoldGained,
                    currentStreak = streakUpdated,
                    longestStreak = longestStreakUpdated,
                    lastStudyDate = todayStr,
                    totalStudyTimeSeconds = profile.totalStudyTimeSeconds + durationSeconds,
                    totalSessionCount = profile.totalSessionCount + 1,
                    totalBossesDefeated = profile.totalBossesDefeated + (if (studyCompleted && !isFreeStudy) 1 else 0),
                    totalGoldEarned = profile.totalGoldEarned + totalGoldGained,
                    totalXpEarned = profile.totalXpEarned + totalXpGained,
                    totalFreeStudySeconds = profile.totalFreeStudySeconds + (if (isFreeStudy) durationSeconds else 0L),
                    redDungeonDays = redDungeonDaysUpdated,
                    isRedDungeonBoostActive = isRedDungeonBoostActiveUpdated,
                    totalRedDungeonsCleared = totalRedDungeonsClearedUpdated
                )
            )
        }
    }

    fun conquerRealBossManual(boss: BossEntity) {
        viewModelScope.launch {
            val finishedBoss = boss.copy(
                isCompleted = true,
                timeSpentSeconds = boss.requiredMinutes * 60L
            )
            repository.updateBoss(finishedBoss)

            val baseRewards = getDifficultyRewards(boss.difficulty)
            val xpEarned = (baseRewards.xp * 1.5f).toInt()
            val goldEarned = (baseRewards.gold * 1.5f).toInt()

            repository.insertSession(
                StudySessionEntity(
                    bossId = boss.id,
                    bossName = boss.name + " (Manual Conquest)",
                    durationSeconds = boss.requiredMinutes * 60L,
                    xpEarned = xpEarned,
                    goldEarned = goldEarned,
                    wasCompleted = true,
                    isFreeStudy = false
                )
            )

            updateProfileCompletingSession(
                durationSeconds = boss.requiredMinutes * 60L,
                xpEarned = xpEarned,
                goldEarned = goldEarned,
                studyCompleted = true,
                isFreeStudy = false
            )

            if (activeBoss?.id == boss.id) {
                timerJob?.cancel()
                activeBoss = null
                isBattleActive = false
                isBattlePaused = false
                isFreeStudyActive = false
                battleTimeLeftSeconds = 0
                battleTimeSpentSeconds = 0
                clearFocusSessionState()
            }
        }
    }

    fun clearNotifications() {
        showStreakResetToast = null
        showLevelUpToast = null
        showPenaltyToast = null
    }

    private fun getDifficultyRewards(difficulty: String): DifficultyRewards {
        return when (difficulty) {
            "Easy" -> DifficultyRewards(75, 40)
            "Medium" -> DifficultyRewards(150, 80)
            "Hard" -> DifficultyRewards(350, 180)
            "Legendary" -> DifficultyRewards(750, 400)
            else -> DifficultyRewards(100, 50)
        }
    }

    private data class DifficultyRewards(val xp: Int, val gold: Int)
}
