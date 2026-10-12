package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.sound.RpgSoundManager

@Composable
fun RPGBottomBar(currentTab: Tab, onTabSelected: (Tab) -> Unit, isBattleActive: Boolean) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0),
        modifier = Modifier.semantics { isTraversalGroup = true },
    ) {
        listOf(Tab.Today, Tab.Dungeons, Tab.Shop, Tab.Stats).forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = currentTab == tab || (currentTab == Tab.Battle && tab == Tab.Dungeons),
                onClick = { RpgSoundManager.playClickSound(); onTabSelected(tab) },
                modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}").semantics {
                    contentDescription = tab.title
                    traversalIndex = index.toFloat()
                    if (tab == Tab.Dungeons && isBattleActive) stateDescription = "Study session active"
                },
                icon = { Icon(tab.icon, null, Modifier.size(22.dp)) },
                label = { Text(tab.title, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
internal fun AppHeader(
    title: String, playerSummary: String, onSettings: () -> Unit,
    onBack: (() -> Unit)? = null, modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("app_header_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Text(playerSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettings, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("open_app_settings")) {
                Icon(Icons.Default.Settings, "Settings")
            }
        }
    }
}

@Composable
internal fun ActiveSessionBar(
    paused: Boolean, title: String, onReturn: () -> Unit, modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onReturn, color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("return_to_session"),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Timer, null, Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(if (paused) "Session paused" else "Session in progress", style = MaterialTheme.typography.labelLarge)
                Text(title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Return", style = MaterialTheme.typography.labelLarge)
        }
    }
}
