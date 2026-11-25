package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// Boss rendering with customized linear themes
@Composable
fun BossCard(
    boss: BossEntity,
    steps: List<BossStepEntity> = emptyList(),
    activeBoss: BossEntity?,
    isBattleActive: Boolean,
    onFightBoss: () -> Unit,
    onDeleteBoss: () -> Unit,
    onConquerRealBoss: () -> Unit,
    onManageSteps: () -> Unit = {},
    onEdit: (() -> Unit)? = null,
    onOpenSecondGate: (() -> Unit)? = null,
    secondGateContent: (@Composable () -> Unit)? = null,
) {
    val isCurrentlyFightingThis = activeBoss?.id == boss.id
    val difficultyColor = getDifficultyColor(boss.difficulty)
    val backgroundBrush = getBossCardBrush(boss.difficulty)
    val cardBorderColor = if (isCurrentlyFightingThis) NeonBlueAccent else if (boss.isRealBoss) RpgGold else DarkCardBorder

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("boss_item_${boss.name.replace(" ", "_")}")
            .border(
                width = if (isCurrentlyFightingThis || boss.isRealBoss) 2.dp else 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface)
    ) {
        Column {
            // Header Image Box with canvas styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(backgroundBrush)
            ) {
                // Procedural overlay grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = 8
                    val rows = 3
                    val gridW = size.width / cols
                    val gridH = size.height / rows
                    for (i in 1 until cols) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(i * gridW, 0f),
                            end = Offset(i * gridW, size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (i in 1 until rows) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(0f, i * gridH),
                            end = Offset(size.width, i * gridH),
                            strokeWidth = 1f
                        )
                    }
                }

                // Banner Details
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Difficulty Tag & Goal category
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .border(1.dp, difficultyColor.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = boss.difficulty.uppercase(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = difficultyColor
                                    )
                                )
                            }

                            if (boss.isRealBoss) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RpgGold.copy(alpha = 0.3f))
                                        .border(1.dp, RpgGold, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "👑 DELIVERABLE",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = RpgGold
                                        )
                                    )
                                }
                            }
                        }

                        // Completion indicator
                        if (boss.isCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(RpgEmerald.copy(alpha = 0.2f))
                                    .border(1.dp, RpgEmerald, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SLAIN",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = RpgEmerald
                                    )
                                )
                            }
                        }
                    }

                    // Boss Title
                    Column {
                        Text(
                            text = boss.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                letterSpacing = 1.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "REQUIRED: ${boss.requiredMinutes} Mins Focus",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextWhite.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (boss.dungeonName.isNotBlank() && boss.dungeonName != "Main Realm") {
                                Text(
                                    text = "📁 ${boss.dungeonName.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = NeonBlueAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                        boss.deadlineDate
                            ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
                            ?.let { deadline ->
                                Text(
                                    text = "📅 DEADLINE: ${deadline.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = RpgGold,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                    }
                }
            }

            // Lower Info Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Info Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progressSeconds = boss.timeSpentSeconds
                    val totalTargetSeconds = boss.requiredMinutes * 60L
                    val completionPercent = if (totalTargetSeconds > 0) {
                        ((progressSeconds.toFloat() / totalTargetSeconds) * 100).coerceAtMost(100f)
                    } else 0f

                    Column {
                        Text(
                            text = "PROGRESS DAMAGE",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = String.format(LocalLocale.current.platformLocale, "%.1f%% DMG", completionPercent),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = NeonBlueAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "INVESTED TIME",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = formatStudyTimeShort(boss.timeSpentSeconds),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val completedSteps = steps.count { it.isCompleted }
                OutlinedButton(
                    onClick = onManageSteps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("manage_boss_steps_${boss.id}"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (steps.isNotEmpty() && completedSteps == steps.size) RpgEmerald else NeonBlueAccent,
                    ),
                ) {
                    Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (steps.isEmpty()) {
                            "PLAN STUDY STEPS"
                        } else {
                            "STUDY STEPS  $completedSteps / ${steps.size}"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (steps.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { completedSteps.toFloat() / steps.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .semantics {
                                contentDescription = "$completedSteps of ${steps.size} study steps completed"
                            },
                        color = if (completedSteps == steps.size) RpgEmerald else NeonBlueAccent,
                        trackColor = SurfaceElevated,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDeleteBoss,
                        enabled = !isBattleActive,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF231E29)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = RpgRuby)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Move ${boss.name} to Deleted goals"
                        )
                    }
                    if (onEdit != null && !isBattleActive) {
                        IconButton(onClick = onEdit, enabled = !isBattleActive) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit ${boss.name}")
                        }
                    }

                    // MANUAL CONQUEST: If it's a real-life deliverable exam/submission, OR if it's the currently active study boss, show Manual "COMPLETED" bypass
                    if (boss.secondGateOfId == null && (boss.isRealBoss || isCurrentlyFightingThis) && !boss.isCompleted) {
                        Button(
                            onClick = onConquerRealBoss,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF332515), contentColor = RpgGold),
                            border = BorderStroke(1.dp, RpgGold)
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CONQUER", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    if (isCurrentlyFightingThis) {
                        Button(
                            onClick = onFightBoss,
                            colors = ButtonDefaults.buttonColors(containerColor = RpgRuby),
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = "Active battle indicator", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ACTIVE FIGHT", fontWeight = FontWeight.Bold)
                        }
                    } else if (!boss.isCompleted) {
                        Button(
                            onClick = onFightBoss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonBlueAccent,
                                contentColor = BlackFantasyBackground
                            ),
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBattleActive) "SWITCH TO THIS" else "BEGIN DEFY QUEST",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (onOpenSecondGate != null) {
                    OutlinedButton(
                        onClick = onOpenSecondGate,
                        enabled = !isBattleActive,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Open Second Gate") }
                }
                if (secondGateContent != null) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    secondGateContent()
                }
            }
        }
    }
}
private fun getBossCardBrush(difficulty: String): Brush {
    return when (difficulty) {
        "Easy" -> Brush.verticalGradient(listOf(Color(0xFF0D251A), Color(0xFF0F3A22)))
        "Medium" -> Brush.verticalGradient(listOf(Color(0xFF0E1A29), Color(0xFF082747)))
        "Hard" -> Brush.verticalGradient(listOf(Color(0xFF2E1712), Color(0xFF4C150A)))
        "Legendary" -> Brush.verticalGradient(listOf(Color(0xFF20112A), Color(0xFF38104E)))
        else -> Brush.verticalGradient(listOf(Color(0xFF131422), Color(0xFF1B1C2E)))
    }
}
