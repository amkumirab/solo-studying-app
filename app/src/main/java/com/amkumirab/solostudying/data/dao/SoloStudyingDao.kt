package com.amkumirab.solostudying.data.dao

import androidx.room.*
import com.amkumirab.solostudying.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SoloStudyingDao {

    // --- Bosses ---
    @Query("SELECT * FROM bosses ORDER BY createdAt DESC")
    fun getAllBosses(): Flow<List<BossEntity>>

    @Query("SELECT * FROM bosses WHERE id = :id LIMIT 1")
    suspend fun getBossById(id: Int): BossEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoss(boss: BossEntity): Long

    @Update
    suspend fun updateBoss(boss: BossEntity)

    @Delete
    suspend fun deleteBoss(boss: BossEntity)

    // --- Dungeons ---
    @Query("SELECT * FROM dungeons ORDER BY id ASC")
    fun getAllDungeons(): Flow<List<DungeonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDungeon(dungeon: DungeonEntity): Long

    @Delete
    suspend fun deleteDungeon(dungeon: DungeonEntity)

    // --- Boss-skill relationships ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBossSkill(bossSkill: BossSkillEntity)

    @Query("DELETE FROM boss_skills WHERE bossId = :bossId")
    suspend fun deleteBossSkillsForBoss(bossId: Int)

    @Query("SELECT * FROM skills INNER JOIN boss_skills ON skills.id = boss_skills.skillId WHERE boss_skills.bossId = :bossId")
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

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
}
