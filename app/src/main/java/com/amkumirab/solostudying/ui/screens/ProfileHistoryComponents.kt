package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.domain.session.SessionNotePolicy
import com.amkumirab.solostudying.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun SessionHistoryCard(
    session: StudySessionEntity,
    onEditNote: () -> Unit,
) {
    val note = session.note?.takeIf { it.isNotBlank() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_history_${session.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
        border = BorderStroke(
            1.dp,
            if (session.wasCompleted) {
                RpgEmerald.copy(alpha = 0.3f)
            } else {
                RpgRuby.copy(alpha = 0.3f)
            },
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (session.wasCompleted) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Cancel
                            },
                            contentDescription = null,
                            tint = if (session.wasCompleted) RpgEmerald else RpgRuby,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = (session.bossName ?: "Unknown Quest").uppercase(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "Studied: ${formatStudyTimeShort(session.durationSeconds)} • ${formatSessionDate(session.timestamp)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${session.xpEarned} XP",
                        color = NeonBlueAccent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "+${session.goldEarned} G",
                        color = RpgGold,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            if (note != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevated, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .testTag("session_note_preview_${session.id}"),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = NeonBlueAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = note,
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            TextButton(
                onClick = onEditNote,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp)
                    .testTag("edit_session_note_${session.id}"),
            ) {
                Icon(
                    imageVector = if (note == null) Icons.AutoMirrored.Filled.NoteAdd else Icons.Default.Edit,
                    contentDescription = null,
                    tint = NeonBlueAccent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (note == null) "ADD NOTE" else "EDIT NOTE",
                    color = NeonBlueAccent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun SessionNoteDialog(
    session: StudySessionEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var note by remember(session.id) { mutableStateOf(session.note.orEmpty()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("session_note_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.5.dp, NeonBlueAccent),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "SESSION JOURNAL",
                    color = NeonBlueAccent,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = session.bossName ?: "Study Session",
                    color = TextWhite,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Capture what you covered, what was difficult, or where to continue next time.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                SessionNoteField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.testTag("session_note_editor"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        Text("CANCEL", color = TextMuted, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onSave(note) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("save_session_note"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonBlueAccent,
                            contentColor = OnAccent,
                        ),
                    ) {
                        Text("SAVE NOTE", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SessionNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { updated ->
            onValueChange(updated.take(SessionNotePolicy.MaxLength))
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text("SESSION NOTE (OPTIONAL)") },
        placeholder = { Text("What did you learn? What should you do next?") },
        minLines = 3,
        maxLines = 6,
        supportingText = {
            Text(
                text = "${value.length}/${SessionNotePolicy.MaxLength}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedContainerColor = SurfaceSubtle,
            unfocusedContainerColor = SurfaceSubtle,
            focusedBorderColor = NeonBlueAccent,
            unfocusedBorderColor = StrongCardBorder,
            focusedLabelColor = NeonBlueAccent,
            unfocusedLabelColor = TextMuted,
            focusedPlaceholderColor = TextSubtle,
            unfocusedPlaceholderColor = TextSubtle,
            focusedSupportingTextColor = TextSubtle,
            unfocusedSupportingTextColor = TextSubtle,
        ),
    )
}
@Composable
fun StatRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite))
    }
}
private fun formatSessionDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
