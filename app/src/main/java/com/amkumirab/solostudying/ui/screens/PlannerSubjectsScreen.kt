package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.PlannerSubject
import java.time.LocalDate

@Composable
internal fun PlannerSubjectsScreen(
    subjects: List<PlannerSubject>, bosses: List<BossEntity>, today: LocalDate, busy: Boolean,
    error: String?, unreadable: Boolean, onSave: (PlannerSubject, () -> Unit) -> Unit,
    onRemove: (Int, () -> Unit) -> Unit, onReset: (() -> Unit) -> Unit, modifier: Modifier = Modifier,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var removingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var resetting by rememberSaveable { mutableStateOf(false) }
    val editing = subjects.firstOrNull { it.bossId == editingId }
    if ((adding || editing != null) && !unreadable) {
        PlannerSubjectEditor(editing, subjects, bosses, today, busy, error, onSave,
            onCancel = { adding = false; editingId = null }, modifier = modifier)
    } else LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Your subjects", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text("Set course details here. Edit or rebuild your schedule to apply priority and deadline changes.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (unreadable) OutlinedButton(onClick = { resetting = true }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Reset saved subjects") }
            else Button(onClick = { adding = true; editingId = null }, enabled = !busy && subjects.size < 20,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Add subject") }
            if (subjects.isEmpty() && !unreadable) Text("No subjects yet. Add one by linking an unfinished goal.")
        }
        items(subjects, key = { it.bossId }) { subject ->
            val goal = bosses.firstOrNull { it.id == subject.bossId }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlannerSubjectTitle(subject.name, subject.color)
                    Text("Linked goal: ${goal?.name ?: "Removed goal"}", style = MaterialTheme.typography.bodySmall)
                    Text("Priority ${subject.priority} · ${subjectPriorityLabel(subject.priority)} · ${subject.color.name}")
                    Text(subject.preferredWeeklyMinutes?.let { "Preferred: ${subjectHours(it)} h per week" } ?: "No weekly preference")
                    Text(subject.deadline?.let { "Deadline: $it" } ?: "No deadline")
                    if (goal == null || goal.isCompleted) Text(if (goal == null) "The linked goal was removed." else "The linked goal is complete.", color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editingId = subject.bossId }, enabled = !busy && goal != null && !goal.isCompleted,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("edit_subject_${subject.bossId}")) { Text("Edit") }
                        TextButton(onClick = { removingId = subject.bossId }, enabled = !busy,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("remove_subject_${subject.bossId}")) { Text("Remove") }
                    }
                }
            }
        }
    }
    removingId?.let { id ->
        AlertDialog(onDismissRequest = { if (!busy) removingId = null }, title = { Text("Remove this subject?") },
            text = { Text("Remove its settings and planned sessions. The linked goal, recorded study and rewards are kept.") },
            confirmButton = { TextButton(onClick = { onRemove(id) { removingId = null } }, enabled = !busy,
                modifier = Modifier.testTag("confirm_remove_subject")) { Text("Remove subject") } },
            dismissButton = { TextButton(onClick = { removingId = null }, enabled = !busy) { Text("Cancel") } })
    }
    if (resetting) AlertDialog(onDismissRequest = { if (!busy) resetting = false }, title = { Text("Reset saved subjects?") },
        text = { Text("Remove only unreadable subject settings. Your accepted plan, goals and study history are kept.") },
        confirmButton = { TextButton(onClick = { onReset { resetting = false } }, enabled = !busy) { Text("Reset subjects") } },
        dismissButton = { TextButton(onClick = { resetting = false }, enabled = !busy) { Text("Cancel") } })
}
