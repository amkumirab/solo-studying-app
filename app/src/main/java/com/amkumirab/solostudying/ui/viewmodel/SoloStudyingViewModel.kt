package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.domain.session.SessionSummary
import com.amkumirab.solostudying.notification.ReminderSettings

class SoloStudyingViewModel(
    val statusViewModel: StatusViewModel,
    val dungeonViewModel: DungeonViewModel,
    val battleViewModel: BattleViewModel,
    val skillViewModel: SkillViewModel,
    val shopViewModel: ShopViewModel,
    val tutorialViewModel: TutorialViewModel,
    val breakViewModel: BreakViewModel,
    val dailyQuestViewModel: DailyQuestViewModel,
) : ViewModel() {

    // --- StateFlow Delegation to Feature ViewModels ---
    val bosses = dungeonViewModel.bosses
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
    val activeBreak get() = breakViewModel.activeBreak
    val breakTimeLeftSeconds: Long get() = breakViewModel.breakTimeLeftSeconds
    val showBreakComplete: Boolean get() = breakViewModel.showBreakComplete
    val breakSuggestionsEnabled: Boolean get() = breakViewModel.breakSuggestionsEnabled

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

    fun skipBreak() {
        breakViewModel.skipBreak()
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

    fun activateRedDungeonXpBoost() {
        dungeonViewModel.activateRedDungeonXpBoost()
    }

    // --- Focus Battle / Free Study Operations ---
    fun selectAndStartBattle(boss: BossEntity) {
        battleViewModel.selectAndStartBattle(boss)
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

    fun abandonActiveBoss(applyHeavyPenalty: Boolean = true) {
        battleViewModel.abandonActiveBoss(applyHeavyPenalty)
    }

    fun completeActiveBoss() {
        battleViewModel.completeActiveBoss()
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
    ) {
        dailyQuestViewModel.createQuest(title, durationMinutes, skillId, priority)
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
            val battleVM = BattleViewModel(repository, context)
            val skillVM = SkillViewModel(repository, context)
            val shopVM = ShopViewModel(repository)
            val tutorialVM = TutorialViewModel(repository, context)
            val breakVM = BreakViewModel(context)
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
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
