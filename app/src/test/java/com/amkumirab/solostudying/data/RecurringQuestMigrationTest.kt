package com.amkumirab.solostudying.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecurringQuestMigrationTest {

    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper
    private val databaseName = "recurring-quest-migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(10) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE `daily_quests` (" +
                                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "`title` TEXT NOT NULL, " +
                                    "`durationMinutes` INTEGER NOT NULL, " +
                                    "`skillId` INTEGER, " +
                                    "`scheduledDate` TEXT NOT NULL, " +
                                    "`priority` INTEGER NOT NULL, " +
                                    "`isCompleted` INTEGER NOT NULL, " +
                                    "`createdAt` INTEGER NOT NULL, " +
                                    "`completedAt` INTEGER)",
                            )
                            db.execSQL(
                                "CREATE INDEX `index_daily_quests_scheduledDate` " +
                                    "ON `daily_quests` (`scheduledDate`)",
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `migration from ten preserves quests and adds recurring schedules`() {
        val database = helper.writableDatabase
        database.execSQL(
            "INSERT INTO `daily_quests` " +
                "(`title`, `durationMinutes`, `skillId`, `scheduledDate`, `priority`, " +
                "`isCompleted`, `createdAt`, `completedAt`) " +
                "VALUES ('Existing quest', 25, NULL, '2026-09-09', 1, 0, 1000, NULL)",
        )

        SoloStudyingDatabase.MIGRATION_10_11.migrate(database)

        database.query(
            "SELECT `title`, `durationMinutes`, `recurringQuestId`, `isSkipped` " +
                "FROM `daily_quests` WHERE `id` = 1",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Existing quest", cursor.getString(0))
            assertEquals(25, cursor.getInt(1))
            assertNull(cursor.getString(2))
            assertEquals(0, cursor.getInt(3))
        }

        val columns = mutableListOf<String>()
        database.query("PRAGMA table_info(`recurring_quests`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
        }
        assertEquals(
            listOf(
                "id",
                "title",
                "durationMinutes",
                "skillId",
                "priority",
                "weekdaysMask",
                "isActive",
                "createdAt",
            ),
            columns,
        )

        database.execSQL(
            "INSERT INTO `recurring_quests` " +
                "(`title`, `durationMinutes`, `skillId`, `priority`, `weekdaysMask`, `isActive`, `createdAt`) " +
                "VALUES ('Physics routine', 45, NULL, 2, 31, 1, 2000)",
        )
        repeat(2) {
            database.execSQL(
                "INSERT OR IGNORE INTO `daily_quests` " +
                    "(`title`, `durationMinutes`, `skillId`, `scheduledDate`, `priority`, " +
                    "`isCompleted`, `createdAt`, `completedAt`, `recurringQuestId`, `isSkipped`) " +
                    "VALUES ('Physics routine', 45, NULL, '2026-09-10', 2, 0, 2000, NULL, 1, 0)",
            )
        }
        database.query(
            "SELECT COUNT(*) FROM `daily_quests` " +
                "WHERE `recurringQuestId` = 1 AND `scheduledDate` = '2026-09-10'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }
}
