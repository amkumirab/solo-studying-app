package com.amkumirab.solostudying.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amkumirab.solostudying.notification.*

@Composable
internal fun TimerAlarmSettingsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember(context) { TimerAlarmSettingsStore(context) }
    var enabled by remember { mutableStateOf(store.isEnabled()) }
    var hasFullScreen by remember { mutableStateOf(TimerAlarmAlerts.canShowFullScreen(context)) }
    var hasNotifications by remember { mutableStateOf(canPostNotifications(context)) }
    var preciseTimers by remember { mutableStateOf(hasPreciseTimerAccess(context)) }
    var error by remember { mutableStateOf<String?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = store.isEnabled()
                hasFullScreen = TimerAlarmAlerts.canShowFullScreen(context)
                hasNotifications = canPostNotifications(context)
                preciseTimers = hasPreciseTimerAccess(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    fun openSettings(intent: Intent) {
        if (runCatching { context.startActivity(intent) }.isFailure) error = "These settings are unavailable on this device. Open the app's notification settings manually."
    }
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Timer completion alarm", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() })
                Switch(enabled, onCheckedChange = { value ->
                    if (store.setEnabled(value)) {
                        enabled = value; error = null
                        if (!value) TimerAlarmAlerts.dismissCurrent(context)
                    } else error = "Could not save timer alert settings. Try again."
                }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("timer_alarm_enabled")
                    .semantics { contentDescription = "Alarm-style timer completion" })
            }
            Text("A clear completion screen and up to 30 seconds of vibration. Silent mode stays silent; Do Not Disturb is respected.", style = MaterialTheme.typography.bodyMedium)
            if (enabled) {
                Text(if (hasNotifications) "Notifications: allowed" else "Notifications are off. Background alerts cannot appear until you enable them.",
                    color = if (hasNotifications) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
                Text(if (hasFullScreen) "Full-screen access: allowed. Android chooses a lock-screen alert or banner."
                    else "Full-screen access is off. A notification is used instead; enable access to request screen wake-up.")
                if (Build.VERSION.SDK_INT >= 34 && !hasFullScreen) {
                    OutlinedButton(onClick = { openSettings(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        "package:${context.packageName}".toUri())) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("timer_alarm_fullscreen_access")) {
                        Text("Allow lock-screen alerts")
                    }
                }
                OutlinedButton(onClick = { openSettings(if (Build.VERSION.SDK_INT >= 26)
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Notification settings") }
                Text(if (preciseTimers) "Precise timer access: allowed" else "Precise timer access is off. Background completion may be delayed.")
                if (Build.VERSION.SDK_INT >= 31 && !preciseTimers) {
                    OutlinedButton(onClick = { openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        "package:${context.packageName}".toUri())) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Allow precise timers")
                    }
                }
                Text("Battery restrictions can delay background timers. Screen wake-up depends on Android and your phone's settings.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    if (TimerAlarmAlerts.begin(context, CompletedTimer.FOCUS, isTest = true)) {
                        TimerAlarmAlerts.events.value?.let { context.startActivity(TimerAlarmAlerts.screenIntent(context, it)) }
                    } else error = "Could not preview the alert. Turn off Do Not Disturb and try again."
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("timer_alarm_test")) { Text("Test alert now") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
