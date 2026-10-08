package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate

@Composable
internal fun StudyPlannerToday(
    plan: StudyPlan?, bosses: List<BossEntity>, today: LocalDate,
    loaded: Boolean, unavailable: Boolean, error: String?, onOpen: () -> Unit,
    onStart: (BossEntity, Int) -> Unit, onSave: (StudyPlan, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(today.toString()) { mutableStateOf(false) }
    var postponing by rememberSaveable(today.toString()) { mutableStateOf(false) }
    val entries = remember(plan, bosses, today) { plan?.let { todayStudyBlocks(it, bosses, today) }.orEmpty() }
    val available = entries.filter { !it.missingCourse }
    val pending = available.filter { it.remainingSeconds > 0 }
    val total = available.sumOf { it.block.minutes }
    val credited = available.sumOf { it.creditedSeconds }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Planned courses today", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            !loaded -> Text("Loading study plan…")
            plan == null -> Text("Plan your exam preparation with offline guided questions.")
            available.isEmpty() -> Text(if (plan.config.capacity(today) == 0) "No course sessions today · rest day" else "No course sessions scheduled today.")
            else -> {
                Text("${credited / 60} of $total planned minutes credited", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { (credited.toFloat() / (total * 60L)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                if (pending.isEmpty()) Text("Today's planned sessions are complete.")
                val ordered = available.sortedWith(compareBy<StudyBlockProgress> { it.remainingSeconds == 0L }.thenBy { it.block.id })
                (if (expanded) ordered else ordered.take(3)).forEach { entry ->
                    val boss = bosses.first { it.id == entry.block.bossId }
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(boss.name, style = MaterialTheme.typography.titleSmall)
                        Text(if (entry.remainingSeconds == 0L) "Done · ${entry.block.minutes} min"
                            else "${entry.remainingMinutes} min left · ${entry.creditedSeconds / 60} min credited", style = MaterialTheme.typography.bodySmall)
                        if (entry.remainingSeconds > 0) Button(
                            onClick = { if (!unavailable) onStart(boss, entry.remainingMinutes) }, enabled = !unavailable,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("today_planner_start_${entry.block.id}"),
                        ) { Text(if (unavailable) "Session active or saving" else "Study ${boss.name}") }
                    }
                }
                if (ordered.size > 3) TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (expanded) "Show fewer sessions" else "Show all ${ordered.size} sessions")
                }
            }
        }
        if (entries.any { it.missingCourse }) Text("A planned course was removed. Open the weekly plan to update it.", color = MaterialTheme.colorScheme.error)
        if (plan != null) {
            val overdue = remember(plan, bosses, today) { blockProgress(plan, bosses).count { !it.missingCourse && it.remainingSeconds > 0 && it.block.date < today } }
            if (overdue > 0) Text("$overdue earlier sessions are unfinished. Open the weekly plan to review them.", color = MaterialTheme.colorScheme.error)
            if (plan.shortfalls.isNotEmpty()) Text("Some course time could not fit before its deadline. Open the weekly plan to adjust your answers.", color = MaterialTheme.colorScheme.error)
            if (pending.isNotEmpty()) OutlinedButton(onClick = { postponing = true }, enabled = !unavailable,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("postpone_today")) { Text("No time today") }
        }
        TextButton(onClick = onOpen, enabled = loaded && !unavailable,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_study_planner")) {
            Text(if (!loaded) "Loading…" else if (plan == null) "Plan exam preparation" else "Open weekly plan")
        }
    }
    if (postponing && plan != null) StudyPostponeDialog(plan, bosses, today, unavailable, error,
        onDismiss = { postponing = false }, onSave = { updated -> onSave(updated) { postponing = false } })
}
