package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.notification.NotificationHelper
import com.amkumirab.solostudying.notification.NotificationReceiver
import com.amkumirab.solostudying.notification.ReminderSettings
import com.amkumirab.solostudying.notification.ReminderSettingsStore
import com.amkumirab.solostudying.sound.RpgSoundManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class MissedDayUpdate(
    val profile: UserProfileEntity,
    val message: String,
)

internal fun calculateMissedDayUpdate(
    profile: UserProfileEntity,
    today: LocalDate,
): MissedDayUpdate? {
    val lastStudyDate = profile.lastStudyDate
        ?.let { date -> runCatching { LocalDate.parse(date) }.getOrNull() }
        ?: return null
    val missedDays = ChronoUnit.DAYS.between(lastStudyDate, today)
    if (missedDays <= 1L) return null

    val redDungeonLevel = missedDays.coerceIn(1L, 3L).toInt()
    val updatedRedDungeonLevel = maxOf(profile.redDungeonDays, redDungeonLevel)
    val shouldApplyGoldPenalty = profile.currentStreak > 0

    if (!shouldApplyGoldPenalty && updatedRedDungeonLevel == profile.redDungeonDays) {
        return null
    }

    val penaltyGold = if (shouldApplyGoldPenalty) 25 else 0
    val updatedProfile = profile.copy(
        currentStreak = 0,
        gold = (profile.gold - penaltyGold).coerceAtLeast(0),
        redDungeonDays = updatedRedDungeonLevel,
    )
    val message = if (shouldApplyGoldPenalty) {
        "Streak Broken! You missed $missedDays days of studying. Streak reset to 0. " +
            "Lost $penaltyGold Gold. RED GATES ACTIVE: Level $updatedRedDungeonLevel " +
            "breach detected. Purification required!"
    } else {
        "The Red Dungeon expanded to Level $updatedRedDungeonLevel after $missedDays " +
            "days away. Return to studying to reduce it."
    }

    return MissedDayUpdate(profile = updatedProfile, message = message)
}

class StatusViewModel(
    private val repository: SoloStudyingRepository,
    context: Context,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val context = context.applicationContext
    private val reminderSettingsStore = ReminderSettingsStore(this.context)

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val sessions: StateFlow<List<StudySessionEntity>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val reminderSettings: StateFlow<ReminderSettings> = reminderSettingsStore.settings

    // Notification states for RPG-like banners
    var showStreakResetToast by mutableStateOf<String?>(null)
    var showLevelUpToast by mutableStateOf<Pair<Int, Int>?>(null) // Pair(oldLevel, newLevel)
    var showPenaltyToast by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            userProfile.first { true } // Trigger first emission
            val profile = repository.getProfileSync()
            if (profile == null) {
                // Initialize default profile
                repository.insertOrUpdateProfile(UserProfileEntity())
            } else {
                checkStreakOnStartup(profile)
            }
        }
    }

    private suspend fun checkStreakOnStartup(profile: UserProfileEntity) {
        val update = calculateMissedDayUpdate(profile, todayProvider()) ?: return
        repository.insertOrUpdateProfile(update.profile)
        showStreakResetToast = update.message
    }

    fun finishOnboarding(
        name: String,
        hunterClass: String,
        mainGoal: String,
        learningPath: String,
        scheduleDays: String,
        scheduleMinutes: Int,
        scheduleFlexibility: String,
        weekdayMinutes: String
    ) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: UserProfileEntity()
            val updated = current.copy(
                name = name,
                hunterClass = hunterClass,
                mainGoal = mainGoal,
                learningPath = learningPath,
                scheduleDays = scheduleDays,
                scheduleMinutesPerDay = scheduleMinutes,
                scheduleFlexibility = scheduleFlexibility,
                scheduleWeekdayMinutes = weekdayMinutes,
                hasCompletedOnboarding = true,
                gold = 100,
                xp = 0,
                level = 1,
                currentStreak = 1,
                lastStudyDate = todayProvider().toString()
            )
            repository.insertOrUpdateProfile(updated)
        }
    }

    fun finishOnboardingLegacy(name: String) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: UserProfileEntity()
            val updated = current.copy(
                name = name,
                hasCompletedOnboarding = true,
                currentStreak = 1,
                lastStudyDate = todayProvider().toString()
            )
            repository.insertOrUpdateProfile(updated)
        }
    }

    fun updateScheduleWithWeekdays(days: String, mins: Int, flexibility: String, weekdayMins: String) {
        viewModelScope.launch {
            val profile = repository.getProfileSync() ?: return@launch
            val updated = profile.copy(
                scheduleDays = days,
                scheduleMinutesPerDay = mins,
                scheduleFlexibility = flexibility,
                scheduleWeekdayMinutes = weekdayMins
            )
            repository.insertOrUpdateProfile(updated)
        }
    }

    fun clearNotifications() {
        showStreakResetToast = null
        showLevelUpToast = null
        showPenaltyToast = null
    }

    fun simulateCompanionNotification(action: String) {
        viewModelScope.launch {
            val intent = android.content.Intent(context, NotificationReceiver::class.java).apply {
                this.action = action
            }
            context.sendBroadcast(intent)
        }
    }

    fun updateReminderSettings(settings: ReminderSettings) {
        reminderSettingsStore.save(settings)
        NotificationHelper.scheduleDailyAlarms(context, settings)
    }

    // Basic XP/Gold modification interface for VM communication
    fun awardRewards(xpGained: Int, goldGained: Int) {
        viewModelScope.launch {
            val profile = repository.getProfileSync() ?: return@launch
            var newXp = profile.xp + xpGained
            var newLevel = profile.level
            var nextLevelXp = newLevel * 150
            
            while (newXp >= nextLevelXp) {
                newXp -= nextLevelXp
                newLevel++
                nextLevelXp = newLevel * 150
                showLevelUpToast = Pair(profile.level, newLevel)
                RpgSoundManager.playLevelUpSound()
            }

            val updatedProfile = profile.copy(
                xp = newXp,
                level = newLevel,
                gold = profile.gold + goldGained,
                totalGoldEarned = profile.totalGoldEarned + goldGained,
                totalXpEarned = profile.totalXpEarned + xpGained
            )
            repository.insertOrUpdateProfile(updatedProfile)
        }
    }

    fun resetProfileData() {
        viewModelScope.launch {
            repository.insertOrUpdateProfile(UserProfileEntity(id = 1))
        }
    }
}
