package com.amkumirab.solostudying.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StudyPlannerWizard(
    bosses: List<BossEntity>, today: LocalDate, initialPlan: StudyPlan?,
    weekdayDefaults: List<Int>, busy: Boolean, onAccept: (StudyPlan) -> Unit,
    onCancel: () -> Unit, modifier: Modifier = Modifier,
) {
    val initialCourses = remember { initialPlan?.let { remainingPlannerCourses(it, bosses) }.orEmpty() }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var courseIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedIds by rememberSaveable { mutableStateOf(initialCourses.map { it.bossId }) }
    var hours by rememberSaveable { mutableStateOf(mapOf<Int, String>()) }
    var dates by rememberSaveable { mutableStateOf(mapOf<Int, String>()) }
    var priorities by rememberSaveable { mutableStateOf(mapOf<Int, Int>()) }
    var capacity by rememberSaveable {
        mutableStateOf((initialPlan?.config?.weekdayMinutes ?: weekdayDefaults).let { values ->
            List(7) { values.getOrNull(it)?.coerceIn(0, 480)?.toString() ?: "60" }
        })
    }
    var sessionMinutes by rememberSaveable { mutableIntStateOf(initialPlan?.config?.sessionMinutes ?: 25) }
    val available = bosses.filter { !it.isCompleted }
    val selected = selectedIds.mapNotNull { id -> available.firstOrNull { it.id == id } }
    fun remaining(boss: BossEntity): Int = initialCourses.firstOrNull { it.bossId == boss.id }?.remainingMinutes
        ?: ((boss.requiredMinutes * 60L - boss.timeSpentSeconds.coerceAtLeast(0)).coerceAtLeast(0) + 59).div(60).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    fun hoursText(boss: BossEntity) = hours[boss.id] ?: BigDecimal(remaining(boss)).divide(BigDecimal(60), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    fun minutes(boss: BossEntity): Int? = if (boss.id !in hours) remaining(boss).takeIf { it in 1..60000 } else plannerHoursToMinutes(hoursText(boss))
    fun finishBy(boss: BossEntity): LocalDate? = runCatching {
        LocalDate.parse(dates[boss.id] ?: initialCourses.firstOrNull { it.bossId == boss.id }?.finishBy?.toString()
            ?: boss.deadlineDate ?: today.plusDays(28).toString())
    }.getOrNull()
    val context = LocalContext.current
    val datePicker = remember { mutableStateOf<DatePickerDialog?>(null) }
    DisposableEffect(Unit) { onDispose { datePicker.value?.dismiss() } }
    val current = selected.getOrNull(courseIndex)
    val capacities = capacity.map { it.toIntOrNull() }
    val validCapacity = capacities.all { it != null && it in 0..480 } && capacities.any { it != null && it > 0 }
    val validCourse = current != null && minutes(current) != null && finishBy(current)?.let { it in today..today.plusDays(365) } == true
    val config = if (step == 4) runCatching {
        require(selected.size == selectedIds.size)
        PlannerConfig(today, selected.map { boss -> PlannerCourse(boss.id, minutes(boss)!!, finishBy(boss)!!,
            priorities[boss.id] ?: initialCourses.firstOrNull { it.bossId == boss.id }?.priority ?: 2,
            boss.timeSpentSeconds.coerceAtLeast(0)) }, capacities.map { it!! }, sessionMinutes)
    }.getOrNull() else null
    val preview by produceState<StudyPlan?>(null, config) {
        value = null
        if (config != null) value = withContext(Dispatchers.Default) { buildStudyPlan(config) }
    }
    val canContinue = when (step) {
        0 -> selected.isNotEmpty() && selected.size == selectedIds.size
        1 -> validCourse
        2 -> validCapacity
        else -> true
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Step ${step + 1} of 5", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        when (step) {
            0 -> {
                Text("Which courses are you preparing for?", style = MaterialTheme.typography.headlineSmall)
                Text("Choose up to 20 unfinished goals. Your existing study history stays unchanged.")
                if (available.isEmpty()) Text("Create a course goal in Study Hub first, then return here.")
                available.forEach { boss ->
                    FilterChip(
                        selected = boss.id in selectedIds,
                        onClick = { selectedIds = if (boss.id in selectedIds) selectedIds - boss.id else selectedIds + boss.id },
                        enabled = boss.id in selectedIds || selectedIds.size < 20,
                        label = { Text(boss.name) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    )
                }
            }
            1 -> if (current != null) {
                Text("Course ${courseIndex + 1} of ${selected.size}: ${current.name}", style = MaterialTheme.typography.headlineSmall)
                Text("How much focused study does this course still need?")
                OutlinedTextField(
                    value = hoursText(current), onValueChange = { hours = hours + (current.id to it.take(12)) },
                    label = { Text("Remaining focus hours") }, supportingText = { Text("0.01 to 1000 hours; not the course's original total") },
                    isError = minutes(current) == null, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("planner_hours"),
                )
                Text("When should your preparation be finished?", style = MaterialTheme.typography.titleMedium)
                val deadline = finishBy(current) ?: today
                OutlinedButton(onClick = {
                    datePicker.value?.dismiss()
                    datePicker.value = pickPlannerDate(context, deadline.coerceIn(today, today.plusDays(365)), today, today.plusDays(365)) {
                        dates = dates + (current.id to it.toString())
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Finish by: $deadline") }
                Text("This date includes study time. Choose the day before your exam if you need to finish earlier.")
                if (deadline !in today..today.plusDays(365)) Text("Choose a finish date between today and one year ahead.", color = MaterialTheme.colorScheme.error)
                Text("How important is this course?", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Normal", "Important", "Critical").forEachIndexed { index, label ->
                        FilterChip(
                            selected = (priorities[current.id] ?: initialCourses.firstOrNull { it.bossId == current.id }?.priority ?: 2) == index + 1,
                            onClick = { priorities = priorities + (current.id to index + 1) },
                            label = { Text(label) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        )
                    }
                }
                Text("Earlier deadlines come first. Priority decides between equal deadlines.", style = MaterialTheme.typography.bodySmall)
            } else Text("This course is no longer available. Go back and select another goal.")
            2 -> {
                Text("When can you study?", style = MaterialTheme.typography.headlineSmall)
                Text("Enter focused minutes for each weekday. Use 0 for a rest day; leave extra time for breaks.")
                listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").forEachIndexed { index, day ->
                    OutlinedTextField(
                        value = capacity[index], onValueChange = { value -> capacity = capacity.toMutableList().also { it[index] = value.take(4) } },
                        label = { Text("$day focus minutes") }, isError = capacities[index]?.let { it in 0..480 } != true,
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("planner_capacity_$index"),
                    )
                }
                if (!validCapacity) Text("Use 0 to 480 minutes per day, with at least one study day.", color = MaterialTheme.colorScheme.error)
            }
            3 -> {
                Text("How long should each focus block be?", style = MaterialTheme.typography.headlineSmall)
                listOf(15, 25, 45, 60, 90, 120).forEach { length ->
                    FilterChip(selected = sessionMinutes == length, onClick = { sessionMinutes = length },
                        label = { Text("$length min") }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
                }
                Text("Shorter final blocks fit your remaining time and daily capacity. Breaks use the existing break timer.")
            }
            4 -> {
                if (config == null) Text("Some answers are no longer valid. Go back to update them.", color = MaterialTheme.colorScheme.error)
                else preview?.let { StudyPlannerPreview(it, bosses, busy, onAccept) } ?: Text("Building your local plan…")
            }
        }
        if (step < 4) {
            Button(onClick = {
                if (step == 1 && courseIndex < selected.lastIndex) courseIndex++ else { step++; if (step == 1) courseIndex = 0 }
            }, enabled = canContinue && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("planner_next")) {
                Text(if (step == 3) "Preview plan" else "Continue")
            }
        }
        OutlinedButton(onClick = {
            if (step == 0) onCancel() else if (step == 1 && courseIndex > 0) courseIndex-- else step--
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (step == 0) "Cancel" else "Back") }
    }
}

@Composable
fun StudyPlannerPreview(plan: StudyPlan, bosses: List<BossEntity>, busy: Boolean, onAccept: (StudyPlan) -> Unit) {
    var acknowledged by rememberSaveable(plan) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your study plan", style = MaterialTheme.typography.headlineSmall)
        Text("${plan.config.courses.size} courses · ${plan.blocks.sumOf { it.minutes }} scheduled minutes\n${plan.config.start} to ${plan.config.courses.maxOf { it.finishBy }}")
        if (plan.shortfalls.isEmpty()) Text("Your preparation fits the available study time.")
        else {
            Text("Not enough study time", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            plan.shortfalls.forEach { (id, minutes) -> Text("${bosses.firstOrNull { it.id == id }?.name ?: "Removed course"}: $minutes min cannot fit before the finish date.") }
            Text("Add available minutes, reduce the remaining effort, or extend a finish date. Saving this plan does not solve the shortage.")
            FilterChip(selected = acknowledged, onClick = { acknowledged = !acknowledged },
                label = { Text("I understand this plan is incomplete") }, modifier = Modifier.heightIn(min = 48.dp).testTag("planner_acknowledge"))
        }
        Text("First seven days", style = MaterialTheme.typography.titleMedium)
        repeat(7) { index ->
            val date = plan.config.start.plusDays(index.toLong())
            val blocks = plan.blocks.filter { it.date == date }
            Text("${date.dayOfWeek} · $date: ${blocks.sumOf { it.minutes }} min")
            blocks.groupBy { it.bossId }.forEach { (id, values) -> Text("${bosses.firstOrNull { it.id == id }?.name ?: "Removed course"} — ${values.sumOf { it.minutes }} min", style = MaterialTheme.typography.bodySmall) }
        }
        Button(onClick = { onAccept(plan) }, enabled = !busy && (plan.shortfalls.isEmpty() || acknowledged),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("planner_accept")) { Text(if (busy) "Saving…" else "Save plan") }
    }
}

internal fun pickPlannerDate(context: Context, selected: LocalDate, min: LocalDate, max: LocalDate, onPick: (LocalDate) -> Unit): DatePickerDialog =
    DatePickerDialog(context, { _, year, month, day -> onPick(LocalDate.of(year, month + 1, day)) },
        selected.year, selected.monthValue - 1, selected.dayOfMonth).apply {
        datePicker.minDate = min.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        datePicker.maxDate = max.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        show()
    }
