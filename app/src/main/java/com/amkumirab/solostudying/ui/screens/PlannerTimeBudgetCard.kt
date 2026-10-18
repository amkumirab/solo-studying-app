package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import java.time.LocalDate
import kotlin.math.abs

@Composable
internal fun PlannerTimeBudgetCard(
    original: PlannerConfig,
    scenario: PlannerConfig,
    bosses: List<BossEntity>,
    dailyAdjustment: Int,
    usingRequiredBudget: Boolean,
    onAdjustmentChange: (Int) -> Unit,
    onUseRequired: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subjects: List<PlannerSubject> = emptyList(),
    availability: WeeklyAvailability? = null,
) {
    val report = remember(scenario) { analyzePlannerTimeBudget(scenario) }
    val originalReport = remember(original) { analyzePlannerTimeBudget(original) }
    val requiredConfig = remember(original) { requiredPlannerDailyTime(original) }
    var expanded by rememberSaveable(original) { mutableStateOf(false) }
    val change = report.availableMinutes - originalReport.availableMinutes
    OutlinedCard(modifier.fillMaxWidth().testTag("planner_time_budget")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Study time check", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            Text("${plannerTimeLabel(report.requiredMinutes)} remaining · ${report.studyDays} study days")
            val required = report.requiredDailyMinutes
            if (required == null) {
                Text("A deadline has no available study days. Change your weekdays or finish date.",
                    color = MaterialTheme.colorScheme.error)
            } else {
                Text("$required min per study day", style = MaterialTheme.typography.titleMedium)
                Text("Minimum if every study day has the same budget. Shared by all courses; earlier deadlines are included.",
                    style = MaterialTheme.typography.bodySmall)
                if (required > 480) Text("This exceeds the 480 min daily limit. Add study days, extend a deadline or revise the remaining effort.",
                    color = MaterialTheme.colorScheme.error)
            }
            HorizontalDivider()
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Available through ${report.deadlines.last().finishBy}: ${plannerTimeLabel(report.availableMinutes)}")
                Text(when {
                    report.balanceMinutes > 0 -> "${plannerTimeLabel(report.balanceMinutes)} spare overall"
                    report.balanceMinutes < 0 -> "${plannerTimeLabel(-report.balanceMinutes)} short overall"
                    else -> "No spare time overall"
                }, style = MaterialTheme.typography.titleMedium)
                Text(when {
                    change > 0 -> "${plannerTimeLabel(change)} more than your entered budget"
                    change < 0 -> "${plannerTimeLabel(-change)} less than your entered budget"
                    else -> "Same total time as your entered budget"
                })
                val shortage = report.deadlines.firstOrNull { it.balanceMinutes < 0 }
                if (shortage != null) Text("Deadline shortage: ${plannerTimeLabel(-shortage.balanceMinutes)} by ${shortage.finishBy}. Later spare time cannot cover this deadline.",
                    color = MaterialTheme.colorScheme.error)
                else Text("All deadlines fit this budget.", color = MaterialTheme.colorScheme.primary)
            }
            Text("Try a different daily budget", style = MaterialTheme.typography.titleMedium)
            Text("${if (dailyAdjustment > 0) "+" else ""}$dailyAdjustment min per study day from ${if (usingRequiredBudget) "the required" else "your entered"} budget")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onAdjustmentChange(dailyAdjustment - 15) },
                    enabled = enabled && adjustPlannerDailyTime(scenario, -15) != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("budget_decrease"),
                ) { Text("-15 min / day") }
                OutlinedButton(
                    onClick = { onAdjustmentChange(dailyAdjustment + 15) },
                    enabled = enabled && adjustPlannerDailyTime(scenario, 15)?.fitsAvailability(availability) == true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("budget_increase"),
                ) { Text("+15 min / day") }
            }
            Text("Applies equally to each study weekday. Rest days stay unchanged; study days stay within 1 to 480 min.",
                style = MaterialTheme.typography.bodySmall)
            if (availability != null) Text("Saved availability is the daily ceiling. Edit your windows to add more capacity.", style = MaterialTheme.typography.bodySmall)
            if (requiredConfig != null) {
                OutlinedButton(onClick = onUseRequired, enabled = enabled && requiredConfig != scenario && requiredConfig.fitsAvailability(availability),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("budget_use_required")) {
                    Text("Use required daily time")
                }
            }
            TextButton(onClick = onReset, enabled = enabled && (usingRequiredBudget || dailyAdjustment != 0),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("budget_reset")) {
                Text("Reset to entered budget")
            }
            TextButton(onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (expanded) "Hide deadline breakdown" else "Show deadline breakdown")
            }
            if (expanded) {
                Text("Daily budgets", style = MaterialTheme.typography.titleMedium)
                listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").forEachIndexed { index, day ->
                    val minutes = scenario.weekdayMinutes[index]
                    Text("$day: ${if (minutes == 0) "Rest day" else plannerTimeLabel(minutes)}")
                }
                report.deadlines.forEach { deadline ->
                    HorizontalDivider()
                    Text("Finish by ${deadline.finishBy}", style = MaterialTheme.typography.titleMedium)
                    deadline.courseIds.forEach { id ->
                        val course = scenario.courses.first { it.bossId == id }
                        Text("${subjects.firstOrNull { it.bossId == id }?.name ?: bosses.firstOrNull { it.id == id }?.name ?: "Removed course"}: ${plannerTimeLabel(course.remainingMinutes)} remaining")
                    }
                    Text("All courses due by this date: ${plannerTimeLabel(deadline.requiredMinutes)}\n" +
                        "${deadline.studyDays} study days · ${plannerTimeLabel(deadline.availableMinutes)} available")
                    Text("${plannerTimeLabel(abs(deadline.balanceMinutes))} ${if (deadline.balanceMinutes < 0) "short" else "spare"} by this date",
                        color = if (deadline.balanceMinutes < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
            }
            Text("Preview only. Nothing changes until you save the plan.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun plannerTimeLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun PlannerTimeBudgetPreview() {
    val start = LocalDate.of(2026, 10, 5)
    val config = PlannerConfig(start, listOf(PlannerCourse(1, 6000, start.plusDays(55), 2, 0)),
        listOf(150, 150, 150, 150, 150, 0, 0), 25)
    SoloStudyingTheme {
        PlannerTimeBudgetCard(config, config, emptyList(), 0, false, {}, {}, {})
    }
}
