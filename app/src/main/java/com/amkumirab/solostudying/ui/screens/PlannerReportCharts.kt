package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun PlannerReportCharts(
    section: String, report: WeeklyPlannerReport, plan: StudyPlan,
    bosses: List<BossEntity>, subjects: List<PlannerSubject>, modifier: Modifier = Modifier,
) {
    var dayIndex by rememberSaveable(report.weekStart) { mutableIntStateOf(0) }
    var courseId by rememberSaveable(report.weekStart) { mutableIntStateOf(report.courses.first().bossId) }
    var priorityName by rememberSaveable(report.weekStart) { mutableStateOf(PlannerPriorityBand.High.name) }
    fun courseName(id: Int) = subjects.firstOrNull { it.bossId == id }?.name
        ?: bosses.firstOrNull { it.id == id }?.name ?: "Removed course #$id"
    @Composable fun Detail(text: String) {
        Text(text, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).testTag("report_detail"))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (section) {
            "Subjects" -> {
                Text("Share of planned focus · tap a subject for details", style = MaterialTheme.typography.bodySmall)
                val selectedId = courseId.takeIf { id -> report.courses.any { it.bossId == id } } ?: report.courses.first().bossId
                report.courses.forEach { course ->
                    val selected = selectedId == course.bossId
                    PlannerReportBar(courseName(course.bossId), course.plannedMinutes, report.plannedMinutes, selected,
                        { courseId = course.bossId }, Modifier.testTag("report_course_${course.bossId}"),
                        subjects.firstOrNull { it.bossId == course.bossId }?.color?.tint() ?: MaterialTheme.colorScheme.primary)
                    if (selected) Detail(buildString {
                        append("${courseName(course.bossId)}\nPriority ${course.priority} of 5 · ${subjectPriorityLabel(course.priority)}\n")
                        append("Credit assigned: ${plannerCreditLabel(course.creditedSeconds)}")
                        if (course.missingCourse) append("\nGoal removed · no study credit available")
                    })
                }
            }
            "Priority" -> {
                Text("Share of planned focus · priorities from the accepted plan", style = MaterialTheme.typography.bodySmall)
                report.priorities.forEach { item ->
                    val selected = item.band.name == priorityName
                    val range = if (item.band == PlannerPriorityBand.Normal) "3" else "${item.band.levels.first}–${item.band.levels.last}"
                    PlannerReportBar("${item.band.name} · $range", item.plannedMinutes, report.plannedMinutes, selected,
                        { priorityName = item.band.name }, Modifier.testTag("report_priority_${item.band.name}"))
                    if (selected) Detail(report.courses.filter { it.priority in item.band.levels }.joinToString("\n") {
                        "${courseName(it.bossId)} · ${plannerTimeLabel(it.plannedMinutes)} planned"
                    }.ifEmpty { "No subjects in this priority group" })
                }
            }
            else -> {
                Text("Daily planned focus · bars share the busiest day's scale", style = MaterialTheme.typography.bodySmall)
                val scale = report.days.maxOf { it.plannedMinutes }
                report.days.forEachIndexed { index, day ->
                    val selected = index == dayIndex
                    PlannerReportBar(day.date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)),
                        day.plannedMinutes, scale, selected, { dayIndex = index }, Modifier.testTag("report_day_$index"))
                    if (selected) {
                        val sessions = plan.blocks.filter { it.date == day.date }
                            .sortedWith(compareBy<StudyBlock> { it.startMinute ?: 0 }.thenBy { it.id })
                        Detail(buildString {
                            append("${day.sessionCount} ${if (day.sessionCount == 1) "session" else "sessions"} · ${plannerTimeLabel(day.breakMinutes)} reserved breaks\n")
                            append(sessions.joinToString("\n") { block ->
                                val clock = block.clockRange()?.let { "$it · " }.orEmpty()
                                "$clock${courseName(block.bossId)} · ${plannerTimeLabel(block.minutes)} planned"
                            }.ifEmpty { "No sessions planned" })
                        })
                    }
                }
            }
        }
    }
}

internal fun plannerCreditLabel(seconds: Long): String {
    if (seconds == 0L) return "0 min"
    return buildList {
        if (seconds >= 3600) add("${seconds / 3600} h")
        if (seconds % 3600 / 60 > 0) add("${seconds % 3600 / 60} min")
        if (seconds % 60 > 0) add("${seconds % 60} sec")
    }.joinToString(" ")
}
