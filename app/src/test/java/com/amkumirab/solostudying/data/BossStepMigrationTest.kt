package com.amkumirab.solostudying.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
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
class BossStepMigrationTest {

    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper
    private val databaseName = "boss-step-migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(9) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE `bosses` (" +
                                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "`name` TEXT NOT NULL, " +
                                    "`difficulty` TEXT NOT NULL, " +
                                    "`requiredMinutes` INTEGER NOT NULL, " +
                                    "`imagePath` TEXT, " +
                                    "`timeSpentSeconds` INTEGER NOT NULL, " +
                                    "`isCompleted` INTEGER NOT NULL, " +
                                    "`createdAt` INTEGER NOT NULL, " +
                                    "`deadlineDate` TEXT, " +
                                    "`dungeonName` TEXT NOT NULL, " +
                                    "`isRealBoss` INTEGER NOT NULL)",
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
    fun `migration from nine adds ordered study steps without changing bosses`() {
        val database = helper.writableDatabase
        database.execSQL(
            "INSERT INTO `bosses` " +
                "(`name`, `difficulty`, `requiredMinutes`, `imagePath`, `timeSpentSeconds`, " +
                "`isCompleted`, `createdAt`, `deadlineDate`, `dungeonName`, `isRealBoss`) " +
                "VALUES ('Physics Exam', 'Hard', 300, NULL, 600, 0, 1000, " +
                "'2026-10-20', 'Academic Citadel', 0)",
        )

        SoloStudyingDatabase.MIGRATION_9_10.migrate(database)

        val columns = mutableListOf<String>()
        database.query("PRAGMA table_info(`boss_steps`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
        }
        assertEquals(
            listOf(
                "id",
                "bossId",
                "title",
                "estimatedMinutes",
                "sortOrder",
                "isCompleted",
                "createdAt",
                "completedAt",
            ),
            columns,
        )

        database.execSQL(
            "INSERT INTO `boss_steps` " +
                "(`bossId`, `title`, `estimatedMinutes`, `sortOrder`, `isCompleted`, `createdAt`, `completedAt`) " +
                "VALUES (1, 'Review wave propagation', 45, 0, 0, 2000, NULL)",
        )

        database.query("SELECT `title`, `estimatedMinutes` FROM `boss_steps` WHERE `bossId` = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Review wave propagation", cursor.getString(0))
            assertEquals(45, cursor.getInt(1))
        }
        database.query("SELECT `name`, `deadlineDate` FROM `bosses` WHERE `id` = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Physics Exam", cursor.getString(0))
            assertEquals("2026-10-20", cursor.getString(1))
        }
    }
}
