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
class DeadlineGoalMigrationTest {

    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper
    private val databaseName = "deadline-goal-migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(8) {
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
    fun `migration from eight preserves bosses and adds nullable deadline`() {
        val database = helper.writableDatabase
        database.execSQL(
            "INSERT INTO `bosses` " +
                "(`name`, `difficulty`, `requiredMinutes`, `imagePath`, `timeSpentSeconds`, " +
                "`isCompleted`, `createdAt`, `dungeonName`, `isRealBoss`) " +
                "VALUES ('Existing Exam', 'Medium', 300, NULL, 600, 0, 1000, 'Main Realm', 0)",
        )

        SoloStudyingDatabase.MIGRATION_8_9.migrate(database)

        val columns = mutableListOf<String>()
        database.query("PRAGMA table_info(`bosses`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
        }
        assertTrue("deadlineDate column was not added", "deadlineDate" in columns)

        database.query("SELECT `name`, `timeSpentSeconds`, `deadlineDate` FROM `bosses` WHERE `id` = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Existing Exam", cursor.getString(0))
            assertEquals(600L, cursor.getLong(1))
            assertNull(cursor.getString(2))
        }
    }
}
