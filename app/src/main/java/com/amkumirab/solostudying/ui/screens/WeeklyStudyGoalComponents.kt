package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate

internal fun weeklyGoalMinutes(text: String): Int? = when {
    text.length > 12 -> null
    Regex("0+([.,]0{1,2})?").matches(text.trim()) -> 0
    else -> plannerHoursToMinutes(text)?.takeIf { it <= 3360 }
}

@Composable
internal fun WeeklyStudyGoalEditor(value: String, onChange: (String) -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    OutlinedTextField(value, { onChange(it.take(12)) }, enabled = enabled,
        label = { Text("Weekly goal (hours)") },
        supportingText = { Text("0 to 56 hours. Add a goal to preview a recommended split between your courses. Leave blank to keep only your deadline plan.") },
        isError = value.isNotBlank() && weeklyGoalMinutes(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
        modifier = modifier.fillMaxWidth().testTag("planner_weekly_goal"))
}

@Composable
internal fun WeeklyAllocationCard(plan: StudyPlan, bosses: List<BossEntity>, subjects: List<PlannerSubject>, modifier: Modifier = Modifier) {
    if (plan.config.weeklyGoalMinutes == null) return
    val weekStart = plan.config.start.minusDays(plan.config.start.dayOfWeek.value - 1L)
    val allocation = remember(plan.config, subjects, weekStart) { recommendWeeklyHours(plan.config, subjects, weekStart) }
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recommended weekly split", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("${maxOf(weekStart, plan.config.start)} to ${weekStart.plusDays(6)}", style = MaterialTheme.typography.bodySmall)
            Text("Goal: ${plannerTimeLabel(allocation.goalMinutes)} · Available: ${plannerTimeLabel(allocation.availableMinutes)}")
            Text("Recommended: ${plannerTimeLabel(allocation.allocatedMinutes)}", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("weekly_recommended_total"))
            Text("Priority and nearby finish dates increase a course's share. Preferred weekly hours act as a ceiling for this recommendation.", style = MaterialTheme.typography.bodySmall)
            if (plan.config.start != weekStart) Text("This is a partial week. Capacity before your plan's start date is excluded.", style = MaterialTheme.typography.bodySmall)
            plan.config.courses.forEach { course ->
                val subject = subjects.firstOrNull { it.bossId == course.bossId }
                val name = subject?.name ?: bosses.firstOrNull { it.id == course.bossId }?.name ?: "Removed course"
                val minutes = allocation.minutesByCourse[course.bossId] ?: 0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PlannerSubjectTitle(name, subject?.color)
                    Text("${plannerTimeLabel(minutes)} recommended · Priority ${course.priority}",
                        modifier = Modifier.testTag("weekly_share_${course.bossId}"))
                    LinearProgressIndicator(progress = { if (allocation.goalMinutes == 0) 0f else minutes.toFloat() / allocation.goalMinutes }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (allocation.unallocatedMinutes > 0) {
                Text("${plannerTimeLabel(allocation.unallocatedMinutes)} of your goal could not be allocated.", color = MaterialTheme.colorScheme.error)
                Text("Review available days, finish dates, remaining work and preferred weekly limits.", style = MaterialTheme.typography.bodySmall)
            } else Text("Your goal is fully covered by this recommendation.")
            Text("Free capacity: ${plannerTimeLabel(allocation.availableMinutes - allocation.allocatedMinutes)}")
            Text("Recommendations are separate from the date-only schedule below. Saving keeps the displayed sessions.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun WeeklyGoalSummary(plan: StudyPlan, weekStart: LocalDate, modifier: Modifier = Modifier) {
    val goal = plan.config.weeklyGoalMinutes ?: return
    val weekEnd = weekStart.plusDays(6)
    val scheduled = plan.blocks.filter { it.date in weekStart..weekEnd }.sumOf { it.minutes }
    val available = (0L..6L).sumOf { offset ->
        val day = weekStart.plusDays(offset)
        if (day < plan.config.start) 0 else plan.config.capacity(day)
    }
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Weekly study goal", style = MaterialTheme.typography.titleMedium)
            Text("Goal: ${plannerTimeLabel(goal)} · Scheduled: ${plannerTimeLabel(scheduled)}", modifier = Modifier.testTag("weekly_actual_total"))
            LinearProgressIndicator(progress = { if (goal == 0) 0f else (scheduled.toFloat() / goal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("Available: ${plannerTimeLabel(available)} · Free: ${plannerTimeLabel((available - scheduled).coerceAtLeast(0))}")
            Text(if (scheduled > goal) "Scheduled above your goal: ${plannerTimeLabel(scheduled - goal)}"
                else "Still to schedule: ${plannerTimeLabel(goal - scheduled)}")
            Text("Scheduled time is planned work, not completed study. Edit answers to review your recommended split.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
