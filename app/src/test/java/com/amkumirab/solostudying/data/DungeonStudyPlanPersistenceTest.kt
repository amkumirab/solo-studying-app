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

    @Test fun `version eleven upgrades through Room validation without losing existing records`() =
        verifyMigration(11)

    @Test fun `version twelve preserves goals study steps history and course targets`() =
        verifyMigration(12)

    private fun verifyMigration(version: Int) = runBlocking {
        val source = database.openHelper.writableDatabase
        val schema = mutableListOf<String>()
        source.query("SELECT name, sql FROM sqlite_master WHERE sql IS NOT NULL AND type IN ('table', 'index') ORDER BY type DESC").use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                if (name !in listOf("bosses", "index_bosses_secondGateOfId", "room_master_table",
                        "android_metadata", "sqlite_sequence") && (version == 12 || name != "dungeons")
                ) schema += cursor.getString(1)
            }
        }
        val name = "dungeon-plan-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE bosses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, difficulty TEXT NOT NULL, requiredMinutes INTEGER NOT NULL, imagePath TEXT, timeSpentSeconds INTEGER NOT NULL, isCompleted INTEGER NOT NULL, createdAt INTEGER NOT NULL, deadlineDate TEXT, dungeonName TEXT NOT NULL, isRealBoss INTEGER NOT NULL)")
                        schema.forEach(db::execSQL)
                        if (version == 11) db.execSQL("CREATE TABLE dungeons (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, status TEXT NOT NULL, unlockedTitle TEXT NOT NULL)")
                        db.execSQL("INSERT INTO dungeons (id, name, description, status, unlockedTitle) VALUES (7, 'Operating Systems', 'Course', 'Unlocked', 'Scholar')")
                        if (version == 12) db.execSQL("UPDATE dungeons SET targetMinutes = 4200, studyWeekdaysMask = 31 WHERE id = 7")
                        db.execSQL("INSERT INTO bosses (id, name, difficulty, requiredMinutes, imagePath, timeSpentSeconds, isCompleted, createdAt, deadlineDate, dungeonName, isRealBoss) VALUES (8, 'Processes', 'Medium', 6000, NULL, 3600, 0, 1000, NULL, 'Operating Systems', 0)")
                        db.execSQL("INSERT INTO boss_steps (id, bossId, title, estimatedMinutes, sortOrder, isCompleted, createdAt) VALUES (9, 8, 'Scheduling', 60, 0, 1, 1000)")
                        db.execSQL("INSERT INTO study_sessions (id, bossId, bossName, durationSeconds, xpEarned, goldEarned, timestamp, wasCompleted, isFreeStudy, note) VALUES (10, 8, 'Processes', 3600, 60, 20, 1000, 1, 0, 'Ready for revision')")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            ).build(),
        )
        helper.writableDatabase
        helper.close()
        val upgraded = Room.databaseBuilder(context, SoloStudyingDatabase::class.java, name)
            .addMigrations(SoloStudyingDatabase.MIGRATION_11_12, SoloStudyingDatabase.MIGRATION_12_13)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE).allowMainThreadQueries().build()
        try {
            val repo = SoloStudyingRepository(upgraded)
            val dungeon = repo.allDungeons.first().single()
            assertEquals(7, dungeon.id)
            assertEquals("Course", dungeon.description)
            assertEquals(if (version == 12) 4200 else null, dungeon.targetMinutes)
            assertEquals(if (version == 12) 31 else 127, dungeon.studyWeekdaysMask)
            val goal = checkNotNull(repo.getBossById(8))
            assertEquals(3_600L, goal.timeSpentSeconds)
            assertNull(goal.archivedAtMillis)
            assertNull(goal.secondGateOfId)
            assertEquals("Scheduling", repo.getBossStepById(9)?.title)
            assertEquals("Ready for revision", repo.allSessions.first().single().note)
            repo.saveDungeonStudyPlan(input, today)
        } finally { upgraded.close() }
        val reopened = Room.databaseBuilder(context, SoloStudyingDatabase::class.java, name)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE).allowMainThreadQueries().build()
        try { assertEquals(6_000, reopened.soloStudyingDao().getAllDungeons().first().single().targetMinutes) }
        finally { reopened.close() }
    }
}
