package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.focus.FocusSessionSnapshot
import com.amkumirab.solostudying.focus.FocusSessionStore
import com.amkumirab.solostudying.focus.reconcileFocusSession
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
    private val context: Context,
    private val focusSessionStore: FocusSessionStore = FocusSessionStore(context),
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

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
    private var isCompletingSession = false

    // For level ups or streak updates that need to be triggered from BattleViewModel
    var showStreakResetToast by mutableStateOf<String?>(null)
    var showLevelUpToast by mutableStateOf<Pair<Int, Int>?>(null)
    var showPenaltyToast by mutableStateOf<String?>(null)

    init {
        restoreSavedFocusSession()
    }

    private fun saveFocusSessionState() {
        if (!isBattleActive) {
            focusSessionStore.clear()
            return
        }

        focusSessionStore.write(
            FocusSessionSnapshot(
                isActive = isBattleActive,
                isFreeStudy = isFreeStudyActive,
                isPaused = isBattlePaused,
                timeLeftSeconds = battleTimeLeftSeconds,
                timeSpentSeconds = battleTimeSpentSeconds,
                initialBossTimeSpentSeconds = initialBossTimeSpent,
                lastTickTimeMillis = lastTickTimeMillis,
                bossId = activeBoss?.id,
                skillId = selectedSkillToTrain?.id,
            ),
        )
    }

    private fun clearFocusSessionState() {
        focusSessionStore.clear()
    }

    private fun restoreSavedFocusSession() {
        viewModelScope.launch {
            val savedSnapshot = focusSessionStore.read() ?: return@launch
            val restoredSnapshot = reconcileFocusSession(savedSnapshot, clock())

            if (restoredSnapshot.bossId != null) {
                activeBoss = repository.getBossById(restoredSnapshot.bossId)
            }
            if (!restoredSnapshot.isFreeStudy && activeBoss == null) {
                clearFocusSessionState()
                return@launch
            }
            if (restoredSnapshot.skillId != null) {
                selectedSkillToTrain = repository.getSkillById(restoredSnapshot.skillId)
            }

            initialBossTimeSpent = restoredSnapshot.initialBossTimeSpentSeconds
            isFreeStudyActive = restoredSnapshot.isFreeStudy
            isBattleActive = true
            isBattlePaused = restoredSnapshot.isPaused
            battleTimeLeftSeconds = restoredSnapshot.timeLeftSeconds
            battleTimeSpentSeconds = restoredSnapshot.timeSpentSeconds
            lastTickTimeMillis = restoredSnapshot.lastTickTimeMillis

            if (restoredSnapshot != savedSnapshot && activeBoss != null) {
                saveIncrementalBossProgress()
            }
            saveFocusSessionState()

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
            RpgSoundManager.playBeginBattleSound()
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
            RpgSoundManager.playBeginBattleSound()
        }
    }

    fun pauseBattle(onPaused: () -> Unit = {}) {
        if (!isBattleActive) return
        if (isBattlePaused) {
            saveFocusSessionState()
            onPaused()
            return
        }
        val pausedAtMillis = clock()
        isBattlePaused = true
        timerJob?.cancel()
        viewModelScope.launch {
            advanceSessionClock(pausedAtMillis, forceBossSync = true)
            saveFocusSessionState()
            RpgSoundManager.playPauseStudySound()
            onPaused()
        }
    }

    fun resumeBattle() {
        if (!isBattleActive || !isBattlePaused) return
        isBattlePaused = false
        if (battleTimeLeftSeconds <= 0L) {
            completeActiveBoss()
        } else {
            startTimer()
            RpgSoundManager.playResumeStudySound()
        }
    }

    fun syncFocusSessionTime() {
        if (!isBattleActive || isBattlePaused || isCompletingSession) return
        timerJob?.cancel()
        viewModelScope.launch {
            advanceSessionClock(clock(), forceBossSync = true)
            if (battleTimeLeftSeconds <= 0L) {
                completeActiveBoss()
            } else {
                startTimer()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        lastTickTimeMillis = clock()
        saveFocusSessionState()
        timerJob = viewModelScope.launch {
            while (isBattleActive && !isBattlePaused && battleTimeLeftSeconds > 0L) {
                delay(1000L)
                val appliedSeconds = advanceSessionClock(clock())
                if (appliedSeconds > 0L && battleTimeLeftSeconds in 1..5) {
                    RpgSoundManager.playWarningAlarmSound()
                }
            }
            if (isBattleActive && !isBattlePaused && battleTimeLeftSeconds <= 0L) {
                completeActiveBoss()
            }
        }
    }

    private suspend fun advanceSessionClock(
        nowMillis: Long,
        forceBossSync: Boolean = false,
    ): Long {
        if (!isBattleActive || lastTickTimeMillis <= 0L) return 0L

        val elapsedSeconds = ((nowMillis - lastTickTimeMillis).coerceAtLeast(0L)) / 1_000L
        if (elapsedSeconds <= 0L) return 0L

        val appliedSeconds = minOf(elapsedSeconds, battleTimeLeftSeconds)
        battleTimeLeftSeconds -= appliedSeconds
        battleTimeSpentSeconds += appliedSeconds
        lastTickTimeMillis += appliedSeconds * 1_000L

        if (
            activeBoss != null &&
            (forceBossSync || battleTimeSpentSeconds % 10L == 0L || appliedSeconds >= 10L)
        ) {
            saveIncrementalBossProgress()
        }
        saveFocusSessionState()
        return appliedSeconds
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
        if (!isBattleActive || isCompletingSession) return
        isCompletingSession = true
        timerJob?.cancel()
        viewModelScope.launch {
            try {
                advanceSessionClock(clock(), forceBossSync = true)
                val finalDuration = battleTimeSpentSeconds
                val xpEarned: Int
                val goldEarned: Int
                val boss = activeBoss
                val completedFreeStudy = isFreeStudyActive

                if (completedFreeStudy) {
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
                            isFreeStudy = true,
                        ),
                    )
                } else if (boss != null) {
                    val finishedBoss = boss.copy(
                        timeSpentSeconds = initialBossTimeSpent + finalDuration,
                        isCompleted = true,
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
                            isFreeStudy = false,
                        ),
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
                            isUnlocked = skill.isUnlocked || isNowUnlocked,
                        ),
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
                    isFreeStudy = completedFreeStudy,
                )

                resetActiveSession()
            } finally {
                isCompletingSession = false
            }
        }
    }

    private fun resetActiveSession() {
        activeBoss = null
        isBattleActive = false
        isBattlePaused = false
        isFreeStudyActive = false
        selectedSkillToTrain = null
        battleTimeLeftSeconds = 0L
        battleTimeSpentSeconds = 0L
        initialBossTimeSpent = 0L
        lastTickTimeMillis = 0L
        clearFocusSessionState()
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

        resetActiveSession()
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
                saveFocusSessionState()
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
                resetActiveSession()
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

    override fun onCleared() {
        timerJob?.cancel()
        if (isBattleActive) {
            saveFocusSessionState()
        }
        super.onCleared()
    }
}
