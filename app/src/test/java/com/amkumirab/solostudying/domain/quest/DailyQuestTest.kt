package com.amkumirab.solostudying.domain.quest

import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.entity.RecurringQuestEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.time.DayOfWeek
import java.time.LocalDate

class DailyQuestTest {

    @Test
    fun `today board includes pending older quests but excludes old completed quests`() {
        val quests = listOf(
            quest(id = 1, date = "2026-08-31", completed = false, priority = 0),
            quest(id = 2, date = "2026-08-31", completed = true, priority = 2),
            quest(id = 3, date = "2026-09-01", completed = false, priority = 2),
            quest(id = 4, date = "2026-09-02", completed = false, priority = 1),
        )

        val visible = selectVisibleDailyQuests(quests, today = "2026-09-01")

        assertEquals(listOf(3, 1), visible.map { it.id })
        assertTrue(isCarriedDailyQuest(visible[1], "2026-09-01"))
        assertFalse(isCarriedDailyQuest(visible[0], "2026-09-01"))
    }

    @Test
    fun `completed quests move below pending quests`() {
        val visible = selectVisibleDailyQuests(
            quests = listOf(
                quest(id = 1, date = "2026-09-01", completed = true, priority = 2),
                quest(id = 2, date = "2026-09-01", completed = false, priority = 0),
            ),
            today = "2026-09-01",
        )

        assertEquals(listOf(2, 1), visible.map { it.id })
    }

    @Test
    fun `skipped recurring occurrence stays off the daily board`() {
        val visible = selectVisibleDailyQuests(
            quests = listOf(
                quest(id = 1, date = "2026-09-01", completed = false, priority = 1)
                    .copy(recurringQuestId = 7, isSkipped = true),
                quest(id = 2, date = "2026-09-01", completed = false, priority = 1),
            ),
            today = "2026-09-01",
        )

        assertEquals(listOf(2), visible.map { it.id })
    }

    @Test
    fun `date key respects the supplied timezone`() {
        val utc = TimeZone.getTimeZone("UTC")
        val rome = TimeZone.getTimeZone("Europe/Rome")
        val timestamp = Calendar.getInstance(utc).apply {
            clear()
            set(2026, Calendar.AUGUST, 31, 22, 30)
        }.timeInMillis

        assertEquals("2026-08-31", dailyQuestDateKey(timestamp, utc))
        assertEquals("2026-09-01", dailyQuestDateKey(timestamp, rome))
    }

    @Test
    fun `selected weekday mask schedules only chosen days`() {
        val mask = weekdaysMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        val recurring = recurringQuest(mask = mask)

        assertTrue(isRecurringQuestDue(recurring, LocalDate.of(2026, 9, 9)))
        assertFalse(isRecurringQuestDue(recurring, LocalDate.of(2026, 9, 10)))
        assertEquals(
            LocalDate.of(2026, 9, 14),
            nextRecurringQuestDate(recurring, LocalDate.of(2026, 9, 10)),
        )
        assertEquals("Mon, Wed", recurringQuestScheduleLabel(mask))
    }

    @Test
    fun `daily schedule creates a linked occurrence`() {
        val recurring = recurringQuest(mask = ALL_WEEKDAYS_MASK)

        val occurrence = buildRecurringQuestOccurrence(
            quest = recurring,
            date = LocalDate.of(2026, 9, 10),
            createdAt = 1234L,
        ) ?: error("Occurrence was not created")

        assertEquals(recurring.id, occurrence.recurringQuestId)
        assertEquals(recurring.title, occurrence.title)
        assertEquals("2026-09-10", occurrence.scheduledDate)
        assertEquals("Every day", recurringQuestScheduleLabel(recurring.weekdaysMask))
    }

    @Test
    fun `paused recurring quest has no occurrence or next date`() {
        val recurring = recurringQuest(mask = ALL_WEEKDAYS_MASK).copy(isActive = false)

        assertNull(buildRecurringQuestOccurrence(recurring, LocalDate.of(2026, 9, 10), 1234L))
        assertNull(nextRecurringQuestDate(recurring, LocalDate.of(2026, 9, 10)))
    }

    private fun quest(
        id: Int,
        date: String,
        completed: Boolean,
        priority: Int,
    ) = DailyQuestEntity(
        id = id,
        title = "Quest $id",
        durationMinutes = 25,
        scheduledDate = date,
        isCompleted = completed,
        priority = priority,
        createdAt = id.toLong(),
    )

    private fun recurringQuest(mask: Int) = RecurringQuestEntity(
        id = 7,
        title = "Study physics",
        durationMinutes = 45,
        priority = QuestPriority.High.value,
        weekdaysMask = mask,
        createdAt = 1000L,
    )
}
