package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.ui.theme.DarkCardBorder
import com.amkumirab.solostudying.ui.theme.NeonBlueAccent
import com.amkumirab.solostudying.ui.theme.RpgEmerald
import com.amkumirab.solostudying.ui.theme.TextMuted
import com.amkumirab.solostudying.ui.theme.TextWhite
import java.util.Locale

@Composable
fun BreakDurationDialog(
    onDismiss: () -> Unit,
    onStartBreak: (Int) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("break_duration_dialog")
                .semantics {
                    contentDescription = "Choose break duration"
                    isTraversalGroup = true
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111A2C)),
            border = BorderStroke(2.dp, NeonBlueAccent),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Coffee,
                    contentDescription = null,
                    tint = NeonBlueAccent,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "CHOOSE YOUR BREAK",
                    color = NeonBlueAccent,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Rest without affecting XP, Gold, streaks, or study history.",
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(5, 10, 15).forEach { minutes ->
                        OutlinedButton(
                            onClick = { onStartBreak(minutes) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("break_${minutes}_minutes"),
                            border = BorderStroke(1.dp, NeonBlueAccent.copy(alpha = 0.7f)),
                        ) {
                            Text("$minutes MIN", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("break_duration_cancel"),
                ) {
                    Text("BACK", color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun BreakTimerDialog(
    durationSeconds: Long,
    remainingSeconds: Long,
    onSkipBreak: () -> Unit,
) {
    val safeDuration = durationSeconds.coerceAtLeast(1L)
    val elapsedProgress = (1f - remainingSeconds.toFloat() / safeDuration).coerceIn(0f, 1f)

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("break_timer_dialog")
                .semantics {
                    contentDescription = "Break timer, ${formatBreakTime(remainingSeconds)} remaining"
                    isTraversalGroup = true
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102326)),
            border = BorderStroke(2.dp, RpgEmerald),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = RpgEmerald,
                    modifier = Modifier.size(54.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "RECOVERY IN PROGRESS",
                    color = RpgEmerald,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = formatBreakTime(remainingSeconds),
                    color = TextWhite,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.testTag("break_time_remaining"),
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { elapsedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = RpgEmerald,
                    trackColor = Color(0xFF071416),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "You can leave the app. A notification will tell you when recovery is complete.",
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onSkipBreak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("skip_break_button"),
                    border = BorderStroke(1.dp, DarkCardBorder),
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SKIP BREAK", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BreakCompleteDialog(
    onDone: () -> Unit,
    onStartNextSession: () -> Unit,
    message: String = "Recovery complete. Ready for another focused quest?",
    startButtonLabel: String = "START NEXT SESSION",
    doneButtonLabel: String = "DONE",
) {
    Dialog(onDismissRequest = onDone) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("break_complete_dialog")
                .semantics {
                    contentDescription = "Break complete"
                    isTraversalGroup = true
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102326)),
            border = BorderStroke(2.dp, RpgEmerald),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Coffee,
                    contentDescription = null,
                    tint = RpgEmerald,
                    modifier = Modifier.size(54.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "BREAK COMPLETE",
                    color = RpgEmerald,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onStartNextSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("break_start_next_session"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RpgEmerald,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text(startButtonLabel, fontWeight = FontWeight.Black)
                }
                TextButton(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("break_complete_done"),
                ) {
                    Text(doneButtonLabel, color = TextWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatBreakTime(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    return String.format(Locale.ROOT, "%02d:%02d", safeSeconds / 60L, safeSeconds % 60L)
}
