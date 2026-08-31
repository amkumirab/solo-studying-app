package com.amkumirab.solostudying.domain.insights

import com.amkumirab.solostudying.data.entity.StudySessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

enum class InsightRange(val displayName: String, val dayCount: Int?) {
    Last7Days("7 DAYS", 7),
    Last30Days("30 DAYS", 30),
    AllTime("ALL TIME", null),
}

data class InsightBucket(
    val label: String,
    val studySeconds: Long,
    val targetSeconds: Long,
)

data class StudyInsights(
    val range: InsightRange,
    val buckets: List<InsightBucket>,
    val totalStudySeconds: Long,
    val sessionCount: Int,
    val totalXp: Int,
    val totalGold: Int,
    val averageSessionSeconds: Long,
    val targetSeconds: Long,
    val targetCompletionPercent: Int,
    val bestDayLabel: String?,
    val bestDaySeconds: Long,
    val topSubject: String?,
    val topSubjectSeconds: Long,
    val comparisonPercent: Int?,
)

fun calculateStudyInsights(
    sessions: List<StudySessionEntity>,
    range: InsightRange,
    weekdayTargetMinutes: List<Int>,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault(),
): StudyInsights {
    val normalizedTargets = weekdayTargetMinutes.takeIf { it.size == 7 }
        ?.map { it.coerceAtLeast(0) }
        ?: List(7) { 45 }
    val todayStart = startOfDay(nowMillis, timeZone)
    val rangeStart = when (val days = range.dayCount) {
        null -> sessions.minOfOrNull { startOfDay(it.timestamp, timeZone) } ?: todayStart
        else -> addDays(todayStart, -(days - 1), timeZone)
    }
    val periodEnd = addDays(todayStart, 1, timeZone)
    val rangeSessions = sessions.filter { it.timestamp in rangeStart until periodEnd }
    val dailyTotals = rangeSessions.groupBy { startOfDay(it.timestamp, timeZone) }
        .mapValues { (_, records) -> records.sumOf { it.durationSeconds } }
    val buckets = if (range == InsightRange.AllTime) {
        buildMonthlyBuckets(
            sessions = rangeSessions,
            rangeStart = rangeStart,
            periodEnd = periodEnd,
            weekdayTargetMinutes = normalizedTargets,
            timeZone = timeZone,
            locale = locale,
        )
    } else {
        buildDailyBuckets(
            rangeStart = rangeStart,
            periodEnd = periodEnd,
            dailyTotals = dailyTotals,
            weekdayTargetMinutes = normalizedTargets,
            timeZone = timeZone,
            locale = locale,
            compactLabels = range == InsightRange.Last7Days,
        )
    }

    val totalStudySeconds = rangeSessions.sumOf { it.durationSeconds }
    val targetSeconds = sumDailyTargets(
        startMillis = rangeStart,
        endExclusiveMillis = periodEnd,
        weekdayTargetMinutes = normalizedTargets,
        timeZone = timeZone,
    )
    val bestDay = dailyTotals.maxByOrNull { it.value }
    val subjectTotals = rangeSessions.groupBy(::sessionSubject)
        .mapValues { (_, records) -> records.sumOf { it.durationSeconds } }
    val topSubject = subjectTotals.maxByOrNull { it.value }

    val comparisonPercent = range.dayCount?.let { days ->
        val previousStart = addDays(rangeStart, -days, timeZone)
        val previousSeconds = sessions
            .filter { it.timestamp in previousStart until rangeStart }
            .sumOf { it.durationSeconds }
        if (previousSeconds > 0L) {
            (((totalStudySeconds - previousSeconds).toDouble() / previousSeconds) * 100.0)
                .roundToInt()
        } else {
            null
        }
    }

    return StudyInsights(
        range = range,
        buckets = buckets,
        totalStudySeconds = totalStudySeconds,
        sessionCount = rangeSessions.size,
        totalXp = rangeSessions.sumOf { it.xpEarned },
        totalGold = rangeSessions.sumOf { it.goldEarned },
        averageSessionSeconds = if (rangeSessions.isEmpty()) 0L else {
            totalStudySeconds / rangeSessions.size
        },
        targetSeconds = targetSeconds,
        targetCompletionPercent = if (targetSeconds <= 0L) 0 else {
            ((totalStudySeconds.toDouble() / targetSeconds) * 100.0).roundToInt()
        },
        bestDayLabel = bestDay?.let {
            SimpleDateFormat("EEE, d MMM", locale).apply { this.timeZone = timeZone }
                .format(it.key)
        },
        bestDaySeconds = bestDay?.value ?: 0L,
        topSubject = topSubject?.key,
        topSubjectSeconds = topSubject?.value ?: 0L,
        comparisonPercent = comparisonPercent,
    )
}

private fun buildDailyBuckets(
    rangeStart: Long,
    periodEnd: Long,
    dailyTotals: Map<Long, Long>,
    weekdayTargetMinutes: List<Int>,
    timeZone: TimeZone,
    locale: Locale,
    compactLabels: Boolean,
): List<InsightBucket> {
    val labelFormat = SimpleDateFormat(if (compactLabels) "EEE" else "d MMM", locale).apply {
        this.timeZone = timeZone
    }
    val buckets = mutableListOf<InsightBucket>()
    var dayStart = rangeStart
    while (dayStart < periodEnd) {
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = dayStart }
        buckets += InsightBucket(
            label = labelFormat.format(dayStart),
            studySeconds = dailyTotals[dayStart] ?: 0L,
            targetSeconds = targetMinutesForDay(calendar.get(Calendar.DAY_OF_WEEK), weekdayTargetMinutes) * 60L,
        )
        dayStart = addDays(dayStart, 1, timeZone)
    }
    return buckets
}

private fun buildMonthlyBuckets(
    sessions: List<StudySessionEntity>,
    rangeStart: Long,
    periodEnd: Long,
    weekdayTargetMinutes: List<Int>,
    timeZone: TimeZone,
    locale: Locale,
): List<InsightBucket> {
    val firstMonth = startOfMonth(rangeStart, timeZone)
    val labelFormat = SimpleDateFormat("MMM yy", locale).apply { this.timeZone = timeZone }
    val buckets = mutableListOf<InsightBucket>()
    var monthStart = firstMonth
    while (monthStart < periodEnd) {
        val nextMonth = addMonths(monthStart, 1, timeZone)
        val bucketEnd = minOf(nextMonth, periodEnd)
        buckets += InsightBucket(
            label = labelFormat.format(monthStart),
            studySeconds = sessions
                .filter { it.timestamp in monthStart until bucketEnd }
                .sumOf { it.durationSeconds },
            targetSeconds = sumDailyTargets(
                startMillis = maxOf(monthStart, rangeStart),
                endExclusiveMillis = bucketEnd,
                weekdayTargetMinutes = weekdayTargetMinutes,
                timeZone = timeZone,
            ),
        )
        monthStart = nextMonth
    }
    return buckets
}

private fun sumDailyTargets(
    startMillis: Long,
    endExclusiveMillis: Long,
    weekdayTargetMinutes: List<Int>,
    timeZone: TimeZone,
): Long {
    var totalMinutes = 0L
    var dayStart = startMillis
    while (dayStart < endExclusiveMillis) {
        val dayOfWeek = Calendar.getInstance(timeZone).apply { timeInMillis = dayStart }
            .get(Calendar.DAY_OF_WEEK)
        totalMinutes += targetMinutesForDay(dayOfWeek, weekdayTargetMinutes)
        dayStart = addDays(dayStart, 1, timeZone)
    }
    return totalMinutes * 60L
}

private fun targetMinutesForDay(dayOfWeek: Int, targets: List<Int>): Int = when (dayOfWeek) {
    Calendar.MONDAY -> targets[0]
    Calendar.TUESDAY -> targets[1]
    Calendar.WEDNESDAY -> targets[2]
    Calendar.THURSDAY -> targets[3]
    Calendar.FRIDAY -> targets[4]
    Calendar.SATURDAY -> targets[5]
    Calendar.SUNDAY -> targets[6]
    else -> targets[0]
}

private fun sessionSubject(session: StudySessionEntity): String {
    val rawName = session.bossName?.takeIf { it.isNotBlank() }
        ?: if (session.isFreeStudy) "Free Study" else "Study Session"
    return rawName.substringBefore(" (")
}

private fun startOfDay(millis: Long, timeZone: TimeZone): Long = Calendar.getInstance(timeZone).apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun startOfMonth(millis: Long, timeZone: TimeZone): Long = Calendar.getInstance(timeZone).apply {
    timeInMillis = millis
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun addDays(millis: Long, days: Int, timeZone: TimeZone): Long =
    Calendar.getInstance(timeZone).apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

private fun addMonths(millis: Long, months: Int, timeZone: TimeZone): Long =
    Calendar.getInstance(timeZone).apply {
        timeInMillis = millis
        add(Calendar.MONTH, months)
    }.timeInMillis
