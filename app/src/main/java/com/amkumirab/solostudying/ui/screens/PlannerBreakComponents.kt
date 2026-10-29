package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.domain.planner.PlannedStudyBreak
import com.amkumirab.solostudying.domain.planner.plannerClockLabel
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme

internal fun plannerBreakMinutes(text: String): Int? = text.takeIf { Regex("[0-9]{1,2}").matches(it) }
    ?.toIntOrNull()?.takeIf { it in 0..30 }

@Composable
internal fun PlannerBreakEditor(value: String, onChange: (String) -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Breaks between sessions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        OutlinedTextField(value, { onChange(it.take(2)) }, enabled = enabled,
            label = { Text("Break length (minutes)") },
            supportingText = { Text("0 to 30 whole minutes. Use 0 for no planned breaks.") },
            isError = plannerBreakMinutes(value) == null, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("planner_break_minutes"))
        Text("This minimum gap fits inside your available clock time. Gaps between windows count too. Breaks are not study hours and do not start a timer automatically.",
            style = MaterialTheme.typography.bodySmall)
        Text("Changing this answer only affects the preview until you save. Locked slots must stay compatible.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun PlannerBreakInterval(rest: PlannedStudyBreak, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Planned break · ${plannerClockLabel(rest.startMinute)}–${plannerClockLabel(rest.startMinute + rest.minutes)} · ${rest.minutes} min",
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Not study time · no timer starts automatically", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun PlannerBreakEditorPreview() {
    SoloStudyingTheme { PlannerBreakEditor("10", {}, true, Modifier.padding(16.dp)) }
}
