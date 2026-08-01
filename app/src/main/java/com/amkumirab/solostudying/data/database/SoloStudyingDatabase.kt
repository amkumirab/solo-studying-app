package com.amkumirab.solostudying.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.amkumirab.solostudying.data.dao.SoloStudyingDao
import com.amkumirab.solostudying.data.entity.*

@Database(
    entities = [
        BossEntity::class,
        UserProfileEntity::class,
        RewardItemEntity::class,
        RewardBalanceEntity::class,
        StudySessionEntity::class,
        SkillEntity::class,
        DungeonEntity::class,
        BossSkillEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class SoloStudyingDatabase : RoomDatabase() {
    abstract fun soloStudyingDao(): SoloStudyingDao

    companion object {
        @Volatile
        private var INSTANCE: SoloStudyingDatabase? = null

        fun getDatabase(context: Context): SoloStudyingDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    SoloStudyingDatabase::class.java,
                    "solo_studying_database"
                )

                // Only use fallbackToDestructiveMigration in development mode to safeguard production users
                if (isDevelopmentMode(context)) {
                    builder.fallbackToDestructiveMigration()
                }

                // Add prepared migrations for release versions
                builder.addMigrations(MIGRATION_4_5, MIGRATION_5_6)

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        private fun isDevelopmentMode(context: Context): Boolean {
            return try {
                0 != (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
            } catch (e: Exception) {
                true
            }
        }

        /**
         * Migration strategy for release versions from version 4 to 5.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create dungeons table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dungeons` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL DEFAULT 'Locked', " +
                        "`unlockedTitle` TEXT NOT NULL DEFAULT 'Novice Scholar'" +
                        ")"
                )

                // Create boss_skills cross reference relationship table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `boss_skills` (" +
                        "`bossId` INTEGER NOT NULL, " +
                        "`skillId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`bossId`, `skillId`)" +
                        ")"
                )

                // Alter user_profile to add onboarding configuration columns if not exists
                try {
                    db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `hunterClass` TEXT NOT NULL DEFAULT 'Shadow Monarch'")
                    db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `mainGoal` TEXT NOT NULL DEFAULT 'Academic Mastery'")
                    db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `learningPath` TEXT NOT NULL DEFAULT 'Sage Path'")
                } catch (ignored: Exception) {
                    // Columns might already be created by Room in dev builds
                }
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `hasCompletedTutorial` INTEGER NOT NULL DEFAULT 0")
                } catch (ignored: Exception) {
                    // Column might already exist in some dev setups
                }
            }
        }
    }
}
