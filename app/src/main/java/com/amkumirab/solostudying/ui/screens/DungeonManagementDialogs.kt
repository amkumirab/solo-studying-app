package com.amkumirab.solostudying.ui.screens

import android.database.sqlite.SQLiteException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.BossStepEntity
import com.amkumirab.solostudying.domain.dungeon.DungeonGoalEdit
import com.amkumirab.solostudying.domain.dungeon.MAX_GOAL_MINUTES
import com.amkumirab.solostudying.domain.dungeon.goalDifficulties
import java.time.LocalDate
import kotlinx.coroutines.launch

internal enum class GoalAction { Edit, Archive, SecondGate }

@Composable
internal fun ManagedGoalCard(
    goal: BossEntity,
    steps: List<BossStepEntity>,
    activeBoss: BossEntity?,
    isBattleActive: Boolean,
    onFight: (BossEntity) -> Unit,
    onConquer: (BossEntity) -> Unit,
    onSteps: (BossEntity) -> Unit,
    onManage: (BossEntity, GoalAction) -> Unit,
    secondGateContent: (@Composable () -> Unit)? = null,
) {
    BossCard(
        boss = goal,
        steps = steps.filter { it.bossId == goal.id },
        activeBoss = activeBoss,
        isBattleActive = isBattleActive,
        onFightBoss = { onFight(goal) },
        onConquerRealBoss = { onConquer(goal) },
        onManageSteps = { onSteps(goal) },
        onEdit = { onManage(goal, GoalAction.Edit) },
        onDeleteBoss = { onManage(goal, GoalAction.Archive) },
        onOpenSecondGate = if (goal.isCompleted && goal.secondGateOfId == null && secondGateContent == null) {
            { onManage(goal, GoalAction.SecondGate) }
        } else null,
        secondGateContent = secondGateContent,
    )
}

@Composable
internal fun GoalManagementDialog(
    goal: BossEntity,
    action: GoalAction,
    onDismiss: () -> Unit,
    onCommit: suspend (DungeonGoalEdit?, Int?) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(goal.id, action) { mutableStateOf(goal.name) }
    var difficulty by rememberSaveable(goal.id, action) { mutableStateOf(goal.difficulty) }
    var target by rememberSaveable(goal.id, action) {
        mutableStateOf(if (action == GoalAction.SecondGate) "" else goal.requiredMinutes.toString())
    }
    var deadlineText by rememberSaveable(goal.id, action) { mutableStateOf(goal.deadlineDate.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val minutes = target.toIntOrNull()
    val deadline = runCatching { LocalDate.parse(deadlineText.trim()) }.getOrNull()
    val deadlineValid = deadlineText.isBlank() || deadline != null
    val edit = minutes?.let { DungeonGoalEdit(name, difficulty, it, deadline) }
    val valid = when (action) {
        GoalAction.Edit -> deadlineValid && edit?.isValidFor(goal) == true
        GoalAction.Archive -> true
        GoalAction.SecondGate -> minutes != null && minutes in 1..MAX_GOAL_MINUTES
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Text(when (action) {
                GoalAction.Edit -> "Edit goal"
                GoalAction.Archive -> "Move to Deleted goals?"
                GoalAction.SecondGate -> "Open Second Gate"
            })
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (action) {
                    GoalAction.Edit -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Goal name") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        GoalDifficultyPicker(difficulty) { difficulty = it }
                        GoalMinutesField(target, { target = it }, "Target minutes", enabled = !goal.isCompleted)
                        Text(
                            if (goal.isCompleted) "The completed target stays in your history."
                            else "A changed target must exceed the time already studied: ${formatStudyTimeShort(goal.timeSpentSeconds)}.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedTextField(deadlineText, { deadlineText = it }, label = { Text("Deadline (YYYY-MM-DD)") },
                            supportingText = { Text(if (deadlineValid) "Leave blank for no deadline." else "Enter a valid date, such as 2026-12-15.") },
                            isError = !deadlineValid, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    GoalAction.Archive -> Text(
                        "Move ${goal.name} out of your active goals? You can restore it with its study time and steps.",
                    )
                    GoalAction.SecondGate -> {
                        Text("${goal.name} stays completed. Set an additional target for its Second Gate.")
                        GoalMinutesField(target, { target = it }, "Additional minutes")
                    }
                }
                if (error) Text("Couldn't save this change. No records were replaced.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !busy, onClick = {
                busy = true
                scope.launch {
                    val saved = try {
                        onCommit(edit, minutes)
                    } catch (_: SQLiteException) {
                        false
                    }
                    busy = false
                    error = !saved
                    if (saved) onDismiss()
                }
            }) {
                Text(when (action) {
                    GoalAction.Edit -> "Save"
                    GoalAction.Archive -> "Move to Deleted goals"
                    GoalAction.SecondGate -> "Open gate"
                })
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GoalMinutesField(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean = true) {
    OutlinedTextField(
        value, { input -> if (input.all { it in '0'..'9' }) onChange(input) },
        label = { Text(label) }, enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GoalDifficultyPicker(difficulty: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Difficulty: $difficulty")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            goalDifficulties.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onChange(option); expanded = false })
            }
        }
    }
}

@Composable
internal fun DeletedGoalsDialog(
    goals: List<BossEntity>,
    onDismiss: () -> Unit,
    onRestore: suspend (Int) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf<Int?>(null) }
    var failed by remember { mutableStateOf<Int?>(null) }
    val deletedIds = remember(goals) { goals.map { it.id }.toSet() }
    AlertDialog(
        onDismissRequest = { if (restoring == null) onDismiss() },
        title = { Text("Deleted goals") },
        text = {
            if (goals.isEmpty()) {
                Text("No deleted goals. Goals you remove from Study will appear here.")
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(goals, key = { it.id }) { goal ->
                        Column {
                            Text(goal.name, style = MaterialTheme.typography.titleSmall)
                            Text("${formatStudyTimeShort(goal.timeSpentSeconds)} studied · ${goal.requiredMinutes} min target")
                            val parentDeleted = goal.secondGateOfId in deletedIds
                            if (parentDeleted) Text("Restore the original goal first.")
                            if (failed == goal.id) Text("Couldn't restore this goal.", color = MaterialTheme.colorScheme.error)
                            TextButton(
                                modifier = Modifier.testTag("restore_goal_${goal.id}"),
                                enabled = restoring == null && !parentDeleted,
                                onClick = {
                                    restoring = goal.id
                                    scope.launch {
                                        val restored = try {
                                            onRestore(goal.id)
                                        } catch (_: SQLiteException) {
                                            false
                                        }
                                        failed = if (restored) null else goal.id
                                        restoring = null
                                    }
                                },
                            ) { Text("Restore") }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = restoring == null, onClick = onDismiss) { Text("Done") } },
    )
}
