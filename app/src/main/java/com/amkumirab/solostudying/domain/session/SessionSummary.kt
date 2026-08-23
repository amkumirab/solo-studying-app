package com.amkumirab.solostudying.domain.session

enum class SessionEndState {
    Completed,
    Suspended,
    ManualConquest,
}

data class ProgressSummary(
    val name: String,
    val beforeSeconds: Long,
    val afterSeconds: Long,
    val targetSeconds: Long,
) {
    val progressBefore: Float
        get() = fraction(beforeSeconds)

    val progressAfter: Float
        get() = fraction(afterSeconds)

    val gainedSeconds: Long
        get() = (afterSeconds - beforeSeconds).coerceAtLeast(0L)

    private fun fraction(seconds: Long): Float = if (targetSeconds <= 0L) {
        0f
    } else {
        (seconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
    }
}

data class SessionSummary(
    val sessionId: Long,
    val subject: String,
    val durationSeconds: Long,
    val xpEarned: Int,
    val goldEarned: Int,
    val endState: SessionEndState,
    val bossProgress: ProgressSummary? = null,
    val skillProgress: ProgressSummary? = null,
    val previousLevel: Int,
    val currentLevel: Int,
    val previousStreak: Int,
    val currentStreak: Int,
    val streakMessage: String? = null,
    val skillUnlocked: Boolean = false,
)
