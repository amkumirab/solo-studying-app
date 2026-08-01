package com.amkumirab.solostudying.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dungeons")
data class DungeonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val status: String = "Locked", // "Unlocked", "Locked"
    val unlockedTitle: String = "Novice Scholar"
)

@Entity(tableName = "bosses")
data class BossEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val difficulty: String, // "Easy", "Medium", "Hard", "Legendary"
    val requiredMinutes: Int,
    val imagePath: String? = null,
    val timeSpentSeconds: Long = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val dungeonName: String = "Main Realm", // Category folder like Semester 5, Java Course, Life Goals
    val isRealBoss: Boolean = false // Real-life goals (exams, deliverables) requiring manual confirmation
)

@Entity(tableName = "boss_skills", primaryKeys = ["bossId", "skillId"])
data class BossSkillEntity(
    val bossId: Int,
    val skillId: Int
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetMinutes: Int,
    val spentSeconds: Long = 0,
    val isUnlocked: Boolean = false,
    val suggestion: String = "Estimated study duration required"
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Solo Hero",
    val hasCompletedOnboarding: Boolean = false,
    val hasCompletedTutorial: Boolean = false,
    val level: Int = 1,
    val xp: Int = 0,
    val gold: Int = 100, // Starter resources
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStudyDate: String? = null, // "YYYY-MM-DD" style
    val totalStudyTimeSeconds: Long = 0,
    val totalSessionCount: Int = 0,
    val totalBossesDefeated: Int = 0,
    val totalGoldEarned: Int = 100,
    val totalXpEarned: Int = 0,
    val totalFreeStudySeconds: Long = 0, // In Free Study Mode
    val redDungeonDays: Int = 0, // 0 means clean, expands to 7 days cap representing inactivity pressure
    val scheduleDays: String = "Mon,Wed,Fri", // Default scheduled study days
    val scheduleMinutesPerDay: Int = 45, // Target daily minutes focus
    val scheduleFlexibility: String = "Medium", // "Low", "Medium", "High"
    val scheduleWeekdayMinutes: String = "45,45,45,45,45,45,45", // Targets for Mon, Tue, Wed, Thu, Fri, Sat, Sun
    val isRedDungeonBoostActive: Boolean = false,
    val totalRedDungeonsCleared: Int = 0,
    
    // Onboarding preferences
    val hunterClass: String = "Shadow Monarch", // "Shadow Monarch", "Academic Sage", "Code Crusader", "Creative Mystic"
    val mainGoal: String = "Academic Mastery", // "Conquer Exams", "Learn Coding", "Master a Language", "General Growth"
    val learningPath: String = "Sage Path" // "Theoretical Sage", "Code Crusader", "Polyglot Ranger", "Creative Mystic"
) {
    fun getTargetMinutesForCalendarDay(calendarDay: Int): Int {
        val index = when (calendarDay) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            java.util.Calendar.SATURDAY -> 5
            java.util.Calendar.SUNDAY -> 6
            else -> 0
        }
        val list = scheduleWeekdayMinutes.split(",").mapNotNull { it.trim().toIntOrNull() }
        return if (list.size == 7) list[index] else scheduleMinutesPerDay
    }
}

@Entity(tableName = "rewards")
data class RewardItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val cost: Int,
    val rewardType: String, // "Time-Based", "One-Time"
    val rewardValue: Int = 1, // e.g., 1 hour for Time-Based
    val isCustom: Boolean = true
)

@Entity(tableName = "reward_balances")
data class RewardBalanceEntity(
    @PrimaryKey val rewardName: String, // e.g. "Gaming Time" or "Anime Episode"
    val availableHours: Float = 0f,
    val purchaseCount: Int = 0
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bossId: Int?,
    val bossName: String?,
    val durationSeconds: Long,
    val xpEarned: Int,
    val goldEarned: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val wasCompleted: Boolean,
    val isFreeStudy: Boolean = false // Indicates if session was spent on Free Study
)
