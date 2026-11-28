package com.amkumirab.solostudying.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.domain.session.SessionReadPeriod
import com.amkumirab.solostudying.domain.streak.buildWeeklyStreakSnapshot
import com.amkumirab.solostudying.quickstart.QuickStartSelection
import com.amkumirab.solostudying.widget.buildTodayWidgetSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionPeriodReadTest {
    private val rome = ZoneId.of("Europe/Rome")

    @Test
    fun `local period respects both clock changes and the Monday boundary`() {
        val spring = SessionReadPeriod(LocalDate.of(2026, 3, 29), rome)
        assertEquals(Duration.ofHours(23).toMillis(), spring.tomorrowMillis - spring.todayMillis)
        assertEquals(Duration.ofHours(167).toMillis(), spring.nextWeekMillis - spring.weekMillis)
        assertEquals(spring.tomorrowMillis, spring.nextWeekMillis)

        val fall = SessionReadPeriod(LocalDate.of(2026, 10, 25), rome)
        assertEquals(Duration.ofHours(25).toMillis(), fall.tomorrowMillis - fall.todayMillis)
        assertEquals(Duration.ofHours(169).toMillis(), fall.nextWeekMillis - fall.weekMillis)
        assertEquals(fall.tomorrowMillis, fall.nextWeekMillis)
    }

    @Test
    fun `bounded reads preserve widget results and leave stored history intact`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, SoloStudyingDatabase::class.java).build()
        try {
            val repository = SoloStudyingRepository(database)
            val dao = database.soloStudyingDao()
            val period = SessionReadPeriod(LocalDate.of(2026, 10, 25), rome)
            val rows = listOf(
                session(1, period.weekMillis - 1),
                session(2, period.weekMillis),
                session(3, period.todayMillis - 1),
                session(4, period.todayMillis),
                session(5, period.tomorrowMillis - 1),
                session(6, period.nextWeekMillis),
            )
            rows.forEach { dao.insertSession(it) }
            val week = repository.sessionsBetween(period.weekMillis, period.nextWeekMillis).first()
            val today = dao.getSessionsBetween(period.todayMillis, period.tomorrowMillis).first()
            val stored = repository.allSessions.first()
            assertEquals(listOf(5, 4, 3, 2), week.map { it.id })
            assertEquals(listOf(5, 4), today.map { it.id })
            assertEquals(rows.reversed(), stored)

            val profile = UserProfileEntity(hasCompletedOnboarding = true, scheduleDays = "Mon,Sun")
            fun dailyWidget(sessions: List<StudySessionEntity>) = buildTodayWidgetSnapshot(
                profile, sessions, emptyList(), emptyList(), emptyList(), QuickStartSelection(),
                period.today, period.zoneId,
            )
            assertEquals(dailyWidget(stored), dailyWidget(today))
            assertEquals(
                buildWeeklyStreakSnapshot(profile, stored, period.today, period.zoneId),
                buildWeeklyStreakSnapshot(profile, week, period.today, period.zoneId),
            )
        } finally {
            database.close()
        }
    }

    private fun session(id: Int, time: Long) = StudySessionEntity(
        id = id,
        bossId = 42,
        bossName = "Physics",
        durationSeconds = 1500,
        xpEarned = 41,
        goldEarned = 19,
        timestamp = time,
        wasCompleted = true,
        note = "Review the exercise.",
    )
}
