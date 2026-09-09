package com.amkumirab.solostudying.domain.quest

import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.entity.RecurringQuestEntity
import java.time.DayOfWeek
import java.time.LocalDate

const val ALL_WEEKDAYS_MASK = 0b1111111

data class RecurringQuestSchedule(val weekdaysMask: Int) {
    init {
        require(weekdaysMask in 1..ALL_WEEKDAYS_MASK) {
            "At least one repeat day is required"
        }
    }
}

fun weekdayMask(day: DayOfWeek): Int = 1 shl (day.value - 1)

fun weekdaysMask(days: Set<DayOfWeek>): Int =
    days.fold(0) { mask, day -> mask or weekdayMask(day) }

fun selectedWeekdays(mask: Int): List<DayOfWeek> = DayOfWeek.entries
    .filter { day -> mask and weekdayMask(day) != 0 }

fun isRecurringQuestDue(quest: RecurringQuestEntity, date: LocalDate): Boolean =
    quest.isActive && quest.weekdaysMask and weekdayMask(date.dayOfWeek) != 0

fun nextRecurringQuestDate(
    quest: RecurringQuestEntity,
    fromDate: LocalDate,
): LocalDate? {
    if (!quest.isActive || quest.weekdaysMask !in 1..ALL_WEEKDAYS_MASK) return null
    return (0L..6L)
        .map(fromDate::plusDays)
        .firstOrNull { isRecurringQuestDue(quest, it) }
}

fun recurringQuestScheduleLabel(mask: Int): String {
    val days = selectedWeekdays(mask)
    return when {
        days.size == DayOfWeek.entries.size -> "Every day"
        days.size == 5 && days == DayOfWeek.entries.take(5) -> "Weekdays"
        else -> days.joinToString(", ") {
            it.name.take(3).lowercase().replaceFirstChar { character -> character.uppercase() }
        }
    }
}

fun buildRecurringQuestOccurrence(
    quest: RecurringQuestEntity,
    date: LocalDate,
    createdAt: Long,
): DailyQuestEntity? {
    if (!isRecurringQuestDue(quest, date)) return null
    return DailyQuestEntity(
        title = quest.title,
        durationMinutes = quest.durationMinutes,
        skillId = quest.skillId,
        scheduledDate = date.toString(),
        priority = quest.priority,
        createdAt = createdAt,
        recurringQuestId = quest.id,
    )
}
