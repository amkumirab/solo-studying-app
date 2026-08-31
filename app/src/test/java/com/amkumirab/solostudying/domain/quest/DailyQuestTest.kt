package com.amkumirab.solostudying.domain.quest

import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

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
}
