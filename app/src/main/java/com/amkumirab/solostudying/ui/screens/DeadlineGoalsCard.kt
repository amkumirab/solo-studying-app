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
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.deadline.DeadlineGoalPlan
import com.amkumirab.solostudying.domain.deadline.DeadlineGoalStatus
import com.amkumirab.solostudying.domain.deadline.calculateDeadlineGoalPlan
import com.amkumirab.solostudying.ui.theme.DarkCardBorder
import com.amkumirab.solostudying.ui.theme.DarkFantasySurface
import com.amkumirab.solostudying.ui.theme.DangerContainer
import com.amkumirab.solostudying.ui.theme.InfoContainer
import com.amkumirab.solostudying.ui.theme.NeonBlueAccent
import com.amkumirab.solostudying.ui.theme.OnAccent
import com.amkumirab.solostudying.ui.theme.RpgEmerald
import com.amkumirab.solostudying.ui.theme.RpgGold
import com.amkumirab.solostudying.ui.theme.RpgRuby
import com.amkumirab.solostudying.ui.theme.SurfaceElevated
import com.amkumirab.solostudying.ui.theme.TextMuted
import com.amkumirab.solostudying.ui.theme.TextWhite
import com.amkumirab.solostudying.ui.theme.WarningContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun DeadlineGoalsCard(
    bosses: List<BossEntity>,
    scheduleDays: String,
    isSessionActive: Boolean,
    onCreateGoal: () -> Unit,
    onStartGoal: (BossEntity) -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val plans = bosses
        .asSequence()
        .filterNot { it.isCompleted }
        .mapNotNull { boss -> calculateDeadlineGoalPlan(boss, scheduleDays, today) }
        .sortedBy { it.deadline }
        .toList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deadline_goal_planner"),
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
        border = BorderStroke(1.dp, if (plans.isEmpty()) DarkCardBorder else NeonBlueAccent.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = NeonBlueAccent,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DEADLINE PLANNER",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Turn upcoming exams into a daily focus target",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (plans.isEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "No active deadline goals. Add a deadline when you summon your next exam or assignment.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCreateGoal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("create_deadline_goal"),
                    border = BorderStroke(1.dp, NeonBlueAccent),
                ) {
                    Icon(Icons.Default.AddTask, contentDescription = null, tint = NeonBlueAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("ADD DEADLINE GOAL", color = NeonBlueAccent, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(14.dp))
                plans.forEachIndexed { index, plan ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = DarkCardBorder,
                        )
                    }
                    DeadlineGoalRow(
                        plan = plan,
                        isSessionActive = isSessionActive,
                        onStart = { onStartGoal(plan.boss) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeadlineGoalRow(
    plan: DeadlineGoalPlan,
    isSessionActive: Boolean,
    onStart: () -> Unit,
) {
    val statusLabel: String
    val statusColor: Color
    val statusContainer: Color
    when (plan.status) {
        DeadlineGoalStatus.OnTrack -> {
            statusLabel = "ON TRACK"
            statusColor = RpgEmerald
            statusContainer = RpgEmerald.copy(alpha = 0.12f)
        }
        DeadlineGoalStatus.BehindSchedule -> {
            statusLabel = "BEHIND"
            statusColor = RpgGold
            statusContainer = WarningContainer
        }
        DeadlineGoalStatus.DueToday -> {
            statusLabel = "DUE TODAY"
            statusColor = RpgRuby
            statusContainer = DangerContainer
        }
        DeadlineGoalStatus.Overdue -> {
            statusLabel = "OVERDUE"
            statusColor = RpgRuby
            statusContainer = DangerContainer
        }
    }
    val dateLabel = plan.deadline.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
    val timeLabel = when (plan.status) {
        DeadlineGoalStatus.DueToday -> "Deadline is today"
        DeadlineGoalStatus.Overdue -> "Deadline passed"
        else -> "${plan.daysRemaining} day${if (plan.daysRemaining == 1) "" else "s"} left"
    }

    Column(modifier = Modifier.testTag("deadline_goal_${plan.boss.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.boss.name,
                    color = TextWhite,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$dateLabel • $timeLabel",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = statusLabel,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusContainer)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { plan.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .semantics {
                    contentDescription = "${(plan.progress * 100).toInt()} percent complete"
                },
            color = statusColor,
            trackColor = SurfaceElevated,
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${plan.remainingMinutes} MIN REMAINING",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (plan.status == DeadlineGoalStatus.Overdue) {
                        "Reschedule or continue now"
                    } else {
                        "${plan.recommendedMinutesPerStudyDay} min per study day"
                    },
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("deadline_goal_daily_target_${plan.boss.id}"),
                )
            }
            Button(
                onClick = onStart,
                enabled = !isSessionActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonBlueAccent,
                    contentColor = OnAccent,
                    disabledContainerColor = InfoContainer,
                    disabledContentColor = TextMuted,
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("start_deadline_goal_${plan.boss.id}"),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("START", fontWeight = FontWeight.Black)
            }
        }
    }
}
