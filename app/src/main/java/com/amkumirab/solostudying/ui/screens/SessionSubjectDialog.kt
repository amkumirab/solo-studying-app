package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SessionSubjectInput(
    val plan: StudyPlan, val bosses: List<BossEntity>, val today: LocalDate,
    val blockId: Int, val targetBossId: Int,
)

@Composable
internal fun SessionSubjectDialog(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, blockId: Int,
    unavailable: Boolean, onDismiss: () -> Unit, onSave: (StudyPlan) -> Unit,
    modifier: Modifier = Modifier, subjects: List<PlannerSubject> = emptyList(),
) {
    val block = plan.blocks.firstOrNull { it.id == blockId } ?: return
    var targetId by rememberSaveable(plan, blockId) { mutableIntStateOf(block.bossId) }
    var choosingSubject by remember { mutableStateOf(false) }
    val available = plan.config.courses.filter { course -> bosses.any { it.id == course.bossId && !it.isCompleted } }
    val target = available.firstOrNull { it.bossId == targetId }
    val input = SessionSubjectInput(plan, bosses, today, blockId, targetId)
    val preview by produceState<Pair<SessionSubjectInput, StudySessionEdit>?>(null, input) {
        value = null
        value = withContext(Dispatchers.Default) { input to previewReassignStudySession(plan, bosses, today, blockId, targetId) }
    }
    val result = preview?.takeIf { it.first == input }?.second
    val proposed = result?.plan?.takeIf { it != plan }
    val before = remember(plan) { plan.shortfalls }
    val after = remember(proposed) { proposed?.shortfalls }
    fun name(id: Int) = subjects.firstOrNull { it.bossId == id }?.name ?: bosses.firstOrNull { it.id == id }?.name ?: "Removed course"
    AlertDialog(onDismissRequest = { if (!unavailable) onDismiss() }, modifier = modifier,
        title = { Text("Change session subject") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Current course: ${name(block.bossId)}", style = MaterialTheme.typography.titleMedium)
            Text("${block.date} · ${block.clockRange()} · ${block.minutes} min", modifier = Modifier.testTag("subject_fixed_slot"))
            Text("Date, time and duration stay fixed. Only planned work changes; recorded study time, history and rewards stay unchanged.")
            Box {
                OutlinedButton(onClick = { choosingSubject = true }, enabled = !unavailable && available.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("session_subject_picker")) { Text("New course: ${name(targetId)}") }
                DropdownMenu(expanded = choosingSubject, onDismissRequest = { choosingSubject = false }) {
                    available.forEach { course ->
                        DropdownMenuItem(text = { Text(name(course.bossId)) }, onClick = { targetId = course.bossId; choosingSubject = false },
                            enabled = !unavailable, modifier = Modifier.heightIn(min = 48.dp).testTag("session_subject_option_${course.bossId}"))
                    }
                }
            }
            if (available.none { it.bossId != block.bossId }) Text("No other unfinished course in this plan. Edit answers to include another course first.")
            target?.let { course ->
                Text("Selected course deadline: ${course.finishBy}")
                val limit = plan.config.weeklyScheduling?.courseMinuteLimits?.get(targetId)
                Text(limit?.let { "Saved course limit: $it min per week" } ?: "No saved course weekly limit. Existing plan limits still apply.")
            }
            when {
                result == null -> Text("Checking the selected course…")
                result.error != null -> Text(result.error, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("session_subject_error").semantics { liveRegion = LiveRegionMode.Polite })
                proposed == null -> Text("Choose a different course to preview the change.")
                else -> {
                    Text("${name(block.bossId)} unscheduled work: ${before[block.bossId] ?: 0} → ${after?.get(block.bossId) ?: 0} min", modifier = Modifier.testTag("subject_source_unscheduled"))
                    Text("${name(targetId)} unscheduled work: ${before[targetId] ?: 0} → ${after?.get(targetId) ?: 0} min", modifier = Modifier.testTag("subject_destination_unscheduled"))
                    Text("No other sessions change. Lock this manual session if it should stay fixed when rebuilding.")
                }
            }
            if (unavailable) Text("Finish the active session or wait for saving before changing subjects.", color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(onClick = { if (!unavailable) proposed?.let { onSave(it); onDismiss() } }, enabled = !unavailable && proposed != null,
            modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_session_subject")) { Text("Change subject") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !unavailable, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } },
    )
}
