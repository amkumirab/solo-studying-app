package com.amkumirab.solostudying.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.BossSkillEntity
import com.amkumirab.solostudying.data.entity.SkillEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SkillRelationshipPersistenceTest {
    @Test fun `skill joins preserve every field and survive database reopening`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val name = "maintenance_skill_persistence_test"
        app.deleteDatabase(name)
        fun open() = Room.databaseBuilder(app, SoloStudyingDatabase::class.java, name)
            .allowMainThreadQueries().build()
        val expected = SkillEntity(id = 9, name = "Wave propagation", targetMinutes = 240,
            spentSeconds = 75, isUnlocked = true, suggestion = "Review boundary conditions")
        val profile = UserProfileEntity(name = "QA Student", xp = 255, gold = 345)
        var database = open()
        try {
            database.soloStudyingDao().apply {
                insertBoss(BossEntity(id = 7, name = "Physics", difficulty = "Hard", requiredMinutes = 240))
                insertBoss(BossEntity(id = 8, name = "Operating Systems", difficulty = "Medium", requiredMinutes = 180))
                insertSkill(expected)
                insertSkill(expected.copy(id = 10, name = "Other skill"))
                insertBossSkill(BossSkillEntity(bossId = 7, skillId = 9))
                insertBossSkill(BossSkillEntity(bossId = 8, skillId = 10))
                insertOrUpdateProfile(profile)
                assertEquals(listOf(expected), getSkillsForBoss(7).first())
            }
            database.close()
            database = open()
            assertEquals(listOf(expected), database.soloStudyingDao().getSkillsForBoss(7).first())
            assertEquals(profile, database.soloStudyingDao().getProfileSync())
        } finally {
            database.close()
            app.deleteDatabase(name)
        }
    }
}
