package com.amkumirab.solostudying.domain.insights

import com.amkumirab.solostudying.data.entity.StudySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class StudyInsightsTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val targets = List(7) { 60 }

    @Test
    fun `seven day insights calculate totals goals comparison and leaders`() {
        val now = utcMillis(2026, Calendar.AUGUST, 29, 12)
        val sessions = listOf(
            session("Physics", 3_600L, 100, 50, utcMillis(2026, Calendar.AUGUST, 29, 10)),
            session("Physics", 1_800L, 50, 25, utcMillis(2026, Calendar.AUGUST, 28, 18)),
            session("Mathematics", 900L, 25, 10, utcMillis(2026, Calendar.AUGUST, 25, 9)),
            session("Previous Week", 1_800L, 20, 10, utcMillis(2026, Calendar.AUGUST, 20, 9)),
            session("Old Session", 4_000L, 10, 5, utcMillis(2026, Calendar.AUGUST, 1, 9)),
        )

        val insights = calculateStudyInsights(
            sessions = sessions,
            range = InsightRange.Last7Days,
            weekdayTargetMinutes = targets,
            nowMillis = now,
            timeZone = utc,
            locale = Locale.US,
        )

        assertEquals(7, insights.buckets.size)
        assertEquals(6_300L, insights.totalStudySeconds)
        assertEquals(3, insights.sessionCount)
        assertEquals(175, insights.totalXp)
        assertEquals(85, insights.totalGold)
        assertEquals(2_100L, insights.averageSessionSeconds)
        assertEquals(25, insights.targetCompletionPercent)
        assertEquals("Sat, 29 Aug", insights.bestDayLabel)
        assertEquals(3_600L, insights.bestDaySeconds)
        assertEquals("Physics", insights.topSubject)
        assertEquals(5_400L, insights.topSubjectSeconds)
        assertEquals(250, insights.comparisonPercent)
    }

    @Test
    fun `daily buckets follow weekday specific targets`() {
        val insights = calculateStudyInsights(
            sessions = emptyList(),
            range = InsightRange.Last7Days,
            weekdayTargetMinutes = listOf(10, 20, 30, 40, 50, 60, 70),
            nowMillis = utcMillis(2026, Calendar.AUGUST, 29, 12),
            timeZone = utc,
            locale = Locale.US,
        )

        assertEquals(
            listOf(70, 10, 20, 30, 40, 50, 60).map { it * 60L },
            insights.buckets.map { it.targetSeconds },
        )
        assertEquals(0, insights.targetCompletionPercent)
        assertNull(insights.bestDayLabel)
        assertNull(insights.topSubject)
        assertNull(insights.comparisonPercent)
    }

    @Test
    fun `thirty day range excludes older sessions`() {
        val now = utcMillis(2026, Calendar.AUGUST, 29, 12)
        val insights = calculateStudyInsights(
            sessions = listOf(
                session("Recent", 1_200L, 10, 5, utcMillis(2026, Calendar.AUGUST, 2, 8)),
                session("Too Old", 7_200L, 10, 5, utcMillis(2026, Calendar.JULY, 30, 8)),
            ),
            range = InsightRange.Last30Days,
            weekdayTargetMinutes = targets,
            nowMillis = now,
            timeZone = utc,
            locale = Locale.US,
        )

        assertEquals(30, insights.buckets.size)
        assertEquals(1_200L, insights.totalStudySeconds)
        assertEquals(1, insights.sessionCount)
    }

    @Test
    fun `all time uses monthly buckets and normalizes session subjects`() {
        val insights = calculateStudyInsights(
            sessions = listOf(
                session("Physics (Suspended)", 600L, 5, 2, utcMillis(2026, Calendar.JULY, 15, 8)),
                session("Physics (Manual Conquest)", 900L, 8, 4, utcMillis(2026, Calendar.AUGUST, 10, 8)),
                session(null, 300L, 3, 1, utcMillis(2026, Calendar.AUGUST, 12, 8), freeStudy = true),
            ),
            range = InsightRange.AllTime,
            weekdayTargetMinutes = targets,
            nowMillis = utcMillis(2026, Calendar.AUGUST, 29, 12),
            timeZone = utc,
            locale = Locale.US,
        )

        assertEquals(listOf("Jul 26", "Aug 26"), insights.buckets.map { it.label })
        assertEquals(listOf(600L, 1_200L), insights.buckets.map { it.studySeconds })
        assertEquals("Physics", insights.topSubject)
        assertEquals(1_500L, insights.topSubjectSeconds)
        assertNull(insights.comparisonPercent)
    }

    private fun session(
        name: String?,
        durationSeconds: Long,
        xp: Int,
        gold: Int,
        timestamp: Long,
        freeStudy: Boolean = false,
    ) = StudySessionEntity(
        bossId = null,
        bossName = name,
        durationSeconds = durationSeconds,
        xpEarned = xp,
        goldEarned = gold,
        timestamp = timestamp,
        wasCompleted = true,
        isFreeStudy = freeStudy,
    )

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis
}
