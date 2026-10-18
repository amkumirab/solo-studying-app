package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.domain.planner.*

@Composable
internal fun AvailabilityDayEditor(
    dayIndex: Int, initial: StudyDayAvailability, busy: Boolean, otherDaysMinutes: Int = 0,
    onSave: (StudyDayAvailability) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier,
) {
    var starts by rememberSaveable { mutableStateOf(initial.windows.map { availabilityClockLabel(it.startMinute) }) }
    var ends by rememberSaveable { mutableStateOf(initial.windows.map { availabilityClockLabel(it.endMinute) }) }
    var enabled by rememberSaveable { mutableStateOf(initial.enabled) }
    val windows = starts.indices.map { index -> runCatching {
        StudyTimeWindow(availabilityClockMinutes(starts[index])!!, availabilityClockMinutes(ends[index], true)!!)
    }.getOrNull() }
    val parsed = windows.filterNotNull().sortedBy { it.startMinute }
    val overlap = parsed.zipWithNext().any { (first, next) -> first.endMinute > next.startMinute }
    val error = when {
        windows.any { it == null } -> "Use HH:mm with :00 or :30; end must be later than start."
        overlap -> "Study windows must not overlap."
        parsed.sumOf { it.minutes } > 480 -> "Use no more than 8 study hours per day."
        else -> null
    }
    val candidate = if (error == null) StudyDayAvailability(enabled && parsed.isNotEmpty(), parsed) else null
    val next = candidate?.let { nextAvailabilityWindow(it.windows) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${availabilityWeekdays[dayIndex]} windows", style = MaterialTheme.typography.headlineSmall)
        Text("Local 24-hour times, in 30-minute steps. Use 24:00 only as an end time. Split overnight availability across two days.")
        Text(candidate?.let { "Daily capacity: ${subjectHours(it.capacityMinutes)} h" } ?: "Daily capacity: check windows",
            modifier = Modifier.testTag("availability_day_total"))
        Text(candidate?.let { "Weekly capacity: ${subjectHours(otherDaysMinutes + it.capacityMinutes)} h" } ?: "Weekly capacity: check windows",
            modifier = Modifier.testTag("availability_total"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Study on this day", modifier = Modifier.weight(1f))
            Switch(enabled, { enabled = it }, enabled = !busy && starts.isNotEmpty(),
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = "Study on ${availabilityWeekdays[dayIndex]}" })
        }
        starts.indices.forEach { index ->
            AvailabilityWindowFields(availabilityWeekdays[dayIndex], index, starts[index], ends[index], busy,
                onStart = { text -> starts = starts.toMutableList().also { it[index] = text } },
                onEnd = { text -> ends = ends.toMutableList().also { it[index] = text } },
                onRemove = {
                    starts = starts.filterIndexed { position, _ -> position != index }
                    ends = ends.filterIndexed { position, _ -> position != index }
                    if (starts.isEmpty()) enabled = false
                })
        }
        if (starts.isEmpty()) Text("No windows. Add one to make this a study day.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = { next?.let {
            if (starts.isEmpty()) enabled = true
            starts = starts + availabilityClockLabel(it.startMinute)
            ends = ends + availabilityClockLabel(it.endMinute)
        } }, enabled = !busy && next != null, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_availability_window")) { Text("Add window") }
        Button(onClick = { candidate?.let(onSave) }, enabled = !busy && candidate != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save_availability_day")) { Text("Done") }
        OutlinedButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel day changes") }
    }
}
