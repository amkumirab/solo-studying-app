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
internal fun PlannerGuidanceContent(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, weekStart: LocalDate,
    modifier: Modifier = Modifier, subjects: List<PlannerSubject> = emptyList(),
    proposed: Boolean = false, actionsEnabled: Boolean = true, onAction: ((PlannerAdviceAction) -> Unit)? = null,
) {
    val advice = remember(plan, bosses, today, weekStart) { plannerGuidance(plan, bosses, today, weekStart) }
    if (advice.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val scope = if (proposed) "proposed" else "accepted"
    fun name(id: Int) = subjects.firstOrNull { it.bossId == id }?.name ?: bosses.firstOrNull { it.id == id }?.name ?: "Removed course"
    OutlinedCard(modifier.fillMaxWidth().testTag("guidance_$scope")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (proposed) "Proposed plan guidance" else "Planner guidance", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            Text("${if (proposed) "Proposed" else "Accepted"} week · $weekStart to ${weekStart.plusDays(6)}",
                style = MaterialTheme.typography.bodySmall)
            Text("Deadline checks as of $today. Planning advice, not completed study.", style = MaterialTheme.typography.bodySmall)
            advice.take(if (expanded) advice.size else 1).forEachIndexed { index, item ->
                Text(adviceMessage(item, ::name), modifier = Modifier.testTag("guidance_${scope}_message_$index"))
                if (!proposed && onAction != null) item.action?.let { action ->
                    OutlinedButton(onClick = { if (actionsEnabled) onAction(action) }, enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("guidance_${scope}_action_$index")) {
                        Text(when (action) {
                            PlannerAdviceAction.Availability -> "Review availability"
                            PlannerAdviceAction.Subjects -> "Review subjects"
                            PlannerAdviceAction.EditAnswers -> "Edit plan answers"
                            PlannerAdviceAction.ReviewPlan -> "Review remaining plan"
                        })
                    }
                }
            }
            if (advice.size > 1) TextButton(onClick = { expanded = !expanded },
                modifier = Modifier.heightIn(min = 48.dp).testTag("guidance_${scope}_details")
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }) {
                Text(if (expanded) "Show less guidance" else "Show more guidance (${advice.size - 1})")
            }
        }
    }
}

private fun adviceMessage(advice: PlannerAdvice, name: (Int) -> String): String = when (advice) {
    is PlannerAdvice.MissingCourses -> "${advice.count} planned ${if (advice.count == 1) "course has" else "courses have"} a deleted goal. Edit answers to repair the plan; recorded study is kept."
    is PlannerAdvice.Deadline -> when {
        advice.overdue -> "${name(advice.bossId)}: the finish-by date ${advice.finishBy} has passed. ${plannerTimeLabel(advice.uncoveredMinutes)} of remaining work has no future slot. Review your effort and choose a new date."
        advice.uncoveredMinutes > 0 -> "${name(advice.bossId)}: finish by ${advice.finishBy}. ${plannerTimeLabel(advice.uncoveredMinutes)} of remaining work is not covered by future sessions. Review capacity, course limits and dates before changing the plan."
        else -> "${name(advice.bossId)}: finish by ${advice.finishBy}. Remaining work has future slots; keep an eye on this approaching date."
    }
    is PlannerAdvice.Priority -> "${name(advice.bossId)} has the highest priority among unfinished courses (${advice.priority}/5). " +
        if (advice.plannedMinutes == 0) "No study is planned for this selected week. Review its allocation if you intend to study it this week."
        else "${plannerTimeLabel(advice.plannedMinutes)} is planned for this selected week. Subject-setting edits apply only after reviewing a new plan."
    is PlannerAdvice.Weekly -> when (advice.status) {
        WeekPlanningStatus.BudgetShortfall -> "The weekly goal exceeds the accepted study budget by ${plannerTimeLabel(advice.budgetGapMinutes)}. Review availability and your focus budgets, or reduce the goal. Breaks and course limits also apply."
        WeekPlanningStatus.UnfilledGoal -> "${plannerTimeLabel(advice.goalGapMinutes)} still to plan for this weekly goal. Unallocated budget is not guaranteed free clock time. Review remaining work, breaks, course limits and dates."
        WeekPlanningStatus.AboveGoal -> "${plannerTimeLabel(advice.plannedMinutes)} is planned, above the weekly goal. Review your answers if you want to reduce future planning; existing sessions stay unchanged."
        else -> "The weekly goal is fully planned: ${plannerTimeLabel(advice.plannedMinutes)}. No extra allocation is needed to meet this planning target; scheduled time is not completed study."
    }
}
