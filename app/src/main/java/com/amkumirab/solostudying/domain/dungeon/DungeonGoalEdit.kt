package com.amkumirab.solostudying.domain.dungeon

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate

internal const val MAX_GOAL_MINUTES = 100_000
internal val goalDifficulties = listOf("Easy", "Medium", "Hard", "Legendary")

data class DungeonGoalEdit(
    val name: String,
    val difficulty: String,
    val minutes: Int,
    val deadline: LocalDate?,
) {
    fun isValidFor(goal: BossEntity): Boolean =
        name.isNotBlank() && name.trim().length <= maxOf(100, goal.name.length) &&
            difficulty in goalDifficulties &&
            (minutes == goal.requiredMinutes ||
                (!goal.isCompleted && minutes in 1..maxOf(MAX_GOAL_MINUTES, goal.requiredMinutes) &&
                    minutes * 60L > goal.timeSpentSeconds))
}
