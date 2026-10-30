package com.amkumirab.solostudying.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun WeeklyPlannerReportScreen(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, weekStart: LocalDate,
    onWeekChange: (LocalDate) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier,
    subjects: List<PlannerSubject> = emptyList(),
) {
    val report = remember(plan, bosses, weekStart) { weeklyPlannerReport(plan, bosses, weekStart) }
    var section by rememberSaveable { mutableStateOf("Days") }
    var creditHelp by rememberSaveable { mutableStateOf(false) }
    val firstMonday = plan.config.start.minusDays(plan.config.start.dayOfWeek.value - 1L)
    val finish = plan.config.courses.maxOf { it.finishBy }
    val currentMonday = today.minusDays(today.dayOfWeek.value - 1L)
    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH) }
    BackHandler(onBack = onBack)
    Column(modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 48.dp).testTag("report_back")) {
            Text("Back to schedule")
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Weekly report", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text("${weekStart.format(dateFormat)} – ${weekStart.plusDays(6).format(dateFormat)}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onWeekChange(weekStart.minusWeeks(1)) }, enabled = weekStart > firstMonday,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("report_previous_week")) { Text("Previous week") }
                OutlinedButton(onClick = { onWeekChange(weekStart.plusWeeks(1)) }, enabled = weekStart.plusDays(6) < finish,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("report_next_week")) { Text("Next week") }
            }
            if (weekStart != currentMonday) TextButton(onClick = { onWeekChange(currentMonday) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("This week") }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Planned focus: ${plannerTimeLabel(report.plannedMinutes)}", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("report_planned_total"))
                    if (plan.config.breakMinutes > 0) Text("Reserved breaks: ${plannerTimeLabel(report.breakMinutes)} · not study time")
                    Text("Credit assigned to these sessions: ${plannerCreditLabel(report.creditedSeconds)}",
                        modifier = Modifier.testTag("report_credit"))
                    Text("Not study performed during this week.", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { creditHelp = !creditHelp }, modifier = Modifier.heightIn(min = 48.dp).testTag("report_credit_help")) {
                        Text(if (creditHelp) "Hide credit explanation" else "About session credit")
                    }
                    if (creditHelp) Text("Credit follows session order, not the date you studied. Completed goals can fully credit their sessions. For actual study dates, see Profile → History.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Days", "Subjects", "Priority").forEach { tab ->
                    FilterChip(selected = section == tab, onClick = { section = tab }, label = { Text(tab) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("report_section_$tab"))
                }
            }
            if (report.plannedMinutes == 0) Text("No focus time is planned for this week.", style = MaterialTheme.typography.bodyMedium)
            PlannerReportCharts(section, report, plan, bosses, subjects)
        }
    }
}
