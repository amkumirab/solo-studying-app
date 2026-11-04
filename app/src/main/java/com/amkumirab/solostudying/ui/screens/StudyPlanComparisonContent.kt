package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate

@Composable
internal fun StudyPlanComparisonContent(
    comparison: StudyPlanComparison, bosses: List<BossEntity>, subjects: List<PlannerSubject>, today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val first = plannerWeekStart(minOf(comparison.accepted.config.start, comparison.proposed.config.start))
    val last = plannerWeekStart((comparison.accepted.config.courses + comparison.proposed.config.courses).maxOf { it.finishBy })
    var weekText by rememberSaveable { mutableStateOf(plannerWeekStart(today).coerceIn(first, last).toString()) }
    val week = LocalDate.parse(weekText).coerceIn(first, last)
    val totals = remember(comparison, week) { comparison.week(week) }
    val counts = remember(comparison) { comparison.changes.groupingBy { it.kind }.eachCount() }
    val manualCount = remember(comparison) { comparison.manualSlotsReplaced }
    fun name(id: Int) = subjects.firstOrNull { it.bossId == id }?.name
        ?: bosses.firstOrNull { it.id == id }?.name ?: "Removed course #$id"
    Column(modifier.fillMaxWidth().testTag("study_plan_comparison"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Compare with accepted plan", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("All plan dates · Kept: ${counts[StudyPlanChangeKind.Kept] ?: 0} · Replanned: ${counts[StudyPlanChangeKind.Replanned] ?: 0} · Added: ${counts[StudyPlanChangeKind.Added] ?: 0} · Removed: ${counts[StudyPlanChangeKind.Removed] ?: 0}",
            modifier = Modifier.testTag("comparison_counts"))
        Text("The proposal covers remaining work from today. Past or studied slots may disappear; goal progress and study history are kept.", style = MaterialTheme.typography.bodySmall)
        if (manualCount > 0) Text("$manualCount manual ${if (manualCount == 1) "slot" else "slots"} will be replaced. Review their timing before applying.",
            color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("comparison_manual_warning"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { weekText = week.minusWeeks(1).toString() }, enabled = week > first,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("comparison_previous_week")) { Text("Previous week") }
            OutlinedButton(onClick = { weekText = week.plusWeeks(1).toString() }, enabled = week < last,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("comparison_next_week")) { Text("Next week") }
        }
        Text("$week to ${week.plusDays(6)}", style = MaterialTheme.typography.bodySmall)
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Accepted focus: ${plannerTimeLabel(totals.accepted.plannedMinutes)}", modifier = Modifier.testTag("comparison_accepted_focus"))
                Text("Proposed remaining focus: ${plannerTimeLabel(totals.proposed.plannedMinutes)}", modifier = Modifier.testTag("comparison_proposed_focus"))
                Text("Accepted study budget: ${plannerTimeLabel(totals.accepted.availableBudgetMinutes)}")
                Text("Proposed study budget: ${plannerTimeLabel(totals.proposed.availableBudgetMinutes)}")
                Text("Proposed unused budget: ${plannerTimeLabel(totals.proposed.unallocatedBudgetMinutes)}")
                totals.proposed.goalMinutes?.let { Text("Proposed weekly goal: ${plannerTimeLabel(it)}") }
                Text("Budgets are not guaranteed free clock slots; breaks, deadlines and course limits still apply.", style = MaterialTheme.typography.bodySmall)
            }
        }
        PlannerGuidanceContent(comparison.proposed, bosses, today, week, subjects = subjects, proposed = true)
        totals.courses.filter { it.acceptedMinutes > 0 || it.proposedMinutes > 0 }.forEach { course ->
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag("comparison_course_${course.bossId}")) {
                Text(name(course.bossId), style = MaterialTheme.typography.titleSmall)
                val delta = course.proposedMinutes - course.acceptedMinutes
                Text("${plannerTimeLabel(course.acceptedMinutes)} accepted → ${plannerTimeLabel(course.proposedMinutes)} proposed")
                Text(if (delta == 0) "No change in weekly focus" else "${plannerTimeLabel(kotlin.math.abs(delta))} ${if (delta > 0) "more" else "less"} planned focus",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (comparison.proposed.shortfalls.isNotEmpty()) {
            Text("Not scheduled before the deadline", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { heading() })
            comparison.proposed.shortfalls.forEach { (id, minutes) ->
                Text("${name(id)}: ${plannerTimeLabel(minutes)} unscheduled by ${comparison.proposed.config.courses.first { it.bossId == id }.finishBy}",
                    modifier = Modifier.testTag("comparison_shortfall_$id"))
            }
        }
        StudyPlanChangeDetails(comparison, ::name)
    }
}

@Composable
private fun StudyPlanChangeDetails(comparison: StudyPlanComparison, courseName: (Int) -> String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val changes = remember(comparison) { comparison.changes.filter { it.kind != StudyPlanChangeKind.Kept } }
    val lastPage = (changes.lastIndex / 20).coerceAtLeast(0)
    val currentPage = page.coerceIn(0, lastPage)
    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = 48.dp).testTag("comparison_details")
        .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }) {
        Text(if (expanded) "Hide session changes" else "Show session changes")
    }
    if (expanded) {
        Text("Replanned pairs compare same-course slots in time order. Sessions may be split or merged; these are not study-credit identities.", style = MaterialTheme.typography.bodySmall)
        if (changes.isEmpty()) Text("All accepted slots keep their timing and edit flags.")
        else {
            Text("Changed sessions · Page ${currentPage + 1} of ${lastPage + 1}")
            changes.drop(currentPage * 20).take(20).forEachIndexed { index, change ->
                OutlinedCard(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
                    .testTag("comparison_change_${currentPage * 20 + index}")) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${change.kind.name} · ${courseName(change.before?.bossId ?: change.after!!.bossId)}", style = MaterialTheme.typography.titleSmall)
                        Text("Accepted: ${comparisonSlotLabel(change.before)}")
                        Text("Proposed: ${comparisonSlotLabel(change.after)}")
                    }
                }
            }
            if (lastPage > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { page = currentPage - 1 }, enabled = currentPage > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("comparison_previous_page")) { Text("Previous") }
                OutlinedButton(onClick = { page = currentPage + 1 }, enabled = currentPage < lastPage,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("comparison_next_page")) { Text("Next") }
            }
        }
    }
}

private fun comparisonSlotLabel(block: StudyBlock?): String = block?.let {
    val flags = buildList {
        if (it.locked) add("Locked")
        if (it.manual) add("Manual")
        if (it.durationEdited) add("Edited duration")
    }.ifEmpty { listOf("Automatic") }
    "${it.date} · ${it.clockRange() ?: "Date-only"} · ${plannerTimeLabel(it.minutes)} · ${flags.joinToString(" · ")}"
} ?: "No slot"
