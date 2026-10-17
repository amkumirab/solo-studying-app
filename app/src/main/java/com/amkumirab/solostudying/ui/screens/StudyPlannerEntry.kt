package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var subjectsOpen by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    var actionError by rememberSaveable { mutableStateOf<String?>(null) }
    var rebuilding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val busy = state.busy || rebuilding
    // A failed compare-and-swap invalidates remembered drafts and pending move dialogs.
    key(state.editorGeneration) {
        content(state.plan) {
            if (!showDailySessions) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weekly study plan", style = MaterialTheme.typography.titleMedium)
                    Text(state.plan?.let { "${it.config.courses.size} courses · guided weekly scheduling" }
                        ?: "Choose courses, deadlines and available time. Your plan stays on this device.",
                        style = MaterialTheme.typography.bodyMedium)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = { open = true }, enabled = state.loaded && !isSessionActive && !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_study_planner"),
                    ) { Text(if (!state.loaded) "Loading…" else if (state.plan == null) "Create weekly plan" else "Open weekly plan") }
                }
            } else {
                StudyPlannerToday(state.plan, bosses, today, state.loaded, isSessionActive || busy, state.error,
                    onOpen = { open = true }, onStart = onStart,
                    onSave = { updated, saved -> plannerViewModel.save(updated, state.snapshot, saved) }, subjects = state.subjects)
            }
        }
        if (open) Dialog(onDismissRequest = { if (!busy) open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                    TextButton(onClick = { open = false }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close planner") }
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !subjectsOpen, onClick = { subjectsOpen = false }, enabled = !busy,
                            label = { Text("Schedule") }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                        FilterChip(selected = subjectsOpen, onClick = { subjectsOpen = true }, enabled = !busy,
                            label = { Text("Subjects") }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                    }
                    state.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
                        if (state.unreadableRecord) OutlinedButton(onClick = { confirmation = "clear" }, enabled = !busy, modifier = Modifier.padding(16.dp)) { Text("Clear saved plan") }
                    }
                    val plan = state.plan
                    if (subjectsOpen) {
                        PlannerSubjectsScreen(state.subjects, bosses, today, busy, state.subjectError, state.unreadableSubjects,
                            onSave = { subject, saved -> plannerViewModel.saveSubject(subject, bosses, saved) },
                            onRemove = { id, removed -> plannerViewModel.removeSubject(id, removed) },
                            onReset = { reset -> plannerViewModel.clearSubjects(reset) }, modifier = Modifier.weight(1f))
                    } else if (editing || (plan == null && !state.unreadableRecord)) {
                        val draftSnapshot = remember { state.snapshot }
                        StudyPlannerWizard(bosses, today, plan, weekdayDefaults, busy,
                            onAccept = { accepted -> plannerViewModel.save(accepted, draftSnapshot) { editing = false; actionError = null } },
                            onCancel = { if (plan == null) open = false else editing = false },
                            modifier = Modifier.weight(1f), subjects = state.subjects)
                    } else if (plan != null) {
                        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = true }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Edit answers") }
                            OutlinedButton(onClick = { confirmation = "rebuild" }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Rebuild remaining") }
                        }
                        TextButton(onClick = { confirmation = "clear" }, enabled = !busy, modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp)) { Text("Remove plan") }
                        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
                        StudyPlannerWeek(plan, bosses, today, isSessionActive || busy,
                            onStart = { boss, minutes ->
                                if (!isSessionActive && !busy) { onStart(boss, minutes); open = false }
                            }, onMove = { plannerViewModel.save(it, state.snapshot) }, modifier = Modifier.weight(1f), subjects = state.subjects)
                    }
                }
            }
        }
    }
    confirmation?.let { action ->
        AlertDialog(onDismissRequest = { confirmation = null },
            title = { Text(if (action == "clear") "Remove this study plan?" else "Rebuild the remaining plan?") },
            text = { Text(if (action == "clear") "Only planning settings are removed. Your goals, sessions and rewards are kept."
                else "Replace planned blocks using recorded progress and your available days, starting today. Missed work is included. If time is insufficient, edit your answers to review a new plan.") },
            confirmButton = { TextButton(onClick = {
                confirmation = null
                if (action == "clear") plannerViewModel.clear { editing = false; actionError = null }
                else state.plan?.let { plan ->
                    val rebuildSnapshot = state.snapshot
                    val remaining = remainingPlannerCourses(plan, bosses, state.subjects)
                    when {
                        remaining.isEmpty() -> actionError = "All available courses are complete. Edit answers to choose new goals."
                        remaining.any { it.finishBy !in today..today.plusDays(365) } -> actionError = "A finish date is outside the next year. Edit answers to choose new dates before rebuilding."
                        else -> {
                            rebuilding = true
                            scope.launch {
                                val rebuilt = withContext(Dispatchers.Default) { runCatching { buildStudyPlan(plan.config.copy(start = today, courses = remaining)) }.getOrNull() }
                                rebuilding = false
                                if (rebuilt == null) {
                                    actionError = "Could not rebuild this plan. Edit answers to review your course dates and capacity."
                                } else if (rebuilt.shortfalls.isNotEmpty()) {
                                    actionError = "Remaining work no longer fits your schedule. Edit answers to review capacity warnings. Your current plan is unchanged."
                                } else plannerViewModel.save(rebuilt, rebuildSnapshot) { actionError = null }
                            }
                        }
                    }
                }
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
