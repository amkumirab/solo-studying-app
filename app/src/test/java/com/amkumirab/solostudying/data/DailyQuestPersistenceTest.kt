package com.amkumirab.solostudying.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
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
}
