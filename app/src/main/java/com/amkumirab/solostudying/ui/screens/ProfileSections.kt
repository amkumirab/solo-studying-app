package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.ui.theme.*

import com.amkumirab.solostudying.notification.ReminderSettings
import com.amkumirab.solostudying.sound.SoundSettings

internal fun LazyListScope.profileSkills(
    skills: List<SkillEntity>, onAdd: () -> Unit, onDeleteSkill: (SkillEntity) -> Unit,
) {
    // System 6: SKILL ACADEMY INSTRUCTION SECTION
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your skills",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonBlueAccent,
                    letterSpacing = 1.sp
                )
            )

            FilledTonalButton(
                onClick = { onAdd() },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add skill")
            }
        }
    }

    if (skills.isEmpty()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No skills yet",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Add a skill and train it during your study sessions.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, textAlign = TextAlign.Center),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    } else {
        items(skills) { sk ->
            val totalSeconds = sk.targetMinutes * 60L
            val skillProgress = if (totalSeconds > 0) {
                (sk.spentSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
            } else 0f
            val activeLvl = (sk.spentSeconds / 3600) + 1
            val isUnlocked = sk.spentSeconds >= totalSeconds

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13141E)),
                border = BorderStroke(1.dp, if (isUnlocked) RpgGold.copy(alpha = 0.4f) else DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = sk.name.uppercase(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (isUnlocked) RpgGold else TextWhite
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isUnlocked) RpgGold.copy(alpha = 0.15f) else Color.Black)
                                        .border(1.dp, if (isUnlocked) RpgGold else DarkCardBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LVL $activeLvl",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isUnlocked) RpgGold else NeonBlueAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            Text(
                                text = sk.suggestion,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }

                        // Disintegrate action
                        IconButton(
                            onClick = { onDeleteSkill(sk) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Delete skill ${sk.name}", tint = RpgRuby, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Bar to Unlock/Master Skill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUnlocked) "⭐ TRANSCENDED MASTER" else "UNLEASHING RESILIENCE",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isUnlocked) RpgGold else NeonBlueAccent,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "${sk.spentSeconds / 60}/${sk.targetMinutes} MINS",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(skillProgress)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        if (isUnlocked) listOf(RpgGold, Color(0xFFFCD34D)) else listOf(NeonBlueAccent, Color(0xFFC582FF))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }

}
internal fun LazyListScope.profileHistory(
    sessions: List<StudySessionEntity>, onEditNote: (StudySessionEntity) -> Unit,
) {
    // Section: Activity log list
    item {
        Text(
            text = "Session history",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = NeonBlueAccent,
                letterSpacing = 1.sp
            )
        )
    }

    if (sessions.isEmpty()) {
        item {
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "No sessions yet. Complete a study session to see it here.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        items(items = sessions, key = { it.id }) { session ->
            SessionHistoryCard(
                session = session,
                onEditNote = { onEditNote(session) },
            )
        }
    }
}
internal fun LazyListScope.profileSettings(
    nonNullProfile: UserProfileEntity, onEditSchedule: () -> Unit,
    reminderSettings: ReminderSettings, onReminderSettingsChange: (ReminderSettings) -> Unit,
    onTriggerSimulatedNotification: (String) -> Unit, soundSettings: SoundSettings,
    onSoundEnabledChange: (Boolean) -> Unit, onSoundVolumeChange: (Float) -> Unit,
    onPreviewSound: () -> Unit, onReplayTutorial: () -> Unit,
    breakSuggestionsEnabled: Boolean, onBreakSuggestionsEnabledChange: (Boolean) -> Unit,
    focusShieldEnabled: Boolean, focusShieldHasAccess: Boolean,
    onFocusShieldEnabledChange: (Boolean) -> Unit, onRequestFocusShieldAccess: () -> Unit,
) {
    // System 8: DYNAMIC STUDY SCHEDULE GUILD CONTRACT DISPLAY
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Study schedule",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonBlueAccent,
                    letterSpacing = 1.sp
                )
            )

            TextButton(onClick = onEditSchedule, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Edit schedule")
            }
        }
    }

    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val daysPrint = if (nonNullProfile.scheduleDays.isBlank()) "None Selected" else nonNullProfile.scheduleDays
                val flexPrint = if (nonNullProfile.scheduleFlexibility.isBlank()) "Adaptable Study Plan" else nonNullProfile.scheduleFlexibility

                Text(
                    text = "Your study days and daily targets are used by the dashboard.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatRow(label = "Study days", value = daysPrint)

                val daysOfWeekList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val targetsList = nonNullProfile.scheduleWeekdayMinutes.split(",").map { it.trim().toIntOrNull() ?: 45 }
                val weekdayTargetsFormatted = daysOfWeekList.mapIndexed { idx, day ->
                    val mins = if (idx < targetsList.size) targetsList[idx] else 45
                    "$day: ${mins}m"
                }.joinToString(" | ")

                StatRow(label = "Daily targets", value = weekdayTargetsFormatted)
                StatRow(label = "Schedule style", value = flexPrint)
            }
        }
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reminders",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonBlueAccent,
                    letterSpacing = 1.sp
                )
            )
        }
    }

    item {
        ReminderSettingsCard(
            settings = reminderSettings,
            onSettingsChange = onReminderSettingsChange,
            onPreview = onTriggerSimulatedNotification,
        )
    }

    // Replay Tutorial / System Controls Section
    item { TimerAlarmSettingsCard() }
    item {
        SystemControlsCard(
            soundSettings = soundSettings,
            onSoundEnabledChange = onSoundEnabledChange,
            onSoundVolumeChange = onSoundVolumeChange,
            onPreviewSound = onPreviewSound,
            onReplayTutorial = onReplayTutorial,
            breakSuggestionsEnabled = breakSuggestionsEnabled,
            onBreakSuggestionsEnabledChange = onBreakSuggestionsEnabledChange,
            focusShieldEnabled = focusShieldEnabled,
            focusShieldHasAccess = focusShieldHasAccess,
            onFocusShieldEnabledChange = onFocusShieldEnabledChange,
            onRequestFocusShieldAccess = onRequestFocusShieldAccess,
        )
    }

}
