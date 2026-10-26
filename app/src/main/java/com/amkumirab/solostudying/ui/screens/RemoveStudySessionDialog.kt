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

private data class SessionRemovalInput(val plan: StudyPlan, val bosses: List<BossEntity>, val today: LocalDate, val id: Int)

@Composable
internal fun RemoveStudySessionDialog(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, blockId: Int,
    unavailable: Boolean, onDismiss: () -> Unit, onSave: (StudyPlan) -> Unit,
    modifier: Modifier = Modifier, subjects: List<PlannerSubject> = emptyList(),
) {
    val block = plan.blocks.firstOrNull { it.id == blockId } ?: return
    val input = SessionRemovalInput(plan, bosses, today, blockId)
    val preview by produceState<Pair<SessionRemovalInput, StudySessionEdit>?>(null, input) {
        value = null
        value = withContext(Dispatchers.Default) { input to previewRemoveStudySession(plan, bosses, today, blockId) }
    }
    val result = preview?.takeIf { it.first == input }?.second
    val proposed = result?.plan
    val name = subjects.firstOrNull { it.bossId == block.bossId }?.name ?: bosses.firstOrNull { it.id == block.bossId }?.name ?: "Removed course"
    AlertDialog(onDismissRequest = { if (!unavailable) onDismiss() }, modifier = modifier,
        title = { Text("Remove this session?") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text("${block.date} · ${block.clockRange()} · ${block.minutes} min")
            Text("Only this planned slot is removed. The course, recorded study history and rewards stay unchanged. Other sessions will not move.")
            when {
                result == null -> Text("Checking this session…")
                proposed == null -> Text(result.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("remove_session_error"))
                else -> {
                    Text("${block.minutes} min return to unscheduled work, not completed study.")
                    Text("Unscheduled course work: ${plan.shortfalls[block.bossId] ?: 0} → ${proposed.shortfalls[block.bossId] ?: 0} min", modifier = Modifier.testTag("remove_unscheduled"))
                }
            }
            if (unavailable) Text("Finish the active session or wait for saving before removing.", color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(onClick = { if (!unavailable) proposed?.let { onSave(it); onDismiss() } }, enabled = !unavailable && proposed != null,
            modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_remove_session")) { Text("Remove session") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !unavailable, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } },
    )
}
