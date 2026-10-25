package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class DurationDraft(val source: StudyPlan, val goals: List<BossEntity>, val today: LocalDate,
    val id: Int, val minutes: Int?, val result: StudyDurationEdit)

@Composable
internal fun TimedStudyDurationDialog(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, blockId: Int,
    unavailable: Boolean, onDismiss: () -> Unit, onSave: (StudyPlan) -> Unit,
    modifier: Modifier = Modifier, subjects: List<PlannerSubject> = emptyList(),
) {
    val block = plan.blocks.firstOrNull { it.id == blockId } ?: return
    var durationText by rememberSaveable(plan, blockId) { mutableStateOf(block.minutes.toString()) }
    val minutes = durationText.takeIf { Regex("[0-9]{1,3}").matches(it) }?.toIntOrNull()?.takeIf { it in 1..120 }
    val preview by produceState<DurationDraft?>(null, plan, bosses, today, blockId, minutes) {
        value = null
        value = withContext(Dispatchers.Default) { DurationDraft(plan, bosses, today, blockId, minutes,
            minutes?.let { previewStudyDuration(plan, bosses, today, blockId, it) }
                ?: StudyDurationEdit(null, "Enter a whole duration from 1 to 120 minutes.")) }
    }
    val result = preview?.takeIf { it.source == plan && it.goals == bosses && it.today == today && it.id == blockId && it.minutes == minutes }?.result
    val proposed = result?.plan
    val updatedById = proposed?.blocks?.associateBy { it.id }
    val donors = if (updatedById == null) emptyList() else plan.blocks.filter { it.id != blockId && updatedById[it.id] != it }
    val week = plannerWeekStart(block.date)
    val name = subjects.firstOrNull { it.bossId == block.bossId }?.name ?: bosses.firstOrNull { it.id == block.bossId }?.name ?: "Removed course"
    AlertDialog(onDismissRequest = { if (!unavailable) onDismiss() }, modifier = modifier,
        title = { Text("Edit session duration") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("${block.date} · ${block.clockRange()} · ${block.minutes} min")
                Text("Date and start time stay fixed. Extra time uses unscheduled work, then later-day unlocked sessions for this course. Review every change before applying.")
                OutlinedTextField(durationText, { durationText = it.take(3) }, enabled = !unavailable,
                    label = { Text("Duration (minutes)") }, supportingText = { Text("1 to 120 whole minutes; preferred length stays ${plan.config.sessionMinutes} min") },
                    isError = minutes == null, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("duration_minutes"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { minutes?.let { durationText = (it - 15).toString() } },
                        enabled = !unavailable && minutes != null && minutes >= 16,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("duration_minus")) { Text("−15 min") }
                    OutlinedButton(onClick = { minutes?.let { durationText = (it + 15).toString() } },
                        enabled = !unavailable && minutes != null && minutes <= 105,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("duration_plus")) { Text("+15 min") }
                }
                if (unavailable) Text("Finish the active session or wait for saving before editing.", color = MaterialTheme.colorScheme.error)
                when {
                    result == null -> Text("Checking the new duration…")
                    proposed == null -> Text(result.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("duration_error"))
                    else -> {
                        Text("New slot: ${proposed.blocks.first { it.id == blockId }.clockRange()}", modifier = Modifier.testTag("duration_new_slot"))
                        val beforeWeek = plan.blocks.filter { plannerWeekStart(it.date) == week }.sumOf { it.minutes }
                        val afterWeek = proposed.blocks.filter { plannerWeekStart(it.date) == week }.sumOf { it.minutes }
                        Text("Week of $week: $beforeWeek → $afterWeek planned min", modifier = Modifier.testTag("duration_week_total"))
                        Text("Unscheduled course work: ${proposed.shortfalls[block.bossId] ?: 0} min", modifier = Modifier.testTag("duration_unscheduled"))
                        if (minutes != null && minutes < block.minutes) Text("${block.minutes - minutes} min return to unscheduled work. No study time is marked complete.")
                        Text("Recorded study time and rewards do not change.")
                        if (donors.isEmpty()) Text("No other sessions change.")
                        else {
                            Text("Review ${donors.size} later session changes", style = MaterialTheme.typography.titleSmall)
                            donors.forEach { donor ->
                                val updated = updatedById?.get(donor.id)
                                Text("${donor.date} · ${donor.clockRange()}\n" +
                                    if (updated == null) "Removed · ${donor.minutes} min transferred to this session."
                                    else "${donor.minutes} → ${updated.minutes} min · new slot ${updated.clockRange()}",
                                    modifier = Modifier.testTag("duration_donor_${donor.id}"))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (!unavailable) proposed?.let { onSave(it); onDismiss() } },
            enabled = !unavailable && proposed != null && proposed != plan,
            modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_duration_edit")) { Text("Apply change") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !unavailable, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } },
    )
}
