package com.amkumirab.solostudying.domain.session

import com.amkumirab.solostudying.data.entity.BossEntity

/** A focus block is bounded independently of the course's total study budget. */
fun goalSessionSeconds(boss: BossEntity, minutes: Int): Long {
    require(minutes in 1..480) { "Session duration must be between 1 and 480 minutes" }
    val requested = minutes * 60L
    val remaining = (boss.requiredMinutes * 60L - boss.timeSpentSeconds).coerceAtLeast(0L)
    return if (boss.isCompleted || boss.isRealBoss || remaining == 0L) requested else minOf(requested, remaining)
}

fun goalCompletedBySession(boss: BossEntity, afterSeconds: Long): Boolean =
    !boss.isCompleted && !boss.isRealBoss && afterSeconds >= boss.requiredMinutes * 60L

fun suggestedGoalSessionMinutes(dailyMinutes: Int?): Int = dailyMinutes?.takeIf { it > 0 }?.coerceAtMost(60) ?: 25
