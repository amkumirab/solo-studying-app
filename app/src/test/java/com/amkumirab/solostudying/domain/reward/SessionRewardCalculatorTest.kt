package com.amkumirab.solostudying.domain.reward

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionRewardCalculatorTest {

    @Test
    fun `boss rewards use one difficulty table`() {
        assertEquals(SessionReward(75, 40), SessionRewardCalculator.completedBoss("Easy"))
        assertEquals(SessionReward(150, 80), SessionRewardCalculator.completedBoss("Medium"))
        assertEquals(SessionReward(350, 180), SessionRewardCalculator.completedBoss("Hard"))
        assertEquals(SessionReward(750, 400), SessionRewardCalculator.completedBoss("Legendary"))
        assertEquals(SessionReward(100, 50), SessionRewardCalculator.completedBoss("Unknown"))
        assertEquals(SessionReward(225, 120), SessionRewardCalculator.manualBoss("Medium"))
    }

    @Test
    fun `timed rewards reject zero duration and preserve partial study credit`() {
        assertEquals(SessionReward(0, 0), SessionRewardCalculator.completedFreeStudy(0L))
        assertEquals(SessionReward(0, 0), SessionRewardCalculator.completedFreeStudy(-60L))
        assertEquals(SessionReward(2, 1), SessionRewardCalculator.completedFreeStudy(90L))
        assertEquals(SessionReward(1, 0), SessionRewardCalculator.suspendedFreeStudy(30L))
        assertEquals(SessionReward(1, 0), SessionRewardCalculator.suspendedBossStudy(30L))
    }

    @Test
    fun `red dungeon penalty increases by level and caps at thirty percent`() {
        val reward = SessionReward(xp = 100, gold = 100)

        assertEquals(reward, SessionRewardCalculator.applyProgressionModifiers(reward, 0, false))
        assertEquals(SessionReward(90, 90), SessionRewardCalculator.applyProgressionModifiers(reward, 1, false))
        assertEquals(SessionReward(80, 80), SessionRewardCalculator.applyProgressionModifiers(reward, 2, false))
        assertEquals(SessionReward(70, 70), SessionRewardCalculator.applyProgressionModifiers(reward, 3, false))
        assertEquals(SessionReward(70, 70), SessionRewardCalculator.applyProgressionModifiers(reward, 7, false))
    }

    @Test
    fun `xp boost applies only while a red dungeon is active`() {
        val reward = SessionReward(xp = 100, gold = 100)

        assertEquals(reward, SessionRewardCalculator.applyProgressionModifiers(reward, 0, true))
        assertEquals(
            SessionReward(xp = 160, gold = 80),
            SessionRewardCalculator.applyProgressionModifiers(reward, 2, true),
        )
    }
}
