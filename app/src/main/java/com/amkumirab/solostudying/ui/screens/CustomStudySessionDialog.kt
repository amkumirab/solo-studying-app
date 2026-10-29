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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class CustomSessionInput(val plan: StudyPlan, val bosses: List<BossEntity>, val today: LocalDate,
    val course: Int, val date: LocalDate?, val start: Int?, val minutes: Int?)

@Composable
internal fun CustomStudySessionDialog(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, initialDate: LocalDate,
    unavailable: Boolean, onDismiss: () -> Unit, onSave: (StudyPlan) -> Unit,
    modifier: Modifier = Modifier, subjects: List<PlannerSubject> = emptyList(),
) {
    val windows = plan.config.studyWindows ?: return
    val shortfalls = remember(plan) { plan.shortfalls }
    val available = plan.config.courses.filter { course -> course.finishBy >= today && bosses.any { it.id == course.bossId && !it.isCompleted } }
    val initial = available.firstOrNull { (shortfalls[it.bossId] ?: 0) > 0 } ?: available.firstOrNull()
    var courseId by rememberSaveable(plan) { mutableIntStateOf(initial?.bossId ?: 0) }
    var dateText by rememberSaveable(plan) { mutableStateOf(initial?.let {
        maxOf(today, plan.config.start, initialDate).coerceAtMost(it.finishBy).toString()
    } ?: today.toString()) }
    var minutesText by rememberSaveable(plan) { mutableStateOf(minOf(plan.config.sessionMinutes,
        (initial?.let { shortfalls[it.bossId] } ?: plan.config.sessionMinutes)).toString()) }
    var clockText by rememberSaveable(plan) { mutableStateOf(LocalDate.parse(dateText).let { date ->
        val day = windows.days[date.dayOfWeek.value - 1]
        plannerClockLabel(firstStudySlot(day.windows, plan.blocks.filter { it.date == date }, minutesText.toInt(), plan.config.breakMinutes)
            ?: day.windows.firstOrNull()?.startMinute ?: 1020)
    }) }
    var choosingCourse by remember { mutableStateOf(false) }
    val course = available.firstOrNull { it.bossId == courseId }
    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
    val minutes = minutesText.takeIf { Regex("[0-9]{1,3}").matches(it) }?.toIntOrNull()?.takeIf { it in 1..120 }
    val input = CustomSessionInput(plan, bosses, today, courseId, date, parsePlannerClock(clockText), minutes)
    val preview by produceState<Pair<CustomSessionInput, StudySessionEdit>?>(null, input) {
        value = null
        value = withContext(Dispatchers.Default) {
            input to if (date == null || input.start == null || minutes == null) StudySessionEdit(null, "Choose a valid date, a start time in HH:mm and 1 to 120 whole minutes.")
            else previewAddStudySession(plan, bosses, today, courseId, date, input.start, minutes)
        }
    }
    val result = preview?.takeIf { it.first == input }?.second
    val proposed = result?.plan
    val context = LocalContext.current
    val picker = remember { mutableStateOf<DatePickerDialog?>(null) }
    DisposableEffect(Unit) { onDispose { picker.value?.dismiss() } }
    fun name(id: Int) = subjects.firstOrNull { it.bossId == id }?.name ?: bosses.firstOrNull { it.id == id }?.name ?: "Choose a course"
    AlertDialog(onDismissRequest = { if (!unavailable) onDismiss() }, modifier = modifier,
        title = { Text("Add study session") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Use unscheduled work from an existing course. Other sessions and recorded study time stay unchanged. Lock the new session if it should survive rebuilding.")
                if (plan.config.breakMinutes > 0) Text("Leave at least ${plan.config.breakMinutes} min between this slot and other sessions. No study time is used for the gap.", style = MaterialTheme.typography.bodySmall)
                Box {
                    OutlinedButton(onClick = { choosingCourse = true }, enabled = !unavailable && available.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("custom_course")) { Text("Course: ${name(courseId)}") }
                    DropdownMenu(expanded = choosingCourse, onDismissRequest = { choosingCourse = false }) {
                        available.forEach { item -> DropdownMenuItem(text = { Text(name(item.bossId)) }, onClick = {
                            courseId = item.bossId
                            dateText = (date ?: today).coerceIn(maxOf(today, plan.config.start), item.finishBy).toString()
                            choosingCourse = false
                        }, enabled = !unavailable, modifier = Modifier.heightIn(min = 48.dp).testTag("custom_course_${item.bossId}")) }
                    }
                }
                OutlinedButton(onClick = { course?.let { selected ->
                    picker.value?.dismiss()
                    val first = maxOf(today, plan.config.start)
                    picker.value = pickPlannerDate(context, (date ?: first).coerceIn(first, selected.finishBy), first, selected.finishBy) { dateText = it.toString() }
                } }, enabled = !unavailable && course != null, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("custom_date")) { Text("Date: $dateText") }
                OutlinedTextField(clockText, { clockText = it.take(5) }, enabled = !unavailable,
                    label = { Text("Start time") }, supportingText = { Text("Local 24-hour time, e.g. 17:30") }, singleLine = true,
                    isError = input.start == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth().testTag("custom_start_time"))
                OutlinedTextField(minutesText, { minutesText = it.take(3) }, enabled = !unavailable,
                    label = { Text("Minutes") }, supportingText = { Text("1 to 120 whole minutes") }, singleLine = true,
                    isError = minutes == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("custom_minutes"))
                date?.let { selected ->
                    val day = windows.days[selected.dayOfWeek.value - 1]
                    Text(if (!day.enabled) "Rest day: no saved study windows." else "Saved windows: " + day.windows.joinToString(", ") {
                        "${plannerClockLabel(it.startMinute)}–${plannerClockLabel(it.endMinute)}"
                    })
                }
                if (unavailable) Text("Finish the active session or wait for saving before adding.", color = MaterialTheme.colorScheme.error)
                when {
                    result == null -> Text("Checking this slot…")
                    proposed == null -> Text(result.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("custom_error"))
                    else -> {
                        Text("New session: ${proposed.blocks.last().date} · ${proposed.blocks.last().clockRange()} · $minutes min", modifier = Modifier.testTag("custom_new_slot"))
                        Text("Unscheduled course work: ${shortfalls[courseId] ?: 0} → ${proposed.shortfalls[courseId] ?: 0} min", modifier = Modifier.testTag("custom_unscheduled"))
                        Text("No other sessions change. No study time or rewards are added.")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (!unavailable) proposed?.let { onSave(it); onDismiss() } }, enabled = !unavailable && proposed != null,
            modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_custom_session")) { Text("Add session") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !unavailable, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } },
    )
}
