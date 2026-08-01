package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository

class SoloStudyingViewModel(
    val statusViewModel: StatusViewModel,
    val dungeonViewModel: DungeonViewModel,
    val battleViewModel: BattleViewModel,
    val skillViewModel: SkillViewModel,
    val shopViewModel: ShopViewModel,
    val tutorialViewModel: TutorialViewModel
) : ViewModel() {

    // --- StateFlow Delegation to Feature ViewModels ---
    val bosses = dungeonViewModel.bosses
    val dungeons = dungeonViewModel.dungeons
    val userProfile = statusViewModel.userProfile
    val rewards = shopViewModel.rewards
    val balances = shopViewModel.balances
    val sessions = statusViewModel.sessions
    val skills = skillViewModel.skills
    val tutorialState = tutorialViewModel.uiState

    // --- Active Timer State Delegation ---
    val activeBoss: BossEntity? get() = battleViewModel.activeBoss
    val isBattleActive: Boolean get() = battleViewModel.isBattleActive
    val isBattlePaused: Boolean get() = battleViewModel.isBattlePaused
    val isFreeStudyActive: Boolean get() = battleViewModel.isFreeStudyActive
    val battleTimeLeftSeconds: Long get() = battleViewModel.battleTimeLeftSeconds
    val battleTimeSpentSeconds: Long get() = battleViewModel.battleTimeSpentSeconds

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

    fun simulateCompanionNotification(action: String) {
        statusViewModel.simulateCompanionNotification(action)
    }

    // --- Dungeon / Boss Management ---
    fun createBoss(
        name: String,
        difficulty: String,
        durationMinutes: Int,
        imagePath: String?,
        dungeonName: String = "Main Realm",
        isRealBoss: Boolean = false
    ) {
        dungeonViewModel.createBoss(name, difficulty, durationMinutes, imagePath, dungeonName, isRealBoss)
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

    fun pauseBattle() {
        battleViewModel.pauseBattle()
    }

    fun resumeBattle() {
        battleViewModel.resumeBattle()
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
            val shopVM = ShopViewModel(repository, context)
            val tutorialVM = TutorialViewModel(repository, context)
            @Suppress("UNCHECKED_CAST")
            return SoloStudyingViewModel(statusVM, dungeonVM, battleVM, skillVM, shopVM, tutorialVM) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
