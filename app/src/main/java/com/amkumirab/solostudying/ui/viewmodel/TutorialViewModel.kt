package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.BossSkillEntity
import com.amkumirab.solostudying.data.entity.DungeonEntity
import com.amkumirab.solostudying.data.entity.SkillEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TutorialUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 12,
    
    // Step 5 Interactive Battle Simulation
    val fakeBossHp: Int = 600,
    val fakeBossMaxHp: Int = 600,
    val damageNumbers: List<String> = emptyList(), // e.g. "-30 HP", "+30 XP"
    val showBattleCelebration: Boolean = false,

    // Step 11 Wizard Inputs
    val dungeonName: String = "University Semester",
    val bossName: String = "Database Exam",
    val estimatedHours: Int = 20,
    val selectedSkillName: String = "Pomodoro Concentration",
    val scheduleDays: Set<String> = setOf("Mon", "Wed", "Fri"),
    val scheduleMinutes: Int = 45,

    // Flow checks
    val isSkipConfirmationVisible: Boolean = false,
    val isCompleted: Boolean = false
)

class TutorialViewModel(
    private val repository: SoloStudyingRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TutorialUiState())
    val uiState: StateFlow<TutorialUiState> = _uiState.asStateFlow()

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("solo_studying_tutorial_prefs", Context.MODE_PRIVATE)
            val hasOpenedBefore = prefs.getBoolean("has_opened_before", false)
            val isReplaying = prefs.getBoolean("is_replaying_tutorial", false)

            val profile = repository.getProfileSync()
            if (profile != null) {
                if (!hasOpenedBefore) {
                    // Record the first launch before showing onboarding.
                    prefs.edit().putBoolean("has_opened_before", true).apply()
                } else if (!isReplaying) {
                    // Returning user and not explicitly replaying -> skip tutorial automatically
                    if (!profile.hasCompletedTutorial) {
                        repository.insertOrUpdateProfile(profile.copy(hasCompletedTutorial = true))
                    }
                }
            }
        }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < _uiState.value.totalSteps) {
            _uiState.update { it.copy(currentStep = current + 1) }
        } else {
            completeTutorial()
        }
    }

    fun prevStep() {
        val current = _uiState.value.currentStep
        if (current > 1) {
            _uiState.update { it.copy(currentStep = current - 1) }
        }
    }

    fun jumpToStep(step: Int) {
        if (step in 1.._uiState.value.totalSteps) {
            _uiState.update { it.copy(currentStep = step) }
        }
    }

    fun showSkipConfirmation(visible: Boolean) {
        _uiState.update { it.copy(isSkipConfirmationVisible = visible) }
    }

    // Step 5 Interactive Simulation
    fun simulateAttack() {
        val currentHp = _uiState.value.fakeBossHp
        if (currentHp > 0) {
            val dmg = 30
            val newHp = (currentHp - dmg).coerceAtLeast(0)
            val isDefeated = newHp == 0
            _uiState.update { state ->
                state.copy(
                    fakeBossHp = newHp,
                    damageNumbers = state.damageNumbers + listOf("-$dmg HP", "+30 XP", "+15 Gold"),
                    showBattleCelebration = isDefeated
                )
            }
        }
    }

    fun resetFakeBoss() {
        _uiState.update { state ->
            state.copy(
                fakeBossHp = 600,
                damageNumbers = emptyList(),
                showBattleCelebration = false
            )
        }
    }

    // Step 11 Wizard Updates
    fun updateDungeonName(name: String) {
        _uiState.update { it.copy(dungeonName = name) }
    }

    fun updateBossName(name: String) {
        _uiState.update { it.copy(bossName = name) }
    }

    fun updateEstimatedHours(hours: Int) {
        _uiState.update { it.copy(estimatedHours = hours.coerceIn(1, 200)) }
    }

    fun updateSelectedSkill(skillName: String) {
        _uiState.update { it.copy(selectedSkillName = skillName) }
    }

    fun toggleScheduleDay(day: String) {
        _uiState.update { state ->
            val days = state.scheduleDays.toMutableSet()
            if (days.contains(day)) {
                if (days.size > 1) days.remove(day) // Keep at least 1 day scheduled
            } else {
                days.add(day)
            }
            state.copy(scheduleDays = days)
        }
    }

    fun updateScheduleMinutes(minutes: Int) {
        _uiState.update { it.copy(scheduleMinutes = minutes.coerceIn(15, 240)) }
    }

    // Finish Wizard (Step 11 -> Step 12)
    fun createFirstMissionAndCelebrate() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // Create first dungeon
            val dungeonId = repository.insertDungeon(
                DungeonEntity(
                    name = state.dungeonName,
                    description = "Your first major chapter. Conquer your goals step by step.",
                    status = "Unlocked",
                    unlockedTitle = "Newborn Hunter"
                )
            )

            // Find or create selected skill if it doesn't exist
            val allSkills = repository.allSkills.first()
            var skill = allSkills.find { it.name.equals(state.selectedSkillName, ignoreCase = true) }
            if (skill == null) {
                val newSkillId = repository.insertSkill(
                    SkillEntity(
                        name = state.selectedSkillName,
                        targetMinutes = state.estimatedHours * 60,
                        suggestion = "Skill created during your hunter awakening ceremony."
                    )
                )
                skill = repository.getSkillById(newSkillId.toInt())
            }

            // Create first Boss
            val bossId = repository.insertBoss(
                BossEntity(
                    name = state.bossName,
                    difficulty = "Medium", // Standard medium boss
                    requiredMinutes = state.estimatedHours * 60,
                    dungeonName = state.dungeonName,
                    isRealBoss = true // Wizard boss
                )
            )

            if (skill != null) {
                repository.insertBossSkill(
                    BossSkillEntity(
                        bossId = bossId.toInt(),
                        skillId = skill.id
                    )
                )
            }

            // Grant rewards & update profile
            val profile = repository.getProfileSync() ?: UserProfileEntity()
            
            // +100 XP, +50 Gold, unlock title/onboarding flags
            val currentXp = profile.xp + 100
            val levelUpThreshold = profile.level * 150
            var newXp = currentXp
            var newLevel = profile.level
            while (newXp >= levelUpThreshold) {
                newXp -= levelUpThreshold
                newLevel++
            }

            repository.insertOrUpdateProfile(
                profile.copy(
                    xp = newXp,
                    level = newLevel,
                    gold = profile.gold + 50,
                    totalXpEarned = profile.totalXpEarned + 100,
                    totalGoldEarned = profile.totalGoldEarned + 50,
                    hasCompletedOnboarding = true, // Auto-mark onboarding completed too
                    hasCompletedTutorial = false, // Not fully completed yet, until Step 12 is acknowledged
                    // Set detailed schedule to match
                    scheduleDays = state.scheduleDays.joinToString(","),
                    scheduleMinutesPerDay = state.scheduleMinutes,
                    scheduleWeekdayMinutes = List(7) { state.scheduleMinutes }.joinToString(",")
                )
            )

            // Proceed to Step 12
            _uiState.update { it.copy(currentStep = 12) }
        }
    }

    // Final Completion
    fun completeTutorial() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("solo_studying_tutorial_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_replaying_tutorial", false).apply()

            val profile = repository.getProfileSync() ?: UserProfileEntity()
            repository.insertOrUpdateProfile(
                profile.copy(
                    hasCompletedTutorial = true,
                    hasCompletedOnboarding = true // Ensure onboarding is marked complete as well
                )
            )
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    fun replayTutorial() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("solo_studying_tutorial_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_replaying_tutorial", true).apply()

            val profile = repository.getProfileSync() ?: UserProfileEntity()
            repository.insertOrUpdateProfile(
                profile.copy(hasCompletedTutorial = false)
            )
            _uiState.update {
                TutorialUiState(
                    currentStep = 1,
                    isCompleted = false
                )
            }
        }
    }
}
