package com.amkumirab.solostudying.ui.screens

import androidx.compose.ui.graphics.Color
import com.amkumirab.solostudying.ui.theme.*

internal fun getRankLabel(level: Int): String {
    return when {
        level <= 2 -> "Novice Apprentice 🎓"
        level <= 5 -> "Apprentice Spellcaster 🔮"
        level <= 9 -> "Academic Vanguard ⚔️"
        level <= 14 -> "Dungeon Sage Lorekeeper 📖"
        else -> "Master Arch-Scholar Legend 🌟"
    }
}

internal fun getDifficultyColor(difficulty: String): Color {
    return when (difficulty) {
        "Easy" -> RpgEmerald
        "Medium" -> NeonBlueAccent
        "Hard" -> Color(0xFFFA772C)
        "Legendary" -> Color(0xFFC582FF)
        else -> TextWhite
    }
}
internal fun formatStudyTimeShort(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}

internal fun formatStudyTimeFriendly(seconds: Long): String {
    val totalMins = seconds / 60
    val hrs = totalMins / 60
    val mins = totalMins % 60
    return when {
        seconds == 0L -> "0 seconds"
        hrs == 0L -> "$mins min${if (mins != 1L) "s" else ""}"
        mins == 0L -> "$hrs hr${if (hrs != 1L) "s" else ""}"
        else -> "$hrs hr${if (hrs != 1L) "s" else ""} and $mins min${if (mins != 1L) "s" else ""}"
    }
}
