package com.amkumirab.solostudying.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate

@Composable
internal fun PlannerSubjectEditor(
    initial: PlannerSubject?, subjects: List<PlannerSubject>, bosses: List<BossEntity>, today: LocalDate,
    busy: Boolean, error: String?, onSave: (PlannerSubject, () -> Unit) -> Unit, onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var goalId by rememberSaveable { mutableIntStateOf(initial?.bossId ?: 0) }
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var weeklyHours by rememberSaveable { mutableStateOf(initial?.preferredWeeklyMinutes?.let(::subjectHours).orEmpty()) }
    var priority by rememberSaveable { mutableIntStateOf(initial?.priority ?: 3) }
    var colorName by rememberSaveable { mutableStateOf(initial?.color?.name ?: SubjectColor.Cyan.name) }
    var deadline by rememberSaveable { mutableStateOf(initial?.deadline?.toString()) }
    val color = SubjectColor.valueOf(colorName)
    val available = bosses.filter { !it.isCompleted && (it.id == initial?.bossId || subjects.none { subject -> subject.bossId == it.id }) }
    val originalHours = initial?.preferredWeeklyMinutes?.let(::subjectHours).orEmpty()
    val minutes = if (weeklyHours == originalHours) initial?.preferredWeeklyMinutes
        else plannerHoursToMinutes(weeklyHours)?.takeIf { it <= 10080 }
    val selectedGoal = available.firstOrNull { it.id == goalId }
    val candidate = runCatching {
        require(selectedGoal != null && (weeklyHours.isBlank() || minutes != null))
        PlannerSubject(goalId, name.trim(), priority, color, if (weeklyHours.isBlank()) null else minutes, deadline?.let(LocalDate::parse))
            .also { validatePlannerSubjects(subjects.filterNot { subject -> subject.bossId == goalId } + it) }
    }.getOrNull()
    val validation = when {
        selectedGoal == null -> "Choose an unfinished goal to link this subject."
        name.isBlank() || name.trim().length > 80 || name.any { it.isISOControl() } -> "Enter a subject name with 1 to 80 characters."
        weeklyHours.isNotBlank() && minutes == null -> "Use 0.01 to 168 weekly hours, or leave the field blank."
        candidate == null -> "A subject with this name already exists."
        else -> null
    }
    val context = LocalContext.current
    val picker = remember { mutableStateOf<DatePickerDialog?>(null) }
    DisposableEffect(Unit) { onDispose { picker.value?.dismiss() } }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (initial == null) "Add subject" else "Edit subject", style = MaterialTheme.typography.headlineSmall)
        if (initial == null) {
            Text("Link an existing goal. Its study history and total effort stay unchanged.")
            if (available.isEmpty()) Text("Create an unfinished goal in Study Hub first, or remove an existing subject link.")
            available.forEach { goal ->
                FilterChip(selected = goalId == goal.id, onClick = {
                    if (name.isBlank() || name == bosses.firstOrNull { it.id == goalId }?.name) name = goal.name.take(80)
                    goalId = goal.id
                    deadline = runCatching { goal.deadlineDate?.let(LocalDate::parse)?.toString() }.getOrNull()
                }, enabled = !busy, label = { Text(goal.name) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subject_goal_${goal.id}"))
            }
        } else Text("Linked goal: ${bosses.firstOrNull { it.id == goalId }?.name ?: "Removed goal"}")
        OutlinedTextField(name, { name = it.take(80) }, label = { Text("Subject name") }, singleLine = true, enabled = !busy,
            supportingText = { Text("Used in the planner; the linked goal is not renamed.") }, modifier = Modifier.fillMaxWidth().testTag("subject_name"))
        SubjectPrioritySelector(priority, { priority = it }, enabled = !busy)
        SubjectColorSelector(color, { colorName = it.name }, enabled = !busy)
        OutlinedTextField(weeklyHours, { weeklyHours = it.take(12) }, label = { Text("Preferred weekly hours (optional)") },
            supportingText = { Text("A ceiling for the weekly recommendation, separate from the goal's total remaining hours.") },
            enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = weeklyHours.isNotBlank() && minutes == null,
            modifier = Modifier.fillMaxWidth().testTag("subject_weekly_hours"))
        OutlinedButton(onClick = {
            picker.value?.dismiss()
            val selected = deadline?.let(LocalDate::parse)?.coerceIn(today, today.plusDays(365)) ?: today
            picker.value = pickPlannerDate(context, selected, today, today.plusDays(365)) { deadline = it.toString() }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(deadline?.let { "Deadline: $it" } ?: "No deadline") }
        if (deadline != null) TextButton(onClick = { deadline = null }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Clear deadline") }
        validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { candidate?.let { onSave(it, onCancel) } }, enabled = !busy && candidate != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save_subject")) { Text(if (busy) "Saving…" else "Save subject") }
        OutlinedButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel") }
    }
}
