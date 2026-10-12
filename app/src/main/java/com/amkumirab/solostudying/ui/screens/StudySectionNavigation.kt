package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class ProfileSection(val title: String) {
    Overview("Overview"), Skills("Skills"), History("History"), Settings("Settings"),
}

@Composable
internal fun ProfileSectionNavigation(
    section: ProfileSection, onSelected: (ProfileSection) -> Unit, modifier: Modifier = Modifier,
) {
    if (section == ProfileSection.Settings) {
        Row(modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSelected(ProfileSection.Overview) }, modifier = Modifier.size(48.dp).testTag("back_to_profile")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to profile")
            }
            Text("Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        }
    } else {
        Row(
            modifier.fillMaxWidth().padding(horizontal = 12.dp)
                .testTag("profile_sections"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ProfileSection.Overview, ProfileSection.Skills, ProfileSection.History).forEach { destination ->
                    FilterChip(
                        selected = section == destination, onClick = { onSelected(destination) },
                        label = { Text(destination.title) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("profile_${destination.name.lowercase()}"),
                    )
                }
            }
            IconButton(onClick = { onSelected(ProfileSection.Settings) }, modifier = Modifier.size(48.dp).testTag("profile_settings")) {
                Icon(Icons.Default.Settings, "Settings")
            }
        }
    }
}

@Composable
internal fun ExpandableStudySection(
    title: String, summary: String, modifier: Modifier = Modifier,
    tag: String = "daily_tasks_toggle",
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedCard(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(tag),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (expanded) "Collapse $title" else "Expand $title")
            }
        }
        if (expanded) content()
    }
}
