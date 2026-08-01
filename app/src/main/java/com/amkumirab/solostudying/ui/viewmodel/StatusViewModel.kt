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
import com.amkumirab.solostudying.notification.NotificationReceiver
import com.amkumirab.solostudying.sound.RpgSoundManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatusViewModel(
    private val repository: SoloStudyingRepository,
    private val context: Context
) : ViewModel() {

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
        val lastDateStr = profile.lastStudyDate ?: return
        val currentStreak = profile.currentStreak

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val lastDate = sdf.parse(lastDateStr) ?: return
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val diffMs = today.time - lastDate.time
            val diffDays = diffMs / (1000 * 60 * 60 * 24)

            // If more than 1 day has passed, the streak is broken and Red Dungeon expands!
            if (diffDays > 1) {
                val penaltyGold = 25
                val updatedGold = (profile.gold - penaltyGold).coerceAtLeast(0)
                // Implement progressive difficulty for Red Dungeon: level is based on missed days up to 3 (or higher)
                val dungeonGrowth = diffDays.toInt().coerceAtMost(3) // Cap at level 3
                
                val updatedProfile = profile.copy(
                    currentStreak = 0,
                    gold = updatedGold,
                    redDungeonDays = dungeonGrowth
                )
                repository.insertOrUpdateProfile(updatedProfile)
                showStreakResetToast = "Streak Broken! You missed $diffDays days of studying. Streak reset to 0. Lost $penaltyGold Gold. RED GATES ACTIVE: Level $dungeonGrowth breach detected. Purification required!"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                lastStudyDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
                lastStudyDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
