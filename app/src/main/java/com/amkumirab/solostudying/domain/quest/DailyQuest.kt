package com.amkumirab.solostudying.domain.quest

import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class QuestPriority(val value: Int, val displayName: String) {
    Low(0, "LOW"),
    Normal(1, "NORMAL"),
    High(2, "HIGH");

    companion object {
        fun fromValue(value: Int): QuestPriority = entries.firstOrNull { it.value == value } ?: Normal
    }
}

fun dailyQuestDateKey(
    timestamp: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    this.timeZone = timeZone
}.format(Date(timestamp))

fun selectVisibleDailyQuests(
    quests: List<DailyQuestEntity>,
    today: String,
): List<DailyQuestEntity> = quests
    .filter { quest ->
        !quest.isSkipped &&
            (quest.scheduledDate == today || (!quest.isCompleted && quest.scheduledDate < today))
    }
    .sortedWith(
        compareBy<DailyQuestEntity> { it.isCompleted }
            .thenByDescending { it.priority }
            .thenBy { it.scheduledDate }
            .thenBy { it.createdAt },
    )

fun isCarriedDailyQuest(quest: DailyQuestEntity, today: String): Boolean =
    !quest.isCompleted && quest.scheduledDate < today
