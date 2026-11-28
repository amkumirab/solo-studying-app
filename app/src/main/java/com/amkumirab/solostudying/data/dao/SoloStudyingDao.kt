package com.amkumirab.solostudying.data.dao

import androidx.room.*
import com.amkumirab.solostudying.data.entity.*
import kotlinx.coroutines.flow.Flow

private const val VISIBLE_GOAL = """
    goal.archivedAtMillis IS NULL AND (goal.secondGateOfId IS NULL OR EXISTS (
        SELECT 1 FROM bosses AS parent WHERE parent.id = goal.secondGateOfId
        AND parent.archivedAtMillis IS NULL
    ))
"""

@Dao
interface SoloStudyingDao {

    // --- Bosses ---
    @Query("SELECT goal.* FROM bosses AS goal WHERE " + VISIBLE_GOAL + " ORDER BY goal.createdAt DESC")
    fun getAllBosses(): Flow<List<BossEntity>>

    @Query("SELECT goal.* FROM bosses AS goal WHERE goal.id = :id AND " + VISIBLE_GOAL)
    suspend fun getBossById(id: Int): BossEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoss(boss: BossEntity): Long

    @Update
    suspend fun updateBoss(boss: BossEntity)

    @Query("SELECT * FROM bosses WHERE archivedAtMillis IS NOT NULL ORDER BY archivedAtMillis DESC, id DESC")
    fun getArchivedBosses(): Flow<List<BossEntity>>

    @Query("SELECT * FROM bosses WHERE secondGateOfId = :id")
    suspend fun getSecondGate(id: Int): BossEntity?

    @Query("UPDATE bosses SET archivedAtMillis = :archivedAt WHERE id = :id AND archivedAtMillis IS NULL")
    suspend fun archiveBoss(id: Int, archivedAt: Long): Int

    @Query("""
        UPDATE bosses SET archivedAtMillis = NULL WHERE id = :id AND archivedAtMillis IS NOT NULL
        AND (secondGateOfId IS NULL OR secondGateOfId IN (SELECT id FROM bosses WHERE archivedAtMillis IS NULL))
    """)
    suspend fun restoreBoss(id: Int): Int

    @Query("UPDATE bosses SET timeSpentSeconds = :seconds, isCompleted = :completed WHERE id = :id")
    suspend fun updateBossProgress(id: Int, seconds: Long, completed: Boolean)

    // --- Boss study steps ---
    @Query("SELECT * FROM boss_steps ORDER BY bossId ASC, sortOrder ASC, createdAt ASC")
    fun getAllBossSteps(): Flow<List<BossStepEntity>>

    @Query("SELECT * FROM boss_steps WHERE id = :id LIMIT 1")
    suspend fun getBossStepById(id: Int): BossStepEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM boss_steps WHERE bossId = :bossId")
    suspend fun getNextBossStepOrder(bossId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBossStep(step: BossStepEntity): Long

    @Update
    suspend fun updateBossStep(step: BossStepEntity)

    @Delete
    suspend fun deleteBossStep(step: BossStepEntity)

    // --- Dungeons ---
    @Query("SELECT * FROM dungeons ORDER BY id ASC")
    fun getAllDungeons(): Flow<List<DungeonEntity>>

    @Query("SELECT * FROM dungeons WHERE name = :name ORDER BY id ASC LIMIT 1")
    suspend fun getDungeonByName(name: String): DungeonEntity?

    @Update
    suspend fun updateDungeon(dungeon: DungeonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDungeon(dungeon: DungeonEntity): Long

    @Delete
    suspend fun deleteDungeon(dungeon: DungeonEntity)

    // --- Boss-skill relationships ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBossSkill(bossSkill: BossSkillEntity)

    @Query("DELETE FROM boss_skills WHERE bossId = :bossId")
    suspend fun deleteBossSkillsForBoss(bossId: Int)

    @Query("SELECT skills.* FROM skills INNER JOIN boss_skills ON skills.id = boss_skills.skillId WHERE boss_skills.bossId = :bossId")
    fun getSkillsForBoss(bossId: Int): Flow<List<SkillEntity>>

    // --- User Profile ---
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    // --- Rewards ---
    @Query("SELECT * FROM rewards ORDER BY cost ASC")
    fun getAllRewards(): Flow<List<RewardItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: RewardItemEntity): Long

    @Delete
    suspend fun deleteReward(reward: RewardItemEntity)

    // --- Reward Balances ---
    @Query("SELECT * FROM reward_balances ORDER BY rewardName ASC")
    fun getAllBalances(): Flow<List<RewardBalanceEntity>>

    @Query("SELECT * FROM reward_balances WHERE rewardName = :name LIMIT 1")
    suspend fun getBalanceByName(name: String): RewardBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBalance(balance: RewardBalanceEntity)

    // --- Study Sessions ---
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query(
        "SELECT * FROM study_sessions WHERE timestamp >= :startMillis AND timestamp < :endMillis " +
            "ORDER BY timestamp DESC",
    )
    fun getSessionsBetween(startMillis: Long, endMillis: Long): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query("UPDATE study_sessions SET note = :note WHERE id = :sessionId")
    suspend fun updateSessionNote(sessionId: Long, note: String?): Int

    // --- Skills (Time-Based) ---
    @Query("SELECT * FROM skills ORDER BY name ASC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE id = :id LIMIT 1")
    suspend fun getSkillById(id: Int): SkillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity): Long

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Delete
    suspend fun deleteSkill(skill: SkillEntity)

    // --- Daily Quests ---
    @Query("SELECT * FROM daily_quests ORDER BY scheduledDate DESC, isCompleted ASC, priority DESC, createdAt ASC")
    fun getAllDailyQuests(): Flow<List<DailyQuestEntity>>

    @Query("SELECT * FROM daily_quests WHERE id = :id LIMIT 1")
    suspend fun getDailyQuestById(id: Int): DailyQuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyQuest(quest: DailyQuestEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyQuestIfAbsent(quest: DailyQuestEntity): Long

    @Update
    suspend fun updateDailyQuest(quest: DailyQuestEntity)

    @Delete
    suspend fun deleteDailyQuest(quest: DailyQuestEntity)

    // --- Recurring quests ---
    @Query("SELECT * FROM recurring_quests ORDER BY isActive DESC, createdAt ASC")
    fun getAllRecurringQuests(): Flow<List<RecurringQuestEntity>>

    @Query("SELECT * FROM recurring_quests WHERE isActive = 1 ORDER BY createdAt ASC")
    suspend fun getActiveRecurringQuests(): List<RecurringQuestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringQuest(quest: RecurringQuestEntity): Long

    @Update
    suspend fun updateRecurringQuest(quest: RecurringQuestEntity)

    @Delete
    suspend fun deleteRecurringQuest(quest: RecurringQuestEntity)
}
