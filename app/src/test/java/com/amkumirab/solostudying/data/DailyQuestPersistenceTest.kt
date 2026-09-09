package com.amkumirab.solostudying.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.domain.quest.QuestPriority
import com.amkumirab.solostudying.domain.quest.RecurringQuestSchedule
import com.amkumirab.solostudying.domain.quest.weekdaysMask
import com.amkumirab.solostudying.ui.viewmodel.DailyQuestViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyQuestPersistenceTest {
    private lateinit var database: SoloStudyingDatabase
    private lateinit var repository: SoloStudyingRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoloStudyingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SoloStudyingRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `daily quest lifecycle is persisted`() = runBlocking {
        val id = repository.insertDailyQuest(
            DailyQuestEntity(
                title = "Solve physics problems",
                durationMinutes = 45,
                skillId = 7,
                scheduledDate = "2026-09-01",
                priority = 2,
            ),
        ).toInt()

        val saved = repository.getDailyQuestById(id) ?: error("Quest was not saved")
        assertEquals("Solve physics problems", saved.title)
        assertEquals(45, saved.durationMinutes)

        assertTrue(repository.completeDailyQuest(id, completedAt = 123_456L))
        val completed = repository.getDailyQuestById(id) ?: error("Quest was not updated")
        assertTrue(completed.isCompleted)
        assertEquals(123_456L, completed.completedAt)

        repository.deleteDailyQuest(completed)
        assertTrue(repository.allDailyQuests.first().isEmpty())
    }

    @Test
    fun `recurring quest creates one occurrence on each scheduled day`() = runBlocking {
        val utc = TimeZone.getTimeZone("UTC")
        var nowMillis = noonUtc(LocalDate.of(2026, 9, 9))
        val viewModel = DailyQuestViewModel(repository, { nowMillis }, utc)
        val schedule = RecurringQuestSchedule(
            weekdaysMask(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
        )

        viewModel.createQuest(
            title = "Review electromagnetics",
            durationMinutes = 45,
            skillId = null,
            priority = QuestPriority.High,
            repeatSchedule = schedule,
        )

        val recurring = waitForValue {
            repository.allRecurringQuests.first().singleOrNull()
        }
        val firstOccurrence = waitForValue {
            repository.allDailyQuests.first().takeIf { it.size == 1 }
        }.single()

        viewModel.deleteQuest(firstOccurrence)
        waitForValue {
            repository.allDailyQuests.first().singleOrNull()?.takeIf { it.isSkipped }
        }

        repeat(3) { viewModel.refreshToday() }
        delay(50L)
        ShadowLooper.idleMainLooper()
        assertEquals(1, repository.allDailyQuests.first().size)
        assertTrue(repository.allDailyQuests.first().single().isSkipped)

        nowMillis = noonUtc(LocalDate.of(2026, 9, 10))
        viewModel.refreshToday()
        delay(50L)
        ShadowLooper.idleMainLooper()
        assertEquals(1, repository.allDailyQuests.first().size)

        nowMillis = noonUtc(LocalDate.of(2026, 9, 11))
        viewModel.refreshToday()
        val occurrences = waitForValue {
            repository.allDailyQuests.first().takeIf { it.size == 2 }
        }
        assertEquals(
            listOf("2026-09-11", "2026-09-09"),
            occurrences.map { it.scheduledDate },
        )
        assertTrue(occurrences.all { it.recurringQuestId == recurring.id })

        viewModel.setRecurringQuestActive(recurring, false)
        waitForValue {
            repository.allRecurringQuests.first().singleOrNull()?.takeIf { !it.isActive }
        }
        nowMillis = noonUtc(LocalDate.of(2026, 9, 16))
        viewModel.refreshToday()
        delay(50L)
        ShadowLooper.idleMainLooper()
        assertEquals(2, repository.allDailyQuests.first().size)
    }

    private fun noonUtc(date: LocalDate): Long = date
        .atTime(12, 0)
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()

    private suspend fun <T> waitForValue(
        timeoutMillis: Long = 3_000L,
        block: suspend () -> T?,
    ): T {
        val startedAt = System.currentTimeMillis()
        var value = block()
        while (value == null && System.currentTimeMillis() - startedAt < timeoutMillis) {
            delay(25L)
            ShadowLooper.idleMainLooper()
            value = block()
        }
        return value ?: throw AssertionError("Condition not met within $timeoutMillis ms")
    }
}
