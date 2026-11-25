package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.domain.session.ProgressSummary
import com.amkumirab.solostudying.domain.session.SessionEndState
import com.amkumirab.solostudying.domain.session.SessionSummary
import com.amkumirab.solostudying.focuscycle.FocusCyclePhase
import com.amkumirab.solostudying.ui.theme.*
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModel

@Composable
fun NotificationOverlays(
    viewModel: SoloStudyingViewModel,
    onStartAnotherSession: () -> Unit = {},
) {
    var showBreakDurationDialog by remember { mutableStateOf(false) }
    var sessionNoteDraft by remember { mutableStateOf("") }
    val activeSummaryId = viewModel.sessionSummary?.sessionId
    val focusCycle = viewModel.focusCycleState

    LaunchedEffect(activeSummaryId) {
        sessionNoteDraft = ""
    }

    viewModel.activeBreak?.let { activeBreak ->
        BreakTimerDialog(
            durationSeconds = activeBreak.durationSeconds,
            remainingSeconds = viewModel.breakTimeLeftSeconds,
            onSkipBreak = {
                viewModel.skipBreak()
                if (focusCycle == null) onStartAnotherSession()
            },
        )
        return
    }

    if (focusCycle?.phase == FocusCyclePhase.READY_FOR_FOCUS) {
        BreakCompleteDialog(
            message = "Recovery complete. Round ${focusCycle.nextRound} of ${focusCycle.plan.totalRounds} is ready when you are.",
            startButtonLabel = "START ROUND ${focusCycle.nextRound} OF ${focusCycle.plan.totalRounds}",
            doneButtonLabel = "END CYCLE",
            onDone = viewModel::cancelFocusCycle,
            onStartNextSession = {
                viewModel.startNextCycleRound()
                onStartAnotherSession()
            },
        )
        return
    }

    if (viewModel.showBreakComplete) {
        BreakCompleteDialog(
            onDone = viewModel::dismissBreakComplete,
            onStartNextSession = {
                viewModel.dismissBreakComplete()
                onStartAnotherSession()
            },
        )
        return
    }

    if (showBreakDurationDialog) {
        BreakDurationDialog(
            onDismiss = { showBreakDurationDialog = false },
            onStartBreak = { minutes ->
                viewModel.dismissSessionSummary()
                viewModel.startBreak(minutes)
                showBreakDurationDialog = false
            },
        )
        return
    }

    viewModel.sessionSummary?.let { summary ->
        val isCycleRound = focusCycle?.phase == FocusCyclePhase.READY_FOR_BREAK
        val isCycleComplete = focusCycle?.phase == FocusCyclePhase.COMPLETE
        SessionSummaryDialog(
            summary = summary,
            note = sessionNoteDraft,
            onNoteChange = { sessionNoteDraft = it },
            onSaveNote = { note -> viewModel.updateSessionNote(summary.sessionId, note) },
            onDone = viewModel::dismissSessionSummary,
            showBreakOption = if (isCycleRound) true else viewModel.breakSuggestionsEnabled &&
                summary.endState != SessionEndState.Suspended && !isCycleComplete,
            breakButtonLabel = if (isCycleRound) {
                "START ${focusCycle.plan.breakMinutes} MIN BREAK"
            } else {
                "TAKE A BREAK"
            },
            showStartAnotherOption = !isCycleRound && !isCycleComplete,
            onTakeBreak = {
                if (isCycleRound) {
                    viewModel.dismissSessionSummary()
                    viewModel.startCycleBreak()
                } else {
                    showBreakDurationDialog = true
                }
            },
            onStartAnotherSession = {
                viewModel.dismissSessionSummary()
                onStartAnotherSession()
            },
        )
        return
    }

    if (focusCycle?.phase == FocusCyclePhase.READY_FOR_BREAK) {
        CycleBreakReadyDialog(
            state = focusCycle,
            onStartBreak = viewModel::startCycleBreak,
            onEndCycle = viewModel::cancelFocusCycle,
        )
        return
    }

    if (focusCycle?.phase == FocusCyclePhase.COMPLETE) {
        CycleCompleteDialog(
            state = focusCycle,
            onDone = viewModel.focusCycleViewModel::dismissCompleted,
        )
        return
    }

    // Level Up Popup Dialog
    viewModel.showLevelUpToast?.let { pair ->
        Dialog(onDismissRequest = { viewModel.clearNotifications() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14203A)),
                border = BorderStroke(2.dp, RpgGold),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Crown cup",
                        tint = RpgGold,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚡ LEVEL UP ACHIEVED!",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = RpgGold,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Congratulations! Your knowledge of the dungeons has multiplied.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite, textAlign = TextAlign.Center)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "LEVEL ${pair.first}", style = MaterialTheme.typography.titleMedium.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Level advancement arrow", tint = NeonBlueAccent, modifier = Modifier.padding(horizontal = 8.dp))
                        Text(text = "LEVEL ${pair.second}", style = MaterialTheme.typography.titleLarge.copy(color = NeonBlueAccent, fontWeight = FontWeight.ExtraBold))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Claimed level bonus: +50 Gold!",
                        style = MaterialTheme.typography.bodySmall.copy(color = RpgGold, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.clearNotifications() },
                        colors = ButtonDefaults.buttonColors(containerColor = RpgGold, contentColor = Color.Black)
                    ) {
                        Text("ASCEND FORWARD", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    // Heavy Penalty Procrastination Dialog
    viewModel.showPenaltyToast?.let { penaltyText ->
        Dialog(onDismissRequest = { viewModel.clearNotifications() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF34171E)),
                border = BorderStroke(2.dp, RpgRuby),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Flame penalty warning",
                        tint = RpgRuby,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ PROCRASTINATION DEFEAT!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = RpgRuby,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = penaltyText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite, textAlign = TextAlign.Center)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.clearNotifications() },
                        colors = ButtonDefaults.buttonColors(containerColor = RpgRuby, contentColor = Color.White)
                    ) {
                        Text("I WILL NOT PROCRASTINATE AGAIN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Minor Streak Notification Dialog
    viewModel.showStreakResetToast?.let { msg ->
        Dialog(onDismissRequest = { viewModel.clearNotifications() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2833)),
                border = BorderStroke(1.5.dp, NeonBlueAccent),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak flame icon",
                        tint = RpgRuby,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "STREAK CHRONICLE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonBlueAccent
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite, textAlign = TextAlign.Center)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.clearNotifications() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlueAccent, contentColor = Color.Black)
                    ) {
                        Text("CONTINUE QUEST", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SessionSummaryDialog(
    summary: SessionSummary,
    onDone: () -> Unit,
    onStartAnotherSession: () -> Unit,
    showBreakOption: Boolean = true,
    onTakeBreak: () -> Unit = {},
    breakButtonLabel: String = "TAKE A BREAK",
    showStartAnotherOption: Boolean = true,
    note: String = "",
    onNoteChange: (String) -> Unit = {},
    onSaveNote: (String) -> Unit = {},
) {
    val title = when (summary.endState) {
        SessionEndState.Completed -> "SESSION COMPLETE"
        SessionEndState.Suspended -> "SESSION SAVED"
        SessionEndState.ManualConquest -> "BOSS CONQUERED"
    }
    val accent = when (summary.endState) {
        SessionEndState.Suspended -> NeonBlueAccent
        else -> RpgGold
    }

    Dialog(
        onDismissRequest = {
            onSaveNote(note)
            onDone()
        },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .testTag("session_summary_dialog")
                .semantics {
                    contentDescription = "$title for ${summary.subject}"
                    isTraversalGroup = true
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111A2C)),
            border = BorderStroke(2.dp, accent),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = if (summary.endState == SessionEndState.Suspended) {
                        Icons.Default.BookmarkAdded
                    } else {
                        Icons.Default.EmojiEvents
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    color = accent,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = summary.subject,
                    color = TextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryMetric(
                        label = "TIME",
                        value = formatStudyTimeShort(summary.durationSeconds),
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMetric(
                        label = "XP",
                        value = "+${summary.xpEarned}",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMetric(
                        label = "GOLD",
                        value = "+${summary.goldEarned}",
                        modifier = Modifier.weight(1f),
                    )
                }

                summary.bossProgress?.let {
                    Spacer(Modifier.height(16.dp))
                    SessionProgressBlock(label = "BOSS PROGRESS", progress = it, accent = RpgRuby)
                }
                summary.skillProgress?.let {
                    Spacer(Modifier.height(12.dp))
                    SessionProgressBlock(label = "SKILL PROGRESS", progress = it, accent = NeonBlueAccent)
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val levelValue = if (summary.currentLevel > summary.previousLevel) {
                        "${summary.previousLevel} → ${summary.currentLevel}"
                    } else {
                        summary.currentLevel.toString()
                    }
                    SummaryMetric(
                        label = "LEVEL",
                        value = levelValue,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMetric(
                        label = "STREAK",
                        value = "${summary.currentStreak} days",
                        modifier = Modifier.weight(1f),
                    )
                }

                if (summary.currentLevel > summary.previousLevel) {
                    SummaryNotice(
                        text = "Level up! ${summary.previousLevel} → ${summary.currentLevel}",
                        color = RpgGold,
                    )
                }
                summary.streakMessage?.let { SummaryNotice(text = it, color = RpgRuby) }
                if (summary.skillUnlocked) {
                    SummaryNotice(text = "New skill trait unlocked", color = NeonBlueAccent)
                }

                Spacer(Modifier.height(16.dp))
                SessionNoteField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.testTag("session_summary_note"),
                )
                Text(
                    text = "Your note is saved with this session when you leave this screen.",
                    color = TextSubtle,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))
                if (showBreakOption) {
                    Button(
                        onClick = {
                            onSaveNote(note)
                            onTakeBreak()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("session_summary_take_break"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RpgEmerald,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Icon(Icons.Default.Coffee, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(breakButtonLabel, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (showStartAnotherOption) {
                    Button(
                        onClick = {
                            onSaveNote(note)
                            onStartAnotherSession()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("session_summary_start_another"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text("START ANOTHER SESSION", fontWeight = FontWeight.Black)
                    }
                }
                TextButton(
                    onClick = {
                        onSaveNote(note)
                        onDone()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("session_summary_done"),
                ) {
                    Text("DONE", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2740)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SessionProgressBlock(
    label: String,
    progress: ProgressSummary,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF182238), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    progress.name,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "+${formatStudyTimeShort(progress.gainedSeconds)}",
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.progressAfter },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .semantics {
                    stateDescription =
                        "${(progress.progressBefore * 100).toInt()} to ${(progress.progressAfter * 100).toInt()} percent"
                },
            color = accent,
            trackColor = Color(0xFF0D1424),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${(progress.progressBefore * 100).toInt()}% → ${(progress.progressAfter * 100).toInt()}%",
            color = TextMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SummaryNotice(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    )
}
