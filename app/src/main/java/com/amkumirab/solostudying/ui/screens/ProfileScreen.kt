package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.ui.theme.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.notification.ReminderSettings
import com.amkumirab.solostudying.sound.SoundSettings

@Composable
fun StatsTab(
    profile: UserProfileEntity?,
    sessions: List<StudySessionEntity>,
    skills: List<SkillEntity>,
    onCreateSkill: (String, Int, String) -> Unit,
    onDeleteSkill: (SkillEntity) -> Unit,
    onUpdateSchedule: (String, Int, String, String) -> Unit,
    onTriggerSimulatedNotification: (String) -> Unit,
    reminderSettings: ReminderSettings,
    onReminderSettingsChange: (ReminderSettings) -> Unit,
    soundSettings: SoundSettings,
    onSoundEnabledChange: (Boolean) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onPreviewSound: () -> Unit,
    onReplayTutorial: () -> Unit,
    breakSuggestionsEnabled: Boolean,
    onBreakSuggestionsEnabledChange: (Boolean) -> Unit,
    focusShieldEnabled: Boolean,
    focusShieldHasAccess: Boolean,
    onFocusShieldEnabledChange: (Boolean) -> Unit,
    onRequestFocusShieldAccess: () -> Unit,
    onUpdateSessionNote: (Long, String) -> Unit,
    selectedSection: ProfileSection? = null,
    onSectionSelected: ((ProfileSection) -> Unit)? = null,
    showSettingsHeader: Boolean = true,
) {
    val nonNullProfile = profile ?: UserProfileEntity()
    var localSection by rememberSaveable { mutableStateOf(ProfileSection.Overview) }
    val section = selectedSection ?: localSection
    val select: (ProfileSection) -> Unit = { localSection = it; onSectionSelected?.invoke(it) }
    val sectionState = rememberSaveableStateHolder()
    var showSummonSkillDialog by remember { mutableStateOf(false) }
    var showScheduleContractDialog by remember { mutableStateOf(false) }
    var sessionBeingEdited by remember { mutableStateOf<StudySessionEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        if (section != ProfileSection.Settings || showSettingsHeader) {
            ProfileSectionNavigation(section, select)
        }
        sectionState.SaveableStateProvider(section.name) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (section) {
                    ProfileSection.Overview -> profileOverview(nonNullProfile, sessions)
                    ProfileSection.Skills -> profileSkills(skills, { showSummonSkillDialog = true }, onDeleteSkill)
                    ProfileSection.History -> profileHistory(sessions) { sessionBeingEdited = it }
                    ProfileSection.Settings -> profileSettings(
                        nonNullProfile, { showScheduleContractDialog = true },
                        reminderSettings, onReminderSettingsChange, onTriggerSimulatedNotification,
                        soundSettings, onSoundEnabledChange, onSoundVolumeChange, onPreviewSound,
                        onReplayTutorial, breakSuggestionsEnabled, onBreakSuggestionsEnabledChange,
                        focusShieldEnabled, focusShieldHasAccess, onFocusShieldEnabledChange, onRequestFocusShieldAccess,
                    )
                }
            }
        }
    }
    sessionBeingEdited?.let { session ->
        SessionNoteDialog(session, { sessionBeingEdited = null }, { note ->
            onUpdateSessionNote(session.id.toLong(), note)
            sessionBeingEdited = null
        })
    }

    // Custom skill editor
    if (showSummonSkillDialog) {
        var skillName by remember { mutableStateOf("") }
        var targetHrsInput by remember { mutableStateOf("10") }
        var suggInput by remember { mutableStateOf("Specialized expertise training") }

        Dialog(onDismissRequest = { showSummonSkillDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                border = BorderStroke(1.5.dp, NeonBlueAccent),
                modifier = Modifier.padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Add a skill",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = NeonBlueAccent,
                            letterSpacing = 1.sp
                        )
                    )

                    OutlinedTextField(
                        value = skillName,
                        onValueChange = { skillName = it },
                        label = { Text("Skill name") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedLabelColor = NeonBlueAccent,
                            focusedIndicatorColor = NeonBlueAccent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = targetHrsInput,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) targetHrsInput = it
                        },
                        label = { Text("Target minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedLabelColor = NeonBlueAccent,
                            focusedIndicatorColor = NeonBlueAccent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = suggInput,
                        onValueChange = { suggInput = it },
                        label = { Text("Description") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedLabelColor = NeonBlueAccent,
                            focusedIndicatorColor = NeonBlueAccent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showSummonSkillDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCEL")
                        }

                        val valMinutes = targetHrsInput.toIntOrNull() ?: 600
                        Button(
                            onClick = {
                                if (skillName.isNotBlank() && valMinutes > 0) {
                                    onCreateSkill(skillName, valMinutes, suggInput)
                                    showSummonSkillDialog = false
                                }
                            },
                            enabled = skillName.isNotBlank() && valMinutes > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlueAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Add skill", color = BlackFantasyBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Interactive Dialog 2: Dynamic Study Schedule Guild Contract editor
    if (showScheduleContractDialog) {
        var flexState by remember { mutableStateOf(if (nonNullProfile.scheduleFlexibility.isNotBlank()) nonNullProfile.scheduleFlexibility else "Adaptable Study Plan") }

        // Days checkbox states
        val daysOfWeekList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val currentDaysSelected = remember {
            mutableStateMapOf<String, Boolean>().apply {
                daysOfWeekList.forEach { day ->
                    put(day, nonNullProfile.scheduleDays.contains(day))
                }
            }
        }

        // Initialize targets for each day from CSV scheduleWeekdayMinutes
        val weekdayTargets = remember {
            val initialList = nonNullProfile.scheduleWeekdayMinutes.split(",").map { it.trim().toIntOrNull() ?: 45 }
            mutableStateMapOf<String, String>().apply {
                daysOfWeekList.forEachIndexed { index, day ->
                    val mins = if (index < initialList.size) initialList[index] else nonNullProfile.scheduleMinutesPerDay
                    put(day, mins.toString())
                }
            }
        }

        val flexClasses = listOf("Casual Study", "Adaptable Study Plan", "Rigorous Schedule", "Elite Grindhouse")

        Dialog(onDismissRequest = { showScheduleContractDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                border = BorderStroke(1.5.dp, RpgGold),
                modifier = Modifier.padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit study schedule",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = RpgGold,
                            letterSpacing = 1.sp
                        )
                    )

                    Text(
                        text = "Customize study target focus budgets for each day. The system monitors alignment dynamically according to the current calendar day.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    // Daily custom targets list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "🛡️ DAILY FOCUS COMMITMENTS",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        daysOfWeekList.forEach { day ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = currentDaysSelected[day] ?: false,
                                        onCheckedChange = { currentDaysSelected[day] = it },
                                        colors = CheckboxDefaults.colors(checkedColor = NeonBlueAccent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = day.uppercase(),
                                        color = if (currentDaysSelected[day] == true) TextWhite else TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = weekdayTargets[day] ?: "0",
                                        onValueChange = { newValue ->
                                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                                weekdayTargets[day] = newValue
                                                if ((newValue.toIntOrNull() ?: 0) > 0) {
                                                    currentDaysSelected[day] = true
                                                }
                                            }
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier
                                            .width(70.dp)
                                            .background(Color.Black, RoundedCornerShape(4.dp))
                                            .border(1.dp, NeonBlueAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(vertical = 6.dp, horizontal = 8.dp)
                                    )
                                    Text(
                                        text = "MINS",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                    )
                                }
                            }
                        }
                    }

                    // Flexibility Dropdown
                    Column {
                        Text("FLEXIBILITY INTENSIVENESS", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        flexClasses.forEach { flexClass ->
                            val isSel = flexState == flexClass
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) RpgGold.copy(alpha = 0.2f) else Color.Black)
                                    .border(1.dp, if (isSel) RpgGold else DarkCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { flexState = flexClass }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = flexClass.uppercase(),
                                    color = if (isSel) RpgGold else TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showScheduleContractDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ABORT")
                        }

                        Button(
                            onClick = {
                                val selectedDaysCsv = daysOfWeekList.filter { currentDaysSelected[it] == true }.joinToString(",")
                                val weekdayMinutesCsv = daysOfWeekList.map { weekdayTargets[it]?.toIntOrNull() ?: 0 }.joinToString(",")
                                val averageMins = daysOfWeekList.map { weekdayTargets[it]?.toIntOrNull() ?: 0 }.filter { it > 0 }.average().toInt().coerceAtLeast(30)
                                onUpdateSchedule(selectedDaysCsv, averageMins, flexState, weekdayMinutesCsv)
                                showScheduleContractDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RpgGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("SEAL TREATY", color = BlackFantasyBackground, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
