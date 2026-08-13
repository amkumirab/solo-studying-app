package com.amkumirab.solostudying.data.repository

import androidx.room.withTransaction
import com.amkumirab.solostudying.data.dao.SoloStudyingDao
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.*
import kotlinx.coroutines.flow.Flow

enum class RewardPurchaseStatus {
    Purchased,
    InsufficientGold,
    MissingProfile,
}

class SoloStudyingRepository(private val database: SoloStudyingDatabase) {

    private val dao: SoloStudyingDao = database.soloStudyingDao()

    val allBosses: Flow<List<BossEntity>> = dao.getAllBosses()
    val allDungeons: Flow<List<DungeonEntity>> = dao.getAllDungeons()
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    val allRewards: Flow<List<RewardItemEntity>> = dao.getAllRewards()
    val allBalances: Flow<List<RewardBalanceEntity>> = dao.getAllBalances()
    val allSessions: Flow<List<StudySessionEntity>> = dao.getAllSessions()
    val allSkills: Flow<List<SkillEntity>> = dao.getAllSkills()

    suspend fun getBossById(id: Int): BossEntity? {
        return dao.getBossById(id)
    }

    suspend fun insertBoss(boss: BossEntity): Long {
        return dao.insertBoss(boss)
    }

    suspend fun updateBoss(boss: BossEntity) {
        dao.updateBoss(boss)
    }

    suspend fun deleteBoss(boss: BossEntity) {
        dao.deleteBoss(boss)
    }

    // --- Dungeon Operations ---
    suspend fun insertDungeon(dungeon: DungeonEntity): Long {
        return dao.insertDungeon(dungeon)
    }

    suspend fun deleteDungeon(dungeon: DungeonEntity) {
        dao.deleteDungeon(dungeon)
    }

    // --- Boss Skill Cross-Reference Operations ---
    suspend fun insertBossSkill(bossSkill: BossSkillEntity) {
        dao.insertBossSkill(bossSkill)
    }

    suspend fun deleteBossSkillsForBoss(bossId: Int) {
        dao.deleteBossSkillsForBoss(bossId)
    }

    fun getSkillsForBoss(bossId: Int): Flow<List<SkillEntity>> {
        return dao.getSkillsForBoss(bossId)
    }

    // --- Profile Operations ---
    suspend fun getProfileSync(): UserProfileEntity? {
        return dao.getProfileSync()
    }

    suspend fun insertOrUpdateProfile(profile: UserProfileEntity) {
        dao.insertOrUpdateProfile(profile)
    }

    // --- Reward Operations ---
    suspend fun insertReward(reward: RewardItemEntity): Long {
        return dao.insertReward(reward)
    }

    suspend fun deleteReward(reward: RewardItemEntity) {
        dao.deleteReward(reward)
    }

    suspend fun getBalanceByName(name: String): RewardBalanceEntity? {
        return dao.getBalanceByName(name)
    }

    suspend fun insertOrUpdateBalance(balance: RewardBalanceEntity) {
        dao.insertOrUpdateBalance(balance)
    }

    suspend fun purchaseReward(reward: RewardItemEntity): RewardPurchaseStatus =
        database.withTransaction {
            val profile = dao.getProfileSync() ?: return@withTransaction RewardPurchaseStatus.MissingProfile
            if (profile.gold < reward.cost) {
                return@withTransaction RewardPurchaseStatus.InsufficientGold
            }

            val currentBalance = dao.getBalanceByName(reward.name)
                ?: RewardBalanceEntity(rewardName = reward.name)
            val purchasedAmount = if (reward.rewardType == "Time-Based") {
                reward.rewardValue.toFloat()
            } else {
                1f
            }

            dao.insertOrUpdateProfile(profile.copy(gold = profile.gold - reward.cost))
            dao.insertOrUpdateBalance(
                currentBalance.copy(
                    availableHours = currentBalance.availableHours + purchasedAmount,
                    purchaseCount = currentBalance.purchaseCount + 1,
                ),
            )
            RewardPurchaseStatus.Purchased
        }

    suspend fun useReward(rewardName: String, amountToUse: Float): Boolean =
        database.withTransaction {
            if (!amountToUse.isFinite() || amountToUse <= 0f) {
                return@withTransaction false
            }
            val currentBalance = dao.getBalanceByName(rewardName)
                ?: return@withTransaction false
            if (currentBalance.availableHours < amountToUse) {
                return@withTransaction false
            }

            dao.insertOrUpdateBalance(
                currentBalance.copy(
                    availableHours = currentBalance.availableHours - amountToUse,
                ),
            )
            true
        }

    suspend fun insertSession(session: StudySessionEntity): Long {
        return dao.insertSession(session)
    }

    // --- Skills CRUD ---
    suspend fun getSkillById(id: Int): SkillEntity? {
        return dao.getSkillById(id)
    }

    suspend fun insertSkill(skill: SkillEntity): Long {
        return dao.insertSkill(skill)
    }

    suspend fun updateSkill(skill: SkillEntity) {
        dao.updateSkill(skill)
    }

    suspend fun deleteSkill(skill: SkillEntity) {
        dao.deleteSkill(skill)
    }

    suspend fun <T> runInTransaction(
        block: suspend SoloStudyingRepository.() -> T,
    ): T = database.withTransaction {
        this@SoloStudyingRepository.block()
    }
}
