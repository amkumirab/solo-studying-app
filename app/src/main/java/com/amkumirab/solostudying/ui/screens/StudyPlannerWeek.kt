package com.amkumirab.solostudying.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate

@Composable
fun StudyPlannerWeek(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, isSessionActive: Boolean,
    onStart: (BossEntity, Int) -> Unit, onMove: (StudyPlan) -> Unit, modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val datePicker = remember { mutableStateOf<DatePickerDialog?>(null) }
    DisposableEffect(Unit) { onDispose { datePicker.value?.dismiss() } }
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    var moveError by rememberSaveable { mutableStateOf<String?>(null) }
    val currentMonday = today.minusDays(today.dayOfWeek.value - 1L)
    val weekStart = currentMonday.plusWeeks(weekOffset.toLong())
    val firstMonday = plan.config.start.minusDays(plan.config.start.dayOfWeek.value - 1L)
    val finish = plan.config.courses.maxOf { it.finishBy }
    val progress = remember(plan, bosses) { blockProgress(plan, bosses) }
    val latestPlan by rememberUpdatedState(plan)
    val latestBosses by rememberUpdatedState(bosses)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Weekly study plan", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text("$weekStart — ${weekStart.plusDays(6)}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { weekOffset-- }, enabled = weekStart > firstMonday, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Previous week") }
                OutlinedButton(onClick = { weekOffset++ }, enabled = weekStart.plusDays(6) < finish, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Next week") }
            }
            if (weekOffset != 0) TextButton(onClick = { weekOffset = 0 }, modifier = Modifier.heightIn(min = 48.dp)) { Text("This week") }
            if (isSessionActive) Text("Finish the active study session before starting another.")
            if (plan.shortfalls.isNotEmpty()) {
                Text("Incomplete plan", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                plan.shortfalls.forEach { (id, minutes) ->
                    Text("${bosses.firstOrNull { it.id == id }?.name ?: "Removed course"}: $minutes min were not scheduled. Edit answers to increase capacity or extend dates.")
                }
            }
            Text("Study time credits blocks in their original order for each course. Moving a session never changes existing credit.", style = MaterialTheme.typography.bodySmall)
            val missing = plan.config.courses.filter { course -> bosses.none { it.id == course.bossId } }
            if (missing.isNotEmpty()) Text("${missing.size} courses were removed. Edit answers to update this plan.", color = MaterialTheme.colorScheme.error)
            val unfinished = progress.filter { !it.missingCourse && it.remainingSeconds > 0 }
            Text("${unfinished.size} unfinished blocks · ${unfinished.count { it.block.date < today }} past-due blocks", style = MaterialTheme.typography.bodyMedium)
            if (remainingPlannerCourses(plan, bosses).isEmpty() && missing.isEmpty()) Text("All planned courses are complete.", color = MaterialTheme.colorScheme.primary)
            moveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        repeat(7) { offset ->
            val date = weekStart.plusDays(offset.toLong())
            val dayBlocks = progress.filter { it.block.date == date }
            item(key = "day_$date") {
                Text("${date.dayOfWeek} · $date", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Text("${dayBlocks.sumOf { it.block.minutes }} / ${plan.config.capacity(date)} planned focus minutes", style = MaterialTheme.typography.bodySmall)
                if (dayBlocks.isEmpty()) Text(if (plan.config.capacity(date) == 0) "Rest day" else "No sessions planned")
            }
            items(dayBlocks, key = { "block_${it.block.id}" }) { entry ->
                val block = entry.block
                val boss = bosses.firstOrNull { it.id == block.bossId }
                val deadline = plan.config.courses.first { it.bossId == block.bossId }.finishBy
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(boss?.name ?: "Removed course", style = MaterialTheme.typography.titleMedium)
                        Text(when {
                            entry.missingCourse -> "This goal was deleted. Edit answers to remove it."
                            entry.remainingSeconds == 0L -> "Done · ${block.minutes} min"
                            date < today -> "Missed / unfinished · ${entry.remainingMinutes} min left"
                            entry.creditedSeconds > 0 -> "Partly studied · ${entry.remainingMinutes} min left"
                            else -> "${entry.remainingMinutes} min left"
                        })
                        if (entry.remainingSeconds > 0 && boss != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onStart(boss, entry.remainingMinutes) }, enabled = !isSessionActive,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("planner_start_${block.id}")) { Text("Study course") }
                                OutlinedButton(onClick = {
                                    datePicker.value?.dismiss()
                                    datePicker.value = pickPlannerDate(context, maxOf(today, date), today, deadline) { picked ->
                                        val latestEntry = blockProgress(latestPlan, latestBosses).firstOrNull { it.block.id == block.id && !it.missingCourse }
                                        val moved = latestEntry?.let { moveStudyBlock(latestPlan, block.id, picked, today, it.creditedSeconds) }
                                        if (moved == null) moveError = "That day is unavailable, full, or this block has already been studied."
                                        else { moveError = null; onMove(moved) }
                                    }
                                }, enabled = !isSessionActive && entry.creditedSeconds == 0L && deadline >= today,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("planner_move_${block.id}")) { Text("Move") }
                            }
                        }
                    }
                }
            }
        }
    }
}
