package com.amkumirab.solostudying.ui.screens

import android.content.pm.ApplicationInfo
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.focuscycle.FocusCyclePhase
import com.amkumirab.solostudying.sound.RpgSoundManager
import com.amkumirab.solostudying.ui.theme.*
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModel
import java.util.*

@Composable
fun BattleTab(
    viewModel: SoloStudyingViewModel,
    onGoToDungeons: () -> Unit
) {
    val boss = viewModel.activeBoss
    val isFreeStudy = viewModel.isFreeStudyActive
    var showTestControls by remember { mutableStateOf(false) }
    val isDebugBuild = LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    if (boss == null && !isFreeStudy) {
        // No Quest Empty State
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    tint = NeonBlueAccent.copy(alpha = 0.25f),
                    contentDescription = "Empty battle portrait",
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NO QUEST UNDERWAY",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        letterSpacing = 1.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter a dungeon challenge to summon and duel an academic boss, or step through the Astral Portal for casual study!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onGoToDungeons,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlueAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Enter Dungeons", color = BlackFantasyBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // Battle Screen Layout - supporting both Boss and Casual Meditation
        val targetSeconds = if (isFreeStudy || viewModel.activeBossStepId != null) {
            (viewModel.battleTimeLeftSeconds + viewModel.battleTimeSpentSeconds).coerceAtLeast(60L)
        } else {
            (boss?.requiredMinutes ?: 30) * 60L
        }
        val remainingProgressSeconds = if (viewModel.isTimedBossSession) {
            (targetSeconds - viewModel.currentBossProgressSeconds).coerceAtLeast(0L)
        } else viewModel.battleTimeLeftSeconds
        val hpPercent = if (targetSeconds > 0) {
            (remainingProgressSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
        } else 0f
        val damagePercent = 1f - hpPercent

        val categoryText = if (isFreeStudy) {
            if (viewModel.activeDailyQuestId != null) "DAILY QUEST FOCUS" else "ASTRAL PLANE FOCUS"
        } else if (viewModel.activeBossStepId != null) {
            "STUDY STEP · ${(boss?.name ?: "Goal").uppercase()}"
        } else {
            "DUNGEON: ${(boss?.dungeonName ?: "Main Realm").uppercase()}"
        }

        val nameText = if (isFreeStudy) {
            viewModel.activeDailyQuestTitle?.uppercase() ?: "ASTRAL FREE STUDY ZONE"
        } else if (viewModel.activeBossStepId != null) {
            viewModel.activeBossStepTitle?.uppercase() ?: boss?.name?.uppercase().orEmpty()
        } else {
            boss?.name?.uppercase() ?: ""
        }

        val requiredMins = if (isFreeStudy || viewModel.activeBossStepId != null) {
            (targetSeconds / 60).toInt()
        } else {
            boss?.requiredMinutes ?: 30
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                viewModel.focusCycleState?.takeIf { it.phase == FocusCyclePhase.FOCUS }?.let { cycle ->
                    Surface(
                        color = RpgGold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, RpgGold.copy(alpha = 0.75f)),
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .testTag("focus_cycle_round"),
                    ) {
                        Text(
                            text = "ROUND ${cycle.currentRound} OF ${cycle.plan.totalRounds}",
                            color = RpgGold,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
                if (viewModel.isFocusShieldActive) {
                    Surface(
                        color = RpgEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, RpgEmerald.copy(alpha = 0.75f)),
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .testTag("focus_shield_active_badge"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.DoNotDisturbOn,
                                contentDescription = null,
                                tint = RpgEmerald,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "FOCUS SHIELD ACTIVE",
                                color = RpgEmerald,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                            )
                        }
                    }
                }
                Text(
                    text = when {
                        viewModel.activeDailyQuestId != null -> "📜 ACTIVE DAILY QUEST"
                        viewModel.activeBossStepId != null -> "🧭 ACTIVE STUDY STEP"
                        isFreeStudy -> "🌌 CASUAL MEDITATION PORTAL"
                        else -> "⚔️ ACTIVE BOSS ENCOUNTER"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isFreeStudy) NeonBlueAccent else RpgRuby,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = nameText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )

                // Trained Skill indicator
                viewModel.selectedSkillToTrain?.let { sk ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonBlueAccent.copy(alpha = 0.15f))
                            .border(1.dp, NeonBlueAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🎯 TRAINING ACADEMY SKILL: ${sk.name.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonBlueAccent,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Animated glowing landscape for active fight - THE RITUAL BATTLE RING
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Frame containing radial ambient glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        (if (isFreeStudy) NeonBlueAccent else RpgRuby).copy(alpha = 0.08f),
                                        Color.Transparent
                                    ),
                                    radius = 350f
                                )
                            )
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        ) {
                            // Boss Ritual Ring layout
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // 1. Dashed Ring on Canvas
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = (if (isFreeStudy) NeonBlueAccent else RpgRuby).copy(alpha = 0.15f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                        )
                                    )
                                }
                                // 2. Inner thin Solid Ring
                                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                    drawCircle(
                                        color = (if (isFreeStudy) NeonBlueAccent else RpgRuby).copy(alpha = 0.25f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                    )
                                }
                                // Active boss portal
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(18.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(SurfaceElevated, SurfaceSubtle)
                                            )
                                        )
                                        .border(1.dp, StrongCardBorder, RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isFreeStudy) {
                                        // Meditative vortex icon
                                        Icon(
                                            imageVector = Icons.Default.Cyclone,
                                            contentDescription = null,
                                            tint = NeonBlueAccent,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    } else {
                                        // Three glowing neon pillar lines representing the Machine Learning Hydra core
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(6.dp)
                                                    .height(40.dp)
                                                    .clip(CircleShape)
                                                    .background(RpgRuby)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(6.dp)
                                                    .height(56.dp)
                                                    .clip(CircleShape)
                                                    .background(RpgRuby)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(6.dp)
                                                    .height(40.dp)
                                                    .clip(CircleShape)
                                                    .background(RpgRuby)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background((if (isFreeStudy) NeonBlueAccent else getDifficultyColor(boss?.difficulty ?: "Medium")).copy(alpha = 0.15f))
                                    .border(1.dp, (if (isFreeStudy) NeonBlueAccent else getDifficultyColor(boss?.difficulty ?: "Medium")).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isFreeStudy) "ASTRAL CHILL" else "LEVEL ${boss?.difficulty?.uppercase() ?: "MEDIUM"}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isFreeStudy) NeonBlueAccent else getDifficultyColor(boss?.difficulty ?: "Medium"),
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Health/Energy Bar of representation
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (isFreeStudy) "SOUL RESONANCE" else "BOSS VITALITY",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFreeStudy) NeonBlueAccent else RpgRuby,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (isFreeStudy) {
                                "${(viewModel.battleTimeLeftSeconds / 60) + 1} / $requiredMins MINS"
                            } else {
                                "${(remainingProgressSeconds + 59) / 60} / ${boss?.requiredMinutes ?: 30} HP"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceInteractive)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        // Health sliding Red to Cyan Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(hpPercent)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        if (isFreeStudy) listOf(Color(0xFF2B5C8F), NeonBlueAccent) else listOf(RpgRuby, NeonBlueAccent)
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = categoryText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                              )
                        )
                        Text(
                            text = if (isFreeStudy) {
                                "FOCUSED: ${viewModel.battleTimeSpentSeconds}S"
                            } else {
                                "DEALT ${String.format(LocalLocale.current.platformLocale, "%.1f", damagePercent * 100f)}% DAMAGE"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFreeStudy) NeonBlueAccent else RpgRuby,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Battle Timer & Active Progress
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceSubtle)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (viewModel.isTimedBossSession) {
                            Text("SESSION TIME LEFT", color = NeonBlueAccent, style = MaterialTheme.typography.labelLarge)
                            Text(
                                "Whole goal: ${formatCountdown(viewModel.currentBossProgressSeconds)} / ${formatCountdown(targetSeconds)}",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp).testTag("goal_session_progress"),
                            )
                        }
                        Text(
                            text = if (viewModel.isBattlePaused) "ENGAGED - FIGHT PAUSED" else "ENGAGED IN COMBAT",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonBlueAccent,
                                letterSpacing = 3.sp,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Massive tab-digit font-mono countdown text
                        Text(
                            text = formatCountdown(viewModel.battleTimeLeftSeconds),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = TextWhite,
                                fontSize = 56.sp,
                                letterSpacing = (-1).sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "STUDIED SECONDS: ${viewModel.battleTimeSpentSeconds}S",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RpgGold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Real Interactive Controls
            item {
                StrictFocusControl(
                    isPaused = viewModel.isBattlePaused,
                    isRequested = viewModel.strictFocusRequested,
                    isPinned = viewModel.isStrictFocusPinned,
                    onEnable = viewModel::requestStrictFocus,
                )
            }

            item {
                BattleActionControls(
                    isPaused = viewModel.isBattlePaused,
                    isFreeStudy = isFreeStudy,
                    isStudyStep = viewModel.activeBossStepId != null || viewModel.isTimedBossSession,
                    onPause = {
                        RpgSoundManager.playClickSound()
                        viewModel.pauseBattle()
                    },
                    onResume = {
                        RpgSoundManager.playClickSound()
                        viewModel.resumeBattle()
                    },
                    onFinish = {
                        viewModel.completeActiveBoss()
                    },
                    onRetreat = {
                        RpgSoundManager.playClickSound()
                        viewModel.abandonActiveBoss()
                    },
                )
            }

            if (isDebugBuild) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { showTestControls = !showTestControls },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .testTag("toggle_test_controls"),
                        ) {
                            Icon(
                                imageVector = if (showTestControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSubtle,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (showTestControls) "HIDE TEST CONTROLS" else "SHOW TEST CONTROLS",
                                color = TextSubtle,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        AnimatedVisibility(visible = showTestControls) {
                            BattleTestControls(
                                onAddFiveMinutes = { viewModel.simulateStudySeconds(300L) },
                                onAddTwentyMinutes = { viewModel.simulateStudySeconds(1200L) },
                                onComplete = viewModel::completeActiveBoss,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StrictFocusControl(
    isPaused: Boolean,
    isRequested: Boolean,
    isPinned: Boolean,
    onEnable: () -> Unit,
) {
    var showConfirmation by remember { mutableStateOf(false) }

    if (isRequested) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("strict_focus_status"),
            colors = CardDefaults.cardColors(containerColor = RpgEmerald.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, RpgEmerald.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = RpgEmerald,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isPinned) "STRICT FOCUS ACTIVE" else "CONFIRM SCREEN PINNING",
                        color = RpgEmerald,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (isPinned) {
                            "This study session is pinned. Pausing or finishing will release it."
                        } else {
                            "Complete the Android confirmation to lock into this session."
                        },
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    } else if (!isPaused) {
        OutlinedButton(
            onClick = { showConfirmation = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("enable_strict_focus_button")
                .semantics {
                    contentDescription = "Enable Strict Focus for this session"
                },
            border = BorderStroke(1.dp, RpgGold.copy(alpha = 0.75f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RpgGold),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("ENABLE STRICT FOCUS", fontWeight = FontWeight.Black)
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = RpgGold) },
            title = { Text("LOCK INTO THIS SESSION?", color = TextWhite, fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "Android will pin Solo Studying to the screen so accidental Home and Recent Apps actions cannot interrupt this session. You can still use Android's emergency unpin gesture, and pausing or ending the session releases the lock.",
                    color = TextMuted,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmation = false
                        onEnable()
                    },
                    modifier = Modifier.testTag("confirm_strict_focus_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RpgGold,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text("CONTINUE", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("NOT NOW", color = TextMuted)
                }
            },
            containerColor = DarkFantasySurface,
        )
    }
}

@Composable
fun BattleActionControls(
    isPaused: Boolean,
    isFreeStudy: Boolean,
    isStudyStep: Boolean = false,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onRetreat: () -> Unit,
) {
    val finishAccent = if (isFreeStudy) RpgEmerald else RpgGold
    val finishContainer = if (isFreeStudy) SuccessContainer else WarningContainer

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = if (isPaused) onResume else onPause,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPaused) NeonBlueAccent else InfoContainer,
                contentColor = if (isPaused) OnAccent else TextWhite,
            ),
            border = BorderStroke(1.dp, NeonBlueAccent.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("battle_pause_resume_button"),
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isPaused) "RESUME TIMER" else "PAUSE TIMER",
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = finishContainer,
                    contentColor = finishAccent,
                ),
                border = BorderStroke(1.dp, finishAccent.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag("battle_finish_button"),
            ) {
                Icon(
                    imageVector = if (isFreeStudy || isStudyStep) {
                        Icons.Default.CheckCircleOutline
                    } else {
                        Icons.Default.Celebration
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        isStudyStep -> "END EARLY"
                        isFreeStudy -> "END SESSION"
                        else -> "END EARLY"
                    },
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }

            Button(
                onClick = onRetreat,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerContainer,
                    contentColor = RpgRuby,
                ),
                border = BorderStroke(1.dp, RpgRuby.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag("battle_retreat_button"),
            ) {
                Icon(Icons.Default.OutlinedFlag, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("RETREAT", fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BattleTestControls(
    onAddFiveMinutes: () -> Unit,
    onAddTwentyMinutes: () -> Unit,
    onComplete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("battle_test_controls"),
        colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
        border = BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "DEVELOPER TEST CONTROLS",
                color = TextSubtle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onAddFiveMinutes,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InfoContainer,
                        contentColor = NeonBlueAccent,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+5 MIN", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Button(
                    onClick = onAddTwentyMinutes,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceInteractive,
                        contentColor = Color(0xFFE9B7FF),
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+20 MIN", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessContainer,
                        contentColor = RpgEmerald,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("COMPLETE", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
private fun formatCountdown(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}
