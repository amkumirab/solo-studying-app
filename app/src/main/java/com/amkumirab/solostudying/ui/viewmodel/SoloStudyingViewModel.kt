package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.domain.session.SessionSummary
import com.amkumirab.solostudying.focuscycle.FocusCyclePlan
import com.amkumirab.solostudying.focusprofile.FocusProfile
import com.amkumirab.solostudying.focusprofile.FocusProfileType
import com.amkumirab.solostudying.notification.ReminderSettings
import com.amkumirab.solostudying.domain.quest.RecurringQuestSchedule

class SoloStudyingViewModel(
    val statusViewModel: StatusViewModel,
    val dungeonViewModel: DungeonViewModel,
    val battleViewModel: BattleViewModel,
    val skillViewModel: SkillViewModel,
    val shopViewModel: ShopViewModel,
    val tutorialViewModel: TutorialViewModel,
    val breakViewModel: BreakViewModel,
    val dailyQuestViewModel: DailyQuestViewModel,
    val focusCycleViewModel: FocusCycleViewModel,
    val focusProfileViewModel: FocusProfileViewModel,
) : ViewModel() {

    var focusNavigationRequest by mutableIntStateOf(0)
        private set

    var quickFocusRequest by mutableIntStateOf(0)
        private set

    var statsNavigationRequest by mutableIntStateOf(0)
        private set

    // --- StateFlow Delegation to Feature ViewModels ---
    val bosses = dungeonViewModel.bosses
    val bossSteps = dungeonViewModel.bossSteps
    val dungeons = dungeonViewModel.dungeons
    val userProfile = statusViewModel.userProfile
    val rewards = shopViewModel.rewards
    val balances = shopViewModel.balances
    val sessions = statusViewModel.sessions
    val reminderSettings = statusViewModel.reminderSettings
    val skills = skillViewModel.skills
    val tutorialState = tutorialViewModel.uiState
    val dailyQuests = dailyQuestViewModel.todayQuests
    val allDailyQuests = dailyQuestViewModel.allQuests
    val recurringQuests = dailyQuestViewModel.recurringQuests
    val focusProfiles get() = focusProfileViewModel.profiles

    // --- Active Timer State Delegation ---
    val activeBoss: BossEntity? get() = battleViewModel.activeBoss
    val isBattleActive: Boolean get() = battleViewModel.isBattleActive
    val isBattlePaused: Boolean get() = battleViewModel.isBattlePaused
    val isFreeStudyActive: Boolean get() = battleViewModel.isFreeStudyActive
    val battleTimeLeftSeconds: Long get() = battleViewModel.battleTimeLeftSeconds
    val battleTimeSpentSeconds: Long get() = battleViewModel.battleTimeSpentSeconds
    val sessionSummary: SessionSummary? get() = battleViewModel.sessionSummary
    val activeDailyQuestId: Int? get() = battleViewModel.activeDailyQuestId
    val activeDailyQuestTitle: String? get() = battleViewModel.activeDailyQuestTitle
    val activeBossStepId: Int? get() = battleViewModel.activeBossStepId
    val activeBossStepTitle: String? get() = battleViewModel.activeBossStepTitle
    val activeBreak get() = breakViewModel.activeBreak
    val breakTimeLeftSeconds: Long get() = breakViewModel.breakTimeLeftSeconds
    val showBreakComplete: Boolean get() = breakViewModel.showBreakComplete
    val breakSuggestionsEnabled: Boolean get() = breakViewModel.breakSuggestionsEnabled
    val focusCycleState get() = focusCycleViewModel.state
    val focusShieldEnabled: Boolean get() = battleViewModel.focusShieldEnabled
    val focusShieldHasAccess: Boolean get() = battleViewModel.focusShieldHasAccess
    val isFocusShieldActive: Boolean get() = battleViewModel.isFocusShieldActive
    val strictFocusRequested: Boolean get() = battleViewModel.strictFocusRequested
    val isStrictFocusPinned: Boolean get() = battleViewModel.isStrictFocusPinned
    val strictFocusOwnsPinning: Boolean get() = battleViewModel.strictFocusOwnsPinning

    var selectedSkillToTrain: SkillEntity?
        get() = battleViewModel.selectedSkillToTrain
        set(value) {
            battleViewModel.selectedSkillToTrain = value
        }

    // --- Notification / Toast Flags Delegation ---
    var showStreakResetToast: String?
        get() = statusViewModel.showStreakResetToast ?: battleViewModel.showStreakResetToast
        set(value) {
            statusViewModel.showStreakResetToast = value
            battleViewModel.showStreakResetToast = value
        }

    var showLevelUpToast: Pair<Int, Int>?
        get() = statusViewModel.showLevelUpToast ?: battleViewModel.showLevelUpToast
        set(value) {
            statusViewModel.showLevelUpToast = value
            battleViewModel.showLevelUpToast = value
        }

    var showPenaltyToast: String?
        get() = statusViewModel.showPenaltyToast ?: battleViewModel.showPenaltyToast
        set(value) {
            statusViewModel.showPenaltyToast = value
            battleViewModel.showPenaltyToast = value
        }

    // --- Onboarding & Profile Operations ---
    fun finishOnboarding(name: String) {
        statusViewModel.finishOnboardingLegacy(name)
    }

    fun finishDetailedOnboarding(
        name: String,
        hunterClass: String,
        mainGoal: String,
        learningPath: String,
        scheduleDays: String,
        scheduleMinutes: Int,
        scheduleFlexibility: String,
        weekdayMinutes: String
    ) {
        statusViewModel.finishOnboarding(
            name = name,
            hunterClass = hunterClass,
            mainGoal = mainGoal,
            learningPath = learningPath,
            scheduleDays = scheduleDays,
            scheduleMinutes = scheduleMinutes,
            scheduleFlexibility = scheduleFlexibility,
            weekdayMinutes = weekdayMinutes
        )
    }

    fun updateScheduleWithWeekdays(days: String, mins: Int, flexibility: String, weekdayMins: String) {
        statusViewModel.updateScheduleWithWeekdays(days, mins, flexibility, weekdayMins)
    }

    fun clearNotifications() {
        statusViewModel.clearNotifications()
        battleViewModel.clearNotifications()
    }

    fun dismissSessionSummary() {
        battleViewModel.dismissSessionSummary()
    }

    fun startBreak(minutes: Int) {
        breakViewModel.startBreak(minutes)
    }

    fun syncBreakTime() {
        breakViewModel.syncBreakTime()
    }

    fun processPendingFocusAction() {
        battleViewModel.processPendingFocusAction()
    }

    fun requestFocusScreen() {
        focusNavigationRequest += 1
    }

    fun requestQuickFocus() {
        quickFocusRequest += 1
    }

    fun requestStatsScreen() {
        statsNavigationRequest += 1
    }

    fun selectAndStartQuickFocus(minutes: Int, skillId: Int?) {
        battleViewModel.selectAndStartQuickFocus(minutes, skillId)
    }

    fun startFocusCycle(plan: FocusCyclePlan) {
        focusCycleViewModel.start(plan)
        battleViewModel.selectAndStartQuickFocus(
            minutes = plan.focusMinutes,
            skillId = plan.skillId,
            useFocusShield = plan.useFocusShield,
            useStrictFocus = plan.useStrictFocus,
        )
    }

    fun startCycleBreak() {
        focusCycleViewModel.startBreak()?.let(breakViewModel::startBreak)
    }

    fun startNextCycleRound() {
        val plan = focusCycleViewModel.startNextRound() ?: return
        breakViewModel.dismissBreakComplete()
        battleViewModel.selectAndStartQuickFocus(
            minutes = plan.focusMinutes,
            skillId = plan.skillId,
            useFocusShield = plan.useFocusShield,
            useStrictFocus = plan.useStrictFocus,
        )
    }

    fun cancelFocusCycle() {
        focusCycleViewModel.cancel()
        breakViewModel.dismissBreakComplete()
    }

    fun saveFocusProfile(
        existingId: String?,
        name: String,
        type: FocusProfileType,
        focusMinutes: Int,
        breakMinutes: Int,
        rounds: Int,
        skillId: Int?,
        useFocusShield: Boolean,
        useStrictFocus: Boolean,
    ) {
        focusProfileViewModel.save(
            existingId = existingId,
            name = name,
            type = type,
            focusMinutes = focusMinutes,
            breakMinutes = breakMinutes,
            rounds = rounds,
            skillId = skillId,
            useFocusShield = useFocusShield,
            useStrictFocus = useStrictFocus,
        )
    }

    fun deleteFocusProfile(profile: FocusProfile) {
        focusProfileViewModel.delete(profile)
    }

    fun startFocusProfile(profile: FocusProfile, validSkillIds: Set<Int>) {
        val skillId = profile.skillId?.takeIf(validSkillIds::contains)
        focusProfileViewModel.markUsed(profile)
        if (profile.type == FocusProfileType.CYCLE) {
            startFocusCycle(
                FocusCyclePlan(
                    focusMinutes = profile.focusMinutes,
                    breakMinutes = profile.breakMinutes,
                    totalRounds = profile.rounds,
                    skillId = skillId,
                    useFocusShield = profile.useFocusShield,
                    useStrictFocus = profile.useStrictFocus,
                ),
            )
        } else {
            battleViewModel.selectAndStartQuickFocus(
                minutes = profile.focusMinutes,
                skillId = skillId,
                useFocusShield = profile.useFocusShield,
                useStrictFocus = profile.useStrictFocus,
            )
        }
    }

    fun skipBreak() {
        breakViewModel.skipBreak()
        focusCycleViewModel.finishBreak()
    }

    fun dismissBreakComplete() {
        breakViewModel.dismissBreakComplete()
    }

    fun setBreakSuggestionsEnabled(enabled: Boolean) {
        breakViewModel.updateBreakSuggestions(enabled)
    }

    fun simulateCompanionNotification(action: String) {
        statusViewModel.simulateCompanionNotification(action)
    }

    fun updateReminderSettings(settings: ReminderSettings) {
        statusViewModel.updateReminderSettings(settings)
    }

    fun updateSessionNote(sessionId: Long, note: String) {
        statusViewModel.updateSessionNote(sessionId, note)
    }

    // --- Dungeon / Boss Management ---
    fun createBoss(
        name: String,
        difficulty: String,
        durationMinutes: Int,
        imagePath: String?,
        dungeonName: String = "Main Realm",
        isRealBoss: Boolean = false,
        deadlineDate: String? = null,
    ) {
        dungeonViewModel.createBoss(
            name = name,
            difficulty = difficulty,
            durationMinutes = durationMinutes,
            imagePath = imagePath,
            dungeonName = dungeonName,
            isRealBoss = isRealBoss,
            deadlineDate = deadlineDate,
        )
    }

    fun deleteBoss(boss: BossEntity) {
        dungeonViewModel.deleteBoss(boss)
    }

    fun createBossStep(bossId: Int, title: String, estimatedMinutes: Int) {
        dungeonViewModel.createBossStep(bossId, title, estimatedMinutes)
    }

    fun updateBossStep(step: BossStepEntity, title: String, estimatedMinutes: Int) {
        dungeonViewModel.updateBossStep(step, title, estimatedMinutes)
    }

    fun setBossStepCompleted(step: BossStepEntity, completed: Boolean) {
        dungeonViewModel.setBossStepCompleted(step, completed)
    }

    fun deleteBossStep(step: BossStepEntity) {
        dungeonViewModel.deleteBossStep(step)
    }

    fun activateRedDungeonXpBoost() {
        dungeonViewModel.activateRedDungeonXpBoost()
    }

    // --- Focus Battle / Free Study Operations ---
    fun selectAndStartBattle(boss: BossEntity) {
        battleViewModel.selectAndStartBattle(boss)
    }

    fun selectAndStartBossStep(boss: BossEntity, step: BossStepEntity) {
        battleViewModel.selectAndStartBossStep(boss, step)
    }

    fun selectAndStartFreeStudy(minutes: Int) {
        battleViewModel.selectAndStartFreeStudy(minutes)
    }

    fun selectAndStartDailyQuest(quest: DailyQuestEntity) {
        battleViewModel.selectAndStartDailyQuest(quest)
    }

    fun pauseBattle(onPaused: () -> Unit = {}) {
        battleViewModel.pauseBattle(onPaused)
    }

    fun resumeBattle() {
        battleViewModel.resumeBattle()
    }

    fun syncFocusSessionTime() {
        battleViewModel.syncFocusSessionTime()
    }

    fun setFocusShieldEnabled(enabled: Boolean) {
        battleViewModel.updateFocusShieldEnabled(enabled)
    }

    fun refreshFocusShield() {
        battleViewModel.refreshFocusShield()
    }

    fun requestStrictFocus() {
        battleViewModel.requestStrictFocus()
    }

    fun clearStrictFocusRequest() {
        battleViewModel.clearStrictFocusRequest()
    }

    fun updateStrictFocusPinningState(isPinned: Boolean) {
        battleViewModel.updateStrictFocusPinningState(isPinned)
    }

    fun markStrictFocusStartAttempted() {
        battleViewModel.markStrictFocusStartAttempted()
    }

    fun markStrictFocusReleased() {
        battleViewModel.markStrictFocusReleased()
    }

    fun abandonActiveBoss(applyHeavyPenalty: Boolean = true) {
        focusCycleViewModel.cancel()
        battleViewModel.abandonActiveBoss(applyHeavyPenalty)
    }

    fun completeActiveBoss() {
        battleViewModel.completeActiveBoss()
    }

    fun endBossStepEarly() {
        battleViewModel.endBossStepEarly()
    }

    fun simulateStudySeconds(seconds: Long) {
        battleViewModel.simulateStudySeconds(seconds)
    }

    fun conquerRealBossManual(boss: BossEntity) {
        battleViewModel.conquerRealBossManual(boss)
    }

    // --- Reward / Shop Operations ---
    fun createReward(name: String, description: String, cost: Int, rewardType: String, rewardValue: Int) {
        shopViewModel.createReward(name, description, cost, rewardType, rewardValue)
    }

    fun deleteReward(reward: RewardItemEntity) {
        shopViewModel.deleteReward(reward)
    }

    fun purchaseReward(reward: RewardItemEntity, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        shopViewModel.purchaseReward(reward, onResult)
    }

    fun useReward(rewardName: String, amountToUse: Float, onResult: (Boolean) -> Unit) {
        shopViewModel.useReward(rewardName, amountToUse, onResult)
    }

    fun useRewardTime(rewardName: String, amountToUse: Float, onResult: (Boolean) -> Unit) {
        shopViewModel.useRewardTime(rewardName, amountToUse, onResult)
    }

    // --- Skill Operations ---
    fun createSkill(name: String, targetMinutes: Int, suggestion: String) {
        skillViewModel.createSkill(name, targetMinutes, suggestion)
    }

    fun deleteSkill(skill: SkillEntity) {
        skillViewModel.deleteSkill(skill)
    }

    fun associateSkillWithBoss(bossId: Int, skillId: Int) {
        skillViewModel.associateSkillWithBoss(bossId, skillId)
    }

    fun getSkillsForBoss(bossId: Int) = skillViewModel.getSkillsForBoss(bossId)

    // --- Daily Quest Operations ---
    fun createDailyQuest(
        title: String,
        durationMinutes: Int,
        skillId: Int?,
        priority: com.amkumirab.solostudying.domain.quest.QuestPriority,
        repeatSchedule: RecurringQuestSchedule?,
    ) {
        dailyQuestViewModel.createQuest(title, durationMinutes, skillId, priority, repeatSchedule)
    }

    fun setDailyQuestCompleted(quest: DailyQuestEntity, completed: Boolean) {
        dailyQuestViewModel.setQuestCompleted(quest, completed)
    }

    fun updateDailyQuest(
        quest: DailyQuestEntity,
        title: String,
        durationMinutes: Int,
        skillId: Int?,
        priority: com.amkumirab.solostudying.domain.quest.QuestPriority,
    ) {
        dailyQuestViewModel.updateQuest(quest, title, durationMinutes, skillId, priority)
    }

    fun deleteDailyQuest(quest: DailyQuestEntity) {
        dailyQuestViewModel.deleteQuest(quest)
    }

    fun updateRecurringQuest(
        quest: RecurringQuestEntity,
        title: String,
        durationMinutes: Int,
        skillId: Int?,
        priority: com.amkumirab.solostudying.domain.quest.QuestPriority,
        repeatSchedule: RecurringQuestSchedule,
    ) {
        dailyQuestViewModel.updateRecurringQuest(
            quest,
            title,
            durationMinutes,
            skillId,
            priority,
            repeatSchedule,
        )
    }

    fun setRecurringQuestActive(quest: RecurringQuestEntity, active: Boolean) {
        dailyQuestViewModel.setRecurringQuestActive(quest, active)
    }

    fun deleteRecurringQuest(quest: RecurringQuestEntity) {
        dailyQuestViewModel.deleteRecurringQuest(quest)
    }

    fun refreshDailyQuests() {
        dailyQuestViewModel.refreshToday()
    }
}

class SoloStudyingViewModelFactory(
    private val repository: SoloStudyingRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SoloStudyingViewModel::class.java)) {
            val statusVM = StatusViewModel(repository, context)
            val dungeonVM = DungeonViewModel(repository, context)
            val focusCycleVM = FocusCycleViewModel(context)
            val focusProfileVM = FocusProfileViewModel(context)
            val battleVM = BattleViewModel(
                repository = repository,
                context = context,
                onSessionFinished = focusCycleVM::onSessionFinished,
            )
            val skillVM = SkillViewModel(repository, context)
            val shopVM = ShopViewModel(repository)
            val tutorialVM = TutorialViewModel(repository, context)
            val breakVM = BreakViewModel(context, onBreakFinished = focusCycleVM::finishBreak)
            val dailyQuestVM = DailyQuestViewModel(repository)
            @Suppress("UNCHECKED_CAST")
            return SoloStudyingViewModel(
                statusVM,
                dungeonVM,
                battleVM,
                skillVM,
                shopVM,
                tutorialVM,
                breakVM,
                dailyQuestVM,
                focusCycleVM,
                focusProfileVM,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
