package com.amkumirab.solostudying.domain.reward

data class SessionReward(
    val xp: Int,
    val gold: Int,
)

object SessionRewardCalculator {

    fun completedBoss(difficulty: String): SessionReward = when (difficulty) {
        "Easy" -> SessionReward(xp = 75, gold = 40)
        "Medium" -> SessionReward(xp = 150, gold = 80)
        "Hard" -> SessionReward(xp = 350, gold = 180)
        "Legendary" -> SessionReward(xp = 750, gold = 400)
        else -> SessionReward(xp = 100, gold = 50)
    }

    fun manualBoss(difficulty: String): SessionReward =
        completedBoss(difficulty).scaledBy(1.5f)

    fun completedFreeStudy(durationSeconds: Long): SessionReward =
        timedReward(durationSeconds, xpPerMinute = 1.5f, goldPerMinute = 0.8f)

    fun suspendedFreeStudy(durationSeconds: Long): SessionReward =
        timedReward(durationSeconds, xpPerMinute = 0.8f, goldPerMinute = 0.4f)

    fun suspendedBossStudy(durationSeconds: Long): SessionReward =
        timedReward(durationSeconds, xpPerMinute = 1.5f, goldPerMinute = 0.8f)

    fun applyProgressionModifiers(
        reward: SessionReward,
        redDungeonDays: Int,
        isXpBoostActive: Boolean,
    ): SessionReward {
        val penaltyMultiplier = when {
            redDungeonDays >= 3 -> 0.7f
            redDungeonDays == 2 -> 0.8f
            redDungeonDays == 1 -> 0.9f
            else -> 1f
        }
        val xpBoostMultiplier = if (redDungeonDays > 0 && isXpBoostActive) 2f else 1f
        return SessionReward(
            xp = (reward.xp * penaltyMultiplier * xpBoostMultiplier)
                .toInt()
                .coerceAtLeast(if (reward.xp > 0) 1 else 0),
            gold = (reward.gold * penaltyMultiplier).toInt(),
        )
    }

    private fun timedReward(
        durationSeconds: Long,
        xpPerMinute: Float,
        goldPerMinute: Float,
    ): SessionReward {
        val safeDuration = durationSeconds.coerceAtLeast(0L)
        if (safeDuration == 0L) return SessionReward(xp = 0, gold = 0)

        val minutesStudied = safeDuration / 60f
        return SessionReward(
            xp = (minutesStudied * xpPerMinute).toInt().coerceAtLeast(1),
            gold = (minutesStudied * goldPerMinute).toInt(),
        )
    }

    private fun SessionReward.scaledBy(multiplier: Float): SessionReward = SessionReward(
        xp = (xp * multiplier).toInt(),
        gold = (gold * multiplier).toInt(),
    )
}
