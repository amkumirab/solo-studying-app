package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.data.entity.SkillEntity
import com.amkumirab.solostudying.quickstart.QUICK_START_PRESET_MINUTES
import com.amkumirab.solostudying.quickstart.QuickStartSelection
import com.amkumirab.solostudying.ui.theme.BlackFantasyBackground
import com.amkumirab.solostudying.ui.theme.DarkCardBorder
import com.amkumirab.solostudying.ui.theme.DarkFantasySurface
import com.amkumirab.solostudying.ui.theme.NeonBlueAccent
import com.amkumirab.solostudying.ui.theme.RpgGold
import com.amkumirab.solostudying.ui.theme.TextMuted
import com.amkumirab.solostudying.ui.theme.TextWhite

@Composable
fun QuickStartCard(
    skills: List<SkillEntity>,
    selection: QuickStartSelection,
    isSessionActive: Boolean,
    onSelectionChange: (QuickStartSelection) -> Unit,
    onStart: (QuickStartSelection) -> Unit,
    onCustomDuration: () -> Unit,
    onFocusCycles: () -> Unit = {},
) {
    val selectedSkill = skills.firstOrNull { it.id == selection.skillId }
    val focusName = selectedSkill?.name ?: "General Focus"
    var skillMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_start_card")
            .semantics { isTraversalGroup = true },
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
        border = BorderStroke(1.5.dp, NeonBlueAccent.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = RpgGold,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "QUICK START",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = "Start a focused session in one tap",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Icon(Icons.Default.Timer, contentDescription = null, tint = NeonBlueAccent)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QUICK_START_PRESET_MINUTES.forEach { minutes ->
                    val isSelected = selection.durationMinutes == minutes
                    OutlinedButton(
                        onClick = { onSelectionChange(selection.copy(durationMinutes = minutes)) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("quick_start_duration_$minutes")
                            .semantics {
                                selected = isSelected
                                stateDescription = if (isSelected) "Selected" else "Not selected"
                                contentDescription = "$minutes minute focus preset"
                            },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) NeonBlueAccent else Color.Transparent,
                            contentColor = if (isSelected) BlackFantasyBackground else TextWhite,
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeonBlueAccent else DarkCardBorder,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("$minutes MIN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                OutlinedButton(
                    onClick = onCustomDuration,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("quick_start_custom"),
                    border = BorderStroke(1.dp, RpgGold.copy(alpha = 0.75f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RpgGold),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("CUSTOM", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .background(Color(0xFF101521), RoundedCornerShape(10.dp))
                        .clickable { skillMenuExpanded = true }
                        .testTag("quick_start_skill_selector")
                        .semantics {
                            contentDescription = "Skill focus, $focusName"
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SKILL FOCUS",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = focusName,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(Icons.Default.ExpandMore, contentDescription = null, tint = NeonBlueAccent)
                }

                DropdownMenu(
                    expanded = skillMenuExpanded,
                    onDismissRequest = { skillMenuExpanded = false },
                    modifier = Modifier.background(DarkFantasySurface),
                ) {
                    DropdownMenuItem(
                        text = { Text("General Focus", color = TextWhite) },
                        onClick = {
                            onSelectionChange(selection.copy(skillId = null))
                            skillMenuExpanded = false
                        },
                        modifier = Modifier.testTag("quick_start_skill_none"),
                    )
                    skills.forEach { skill ->
                        DropdownMenuItem(
                            text = { Text(skill.name, color = TextWhite) },
                            onClick = {
                                onSelectionChange(selection.copy(skillId = skill.id))
                                skillMenuExpanded = false
                            },
                            modifier = Modifier.testTag("quick_start_skill_${skill.id}"),
                        )
                    }
                }
            }

            Button(
                onClick = { onStart(selection) },
                enabled = !isSessionActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("quick_start_button")
                    .semantics {
                        contentDescription = if (isSessionActive) {
                            "A focus session is already active"
                        } else {
                            "Start ${selection.durationMinutes} minute focus session for $focusName"
                        }
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonBlueAccent,
                    contentColor = BlackFantasyBackground,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isSessionActive) "SESSION ACTIVE" else "START ${selection.durationMinutes} MIN FOCUS",
                    fontWeight = FontWeight.Black,
                )
            }

            OutlinedButton(
                onClick = onFocusCycles,
                enabled = !isSessionActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("focus_cycles_button"),
                border = BorderStroke(1.dp, RpgGold.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RpgGold),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Default.Repeat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("FOCUS CYCLES", fontWeight = FontWeight.Black)
            }
        }
    }
}
