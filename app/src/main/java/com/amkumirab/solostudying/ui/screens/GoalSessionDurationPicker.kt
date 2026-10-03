package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.domain.session.suggestedGoalSessionMinutes

@Composable
fun GoalSessionDurationPicker(
    selectedMinutes: Int?,
    dailyMinutes: Int?,
    onMinutesChange: (Int?) -> Unit,
) {
    var customMode by rememberSaveable { mutableStateOf(false) }
    var customText by rememberSaveable { mutableStateOf(selectedMinutes?.toString() ?: "25") }
    val suggested = suggestedGoalSessionMinutes(dailyMinutes)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Session duration", style = MaterialTheme.typography.titleMedium)
        Text("Choose one focus block, not the full course duration.", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(25, 45, 60).forEach { minutes ->
                FilterChip(
                    selected = !customMode && selectedMinutes == minutes,
                    onClick = { customMode = false; onMinutesChange(minutes) },
                    label = { Text("$minutes min") },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                )
            }
        }
        FilterChip(
            selected = customMode,
            onClick = {
                customMode = true
                onMinutesChange(customText.toIntOrNull()?.takeIf { it in 1..480 })
            },
            label = { Text("Custom") },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        if (customMode) {
            OutlinedTextField(
                value = customText,
                onValueChange = {
                    customText = it
                    onMinutesChange(it.toIntOrNull()?.takeIf { value -> value in 1..480 })
                },
                label = { Text("Session minutes") },
                supportingText = { Text("Enter 1 to 480 minutes") },
                isError = selectedMinutes == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("goal_session_custom"),
            )
        }
        if (dailyMinutes != null && dailyMinutes > 0) {
            Text("Plan target: $dailyMinutes min per study day", style = MaterialTheme.typography.bodyMedium)
            AssistChip(
                onClick = { customMode = false; onMinutesChange(suggested) },
                label = { Text("Suggested: $suggested min") },
                modifier = Modifier.heightIn(min = 48.dp),
            )
            if (dailyMinutes > 60) {
                Text("Split today's target into several sessions with breaks.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
