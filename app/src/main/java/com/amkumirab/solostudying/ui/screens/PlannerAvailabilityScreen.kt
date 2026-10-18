package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.planner.encodeStudyPlan
import com.amkumirab.solostudying.planner.encodePlannerSubjects
import com.amkumirab.solostudying.planner.encodeWeeklyAvailability
import java.security.MessageDigest

private fun PlannerSnapshot.availabilityDraftIdentity(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    listOf(plan?.let(::encodeStudyPlan).orEmpty(), encodePlannerSubjects(subjects),
        availability?.let(::encodeWeeklyAvailability).orEmpty()).forEach {
        digest.update(it.toByteArray(Charsets.UTF_8))
        digest.update(0.toByte())
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

@Composable
internal fun PlannerAvailabilityScreen(
    snapshot: PlannerSnapshot, weekdayDefaults: List<Int>, busy: Boolean, error: String?, unreadable: Boolean,
    onSave: (WeeklyAvailability, PlannerSnapshot, () -> Unit) -> Unit, onReset: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var resetting by rememberSaveable { mutableStateOf(false) }
    if (editing && !unreadable) {
        val expected = remember { snapshot }
        // Keep a small baseline token with the restored draft, not the entire accepted plan.
        val identity = rememberSaveable { expected.availabilityDraftIdentity() }
        var conflict by rememberSaveable { mutableStateOf(false) }
        WeeklyAvailabilityEditor(expected.availability ?: defaultWeeklyAvailability(List(7) { weekdayDefaults.getOrNull(it)?.coerceIn(0, 480) ?: 60 }),
            suggested = expected.availability == null, busy,
            error = if (conflict) "Saved settings changed while this draft was closed. Cancel and reopen the editor before saving." else error,
            onSave = {
                if (identity != expected.availabilityDraftIdentity()) conflict = true
                else onSave(it, expected) { editing = false }
            }, onCancel = { editing = false }, modifier = modifier)
    } else Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Weekly availability", style = MaterialTheme.typography.headlineSmall)
        Text("Choose when you can study, in local 24-hour time. Up to 8 hours per day.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (unreadable) OutlinedButton(onClick = { resetting = true }, enabled = !busy,
            modifier = Modifier.heightIn(min = 48.dp)) { Text("Reset saved availability") }
        else {
            Button(onClick = { editing = true }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("edit_availability")) { Text(if (snapshot.availability == null) "Set availability" else "Edit availability") }
            snapshot.availability?.let { AvailabilityWeekSummary(it) } ?: Text("No clock windows saved. Your existing daily budgets stay unchanged.")
            Text("Saved windows set daily capacity when you edit or rebuild. Session start/end times come in the next scheduling update.", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (resetting) AlertDialog(onDismissRequest = { if (!busy) resetting = false },
        title = { Text("Reset saved availability?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Remove only unreadable availability settings. Your accepted plan, subjects, goals and study history are kept.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(onClick = { onReset { resetting = false } }, enabled = !busy) { Text("Reset availability") } },
        dismissButton = { TextButton(onClick = { resetting = false }, enabled = !busy) { Text("Cancel") } })
}
