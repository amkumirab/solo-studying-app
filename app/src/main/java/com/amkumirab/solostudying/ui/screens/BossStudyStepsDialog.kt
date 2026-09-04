package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.BossStepEntity
import com.amkumirab.solostudying.ui.theme.BlackFantasyBackground
import com.amkumirab.solostudying.ui.theme.DarkCardBorder
import com.amkumirab.solostudying.ui.theme.DarkFantasySurface
import com.amkumirab.solostudying.ui.theme.NeonBlueAccent
import com.amkumirab.solostudying.ui.theme.RpgEmerald
import com.amkumirab.solostudying.ui.theme.RpgGold
import com.amkumirab.solostudying.ui.theme.RpgRuby
import com.amkumirab.solostudying.ui.theme.SurfaceElevated
import com.amkumirab.solostudying.ui.theme.TextMuted
import com.amkumirab.solostudying.ui.theme.TextWhite

@Composable
fun BossStudyStepsDialog(
    boss: BossEntity,
    steps: List<BossStepEntity>,
    isSessionActive: Boolean,
    activeStepId: Int?,
    onDismiss: () -> Unit,
    onCreateStep: (String, Int) -> Unit,
    onUpdateStep: (BossStepEntity, String, Int) -> Unit,
    onSetCompleted: (BossStepEntity, Boolean) -> Unit,
    onDeleteStep: (BossStepEntity) -> Unit,
    onStartStep: (BossStepEntity) -> Unit,
) {
    var editingStep by remember { mutableStateOf<BossStepEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BossStepEntity?>(null) }
    val completedCount = steps.count { it.isCompleted }
    val progress = if (steps.isEmpty()) 0f else completedCount.toFloat() / steps.size

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("boss_steps_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, NeonBlueAccent.copy(alpha = 0.65f)),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STUDY STEPS",
                            color = NeonBlueAccent,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp,
                        )
                        Text(
                            text = boss.name,
                            color = TextWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = {
                            editingStep = null
                            showEditor = true
                        },
                        modifier = Modifier.testTag("add_boss_step"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add study step", tint = NeonBlueAccent)
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("boss_steps_progress")
                        .semantics {
                            contentDescription = "$completedCount of ${steps.size} study steps completed"
                        },
                    color = if (steps.isNotEmpty() && completedCount == steps.size) RpgEmerald else NeonBlueAccent,
                    trackColor = SurfaceElevated,
                )
                Text(
                    text = if (steps.isEmpty()) {
                        "Break this goal into small, clear actions so you always know what to study next."
                    } else {
                        "$completedCount of ${steps.size} steps complete"
                    },
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )

                if (steps.isEmpty()) {
                    OutlinedButton(
                        onClick = {
                            editingStep = null
                            showEditor = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_first_boss_step"),
                        border = BorderStroke(1.dp, NeonBlueAccent),
                    ) {
                        Icon(Icons.Default.Route, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("ADD FIRST STEP", fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 390.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(steps, key = { it.id }) { step ->
                            BossStudyStepRow(
                                step = step,
                                isSessionActive = isSessionActive,
                                isActive = activeStepId == step.id,
                                onToggle = { onSetCompleted(step, !step.isCompleted) },
                                onEdit = {
                                    editingStep = step
                                    showEditor = true
                                },
                                onDelete = { pendingDelete = step },
                                onStart = { onStartStep(step) },
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("close_boss_steps"),
                ) {
                    Text("CLOSE", color = TextMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showEditor) {
        BossStepEditorDialog(
            step = editingStep,
            onDismiss = { showEditor = false },
            onSave = { title, minutes ->
                editingStep?.let { onUpdateStep(it, title, minutes) }
                    ?: onCreateStep(title, minutes)
                showEditor = false
            },
        )
    }

    pendingDelete?.let { step ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete study step?") },
            text = { Text("${step.title} will be removed from this goal.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteStep(step)
                        pendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_boss_step"),
                ) {
                    Text("DELETE", color = RpgRuby)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("CANCEL") }
            },
            containerColor = DarkFantasySurface,
            titleContentColor = TextWhite,
            textContentColor = TextMuted,
        )
    }
}

@Composable
private fun BossStudyStepRow(
    step: BossStepEntity,
    isSessionActive: Boolean,
    isActive: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("boss_step_${step.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) NeonBlueAccent.copy(alpha = 0.12f) else SurfaceElevated,
        ),
        border = BorderStroke(1.dp, if (isActive) NeonBlueAccent else DarkCardBorder),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (step.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (step.isCompleted) {
                        "Mark ${step.title} incomplete"
                    } else {
                        "Mark ${step.title} complete"
                    },
                    tint = if (step.isCompleted) RpgEmerald else NeonBlueAccent,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isActive, onClick = onToggle)
                        .padding(4.dp)
                        .testTag("toggle_boss_step_${step.id}"),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.title,
                        color = if (step.isCompleted) TextMuted else TextWhite,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (step.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${step.estimatedMinutes} min focus",
                        color = if (isActive) NeonBlueAccent else RpgGold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                IconButton(
                    onClick = onEdit,
                    enabled = !isActive,
                    modifier = Modifier.testTag("edit_boss_step_${step.id}"),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${step.title}", tint = TextMuted)
                }
                IconButton(
                    onClick = onDelete,
                    enabled = !isActive,
                    modifier = Modifier.testTag("delete_boss_step_${step.id}"),
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${step.title}", tint = RpgRuby)
                }
            }

            if (!step.isCompleted) {
                Button(
                    onClick = onStart,
                    enabled = !isSessionActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_boss_step_${step.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonBlueAccent,
                        contentColor = BlackFantasyBackground,
                    ),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isSessionActive) "SESSION ACTIVE" else "FOCUS THIS STEP",
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun BossStepEditorDialog(
    step: BossStepEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    var title by remember(step?.id) { mutableStateOf(step?.title.orEmpty()) }
    var minutesInput by remember(step?.id) {
        mutableStateOf((step?.estimatedMinutes ?: 25).toString())
    }
    val minutes = minutesInput.toIntOrNull()
    val canSave = title.trim().isNotEmpty() && minutes != null && minutes in 1..480

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("boss_step_editor"),
        title = { Text(if (step == null) "Add study step" else "Edit study step") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("boss_step_title_input"),
                    label = { Text("What will you study?") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { value ->
                        if (value.length <= 3 && value.all(Char::isDigit)) minutesInput = value
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("boss_step_duration_input"),
                    label = { Text("Focus minutes") },
                    supportingText = { Text("Choose between 1 and 480 minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim(), checkNotNull(minutes)) },
                enabled = canSave,
                modifier = Modifier.testTag("save_boss_step"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlueAccent),
            ) {
                Text("SAVE", color = BlackFantasyBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
        containerColor = DarkFantasySurface,
        titleContentColor = TextWhite,
        textContentColor = TextMuted,
    )
}
