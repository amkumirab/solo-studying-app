package com.amkumirab.solostudying.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun CreateBossDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, Boolean, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Medium") }
    var minutesInput by remember { mutableStateOf("30") }
    var dungeonNameInput by remember { mutableStateOf("Main Realm") }
    var isRealBoss by remember { mutableStateOf(false) }
    var hasDeadline by remember { mutableStateOf(false) }
    var deadlineDate by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    val context = LocalContext.current

    val options = listOf("Easy", "Medium", "Hard", "Legendary")
    val defaultMinutesPreset = listOf(15, 25, 30, 45, 60, 90, 120)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.5.dp, DarkCardBorder),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SUMMON ACADEMIC BOSS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    )
                )

                // Boss Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Enemy Name (e.g., Mathematics Exam)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("boss_name_input"),
                    singleLine = true
                )

                // Dungeon Name Folder Input
                OutlinedTextField(
                    value = dungeonNameInput,
                    onValueChange = { dungeonNameInput = it },
                    label = { Text("Dungeon Name (Category, e.g. Semester 5)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Real Deliverable toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRealBoss = !isRealBoss }
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRealBoss) RpgGold.copy(alpha = 0.15f) else Color.Black)
                        .border(1.dp, if (isRealBoss) RpgGold else DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = isRealBoss,
                        onCheckedChange = { isRealBoss = it },
                        colors = CheckboxDefaults.colors(checkedColor = RpgGold)
                    )
                    Column {
                        Text(
                            text = "👑 REAL DELIVERABLE BOSS",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = if (isRealBoss) RpgGold else TextWhite)
                        )
                        Text(
                            text = "Represents real Exams/Assignments. Enables Manual Pass capability bypass.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }

                // Optional exam or assignment deadline
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (hasDeadline) InfoContainer else SurfaceSubtle)
                            .border(
                                1.dp,
                                if (hasDeadline) NeonBlueAccent else DarkCardBorder,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { hasDeadline = !hasDeadline }
                            .padding(12.dp)
                            .testTag("deadline_goal_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Checkbox(
                            checked = hasDeadline,
                            onCheckedChange = { hasDeadline = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonBlueAccent),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📅 ADD EXAM OR ASSIGNMENT DEADLINE",
                                color = if (hasDeadline) NeonBlueAccent else TextWhite,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Build a daily plan from the remaining focus time",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    if (hasDeadline) {
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        deadlineDate = LocalDate.of(year, month + 1, day)
                                    },
                                    deadlineDate.year,
                                    deadlineDate.monthValue - 1,
                                    deadlineDate.dayOfMonth,
                                ).apply {
                                    datePicker.minDate = System.currentTimeMillis() - 1_000L
                                }.show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("deadline_date_button"),
                            border = BorderStroke(1.dp, NeonBlueAccent),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = NeonBlueAccent,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = deadlineDate.format(
                                    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
                                ),
                                color = NeonBlueAccent,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Duration focus Input
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            minutesInput = it
                        }
                    },
                    label = { Text("Required Study Duration (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("boss_duration_input"),
                    singleLine = true
                )

                // Quick presets
                Column {
                    Text(text = "QUICK TIME PRESETS (MINUTES)", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        defaultMinutesPreset.take(4).forEach { min ->
                            Button(
                                onClick = { minutesInput = min.toString() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (minutesInput == min.toString()) NeonBlueAccent else Color(0xFF1E2030),
                                    contentColor = if (minutesInput == min.toString()) Color.Black else TextWhite
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$min m", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        defaultMinutesPreset.drop(4).forEach { min ->
                            Button(
                                onClick = { minutesInput = min.toString() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (minutesInput == min.toString()) NeonBlueAccent else Color(0xFF1E2030),
                                    contentColor = if (minutesInput == min.toString()) Color.Black else TextWhite
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$min m", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // Difficulty selectors
                Column {
                    Text(text = "DIFFICULTY TIER", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        options.forEach { opt ->
                            val isSel = difficulty == opt
                            val col = getDifficultyColor(opt)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) col.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSel) col else DarkCardBorder, RoundedCornerShape(6.dp))
                                    .clickable { difficulty = opt }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) col else TextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Confirm/Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL")
                    }

                    val finalMinutes = minutesInput.toIntOrNull() ?: 30
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && finalMinutes > 0) {
                                onConfirm(
                                    name,
                                    difficulty,
                                    finalMinutes,
                                    dungeonNameInput.trim(),
                                    isRealBoss,
                                    deadlineDate.toString().takeIf { hasDeadline },
                                )
                            }
                        },
                        enabled = name.isNotEmpty() && finalMinutes > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlueSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("conclude_boss_summon_button")
                    ) {
                        Text("SUMMON", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
