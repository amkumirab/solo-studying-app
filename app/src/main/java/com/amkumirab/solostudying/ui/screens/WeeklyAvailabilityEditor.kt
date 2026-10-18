package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.planner.decodeWeeklyAvailability
import com.amkumirab.solostudying.planner.encodeWeeklyAvailability

@Composable
internal fun WeeklyAvailabilityEditor(
    initial: WeeklyAvailability, suggested: Boolean, busy: Boolean, error: String?,
    onSave: (WeeklyAvailability) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier,
) {
    var record by rememberSaveable { mutableStateOf(encodeWeeklyAvailability(initial)) }
    var editingDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val draft = remember(record) { decodeWeeklyAvailability(record) }
    fun updateDay(index: Int, day: StudyDayAvailability) {
        record = encodeWeeklyAvailability(draft.copy(days = draft.days.toMutableList().also { it[index] = day }))
    }
    val dayIndex = editingDay
    if (dayIndex != null) AvailabilityDayEditor(dayIndex, draft.days[dayIndex], busy,
        otherDaysMinutes = draft.totalMinutes - draft.days[dayIndex].capacityMinutes,
        onSave = { updateDay(dayIndex, it); editingDay = null }, onCancel = { editingDay = null }, modifier = modifier)
    else Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Edit weekly availability", style = MaterialTheme.typography.headlineSmall)
        if (suggested) Text("Suggested times only. Existing daily minutes are rounded down to half hours for this unsaved draft. Review the times before saving.")
        Text("Weekly capacity: ${subjectHours(draft.totalMinutes)} h", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("availability_total").semantics { liveRegion = LiveRegionMode.Polite })
        if (draft.totalMinutes == 0) Text("All days are off. You can save this week, but no study plan can be generated until a day is enabled.")
        draft.days.forEachIndexed { index, day ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(availabilityWeekdays[index], style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Switch(day.enabled, { checked ->
                            val windows = day.windows.ifEmpty { listOf(StudyTimeWindow(if (index == 5) 600 else 1020, if (index == 5) 660 else 1080)) }
                            updateDay(index, day.copy(enabled = checked, windows = windows))
                        }, enabled = !busy, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("availability_enabled_$index").semantics { contentDescription = "Study on ${availabilityWeekdays[index]}" })
                    }
                    Text(if (day.enabled) "${subjectHours(day.capacityMinutes)} h · ${day.windows.size} windows" else if (day.windows.isEmpty()) "Rest day · no windows" else "Rest day · windows retained")
                    if (day.windows.isNotEmpty()) Text(day.windows.joinToString(" · ") { "${availabilityClockLabel(it.startMinute)}–${availabilityClockLabel(it.endMinute)}" }, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { editingDay = index }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("edit_availability_day_$index")) { Text("Edit windows") }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Saving settings does not move accepted sessions. Edit or rebuild the schedule to use this capacity.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { onSave(draft) }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save_availability")) { Text(if (busy) "Saving…" else "Save availability") }
        OutlinedButton(onClick = onCancel, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cancel_availability")) { Text("Cancel") }
    }
}
