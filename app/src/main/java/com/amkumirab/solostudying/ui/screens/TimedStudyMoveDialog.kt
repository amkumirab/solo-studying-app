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
internal fun TimedStudyMoveDialog(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, blockId: Int,
    unavailable: Boolean, onDismiss: () -> Unit, onMove: (StudyPlan) -> Unit,
    modifier: Modifier = Modifier, subjects: List<PlannerSubject> = emptyList(),
) {
    val block = plan.blocks.firstOrNull { it.id == blockId } ?: return
    val windows = plan.config.studyWindows ?: return
    val course = plan.config.courses.first { it.bossId == block.bossId }
    val entry = remember(plan, bosses, blockId) { blockProgress(plan, bosses).firstOrNull { it.block.id == blockId } }
    var dateText by rememberSaveable(plan, blockId) { mutableStateOf(maxOf(today, block.date).toString()) }
    var clockText by rememberSaveable(plan, blockId) { mutableStateOf(plannerClockLabel(block.startMinute!!)) }
    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
    val start = parsePlannerClock(clockText)
    val editable = !unavailable && !block.locked && entry != null && !entry.missingCourse && entry.creditedSeconds == 0L
    val proposal = remember(plan, blockId, today, date, start, editable) {
        if (!editable || date == null || start == null) null else moveTimedStudyBlock(plan, blockId, date, start, today, 0)
    }
    val context = LocalContext.current
    val picker = remember { mutableStateOf<DatePickerDialog?>(null) }
    DisposableEffect(Unit) { onDispose { picker.value?.dismiss() } }
    AlertDialog(onDismissRequest = onDismiss, modifier = modifier,
        title = { Text("Move study session") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val name = subjects.firstOrNull { it.bossId == block.bossId }?.name ?: bosses.firstOrNull { it.id == block.bossId }?.name ?: "Removed course"
                Text("$name · ${block.minutes} min")
                Text("Choose a local start time inside this plan's saved windows. Duration and recorded study credit stay unchanged.")
                OutlinedButton(onClick = {
                    picker.value?.dismiss()
                    picker.value = pickPlannerDate(context, (date ?: today).coerceIn(today, course.finishBy), today, course.finishBy) { dateText = it.toString() }
                }, enabled = editable && course.finishBy >= today, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("move_study_date")) {
                    Text("Date: $dateText")
                }
                OutlinedTextField(clockText, { clockText = it.take(5) }, enabled = editable,
                    label = { Text("Start time (HH:mm)") }, singleLine = true,
                    supportingText = { Text("24-hour local time, for example 17:30") }, isError = start == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth().testTag("move_start_time"))
                date?.let { selected ->
                    val day = windows.days[selected.dayOfWeek.value - 1]
                    Text(if (!day.enabled) "Rest day: no saved study windows." else "Saved windows: " + day.windows.joinToString(", ") {
                        "${plannerClockLabel(it.startMinute)}–${plannerClockLabel(it.endMinute)}"
                    })
                }
                if (!editable) Text(if (block.locked) "Unlock this session in the weekly plan before moving it."
                    else "Finish the active session or choose an unstudied session before moving it.", color = MaterialTheme.colorScheme.error)
                else if (proposal == null) Text(if (start == null) "Enter a valid time from 00:00 to 23:59."
                    else "This slot is outside saved windows or course dates, overlaps another session, or exceeds a daily or weekly limit.", color = MaterialTheme.colorScheme.error)
                else Text("New slot: ${proposal.blocks.first { it.id == blockId }.clockRange()}")
            }
        },
        confirmButton = { TextButton(onClick = { proposal?.let { onMove(it); onDismiss() } }, enabled = proposal != null,
            modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_timed_move")) { Text("Move session") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } },
    )
}
