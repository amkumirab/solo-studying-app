package com.amkumirab.solostudying.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.DungeonEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.domain.dungeon.DungeonStudyPlanInput
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DungeonStudyPlanPersistenceTest {
    private val today = LocalDate.of(2026, 10, 1)
    private val input = DungeonStudyPlanInput("Operating Systems", 6_000, today.plusMonths(2), 127)
    private lateinit var context: Context
    private lateinit var database: SoloStudyingDatabase
    private lateinit var repository: SoloStudyingRepository

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SoloStudyingDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = SoloStudyingRepository(database)
    }

    @After fun tearDown() {
        database.close()
        context.deleteDatabase("dungeon-plan-migration-test.db")
    }

    @Test fun `editing and removing a plan preserve dungeon identity and boss progress`() = runBlocking {
        val original = DungeonEntity(id = 10, name = input.dungeonName, description = "Exam prep", unlockedTitle = "Scholar")
        repository.insertDungeon(original)
        val bossId = repository.insertBoss(BossEntity(name = "Processes", difficulty = "Medium", requiredMinutes = 6_000, dungeonName = input.dungeonName, timeSpentSeconds = 3_600)).toInt()
        repository.saveDungeonStudyPlan(input, today)
        repository.saveDungeonStudyPlan(input.copy(targetMinutes = 7_200, weekdaysMask = 31), today.plusDays(1))
        val saved = repository.allDungeons.first().single()
        assertEquals(original.id, saved.id)
        assertEquals("Exam prep", saved.description)
        assertEquals("Scholar", saved.unlockedTitle)
        assertEquals(today.toString(), saved.planStartDate)
        assertEquals(7_200, saved.targetMinutes)
        assertEquals(31, saved.studyWeekdaysMask)
        repository.clearDungeonStudyPlan(input.dungeonName)
        val cleared = repository.allDungeons.first().single()
        assertNull(cleared.targetMinutes)
        assertNull(cleared.planDeadlineDate)
        assertNull(cleared.planStartDate)
        assertEquals(3_600L, repository.getBossById(bossId)!!.timeSpentSeconds)
    }

    @Test fun `concurrent saves create only one named dungeon`() = runBlocking {
        List(2) { async { repository.saveDungeonStudyPlan(input, today) } }.awaitAll()
        assertEquals(1, repository.allDungeons.first().size)
        assertEquals(6_000, repository.allDungeons.first().single().targetMinutes)
    }

    @Test fun `repository rejects invalid input before modifying data`() = runBlocking {
        for (invalid in listOf(input.copy(targetMinutes = -1), input.copy(weekdaysMask = 0), input.copy(deadline = today.minusDays(1)), input.copy(dungeonName = "All"))) {
            assertTrue(runCatching { repository.saveDungeonStudyPlan(invalid, today) }.exceptionOrNull() is IllegalArgumentException)
        }
        assertTrue(repository.allDungeons.first().isEmpty())
    }

    @Test fun `version eleven upgrades through Room validation without losing existing records`() = runBlocking {
        val source = database.openHelper.writableDatabase
        val schema = mutableListOf<String>()
        source.query("SELECT name, sql FROM sqlite_master WHERE sql IS NOT NULL AND type IN ('table', 'index') ORDER BY type DESC").use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                if (name !in listOf("dungeons", "room_master_table", "android_metadata", "sqlite_sequence")) schema += cursor.getString(1)
            }
        }
        val name = "dungeon-plan-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(11) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        schema.forEach(db::execSQL)
                        db.execSQL("CREATE TABLE dungeons (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, status TEXT NOT NULL, unlockedTitle TEXT NOT NULL)")
                        db.execSQL("INSERT INTO dungeons VALUES (7, 'Operating Systems', 'Course', 'Unlocked', 'Scholar')")
                        db.execSQL("INSERT INTO bosses (id, name, difficulty, requiredMinutes, imagePath, timeSpentSeconds, isCompleted, createdAt, deadlineDate, dungeonName, isRealBoss) VALUES (8, 'Processes', 'Medium', 6000, NULL, 3600, 0, 1000, NULL, 'Operating Systems', 0)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            ).build(),
        )
        helper.writableDatabase
        helper.close()
        val upgraded = Room.databaseBuilder(context, SoloStudyingDatabase::class.java, name)
            .addMigrations(SoloStudyingDatabase.MIGRATION_11_12)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE).allowMainThreadQueries().build()
        try {
            val repo = SoloStudyingRepository(upgraded)
            val dungeon = repo.allDungeons.first().single()
            assertEquals(7, dungeon.id)
            assertEquals("Course", dungeon.description)
            assertNull(dungeon.targetMinutes)
            assertEquals(127, dungeon.studyWeekdaysMask)
            assertEquals(3_600L, repo.getBossById(8)!!.timeSpentSeconds)
            repo.saveDungeonStudyPlan(input, today)
        } finally { upgraded.close() }
        val reopened = Room.databaseBuilder(context, SoloStudyingDatabase::class.java, name)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE).allowMainThreadQueries().build()
        try { assertEquals(6_000, reopened.soloStudyingDao().getAllDungeons().first().single().targetMinutes) }
        finally { reopened.close() }
    }
}
