package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.data.entity.SkillEntity
import com.amkumirab.solostudying.focusprofile.FocusProfile
import com.amkumirab.solostudying.focusprofile.FocusProfileType
import com.amkumirab.solostudying.ui.theme.*

@Composable
fun FocusProfilesCard(
    profiles: List<FocusProfile>,
    skills: List<SkillEntity>,
    isSessionActive: Boolean,
    focusShieldHasAccess: Boolean,
    onStart: (FocusProfile) -> Unit,
    onSave: (
        existingId: String?,
        name: String,
        type: FocusProfileType,
        focusMinutes: Int,
        breakMinutes: Int,
        rounds: Int,
        skillId: Int?,
        useFocusShield: Boolean,
        useStrictFocus: Boolean,
    ) -> Unit,
    onDelete: (FocusProfile) -> Unit,
) {
    var editingProfile by remember { mutableStateOf<FocusProfile?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("focus_profiles_card"),
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
        border = BorderStroke(1.5.dp, RpgGold.copy(alpha = 0.65f)),
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
                Column(Modifier.weight(1f)) {
                    Text(
                        "FOCUS PROFILES",
                        color = RpgGold,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "Save complete study setups for one-tap starts",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                FilledTonalButton(
                    onClick = {
                        editingProfile = null
                        showEditor = true
                    },
                    modifier = Modifier.testTag("add_focus_profile"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = RpgGold.copy(alpha = 0.14f),
                        contentColor = RpgGold,
                    ),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("NEW", fontWeight = FontWeight.Bold)
                }
            }

            if (profiles.isEmpty()) {
                Text(
                    "Create a profile for routines such as COMSOL deep work, exam review, or a quick revision.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C1320), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                )
            } else {
                profiles.forEach { profile ->
                    FocusProfileRow(
                        profile = profile,
                        skillName = skills.firstOrNull { it.id == profile.skillId }?.name,
                        isSessionActive = isSessionActive,
                        onStart = { onStart(profile) },
                        onEdit = {
                            editingProfile = profile
                            showEditor = true
                        },
                        onDelete = { onDelete(profile) },
                    )
                }
            }
        }
    }

    if (showEditor) {
        FocusProfileEditorDialog(
            profile = editingProfile,
            skills = skills,
            focusShieldHasAccess = focusShieldHasAccess,
            onDismiss = { showEditor = false },
            onSave = { name, type, focus, breakMinutes, rounds, skillId, shield, strict ->
                onSave(
                    editingProfile?.id,
                    name,
                    type,
                    focus,
                    breakMinutes,
                    rounds,
                    skillId,
                    shield,
                    strict,
                )
                showEditor = false
            },
        )
    }
}

@Composable
private fun FocusProfileRow(
    profile: FocusProfile,
    skillName: String?,
    isSessionActive: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1320)),
        border = BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (profile.type == FocusProfileType.CYCLE) Icons.Default.Repeat else Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (profile.type == FocusProfileType.CYCLE) RpgGold else NeonBlueAccent,
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        profile.name,
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildString {
                            append("${profile.focusMinutes} min focus")
                            if (profile.type == FocusProfileType.CYCLE) {
                                append(" · ${profile.breakMinutes} min break · ${profile.rounds} rounds")
                            }
                            append(" · ${skillName ?: "General Focus"}")
                        },
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_focus_profile_${profile.id}").semantics {
                        contentDescription = "Edit ${profile.name} focus profile"
                    },
                ) { Icon(Icons.Default.Edit, contentDescription = null, tint = TextMuted) }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_focus_profile_${profile.id}").semantics {
                        contentDescription = "Delete ${profile.name} focus profile"
                    },
                ) { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RpgRuby) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (profile.useFocusShield) ProfileBadge("SHIELD", Icons.Default.DoNotDisturbOn, RpgEmerald)
                if (profile.useStrictFocus) ProfileBadge("STRICT", Icons.Default.Lock, RpgGold)
            }

            Button(
                onClick = onStart,
                enabled = !isSessionActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("start_focus_profile_${profile.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonBlueAccent,
                    contentColor = BlackFantasyBackground,
                ),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (isSessionActive) "SESSION ACTIVE" else "START PROFILE", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun RowScope.ProfileBadge(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun FocusProfileEditorDialog(
    profile: FocusProfile?,
    skills: List<SkillEntity>,
    focusShieldHasAccess: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, FocusProfileType, Int, Int, Int, Int?, Boolean, Boolean) -> Unit,
) {
    var name by remember(profile?.id) { mutableStateOf(profile?.name.orEmpty()) }
    var type by remember(profile?.id) { mutableStateOf(profile?.type ?: FocusProfileType.SINGLE) }
    var focusInput by remember(profile?.id) { mutableStateOf((profile?.focusMinutes ?: 25).toString()) }
    var breakInput by remember(profile?.id) { mutableStateOf((profile?.breakMinutes ?: 5).toString()) }
    var roundsInput by remember(profile?.id) { mutableStateOf((profile?.rounds ?: 4).toString()) }
    var skillId by remember(profile?.id) { mutableStateOf(profile?.skillId) }
    var useShield by remember(profile?.id) { mutableStateOf(profile?.useFocusShield ?: false) }
    var useStrict by remember(profile?.id) { mutableStateOf(profile?.useStrictFocus ?: false) }
    var skillMenuExpanded by remember { mutableStateOf(false) }
    val focusMinutes = focusInput.toIntOrNull()
    val breakMinutes = breakInput.toIntOrNull()
    val rounds = roundsInput.toIntOrNull()
    val isValid = name.trim().length in 1..FocusProfile.MAX_NAME_LENGTH &&
        focusMinutes != null && focusMinutes in 1..480 &&
        breakMinutes != null && breakMinutes in 1..60 &&
        rounds != null && rounds in 2..12

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp).testTag("focus_profile_editor"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111A2C)),
            border = BorderStroke(2.dp, RpgGold),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (profile == null) "CREATE FOCUS PROFILE" else "EDIT FOCUS PROFILE",
                    color = RpgGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    modifier = Modifier.semantics { heading() },
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(FocusProfile.MAX_NAME_LENGTH) },
                    label = { Text("Profile name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("focus_profile_name"),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusProfileType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(if (option == FocusProfileType.SINGLE) "SINGLE" else "CYCLE") },
                            leadingIcon = {
                                Icon(
                                    if (option == FocusProfileType.SINGLE) Icons.Default.Timer else Icons.Default.Repeat,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("focus_profile_type_${option.name.lowercase()}"),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileNumberField("FOCUS", focusInput, { focusInput = digits(it) }, Modifier.weight(1f))
                    if (type == FocusProfileType.CYCLE) {
                        ProfileNumberField("BREAK", breakInput, { breakInput = digits(it) }, Modifier.weight(1f))
                        ProfileNumberField("ROUNDS", roundsInput, { roundsInput = digits(it) }, Modifier.weight(1f))
                    }
                }

                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A1220), RoundedCornerShape(10.dp))
                            .clickable { skillMenuExpanded = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("SKILL FOCUS", color = TextMuted, fontSize = 10.sp)
                            Text(
                                skills.firstOrNull { it.id == skillId }?.name ?: "General Focus",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
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
                            onClick = { skillId = null; skillMenuExpanded = false },
                        )
                        skills.forEach { skill ->
                            DropdownMenuItem(
                                text = { Text(skill.name, color = TextWhite) },
                                onClick = { skillId = skill.id; skillMenuExpanded = false },
                            )
                        }
                    }
                }

                ProfileOptionRow(
                    title = "FOCUS SHIELD",
                    description = if (focusShieldHasAccess) {
                        "Silence interruptions for this profile only"
                    } else {
                        "Requires Do Not Disturb access in System Controls"
                    },
                    icon = Icons.Default.DoNotDisturbOn,
                    checked = useShield,
                    onCheckedChange = { useShield = it },
                    testTag = "focus_profile_shield",
                )
                ProfileOptionRow(
                    title = "STRICT FOCUS",
                    description = "Request Android screen pinning when this profile starts",
                    icon = Icons.Default.Lock,
                    checked = useStrict,
                    onCheckedChange = { useStrict = it },
                    testTag = "focus_profile_strict",
                )

                Button(
                    onClick = {
                        onSave(name.trim(), type, focusMinutes!!, breakMinutes!!, rounds!!, skillId, useShield, useStrict)
                    },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_focus_profile"),
                    colors = ButtonDefaults.buttonColors(containerColor = RpgGold, contentColor = Color.Black),
                ) { Text("SAVE PROFILE", fontWeight = FontWeight.Black) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCEL", color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun ProfileNumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 10.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun ProfileOptionRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = if (checked) RpgGold else TextMuted)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextWhite, fontWeight = FontWeight.Bold)
            Text(description, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}

private fun digits(value: String): String = value.filter(Char::isDigit).take(3)
