package com.amkumirab.solostudying.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.DungeonEntity
import com.amkumirab.solostudying.domain.dungeon.*
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun hoursInput(minutes: Int): String = BigDecimal(minutes)
    .divide(BigDecimal(60), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

private fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

@Composable
internal fun DungeonStudyPlanCard(
    plan: DungeonStudyPlan,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(plan.dungeonName, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            Text(
                when (plan.status) {
                    DungeonPlanStatus.Completed -> "Target reached"
                    DungeonPlanStatus.OnTrack -> "On track"
                    DungeonPlanStatus.BehindSchedule -> "Time to catch up"
                    DungeonPlanStatus.DueToday -> "Due today"
                    DungeonPlanStatus.Overdue -> "Deadline passed"
                    DungeonPlanStatus.NoStudyDays -> "No study days before the deadline"
                },
                color = if (plan.status in listOf(DungeonPlanStatus.Overdue, DungeonPlanStatus.NoStudyDays))
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(
                progress = { plan.progress },
                modifier = Modifier.fillMaxWidth().testTag("dungeon_plan_progress")
                    .semantics { contentDescription = "${plan.dungeonName} study progress" },
            )
            Text("${durationLabel((plan.completedSeconds / 60).toInt())} / ${durationLabel(plan.targetMinutes)} studied",
                style = MaterialTheme.typography.bodyMedium)
            Text("${durationLabel(plan.remainingMinutes)} remaining · Finish by ${plan.deadline.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (plan.remainingMinutes > 0) {
                Text(
                    plan.minutesPerStudyDay?.let { "${durationLabel(it)} per study day" }
                        ?: "Extend the deadline or change your study days.",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (plan.studyDaysRemaining > 0) Text(
                    "${plan.studyDaysRemaining} scheduled study days left. The estimate updates with your progress.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit plan") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DungeonStudyPlanDialog(
    initialDungeon: DungeonEntity?,
    existingDungeons: List<DungeonEntity>,
    bosses: List<BossEntity>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: suspend (DungeonStudyPlanInput) -> Unit,
    onRemove: suspend () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialDungeon?.name.orEmpty()) }
    var hours by rememberSaveable { mutableStateOf(initialDungeon?.targetMinutes?.let(::hoursInput).orEmpty()) }
    var deadline by rememberSaveable { mutableStateOf(initialDungeon?.planDeadlineDate.orEmpty()) }
    var mask by rememberSaveable { mutableIntStateOf(initialDungeon?.studyWeekdaysMask ?: 127) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val input = parseDungeonStudyPlanInput(name, hours, deadline, mask, today)
    val preview = input?.let {
        calculateDungeonStudyPlan(
            DungeonEntity(name = it.dungeonName, description = "", targetMinutes = it.targetMinutes,
                planStartDate = today.toString(), planDeadlineDate = it.deadline.toString(), studyWeekdaysMask = it.weekdaysMask),
            bosses, today,
        )
    }
    val submit: (Boolean) -> Unit = { remove ->
        if (!saving && (remove || input != null)) {
            saving = true
            error = null
            scope.launch {
                try {
                    if (remove) onRemove() else onSave(input!!)
                    onDismiss()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error = if (remove) "Could not remove the plan. Please try again."
                        else "Could not save the plan. Please try again."
                } finally { saving = false }
            }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (initialDungeon?.targetMinutes == null) "Plan a dungeon" else "Edit study plan") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Set a course-sized target and spread it across the days you can study.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(100); error = null },
                    label = { Text("Dungeon / course name") }, singleLine = true,
                    enabled = !saving && initialDungeon == null,
                    modifier = Modifier.fillMaxWidth().testTag("dungeon_plan_name"),
                )
                if (initialDungeon == null && existingDungeons.isNotEmpty()) {
                    Text("Or choose an existing dungeon", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        existingDungeons.distinctBy { it.name }.forEach { dungeon ->
                            SuggestionChip(onClick = {
                                name = dungeon.name
                                hours = dungeon.targetMinutes?.let(::hoursInput).orEmpty()
                                deadline = dungeon.planDeadlineDate.orEmpty()
                                mask = dungeon.studyWeekdaysMask
                                error = null
                            }, label = { Text(dungeon.name) }, enabled = !saving)
                        }
                    }
                }
                OutlinedTextField(
                    value = hours, onValueChange = { hours = it.take(8); error = null },
                    label = { Text("Total study hours") }, placeholder = { Text("100") },
                    supportingText = { Text("Up to 10,000 hours. Decimals are welcome, e.g. 100.5.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, enabled = !saving,
                    modifier = Modifier.fillMaxWidth().testTag("dungeon_plan_hours"),
                )
                OutlinedTextField(
                    value = deadline, onValueChange = { deadline = it.take(10); error = null },
                    label = { Text("Finish by (YYYY-MM-DD)") }, singleLine = true, enabled = !saving,
                    supportingText = { Text("Today to 10 years ahead.") },
                    trailingIcon = {
                        TextButton(enabled = !saving, onClick = {
                            val selected = runCatching { LocalDate.parse(deadline) }.getOrDefault(today.plusMonths(2))
                            DatePickerDialog(context, { _, year, month, day ->
                                deadline = LocalDate.of(year, month + 1, day).toString()
                                error = null
                            }, selected.year, selected.monthValue - 1, selected.dayOfMonth).apply {
                                val zone = ZoneId.systemDefault()
                                datePicker.minDate = today.atStartOfDay(zone).toInstant().toEpochMilli()
                                datePicker.maxDate = today.plusYears(10).atStartOfDay(zone).toInstant().toEpochMilli()
                            }.show()
                        }) { Text("Pick") }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("dungeon_plan_deadline"),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (months in 1..3) SuggestionChip(
                        onClick = { deadline = today.plusMonths(months.toLong()).toString(); error = null },
                        label = { Text(if (months == 1) "1 month" else "$months months") }, enabled = !saving,
                    )
                }
                Text("Study days", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { index, day ->
                        val bit = 1 shl index
                        FilterChip(selected = mask and bit != 0, onClick = { mask = mask xor bit; error = null },
                            label = { Text(day) }, enabled = !saving,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("dungeon_plan_day_${index + 1}"))
                    }
                }
                if (preview != null) {
                    Text(preview.minutesPerStudyDay?.let { "Suggested pace: ${durationLabel(it)} per study day" }
                        ?: "No selected study days before this deadline. Choose another date or schedule.",
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Enter a name, valid hours and date, and at least one study day to continue.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Counts recorded progress from all bosses in this dungeon, including completed ones. Free study does not count; deleting a boss removes its contribution.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (initialDungeon?.targetMinutes != null) TextButton(
                    onClick = { confirmRemove = true }, enabled = !saving,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("dungeon_plan_remove"),
                ) { Text("Remove study plan", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit(false) }, enabled = input != null && !saving,
                modifier = Modifier.heightIn(min = 48.dp).testTag("dungeon_plan_save")) {
                Text(if (saving) "Saving…" else "Save plan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } },
    )
    if (confirmRemove) AlertDialog(
        onDismissRequest = { confirmRemove = false },
        title = { Text("Remove study plan?") },
        text = { Text("Your dungeon, bosses and recorded study time will stay. Only the target and schedule will be removed.") },
        confirmButton = { TextButton(onClick = { confirmRemove = false; submit(true) }) { Text("Remove plan") } },
        dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Keep plan") } },
    )
}

@Preview
@Composable
private fun DungeonStudyPlanPreview() {
    SoloStudyingTheme {
        DungeonStudyPlanCard(
            plan = DungeonStudyPlan("Operating Systems", 6_000, 72_000, 4_800, LocalDate.of(2026, 11, 30),
                40, 120, 0.2f, DungeonPlanStatus.OnTrack), onEdit = {},
        )
    }
}
