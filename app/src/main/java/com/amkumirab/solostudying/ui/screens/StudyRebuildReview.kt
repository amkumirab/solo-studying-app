package com.amkumirab.solostudying.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class StudyRebuildDraft(val snapshot: PlannerSnapshot, val bosses: List<BossEntity>, val today: LocalDate,
    val blocked: String?, val result: LockedStudyRebuild, val comparison: StudyPlanComparison?)

@Composable
internal fun StudyRebuildReview(
    snapshot: PlannerSnapshot, bosses: List<BossEntity>, today: LocalDate, busy: Boolean,
    onCancel: () -> Unit, onEditAnswers: () -> Unit, onApply: (StudyPlan, PlannerSnapshot) -> Unit,
    modifier: Modifier = Modifier, blockedMessage: String? = null, isSessionActive: Boolean = false,
) {
    val preview by produceState<StudyRebuildDraft?>(null, snapshot, bosses, today, blockedMessage) {
        value = null
        value = withContext(Dispatchers.Default) {
            val result = if (blockedMessage == null) previewStudyRebuild(snapshot, bosses, today) else LockedStudyRebuild(null, blockedMessage)
            StudyRebuildDraft(snapshot, bosses, today, blockedMessage, result,
                result.plan?.let { compareStudyPlans(checkNotNull(snapshot.plan), it) })
        }
    }
    val ready = preview?.takeIf { it.snapshot == snapshot && it.bosses == bosses && it.today == today && it.blocked == blockedMessage }
    val candidate = ready?.result?.plan
    val applicable = candidate?.takeIf { !busy && !isSessionActive && it.shortfalls.isEmpty() }
    BackHandler { if (!busy) onCancel() }
    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Review schedule changes", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text("Your accepted plan stays unchanged until you confirm. Locked slots keep their exact timing.")
            if (isSessionActive) Text("Finish the active study session before applying changes.", color = MaterialTheme.colorScheme.error)
            if (ready == null) Text("Calculating changes…", modifier = Modifier.testTag("rebuild_comparison_loading"))
            else {
                Text(if (ready.comparison == null) "No applicable proposal" else "Comparison ready",
                    modifier = Modifier.testTag("rebuild_comparison_ready").semantics { liveRegion = LiveRegionMode.Polite })
                ready.result.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("rebuild_comparison_error")) }
                ready.comparison?.let { StudyPlanComparisonContent(it, bosses, snapshot.subjects, today) }
                if (candidate?.shortfalls?.isNotEmpty() == true) Text("Remaining work no longer fits your schedule. Edit answers to review capacity warnings. Your current plan is unchanged.",
                    color = MaterialTheme.colorScheme.error)
                if (blockedMessage == null) OutlinedButton(onClick = onEditAnswers, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("rebuild_edit_answers")) { Text("Edit answers") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, enabled = !busy,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("cancel_rebuild_comparison")) { Text("Cancel") }
            Button(onClick = { applicable?.let { onApply(it, snapshot) } }, enabled = applicable != null,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("confirm_rebuild_comparison")) { Text(if (busy) "Saving…" else "Confirm") }
        }
    }
}
