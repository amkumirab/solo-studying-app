package com.amkumirab.solostudying.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.notification.NotificationReceiver
import com.amkumirab.solostudying.notification.ReminderSchedule
import com.amkumirab.solostudying.notification.ReminderSettings
import com.amkumirab.solostudying.sound.RpgSoundManager
import com.amkumirab.solostudying.sound.SoundSettings
import com.amkumirab.solostudying.ui.theme.*

@Composable
internal fun ReminderSettingsCard(
    settings: ReminderSettings,
    onSettingsChange: (ReminderSettings) -> Unit,
    onPreview: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
        border = BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Choose when each companion message appears. Changes are saved and scheduled immediately.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
            )
            ReminderScheduleRow(
                label = "MORNING QUEST",
                schedule = settings.morning,
                accent = NeonBlueAccent,
                testTagPrefix = "morning_reminder",
                onScheduleChange = { onSettingsChange(settings.copy(morning = it)) },
                onPreview = {
                    RpgSoundManager.playClickSound()
                    onPreview(NotificationReceiver.ACTION_MORNING_QUEST)
                },
            )
            HorizontalDivider(color = DarkCardBorder)
            ReminderScheduleRow(
                label = "STUDY RITUAL",
                schedule = settings.beforeStudy,
                accent = RpgGold,
                testTagPrefix = "study_reminder",
                onScheduleChange = { onSettingsChange(settings.copy(beforeStudy = it)) },
                onPreview = {
                    RpgSoundManager.playClickSound()
                    onPreview(NotificationReceiver.ACTION_BEFORE_STUDY)
                },
            )
            HorizontalDivider(color = DarkCardBorder)
            ReminderScheduleRow(
                label = "EVENING REVIEW",
                schedule = settings.evening,
                accent = RpgRuby,
                testTagPrefix = "evening_reminder",
                onScheduleChange = { onSettingsChange(settings.copy(evening = it)) },
                onPreview = {
                    RpgSoundManager.playClickSound()
                    onPreview(NotificationReceiver.ACTION_EVENING_CAMPAIGN)
                },
            )
        }
    }
}

@Composable
private fun ReminderScheduleRow(
    label: String,
    schedule: ReminderSchedule,
    accent: Color,
    testTagPrefix: String,
    onScheduleChange: (ReminderSchedule) -> Unit,
    onPreview: () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                    ),
                )
                Text(
                    text = if (schedule.enabled) {
                        "Active every day at ${schedule.formattedTime()}"
                    } else {
                        "Disabled"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (schedule.enabled) accent else TextMuted,
                    ),
                )
            }
            Switch(
                checked = schedule.enabled,
                onCheckedChange = { enabled ->
                    onScheduleChange(schedule.copy(enabled = enabled))
                },
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("${testTagPrefix}_switch")
                    .semantics {
                        contentDescription = "$label reminder"
                        stateDescription = if (schedule.enabled) "On" else "Off"
                    },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onScheduleChange(schedule.copy(hour = hour, minute = minute))
                        },
                        schedule.hour,
                        schedule.minute,
                        true,
                    ).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_time_button")
                    .semantics {
                        contentDescription = "Set $label time. Current time ${schedule.formattedTime()}"
                    },
                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(schedule.formattedTime(), color = accent, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier
                    .testTag("${testTagPrefix}_preview_button")
                    .semantics { contentDescription = "Preview $label reminder" },
                border = BorderStroke(1.dp, DarkCardBorder),
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TextMuted)
                Spacer(modifier = Modifier.width(6.dp))
                Text("TEST", color = TextMuted, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun SystemControlsCard(
    soundSettings: SoundSettings,
    onSoundEnabledChange: (Boolean) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onPreviewSound: () -> Unit,
    onReplayTutorial: () -> Unit,
    breakSuggestionsEnabled: Boolean = true,
    onBreakSuggestionsEnabledChange: (Boolean) -> Unit = {},
    focusShieldEnabled: Boolean = false,
    focusShieldHasAccess: Boolean = false,
    onFocusShieldEnabledChange: (Boolean) -> Unit = {},
    onRequestFocusShieldAccess: () -> Unit = {},
) {
    val volumePercent = (soundSettings.volume.coerceIn(0f, 1f) * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true },
    ) {
        Text(
            text = "⚙️ SYSTEM CONTROLS",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = NeonBlueAccent,
                letterSpacing = 1.sp,
            ),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .semantics { heading() },
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, DarkCardBorder),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .semantics { isTraversalGroup = true },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (soundSettings.enabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = null,
                        tint = if (soundSettings.enabled) NeonBlueAccent else TextMuted,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "INTERFACE SOUND",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Black,
                            ),
                        )
                        Text(
                            text = "Battle, reward, warning, and navigation feedback",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                        )
                    }
                    Switch(
                        checked = soundSettings.enabled,
                        onCheckedChange = onSoundEnabledChange,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("sound_enabled_switch")
                            .semantics {
                                contentDescription = "Interface sound"
                                stateDescription = if (soundSettings.enabled) "On" else "Off"
                                traversalIndex = 0f
                            },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "MASTER VOLUME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = "$volumePercent%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonBlueAccent,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sound_volume_slider")
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Master volume"
                            stateDescription = "$volumePercent percent"
                            traversalIndex = 1f
                            if (!soundSettings.enabled) disabled()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Slider(
                        value = soundSettings.volume,
                        onValueChange = onSoundVolumeChange,
                        enabled = soundSettings.enabled,
                        valueRange = 0f..1f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedButton(
                    onClick = onPreviewSound,
                    enabled = soundSettings.enabled && soundSettings.volume > 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("sound_preview_button")
                        .semantics {
                            contentDescription = "Preview interface sound"
                            traversalIndex = 2f
                        },
                    border = BorderStroke(1.dp, NeonBlueAccent.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PREVIEW SYSTEM SOUND")
                }

                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Coffee,
                        contentDescription = null,
                        tint = if (breakSuggestionsEnabled) RpgEmerald else TextMuted,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "POST-SESSION BREAKS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Black,
                            ),
                        )
                        Text(
                            text = "Offer a recovery timer after completed sessions",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                        )
                    }
                    Switch(
                        checked = breakSuggestionsEnabled,
                        onCheckedChange = onBreakSuggestionsEnabledChange,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("break_suggestions_switch")
                            .semantics {
                                contentDescription = "Post-session break suggestions"
                                stateDescription = if (breakSuggestionsEnabled) "On" else "Off"
                                traversalIndex = 3f
                            },
                    )
                }

                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.DoNotDisturbOn,
                        contentDescription = null,
                        tint = if (focusShieldEnabled && focusShieldHasAccess) RpgGold else TextMuted,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FOCUS SHIELD",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Black,
                            ),
                        )
                        Text(
                            text = if (focusShieldEnabled && !focusShieldHasAccess) {
                                "Do Not Disturb access is required"
                            } else {
                                "Silence interruptions during active focus"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (focusShieldEnabled && !focusShieldHasAccess) RpgGold else TextMuted,
                            ),
                        )
                    }
                    Switch(
                        checked = focusShieldEnabled,
                        onCheckedChange = onFocusShieldEnabledChange,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("focus_shield_switch")
                            .semantics {
                                contentDescription = "Focus Shield"
                                stateDescription = when {
                                    !focusShieldEnabled -> "Off"
                                    !focusShieldHasAccess -> "On, permission required"
                                    else -> "On"
                                }
                                traversalIndex = 4f
                            },
                    )
                }

                if (focusShieldEnabled && !focusShieldHasAccess) {
                    OutlinedButton(
                        onClick = onRequestFocusShieldAccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("focus_shield_permission_button")
                            .semantics { contentDescription = "Grant Do Not Disturb access" },
                        border = BorderStroke(1.dp, RpgGold.copy(alpha = 0.7f)),
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = RpgGold)
                        Spacer(Modifier.width(8.dp))
                        Text("GRANT DO NOT DISTURB ACCESS", color = RpgGold, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f))

                Text(
                    text = "Want to re-experience the immersive Hunter Awakening Ritual? You can replay the introductory covenant setup at any time.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        RpgSoundManager.playClickSound()
                        onReplayTutorial()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1B22)),
                    border = BorderStroke(1.dp, RpgGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("replay_tutorial_button")
                        .semantics {
                            contentDescription = "Replay onboarding tutorial"
                            traversalIndex = 5f
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = RpgGold,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "REPLAY AWAKENING CEREMONY",
                        color = RpgGold,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    )
                }
            }
        }
    }
}
