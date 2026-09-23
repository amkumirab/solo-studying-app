package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.data.entity.SkillEntity
import com.amkumirab.solostudying.focuscycle.FocusCyclePlan
import com.amkumirab.solostudying.focuscycle.FocusCycleState
import com.amkumirab.solostudying.ui.theme.*
import java.util.Locale

@Composable
fun FocusCycleSetupDialog(
    skills: List<SkillEntity>,
    initialSkillId: Int?,
    onDismiss: () -> Unit,
    onStart: (FocusCyclePlan) -> Unit,
) {
    var focusInput by remember { mutableStateOf("25") }
    var breakInput by remember { mutableStateOf("5") }
    var roundsInput by remember { mutableStateOf("4") }
    var skillId by remember { mutableStateOf(initialSkillId) }
    var skillMenuExpanded by remember { mutableStateOf(false) }
    val focusMinutes = focusInput.toIntOrNull()
    val breakMinutes = breakInput.toIntOrNull()
    val rounds = roundsInput.toIntOrNull()
    val isValid = focusMinutes in FocusCyclePlan.MIN_FOCUS_MINUTES..FocusCyclePlan.MAX_FOCUS_MINUTES &&
        breakMinutes in FocusCyclePlan.MIN_BREAK_MINUTES..FocusCyclePlan.MAX_BREAK_MINUTES &&
        rounds in FocusCyclePlan.MIN_ROUNDS..FocusCyclePlan.MAX_ROUNDS

    fun applyPreset(plan: FocusCyclePlan) {
        focusInput = plan.focusMinutes.toString()
        breakInput = plan.breakMinutes.toString()
        roundsInput = plan.totalRounds.toString()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("focus_cycle_setup"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111A2C)),
            border = BorderStroke(2.dp, RpgGold),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Repeat, contentDescription = null, tint = RpgGold)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "FOCUS CYCLES",
                            color = RpgGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text("Build momentum across focused rounds", color = TextMuted)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { applyPreset(FocusCyclePlan.STANDARD) },
                        modifier = Modifier.weight(1f).testTag("cycle_preset_standard"),
                        border = BorderStroke(1.dp, NeonBlueAccent),
                    ) { Text("25 / 5 × 4", color = TextWhite) }
                    OutlinedButton(
                        onClick = { applyPreset(FocusCyclePlan.DEEP_WORK) },
                        modifier = Modifier.weight(1f).testTag("cycle_preset_deep_work"),
                        border = BorderStroke(1.dp, RpgGold),
                    ) { Text("50 / 10 × 2", color = TextWhite) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CycleNumberField("FOCUS", focusInput, { focusInput = digitsOnly(it) }, Modifier.weight(1f))
                    CycleNumberField("BREAK", breakInput, { breakInput = digitsOnly(it) }, Modifier.weight(1f))
                    CycleNumberField("ROUNDS", roundsInput, { roundsInput = digitsOnly(it) }, Modifier.weight(1f))
                }

                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A1220), RoundedCornerShape(10.dp))
                            .clickable { skillMenuExpanded = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("SKILL FOCUS", color = TextMuted, fontSize = 10.sp)
                            Text(
                                skills.firstOrNull { it.id == skillId }?.name ?: "General Focus",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = NeonBlueAccent)
                    }
                    DropdownMenu(
                        expanded = skillMenuExpanded,
                        onDismissRequest = { skillMenuExpanded = false },
                        modifier = Modifier.background(DarkFantasySurface),
                    ) {
                        DropdownMenuItem(
                            text = { Text("General Focus", color = TextWhite) },
                            onClick = { skillId = null; skillMenuExpanded = false },
                        )
                        skills.forEach { skill ->
                            DropdownMenuItem(
                                text = { Text(skill.name, color = TextWhite) },
                                onClick = { skillId = skill.id; skillMenuExpanded = false },
                            )
                        }
                    }
                }

                Text(
                    "Focus: 1–480 min  ·  Break: 1–60 min  ·  Rounds: 2–12",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = {
                        onStart(FocusCyclePlan(focusMinutes!!, breakMinutes!!, rounds!!, skillId))
                    },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("start_focus_cycle"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RpgGold,
                        contentColor = Color.Black,
                    ),
                ) { Text("START CYCLE", fontWeight = FontWeight.Black) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCEL", color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun CycleNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 10.sp) },
        suffix = { if (label != "ROUNDS") Text("m", fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedBorderColor = NeonBlueAccent,
            unfocusedBorderColor = DarkCardBorder,
        ),
    )
}

private fun digitsOnly(value: String): String = value.filter(Char::isDigit).take(3)

@Composable
fun CycleBreakReadyDialog(
    state: FocusCycleState,
    onStartBreak: () -> Unit,
    onEndCycle: () -> Unit,
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("cycle_break_ready"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102326)),
            border = BorderStroke(2.dp, RpgEmerald),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Coffee, contentDescription = null, tint = RpgEmerald, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(10.dp))
                Text("ROUND ${state.completedRounds} COMPLETE", color = RpgEmerald, fontWeight = FontWeight.Black)
                Text(
                    "Take a ${state.plan.breakMinutes}-minute break before round ${state.nextRound} of ${state.plan.totalRounds}.",
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Button(
                    onClick = onStartBreak,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cycle_start_break"),
                    colors = ButtonDefaults.buttonColors(containerColor = RpgEmerald, contentColor = Color.Black),
                ) { Text("START ${state.plan.breakMinutes} MIN BREAK", fontWeight = FontWeight.Black) }
                TextButton(onClick = onEndCycle, modifier = Modifier.fillMaxWidth()) {
                    Text("END CYCLE", color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun CycleCompleteDialog(state: FocusCycleState, onDone: () -> Unit) {
    Dialog(onDismissRequest = onDone) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("focus_cycle_complete"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111A2C)),
            border = BorderStroke(2.dp, RpgGold),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Repeat, contentDescription = null, tint = RpgGold, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(10.dp))
                Text("CYCLE COMPLETE", color = RpgGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("${state.completedRounds} focused rounds finished", color = TextWhite)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CycleMetric("FOCUS TIME", formatCycleDuration(state.focusedSeconds), Modifier.weight(1f))
                    CycleMetric("ROUNDS", "${state.completedRounds}/${state.plan.totalRounds}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RpgGold, contentColor = Color.Black),
                ) { Text("DONE", fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun CycleMetric(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1220))) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextMuted, fontSize = 10.sp)
            Text(value, color = TextWhite, fontWeight = FontWeight.Black)
        }
    }
}

private fun formatCycleDuration(seconds: Long): String {
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    return if (hours > 0L) String.format(Locale.ROOT, "%dh %02dm", hours, minutes) else "${minutes}m"
}
