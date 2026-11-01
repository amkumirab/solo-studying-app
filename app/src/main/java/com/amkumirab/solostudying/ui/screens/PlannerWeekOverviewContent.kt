package com.amkumirab.solostudying.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun PlannerWeekOverviewContent(plan: StudyPlan, weekStart: LocalDate, modifier: Modifier = Modifier) {
    val overview = remember(plan, weekStart) { plannerWeekOverview(plan, weekStart) }
    var details by rememberSaveable { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(overview.progress ?: 0f, label = "Weekly goal planned")
    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH) }
    Column(modifier.fillMaxWidth().testTag("planner_week_overview"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Weekly overview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text("${weekStart.format(dateFormat)} – ${weekStart.plusDays(6).format(dateFormat)}", style = MaterialTheme.typography.bodySmall)
        OverviewMetric("Weekly goal", overview.goalMinutes?.let(::plannerTimeLabel)
            ?: if (overview.status == WeekPlanningStatus.OutsidePlan) "Not applicable" else "Not set", Modifier.testTag("overview_goal"))
        OverviewMetric("Planned focus", plannerTimeLabel(overview.plannedMinutes), Modifier.testTag("overview_planned"))
        OverviewMetric("Study budget", plannerTimeLabel(overview.availableBudgetMinutes), Modifier.testTag("overview_budget"))
        OverviewMetric("Unallocated budget", plannerTimeLabel(overview.unallocatedBudgetMinutes), Modifier.testTag("overview_unallocated"))
        if (overview.reservedBreakMinutes > 0) OverviewMetric("Reserved breaks", plannerTimeLabel(overview.reservedBreakMinutes), Modifier.testTag("overview_breaks"))
        overview.progress?.let { target ->
            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().testTag("overview_progress")
                .clearAndSetSemantics {
                    contentDescription = "Weekly goal planned"
                    stateDescription = "${overview.percentPlanned}% planned"
                    progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
                })
            Text("${overview.percentPlanned}% of goal planned", style = MaterialTheme.typography.bodySmall)
        }
        Text(overviewFeedback(overview), style = MaterialTheme.typography.titleSmall,
            color = if (overview.status == WeekPlanningStatus.BudgetShortfall) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("overview_feedback").semantics { liveRegion = LiveRegionMode.Polite })
        if (overview.status == WeekPlanningStatus.BudgetShortfall) Text("${plannerTimeLabel(overview.remainingGoalMinutes!!)} still to plan")
        if (overview.isPartialWeek && overview.goalMinutes != null) Text("Partial week · full weekly goal", style = MaterialTheme.typography.bodySmall)
        Text("Planned time, not completed study.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { details = !details }, modifier = Modifier.heightIn(min = 48.dp).testTag("overview_details")
            .semantics { stateDescription = if (details) "Expanded" else "Collapsed" }) {
            Text(if (details) "Hide planning details" else "Planning details")
        }
        if (details) {
            Text("Unallocated budget is not a guaranteed free slot. Breaks, deadlines, remaining course work and saved limits still apply.", style = MaterialTheme.typography.bodySmall)
            Text("These are your accepted daily focus budgets within the plan dates. Reserved breaks use clock windows, not focus time. Edit answers and review the preview to change your plan.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OverviewMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val root = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
    if (LocalDensity.current.fontScale > 1.3f) {
        Column(root, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    } else Row(root, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.55f))
        Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.45f))
    }
}

private fun overviewFeedback(overview: PlannerWeekOverview): String = when (overview.status) {
    WeekPlanningStatus.OutsidePlan -> "No active plan dates this week"
    WeekPlanningStatus.NoGoal -> "No weekly target · deadline-only plan"
    WeekPlanningStatus.ZeroGoal -> "No focus time requested"
    WeekPlanningStatus.AboveGoal -> "Above goal by ${plannerTimeLabel(overview.aboveGoalMinutes!!)}"
    WeekPlanningStatus.BudgetShortfall -> "Goal exceeds study budget by ${plannerTimeLabel(overview.budgetShortfallMinutes!!)}"
    WeekPlanningStatus.UnfilledGoal -> "${plannerTimeLabel(overview.remainingGoalMinutes!!)} still to plan"
    WeekPlanningStatus.FullyPlanned -> "Weekly goal fully planned"
}
