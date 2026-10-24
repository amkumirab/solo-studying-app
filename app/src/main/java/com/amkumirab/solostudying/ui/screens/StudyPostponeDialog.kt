package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PostponePreview(val source: StudyPlan, val goals: List<BossEntity>, val date: LocalDate, val result: StudyDayPostponement)

@Composable
internal fun StudyPostponeDialog(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, unavailable: Boolean,
    error: String?, onDismiss: () -> Unit, onSave: (StudyPlan) -> Unit,
    subjects: List<PlannerSubject> = emptyList(),
) {
    val preview by produceState<PostponePreview?>(null, plan, bosses, today) {
        value = null
        value = withContext(Dispatchers.Default) { PostponePreview(plan, bosses, today, postponeTodayStudy(plan, bosses, today)) }
    }
    val current = preview?.takeIf { it.source == plan && it.goals == bosses && it.date == today }
    val result = current?.result
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Move today's remaining sessions?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Only today's unfinished sessions move. Existing future sessions, recorded study time and rewards stay unchanged. Partly studied sessions reserve their full slot on the new day.")
                if (plan.config.weeklyScheduling != null) Text("Full weeks and course weekly limits are respected. Sessions may move to a later week, but never past their deadline.")
                val hasLocks = todayStudyBlocks(plan, bosses, today).any { it.block.locked && !it.missingCourse && it.remainingSeconds > 0 }
                if (result == null) Text("Checking available study days…")
                else if (result.unmovedMinutes.isNotEmpty()) {
                    Text(if (hasLocks) "Unlock today's fixed sessions first" else "Not enough room before the deadlines", color = MaterialTheme.colorScheme.error)
                    result.unmovedMinutes.forEach { (id, minutes) -> Text("${subjects.firstOrNull { it.bossId == id }?.name ?: bosses.firstOrNull { it.id == id }?.name ?: "Removed course"}: $minutes min cannot move with the current session sizes.") }
                    Text(if (hasLocks) "Nothing will change. Open the weekly plan and unlock today's unfinished locked sessions before trying again."
                        else "Nothing will change. Open the weekly plan to adjust dates or available time.")
                } else {
                    result.moves.forEach { block -> Text("${subjects.firstOrNull { it.bossId == block.bossId }?.name ?: bosses.firstOrNull { it.id == block.bossId }?.name ?: "Removed course"} · ${block.minutes} min slot → ${block.date}${block.clockRange()?.let { " · $it" }.orEmpty()}") }
                    if (result.moves.isEmpty()) Text("No unfinished sessions remain to move.")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { if (!unavailable) result?.plan?.let(onSave) },
            enabled = !unavailable && result?.plan != null && result.moves.isNotEmpty(),
            modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_postpone_today")) { Text("Confirm move") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } },
    )
}
