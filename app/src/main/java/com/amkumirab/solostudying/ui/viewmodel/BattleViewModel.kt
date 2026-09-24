package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.domain.reward.SessionReward
import com.amkumirab.solostudying.domain.reward.SessionRewardCalculator
import com.amkumirab.solostudying.domain.session.ProgressSummary
import com.amkumirab.solostudying.domain.session.SessionEndState
import com.amkumirab.solostudying.domain.session.SessionSummary
import com.amkumirab.solostudying.focus.FocusSessionSnapshot
import com.amkumirab.solostudying.focus.FocusSessionStore
import com.amkumirab.solostudying.focus.FocusShieldManager
import com.amkumirab.solostudying.focus.reconcileFocusSession
import com.amkumirab.solostudying.notification.FocusSessionActionEvents
import com.amkumirab.solostudying.notification.FocusSessionNotifier
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
    private val focusShieldManager: FocusShieldManager = FocusShieldManager(context),
    private val onSessionFinished: (SessionSummary) -> Unit = {},
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

    var activeDailyQuestId by mutableStateOf<Int?>(null)
        private set

    var activeDailyQuestTitle by mutableStateOf<String?>(null)
        private set

    var activeBossStepId by mutableStateOf<Int?>(null)
        private set

    var activeBossStepTitle by mutableStateOf<String?>(null)
        private set

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

    var sessionSummary by mutableStateOf<SessionSummary?>(null)
        private set

    var focusShieldEnabled by mutableStateOf(focusShieldManager.isEnabled())
        private set

    var focusShieldHasAccess by mutableStateOf(focusShieldManager.hasPolicyAccess())
        private set

    var isFocusShieldActive by mutableStateOf(false)
        private set

    init {
        restoreSavedFocusSession()
        viewModelScope.launch {
            FocusSessionActionEvents.changes.collect {
                syncFocusSessionTime()
            }
        }
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
                dailyQuestId = activeDailyQuestId,
                dailyQuestTitle = activeDailyQuestTitle,
                bossStepId = activeBossStepId,
                bossStepTitle = activeBossStepTitle,
                bossTitle = activeBoss?.name,
                skillTitle = selectedSkillToTrain?.name,
            ),
        )
    }

    private fun publishFocusSessionState() {
        saveFocusSessionState()
        val snapshot = focusSessionStore.read() ?: return
        FocusSessionNotifier.show(context, snapshot)
        if (snapshot.isPaused) {
            FocusSessionNotifier.cancelCompletionAlarm(context)
        } else {
            FocusSessionNotifier.scheduleCompletion(context, snapshot)
        }
    }

    private fun clearFocusSessionState() {
        focusSessionStore.clear()
        FocusSessionNotifier.cancelActive(context)
        FocusSessionNotifier.cancelCompletionAlarm(context)
    }

    private fun restoreSavedFocusSession() {
        viewModelScope.launch {
            val savedSnapshot = focusSessionStore.read() ?: run {
                focusShieldManager.reconcile(sessionActive = false, sessionPaused = false)
                refreshFocusShieldState()
                return@launch
            }
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
            restoredSnapshot.dailyQuestId?.let { questId ->
                val quest = repository.getDailyQuestById(questId)
                if (quest != null && !quest.isCompleted) {
                    activeDailyQuestId = quest.id
                    activeDailyQuestTitle = quest.title
                }
            }
            restoredSnapshot.bossStepId?.let { stepId ->
                val step = repository.getBossStepById(stepId)
                activeBossStepId = stepId
                activeBossStepTitle = step?.title ?: restoredSnapshot.bossStepTitle
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
            if (focusSessionStore.consumeFinishRequest()) {
                finishSessionFromNotification()
            } else if (battleTimeLeftSeconds > 0) {
                if (isBattlePaused) publishFocusSessionState() else startTimer()
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
            activeDailyQuestId = null
            activeDailyQuestTitle = null
            activeBossStepId = null
            activeBossStepTitle = null
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
            isFreeStudyActive = false
            isBattleActive = true
            isBattlePaused = false
            startTimer()
            RpgSoundManager.playBeginBattleSound()
        }
    }

    fun selectAndStartBossStep(boss: BossEntity, step: BossStepEntity) {
        require(step.bossId == boss.id) { "Study step does not belong to this goal" }
        require(step.estimatedMinutes in 1..480) { "Step duration must be between 1 and 480 minutes" }
        viewModelScope.launch {
            if (isBattleActive) {
                suspendCurrentSession(applyHeavyPenalty = false)
            }
            activeDailyQuestId = null
            activeDailyQuestTitle = null
            activeBossStepId = step.id
            activeBossStepTitle = step.title
            activeBoss = boss
            initialBossTimeSpent = boss.timeSpentSeconds
            battleTimeLeftSeconds = step.estimatedMinutes * 60L
            battleTimeSpentSeconds = 0L
            isFreeStudyActive = false
            isBattleActive = true
            isBattlePaused = false
            startTimer()
            RpgSoundManager.playBeginBattleSound()
        }
    }

    fun selectAndStartFreeStudy(minutes: Int) {
        val selectedSkillId = selectedSkillToTrain?.id
        viewModelScope.launch {
            beginFreeStudy(
                minutes = minutes,
                skillId = selectedSkillId,
                dailyQuestId = null,
                dailyQuestTitle = null,
            )
        }
    }

    fun selectAndStartQuickFocus(minutes: Int, skillId: Int?) {
        viewModelScope.launch {
            beginFreeStudy(
                minutes = minutes,
                skillId = skillId,
                dailyQuestId = null,
                dailyQuestTitle = null,
            )
        }
    }

    fun selectAndStartDailyQuest(quest: DailyQuestEntity) {
        viewModelScope.launch {
            beginFreeStudy(
                minutes = quest.durationMinutes,
                skillId = quest.skillId,
                dailyQuestId = quest.id,
                dailyQuestTitle = quest.title,
            )
        }
    }

    private suspend fun beginFreeStudy(
        minutes: Int,
        skillId: Int?,
        dailyQuestId: Int?,
        dailyQuestTitle: String?,
    ) {
        require(minutes in 1..480) { "Study duration must be between 1 and 480 minutes" }
        if (isBattleActive) {
            suspendCurrentSession(applyHeavyPenalty = false)
        }
        selectedSkillToTrain = skillId?.let { repository.getSkillById(it) }
        activeDailyQuestId = dailyQuestId
        activeDailyQuestTitle = dailyQuestTitle
        activeBossStepId = null
        activeBossStepTitle = null
        activeBoss = null
        battleTimeLeftSeconds = minutes * 60L
        battleTimeSpentSeconds = 0L
        initialBossTimeSpent = 0L
        isFreeStudyActive = true
        isBattleActive = true
        isBattlePaused = false
        startTimer()
        RpgSoundManager.playBeginBattleSound()
    }

    fun pauseBattle(onPaused: () -> Unit = {}) {
        if (!isBattleActive) return
        if (isBattlePaused) {
            publishFocusSessionState()
            onPaused()
            return
        }
        val pausedAtMillis = clock()
        isBattlePaused = true
        focusShieldManager.restorePreviousFilter()
        refreshFocusShieldState()
        timerJob?.cancel()
        viewModelScope.launch {
            advanceSessionClock(pausedAtMillis, forceBossSync = true)
            publishFocusSessionState()
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
        if (!isBattleActive || isCompletingSession) return
        timerJob?.cancel()
        viewModelScope.launch {
            val saved = focusSessionStore.read() ?: return@launch
            val current = reconcileFocusSession(saved, clock())
            isBattlePaused = current.isPaused
            battleTimeLeftSeconds = current.timeLeftSeconds
            battleTimeSpentSeconds = current.timeSpentSeconds
            lastTickTimeMillis = current.lastTickTimeMillis
            focusShieldManager.reconcile(
                sessionActive = true,
                sessionPaused = current.isPaused,
            )
            refreshFocusShieldState()
            if (activeBoss != null && current != saved) {
                saveIncrementalBossProgress()
            }
            if (battleTimeLeftSeconds <= 0L) {
                completeActiveBoss()
            } else if (isBattlePaused) {
                publishFocusSessionState()
            } else {
                startTimer()
            }
        }
    }

    fun processPendingFocusAction() {
        if (!isBattleActive || !focusSessionStore.consumeFinishRequest()) return
        finishSessionFromNotification()
    }

    private fun finishSessionFromNotification() {
        if (activeBossStepId != null) endBossStepEarly() else completeActiveBoss()
    }

    private fun startTimer() {
        timerJob?.cancel()
        lastTickTimeMillis = clock()
        focusShieldManager.reconcile(sessionActive = true, sessionPaused = false)
        refreshFocusShieldState()
        FocusSessionNotifier.cancelCompleted(context)
        publishFocusSessionState()
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
                val completedFullTarget = battleTimeLeftSeconds == 0L
                val boss = activeBoss
                val completedFreeStudy = isFreeStudyActive
                val dailyQuestId = activeDailyQuestId
                val dailyQuestTitle = activeDailyQuestTitle
                val bossStepId = activeBossStepId
                val bossStepTitle = activeBossStepTitle
                if (!completedFreeStudy && boss == null) {
                    resetActiveSession()
                    return@launch
                }
                val baseReward = if (completedFreeStudy || bossStepId != null) {
                    SessionRewardCalculator.completedFreeStudy(finalDuration)
                } else {
                    SessionRewardCalculator.completedBoss(checkNotNull(boss).difficulty)
                }
                var skillMastered = false
                var completionResult: SessionCompletionResult? = null
                var pendingSummary: SessionSummary? = null

                repository.runInTransaction {
                    if (!completedFreeStudy && boss != null) {
                        val updatedBossSeconds = initialBossTimeSpent + finalDuration
                        updateBoss(
                            boss.copy(
                                timeSpentSeconds = updatedBossSeconds,
                                isCompleted = boss.isCompleted ||
                                    (bossStepId == null || updatedBossSeconds >= boss.requiredMinutes * 60L),
                            ),
                        )
                    }

                    if (bossStepId != null && completedFullTarget) {
                        getBossStepById(bossStepId)?.let { step ->
                            updateBossStep(
                                step.copy(isCompleted = true, completedAt = clock()),
                            )
                        }
                    }

                    val skill = selectedSkillToTrain
                    if (skill != null && finalDuration > 0) {
                        val updatedSpent = skill.spentSeconds + finalDuration
                        val isNowUnlocked = updatedSpent >= skill.targetMinutes * 60L
                        updateSkill(
                            skill.copy(
                                spentSeconds = updatedSpent,
                                isUnlocked = skill.isUnlocked || isNowUnlocked,
                            ),
                        )
                        skillMastered = isNowUnlocked && !skill.isUnlocked
                    }

                    val result = updateProfileCompletingSession(
                        durationSeconds = finalDuration,
                        baseReward = baseReward,
                        studyCompleted = true,
                        isFreeStudy = completedFreeStudy,
                    )
                    completionResult = result
                    val sessionId = insertSession(
                        StudySessionEntity(
                            bossId = boss?.id,
                            bossName = if (completedFreeStudy) {
                                dailyQuestTitle ?: skill?.name ?: "Astral Free Study"
                            } else {
                                bossStepTitle?.let { "${boss?.name}: $it" } ?: boss?.name
                            },
                            durationSeconds = finalDuration,
                            xpEarned = result.xpAwarded,
                            goldEarned = result.goldAwarded,
                            wasCompleted = true,
                            isFreeStudy = completedFreeStudy,
                        ),
                    )
                    pendingSummary = SessionSummary(
                        sessionId = sessionId,
                        subject = if (completedFreeStudy) {
                            dailyQuestTitle ?: skill?.name ?: "Astral Free Study"
                        } else {
                            bossStepTitle ?: checkNotNull(boss).name
                        },
                        durationSeconds = finalDuration,
                        xpEarned = result.xpAwarded,
                        goldEarned = result.goldAwarded,
                        endState = SessionEndState.Completed,
                        bossProgress = boss?.let {
                            ProgressSummary(
                                name = it.name,
                                beforeSeconds = initialBossTimeSpent,
                                afterSeconds = initialBossTimeSpent + finalDuration,
                                targetSeconds = it.requiredMinutes * 60L,
                            )
                        },
                        skillProgress = skill?.let {
                            ProgressSummary(
                                name = it.name,
                                beforeSeconds = it.spentSeconds,
                                afterSeconds = it.spentSeconds + finalDuration,
                                targetSeconds = it.targetMinutes * 60L,
                            )
                        },
                        previousLevel = result.previousLevel,
                        currentLevel = result.currentLevel,
                        previousStreak = result.previousStreak,
                        currentStreak = result.currentStreak,
                        streakMessage = result.streakMessage,
                        skillUnlocked = skillMastered,
                    )
                    if (dailyQuestId != null && completedFullTarget) {
                        completeDailyQuest(dailyQuestId, clock())
                    }
                }

                if (skillMastered) {
                    RpgSoundManager.playSkillUnlockSound()
                }
                completionResult?.let(::applyProfileCompletionFeedback)
                pendingSummary?.let(::publishSessionSummary)

                resetActiveSession()
            } finally {
                isCompletingSession = false
            }
        }
    }

    fun endBossStepEarly() {
        if (!isBattleActive || activeBossStepId == null || isCompletingSession) return
        isCompletingSession = true
        viewModelScope.launch {
            try {
                suspendCurrentSession(applyHeavyPenalty = false)
            } finally {
                isCompletingSession = false
            }
        }
    }

    private fun resetActiveSession() {
        focusShieldManager.restorePreviousFilter()
        activeBoss = null
        isBattleActive = false
        isBattlePaused = false
        isFreeStudyActive = false
        selectedSkillToTrain = null
        activeDailyQuestId = null
        activeDailyQuestTitle = null
        activeBossStepId = null
        activeBossStepTitle = null
        battleTimeLeftSeconds = 0L
        battleTimeSpentSeconds = 0L
        initialBossTimeSpent = 0L
        lastTickTimeMillis = 0L
        refreshFocusShieldState()
        clearFocusSessionState()
    }

    fun updateFocusShieldEnabled(enabled: Boolean) {
        focusShieldManager.setEnabled(
            enabled = enabled,
            sessionActive = isBattleActive,
            sessionPaused = isBattlePaused,
        )
        refreshFocusShieldState()
    }

    fun refreshFocusShield() {
        focusShieldManager.reconcile(
            sessionActive = isBattleActive,
            sessionPaused = isBattlePaused,
        )
        refreshFocusShieldState()
    }

    private fun refreshFocusShieldState() {
        focusShieldEnabled = focusShieldManager.isEnabled()
        focusShieldHasAccess = focusShieldManager.hasPolicyAccess()
        isFocusShieldActive = isBattleActive && !isBattlePaused && focusShieldManager.isActive()
    }

    suspend fun suspendCurrentSession(applyHeavyPenalty: Boolean = false) {
        timerJob?.cancel()
        val finalSpent = battleTimeSpentSeconds
        val boss = activeBoss
        val wasFreeStudy = isFreeStudyActive
        val skill = selectedSkillToTrain
        val dailyQuestTitle = activeDailyQuestTitle
        val bossStepTitle = activeBossStepTitle
        var completionResult: SessionCompletionResult? = null
        var recordedSessionId: Long? = null
        var skillMastered = false
        var penaltyFeedback: String? = null

        repository.runInTransaction {
            if (wasFreeStudy) {
                if (finalSpent > 5) {
                    val baseReward = SessionRewardCalculator.suspendedFreeStudy(finalSpent)
                    val result = updateProfileCompletingSession(
                        durationSeconds = finalSpent,
                        baseReward = baseReward,
                        studyCompleted = false,
                        isFreeStudy = true,
                    )
                    completionResult = result
                    recordedSessionId = insertSession(
                        StudySessionEntity(
                            bossId = null,
                            bossName = "${dailyQuestTitle ?: skill?.name ?: "Astral Free Study"} (Suspended)",
                            durationSeconds = finalSpent,
                            xpEarned = result.xpAwarded,
                            goldEarned = result.goldAwarded,
                            wasCompleted = false,
                            isFreeStudy = true,
                        ),
                    )
                }
            } else if (boss != null) {
                updateBoss(
                    boss.copy(timeSpentSeconds = initialBossTimeSpent + finalSpent),
                )

                if (finalSpent > 5) {
                    val baseReward = SessionRewardCalculator.suspendedBossStudy(finalSpent)
                    val result = updateProfileCompletingSession(
                        durationSeconds = finalSpent,
                        baseReward = baseReward,
                        studyCompleted = false,
                        isFreeStudy = false,
                    )
                    completionResult = result
                    recordedSessionId = insertSession(
                        StudySessionEntity(
                            bossId = boss.id,
                            bossName = bossStepTitle?.let { "${boss.name}: $it" } ?: boss.name,
                            durationSeconds = finalSpent,
                            xpEarned = result.xpAwarded,
                            goldEarned = result.goldAwarded,
                            wasCompleted = false,
                            isFreeStudy = false,
                        ),
                    )
                }

                if (applyHeavyPenalty) {
                    penaltyFeedback = applyProcrastinationPenalty()
                }
            }

            if (skill != null && finalSpent > 0) {
                val updatedSpent = skill.spentSeconds + finalSpent
                val isNowUnlocked = updatedSpent >= skill.targetMinutes * 60L
                updateSkill(
                    skill.copy(
                        spentSeconds = updatedSpent,
                        isUnlocked = skill.isUnlocked || isNowUnlocked
                    )
                )
                skillMastered = isNowUnlocked && !skill.isUnlocked
            }
        }

        completionResult?.let(::applyProfileCompletionFeedback)
        val result = completionResult
        val sessionId = recordedSessionId
        if (result != null && sessionId != null) {
            publishSessionSummary(SessionSummary(
                sessionId = sessionId,
                subject = if (wasFreeStudy) {
                    dailyQuestTitle ?: skill?.name ?: "Astral Free Study"
                } else {
                    checkNotNull(boss).name
                },
                durationSeconds = finalSpent,
                xpEarned = result.xpAwarded,
                goldEarned = result.goldAwarded,
                endState = SessionEndState.Suspended,
                bossProgress = boss?.let {
                    ProgressSummary(
                        name = it.name,
                        beforeSeconds = initialBossTimeSpent,
                        afterSeconds = initialBossTimeSpent + finalSpent,
                        targetSeconds = it.requiredMinutes * 60L,
                    )
                },
                skillProgress = skill?.let {
                    ProgressSummary(
                        name = it.name,
                        beforeSeconds = it.spentSeconds,
                        afterSeconds = it.spentSeconds + finalSpent,
                        targetSeconds = it.targetMinutes * 60L,
                    )
                },
                previousLevel = result.previousLevel,
                currentLevel = result.currentLevel,
                previousStreak = result.previousStreak,
                currentStreak = if (applyHeavyPenalty && boss != null) 0 else result.currentStreak,
                streakMessage = result.streakMessage,
                skillUnlocked = skillMastered,
            ))
        }
        penaltyFeedback?.let { showPenaltyToast = it }
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
                publishFocusSessionState()
            }
        }
    }

    private suspend fun applyProcrastinationPenalty(): String? {
        val profile = repository.getProfileSync() ?: return null
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
        return penaltyText
    }

    private suspend fun updateProfileCompletingSession(
        durationSeconds: Long,
        baseReward: SessionReward,
        studyCompleted: Boolean,
        isFreeStudy: Boolean = false
    ): SessionCompletionResult {
        val profile = repository.getProfileSync() ?: UserProfileEntity()
        val adjustedReward = SessionRewardCalculator.applyProgressionModifiers(
            reward = baseReward,
            redDungeonDays = profile.redDungeonDays,
            isXpBoostActive = profile.isRedDungeonBoostActive,
        )

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        var streakUpdated = profile.currentStreak
        var longestStreakUpdated = profile.longestStreak
        var streakAdvanced = false

        if (studyCompleted) {
            val lastStudyDate = profile.lastStudyDate
            if (lastStudyDate == null) {
                streakUpdated = 1
                streakAdvanced = true
            } else if (lastStudyDate != todayStr) {
                val lastDate = sdf.parse(lastStudyDate)
                val today = sdf.parse(todayStr)
                if (lastDate != null && today != null) {
                    val diffDays = (today.time - lastDate.time) / (1000 * 60 * 60 * 24)
                    if (diffDays == 1L) {
                        streakUpdated++
                        streakAdvanced = true
                    } else if (diffDays > 1L) {
                        streakUpdated = 1
                        streakAdvanced = true
                    }
                }
            }
            if (streakUpdated > longestStreakUpdated) {
                longestStreakUpdated = streakUpdated
            }
        }

        var bonusGold = 0
        var bonusXp = 0
        var streakMessage: String? = null
        if (studyCompleted && streakAdvanced) {
            if (streakUpdated == 3) {
                bonusXp = 50
                streakMessage = "3-Day Streak Bonus! Received +$bonusXp XP"
            } else if (streakUpdated == 7) {
                bonusGold = 100
                streakMessage = "7-Day Streak Master! Received +$bonusGold Gold"
            }
        }

        val totalGoldGained = adjustedReward.gold + bonusGold
        val totalXpGained = adjustedReward.xp + bonusXp

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

        val levelUpGoldBonus = levelUpsCount * 50
        repository.insertOrUpdateProfile(
            profile.copy(
                level = currentLvl,
                xp = currentXp,
                gold = profile.gold + totalGoldGained + levelUpGoldBonus,
                currentStreak = streakUpdated,
                longestStreak = longestStreakUpdated,
                lastStudyDate = if (studyCompleted) todayStr else profile.lastStudyDate,
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
        return SessionCompletionResult(
            previousLevel = profile.level,
            currentLevel = currentLvl,
            previousStreak = profile.currentStreak,
            currentStreak = streakUpdated,
            streakMessage = streakMessage,
            studyCompleted = studyCompleted,
            xpAwarded = totalXpGained,
            goldAwarded = totalGoldGained + levelUpGoldBonus,
        )
    }

    private fun applyProfileCompletionFeedback(feedback: SessionCompletionResult) {
        if (feedback.currentLevel > feedback.previousLevel) {
            if (feedback.studyCompleted) {
                RpgSoundManager.playLevelUpSound()
            }
        } else if (feedback.studyCompleted) {
            RpgSoundManager.playConquerSound()
        }
    }

    fun conquerRealBossManual(boss: BossEntity) {
        viewModelScope.launch {
            var completionResult: SessionCompletionResult? = null
            var pendingSummary: SessionSummary? = null
            val conquered = repository.runInTransaction {
                val currentBoss = getBossById(boss.id) ?: return@runInTransaction false
                if (currentBoss.isCompleted) return@runInTransaction false
                val baseReward = SessionRewardCalculator.manualBoss(currentBoss.difficulty)
                val durationSeconds = currentBoss.requiredMinutes * 60L
                updateBoss(
                    currentBoss.copy(
                        isCompleted = true,
                        timeSpentSeconds = durationSeconds,
                    ),
                )
                val result = updateProfileCompletingSession(
                    durationSeconds = durationSeconds,
                    baseReward = baseReward,
                    studyCompleted = true,
                    isFreeStudy = false
                )
                completionResult = result
                val sessionId = insertSession(
                    StudySessionEntity(
                        bossId = currentBoss.id,
                        bossName = currentBoss.name + " (Manual Conquest)",
                        durationSeconds = durationSeconds,
                        xpEarned = result.xpAwarded,
                        goldEarned = result.goldAwarded,
                        wasCompleted = true,
                        isFreeStudy = false,
                    ),
                )
                pendingSummary = SessionSummary(
                    sessionId = sessionId,
                    subject = currentBoss.name,
                    durationSeconds = durationSeconds,
                    xpEarned = result.xpAwarded,
                    goldEarned = result.goldAwarded,
                    endState = SessionEndState.ManualConquest,
                    bossProgress = ProgressSummary(
                        name = currentBoss.name,
                        beforeSeconds = currentBoss.timeSpentSeconds,
                        afterSeconds = durationSeconds,
                        targetSeconds = durationSeconds,
                    ),
                    previousLevel = result.previousLevel,
                    currentLevel = result.currentLevel,
                    previousStreak = result.previousStreak,
                    currentStreak = result.currentStreak,
                    streakMessage = result.streakMessage,
                )
                true
            }
            if (!conquered) return@launch
            completionResult?.let(::applyProfileCompletionFeedback)
            pendingSummary?.let(::publishSessionSummary)

            if (activeBoss?.id == boss.id) {
                timerJob?.cancel()
                resetActiveSession()
            }
        }
    }

    fun dismissSessionSummary() {
        sessionSummary = null
    }

    private fun publishSessionSummary(summary: SessionSummary) {
        sessionSummary = summary
        onSessionFinished(summary)
    }

    fun clearNotifications() {
        showStreakResetToast = null
        showLevelUpToast = null
        showPenaltyToast = null
    }

    private data class SessionCompletionResult(
        val previousLevel: Int,
        val currentLevel: Int,
        val previousStreak: Int,
        val currentStreak: Int,
        val streakMessage: String?,
        val studyCompleted: Boolean,
        val xpAwarded: Int,
        val goldAwarded: Int,
    )

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
