package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun TimerAlarmScreen(
    isBreak: Boolean, isTest: Boolean, onDismiss: () -> Unit, onOpenApp: () -> Unit, modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            if (isTest) Text("Test alert", style = MaterialTheme.typography.titleMedium)
            Text(if (isBreak) "Break complete" else "Focus complete", style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() })
            Text(if (isBreak) "Recovery complete. Ready for your next session?" else "Your study timer has finished. Take a moment to recover.",
                style = MaterialTheme.typography.bodyLarge)
            Text("Vibration stops after 30 seconds or when you dismiss. Silent mode stays silent.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("timer_alarm_dismiss")) { Text("Dismiss") }
            OutlinedButton(onClick = onOpenApp, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("timer_alarm_open_app")) { Text("Open app") }
        }
    }
}
