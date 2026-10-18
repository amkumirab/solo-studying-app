package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import com.amkumirab.solostudying.domain.planner.*
import kotlin.math.roundToInt

internal val availabilityWeekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
internal fun availabilityClockLabel(minutes: Int): String = "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"
internal fun availabilityClockMinutes(text: String, allowEndOfDay: Boolean = false): Int? {
    if (!Regex("[0-9]{2}:[0-9]{2}").matches(text)) return null
    if (allowEndOfDay && text == "24:00") return 1440
    val hours = text.take(2).toInt()
    val minutes = text.takeLast(2).toInt()
    return (hours * 60 + minutes).takeIf { hours in 0..23 && minutes in listOf(0, 30) }
}

internal fun nextAvailabilityWindow(windows: List<StudyTimeWindow>): StudyTimeWindow? {
    val remaining = 480 - windows.sumOf { it.minutes }
    if (remaining < 30 || windows.size >= 16) return null
    val preferred = maxOf(540, windows.maxOfOrNull { it.endMinute } ?: 540)
    val starts = ((preferred..1410 step 30) + (540..1410 step 30) + (0..510 step 30)).distinct()
    return starts.firstNotNullOfOrNull { start ->
        if (windows.any { start < it.endMinute && start + 30 > it.startMinute }) null else {
            val gapEnd = windows.filter { it.startMinute >= start }.minOfOrNull { it.startMinute } ?: 1440
            minOf(60, remaining, gapEnd - start).takeIf { it >= 30 }?.let { StudyTimeWindow(start, start + it) }
        }
    }
}

@Composable
internal fun AvailabilityWeekSummary(availability: WeeklyAvailability, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Weekly capacity: ${subjectHours(availability.totalMinutes)} h", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("availability_total").semantics { liveRegion = LiveRegionMode.Polite })
        availability.days.forEachIndexed { index, day ->
            Text("${availabilityWeekdays[index]} · ${day.capacityMinutes} min", style = MaterialTheme.typography.titleSmall)
            Text(if (!day.enabled) "Rest day" else day.windows.joinToString(" · ") { "${availabilityClockLabel(it.startMinute)}–${availabilityClockLabel(it.endMinute)}" },
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("availability_summary_$index"))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AvailabilityWindowFields(
    dayName: String, index: Int, start: String, end: String, busy: Boolean,
    onStart: (String) -> Unit, onEnd: (String) -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier,
) {
    val first = availabilityClockMinutes(start)
    val last = availabilityClockMinutes(end, true)
    val duration = if (first != null && last != null && last > first) last - first else null
    val maximum = minOf(480, 1440 - (first ?: 0)).coerceAtLeast(30)
    val sliderEnabled = !busy && first != null && maximum > 30
    val interactions = remember { MutableInteractionSource() }
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Window ${index + 1}", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(start, { onStart(it.take(5)) }, label = { Text("Start time (HH:mm)") }, singleLine = true,
                enabled = !busy, isError = first == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth().testTag("window_start_$index"))
            OutlinedTextField(end, { onEnd(it.take(5)) }, label = { Text("End time (HH:mm)") }, singleLine = true,
                enabled = !busy, isError = last == null || first != null && last <= first,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth().testTag("window_end_$index"))
            Text("Duration: ${duration?.let { "${subjectHours(it)} h" } ?: "Check times"}")
            // Endpoints are selectable too: steps = number of half-hour intervals minus one.
            // https://developer.android.com/develop/ui/compose/components/slider
            Slider(value = (duration ?: 30).coerceIn(30, maximum).toFloat(),
                interactionSource = interactions,
                thumb = { SliderDefaults.Thumb(interactions, enabled = sliderEnabled, thumbSize = DpSize(4.dp, 48.dp)) },
                onValueChange = { value -> first?.let { onEnd(availabilityClockLabel(it + (value / 30).roundToInt() * 30)) } },
                valueRange = 30f..maxOf(60, maximum).toFloat(), steps = (maxOf(60, maximum) / 30 - 2).coerceAtLeast(0),
                enabled = sliderEnabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("window_duration_$index")
                    .semantics { contentDescription = "$dayName window ${index + 1} duration" })
            TextButton(onClick = onRemove, enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp).testTag("remove_availability_window_$index")) { Text("Remove window") }
        }
    }
}
