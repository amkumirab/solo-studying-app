package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.ui.viewmodel.StudyPlannerViewModel
import java.time.LocalDate

@Composable
fun StudyPlannerEntry(
    bosses: List<BossEntity>, today: LocalDate, weekdayDefaults: List<Int>, isSessionActive: Boolean,
    onStart: (BossEntity, Int) -> Unit, modifier: Modifier = Modifier,
    plannerViewModel: StudyPlannerViewModel = studyPlannerViewModel(),
    showDailySessions: Boolean = true,
    content: @Composable (StudyPlan?, @Composable () -> Unit) -> Unit = { _, section ->
        OutlinedCard(modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { section() } }
    },
) {
    val state by plannerViewModel.state.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    var reportWeek by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSection by rememberSaveable { mutableStateOf("Schedule") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    val busy = state.busy
    // A failed compare-and-swap invalidates remembered drafts and pending move dialogs.
    key(state.editorGeneration) {
        val restoredEditor = rememberSaveableStateHolder()
        content(state.plan) {
            if (!showDailySessions) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val accepted = state.plan
                    if (accepted == null) {
                        Text("Weekly study plan", style = MaterialTheme.typography.titleMedium)
                        Text("Choose courses, deadlines and available time. Your plan stays on this device.", style = MaterialTheme.typography.bodyMedium)
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = { open = true }, enabled = state.loaded && !isSessionActive && !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_study_planner"),
                    ) { Text(if (!state.loaded) "Loading…" else if (state.plan == null) "Create weekly plan" else "Open weekly plan") }
                    if (accepted != null) PlannerWeekOverviewContent(accepted, today.minusDays(today.dayOfWeek.value - 1L))
                }
            } else {
                StudyPlannerToday(state.plan, bosses, today, state.loaded, isSessionActive || busy, state.error,
                    onOpen = { open = true }, onStart = onStart,
                    onSave = { updated, saved -> plannerViewModel.save(updated, state.snapshot, saved) }, subjects = state.subjects)
            }
        }
        if (open) Dialog(onDismissRequest = { if (!busy) { if (reportWeek != null) reportWeek = null else { open = false; confirmation = null } } }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                    TextButton(onClick = { open = false; confirmation = null }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close planner") }
                    if (confirmation != "rebuild" && (reportWeek == null || state.plan == null)) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Schedule", "Subjects", "Availability").forEach { section ->
                            FilterChip(selected = selectedSection == section, onClick = { selectedSection = section }, enabled = !busy,
                                label = { Text(section) }, modifier = Modifier.heightIn(min = 48.dp))
                        }
                    }
                    state.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
                        if (state.unreadableRecord) OutlinedButton(onClick = { confirmation = "clear" }, enabled = !busy, modifier = Modifier.padding(16.dp)) { Text("Clear saved plan") }
                    }
                    val plan = state.plan
                    if (state.unreadableAvailability && selectedSection != "Availability") {
                        TextButton(onClick = { confirmation = null; selectedSection = "Availability" }, enabled = !busy,
                            modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp)) { Text("Review unreadable availability settings") }
                    }
                    if (!state.loaded) {
                        Text("Loading planner settings…", modifier = Modifier.padding(16.dp))
                    } else restoredEditor.SaveableStateProvider("planner-editor") {
                    if (confirmation == "rebuild" && plan != null) {
                        StudyRebuildReview(state.snapshot, bosses, today, busy,
                            onCancel = { confirmation = null },
                            onEditAnswers = { confirmation = null; editing = true; selectedSection = "Schedule" },
                            onApply = { accepted, expected -> plannerViewModel.save(accepted, expected) { confirmation = null } },
                            modifier = Modifier.weight(1f), isSessionActive = isSessionActive,
                            blockedMessage = state.subjectError.takeIf { state.unreadableSubjects }
                                ?: state.availabilityError.takeIf { state.unreadableAvailability })
                    } else if (reportWeek != null && plan != null) {
                        WeeklyPlannerReportScreen(plan, bosses, today, LocalDate.parse(reportWeek),
                            onWeekChange = { reportWeek = it.toString() }, onBack = { reportWeek = null },
                            modifier = Modifier.weight(1f), subjects = state.subjects)
                    } else if (selectedSection == "Availability") {
                        PlannerAvailabilityScreen(state.snapshot, plan?.config?.weekdayMinutes ?: weekdayDefaults, busy, state.availabilityError, state.unreadableAvailability,
                            onSave = { value, expected, saved -> plannerViewModel.saveAvailability(value, expected, saved) },
                            onReset = { plannerViewModel.clearAvailability(it) }, modifier = Modifier.weight(1f))
                    } else if (selectedSection == "Subjects") {
                        PlannerSubjectsScreen(state.subjects, bosses, today, busy, state.subjectError, state.unreadableSubjects,
                            onSave = { subject, saved -> plannerViewModel.saveSubject(subject, bosses, saved) },
                            onRemove = { id, removed -> plannerViewModel.removeSubject(id, removed) },
                            onReset = { reset -> plannerViewModel.clearSubjects(reset) }, modifier = Modifier.weight(1f))
                    } else if (editing || (plan == null && !state.unreadableRecord)) {
                        val draftSnapshot = remember { state.snapshot }
                        StudyPlannerWizard(bosses, today, plan, weekdayDefaults, busy || isSessionActive,
                            onAccept = { accepted -> plannerViewModel.save(accepted, draftSnapshot) { editing = false } },
                            onCancel = { if (plan == null) open = false else editing = false },
                            modifier = Modifier.weight(1f), subjects = state.subjects, availability = state.availability)
                    } else if (plan != null) {
                        if (plan.config.weeklyGoalMinutes != null && plan.config.weeklyScheduling == null) {
                            Text("Your saved weekly goal is advisory. Edit answers to preview weekly-limited sessions, or confirm a rebuild to apply it. Existing sessions are unchanged.", modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        if (state.availability?.let { it != plan.config.studyWindows } == true) {
                            Text("Availability differs from this accepted plan. Edit answers or confirm a rebuild to apply it; existing sessions are unchanged.", modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = true }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Edit answers") }
                            OutlinedButton(onClick = { confirmation = "rebuild" }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Rebuild remaining") }
                        }
                        TextButton(onClick = { confirmation = "clear" }, enabled = !busy, modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp)) { Text("Remove plan") }
                        restoredEditor.SaveableStateProvider("planner-week") {
                        StudyPlannerWeek(plan, bosses, today, isSessionActive || busy,
                            onStart = { boss, minutes ->
                                if (!isSessionActive && !busy) { onStart(boss, minutes); open = false }
                            }, onMove = { plannerViewModel.save(it, state.snapshot) }, modifier = Modifier.weight(1f), subjects = state.subjects,
                            onOpenReport = { reportWeek = it.toString() })
                        }
                    }
                    }
                }
            }
        }
    }
    if (confirmation == "clear") {
        AlertDialog(onDismissRequest = { confirmation = null },
            title = { Text("Remove this study plan?") },
            text = { Text("Only planning settings are removed. Your goals, sessions and rewards are kept.") },
            confirmButton = { TextButton(onClick = {
                confirmation = null
                plannerViewModel.clear { editing = false }
            }, enabled = !busy) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } })
    }
}

@Composable
internal fun studyPlannerViewModel(): StudyPlannerViewModel {
    // Resolve Application from the current owner's CreationExtras, not a cached factory.
    val factory = remember { ViewModelProvider.AndroidViewModelFactory() }
    return viewModel(factory = factory)
}
