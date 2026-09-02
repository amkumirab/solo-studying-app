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
class SessionNoteMigrationTest {

    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper
    private val databaseName = "session-note-migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(7) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE `study_sessions` (" +
                                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "`bossId` INTEGER, " +
                                    "`bossName` TEXT, " +
                                    "`durationSeconds` INTEGER NOT NULL, " +
                                    "`xpEarned` INTEGER NOT NULL, " +
                                    "`goldEarned` INTEGER NOT NULL, " +
                                    "`timestamp` INTEGER NOT NULL, " +
                                    "`wasCompleted` INTEGER NOT NULL, " +
                                    "`isFreeStudy` INTEGER NOT NULL)",
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
    fun `migration from seven preserves sessions and adds nullable note`() {
        val database = helper.writableDatabase
        database.execSQL(
            "INSERT INTO `study_sessions` " +
                "(`bossId`, `bossName`, `durationSeconds`, `xpEarned`, `goldEarned`, " +
                "`timestamp`, `wasCompleted`, `isFreeStudy`) " +
                "VALUES (NULL, 'Physics Review', 1500, 40, 15, 1000, 1, 1)",
        )

        SoloStudyingDatabase.MIGRATION_7_8.migrate(database)

        val columns = mutableListOf<String>()
        database.query("PRAGMA table_info(`study_sessions`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
        }
        assertTrue("note column was not added", "note" in columns)

        database.query("SELECT `bossName`, `note` FROM `study_sessions` WHERE `id` = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Physics Review", cursor.getString(0))
            assertNull(cursor.getString(1))
        }
    }
}
