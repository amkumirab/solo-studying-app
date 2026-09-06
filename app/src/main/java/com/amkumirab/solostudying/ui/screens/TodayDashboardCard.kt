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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.domain.today.TodayPlan
import com.amkumirab.solostudying.domain.today.TodayPlanItem
import com.amkumirab.solostudying.domain.today.TodayPlanItemType
import com.amkumirab.solostudying.domain.today.TodayPlanUrgency
import com.amkumirab.solostudying.ui.theme.BlackFantasyBackground
import com.amkumirab.solostudying.ui.theme.DarkCardBorder
import com.amkumirab.solostudying.ui.theme.DarkFantasySurface
import com.amkumirab.solostudying.ui.theme.DangerContainer
import com.amkumirab.solostudying.ui.theme.NeonBlueAccent
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
internal fun TodayDashboardCard(
    plan: TodayPlan,
    isSessionActive: Boolean,
    onStartItem: (TodayPlanItem) -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val studiedMinutes = plan.studiedSeconds / 60L
    val dateLabel = today.format(
        DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH),
    )
    val borderColor = when {
        plan.isDailyTargetComplete -> RpgEmerald.copy(alpha = 0.75f)
        plan.items.firstOrNull()?.urgency == TodayPlanUrgency.Urgent -> RpgRuby.copy(alpha = 0.8f)
        else -> NeonBlueAccent.copy(alpha = 0.65f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_dashboard"),
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
        border = BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    tint = if (plan.isDailyTargetComplete) RpgEmerald else NeonBlueAccent,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TODAY'S PLAN",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    )
                    Text(dateLabel, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = when {
                        plan.isRestDay -> "REST DAY"
                        plan.isDailyTargetComplete -> "TARGET DONE"
                        else -> "${plan.remainingMinutes} MIN LEFT"
                    },
                    color = if (plan.isDailyTargetComplete) RpgEmerald else RpgGold,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (plan.isDailyTargetComplete) {
                                RpgEmerald.copy(alpha = 0.12f)
                            } else {
                                WarningContainer
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { plan.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .testTag("today_target_progress")
                    .semantics {
                        contentDescription = if (plan.isRestDay) {
                            "No scheduled study target today"
                        } else {
                            "$studiedMinutes of ${plan.targetMinutes} target minutes studied today"
                        }
                    },
                color = if (plan.isDailyTargetComplete) RpgEmerald else NeonBlueAccent,
                trackColor = SurfaceElevated,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = when {
                    plan.isRestDay && studiedMinutes > 0 ->
                        "Rest day · $studiedMinutes bonus minutes completed"
                    plan.isRestDay ->
                        "No study target scheduled. Optional tasks are still available."
                    plan.isDailyTargetComplete ->
                        "Daily target complete · $studiedMinutes minutes focused"
                    else ->
                        "$studiedMinutes of ${plan.targetMinutes} minutes · ${plan.remainingMinutes} minutes remaining"
                },
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = DarkCardBorder)
            Spacer(Modifier.height(12.dp))

            if (plan.items.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RpgEmerald.copy(alpha = 0.1f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RpgEmerald)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("TODAY'S PLAN COMPLETE", color = RpgEmerald, fontWeight = FontWeight.Black)
                        Text(
                            "No unfinished priority items remain.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            } else {
                plan.items.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = DarkCardBorder,
                        )
                    }
                    TodayPlanItemRow(
                        item = item,
                        isNext = index == 0,
                        isSessionActive = isSessionActive,
                        onStart = { onStartItem(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayPlanItemRow(
    item: TodayPlanItem,
    isNext: Boolean,
    isSessionActive: Boolean,
    onStart: () -> Unit,
) {
    val urgencyColor = when (item.urgency) {
        TodayPlanUrgency.Urgent -> RpgRuby
        TodayPlanUrgency.High -> RpgGold
        TodayPlanUrgency.Normal -> NeonBlueAccent
    }
    val urgencyContainer = when (item.urgency) {
        TodayPlanUrgency.Urgent -> DangerContainer
        TodayPlanUrgency.High -> WarningContainer
        TodayPlanUrgency.Normal -> NeonBlueAccent.copy(alpha = 0.1f)
    }
    val icon: ImageVector = when (item.type) {
        TodayPlanItemType.DailyQuest -> Icons.Default.TaskAlt
        TodayPlanItemType.BossStep -> Icons.Default.Route
        TodayPlanItemType.Boss -> Icons.Default.Flag
        TodayPlanItemType.QuickFocus -> Icons.Default.Timer
    }
    val itemTag = "${item.type.name.lowercase()}_${item.sourceId}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_plan_item_$itemTag"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = urgencyColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNext) {
                        Text(
                            text = "NEXT UP",
                            color = urgencyColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(urgencyContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = "${item.durationMinutes} MIN",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.title,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.context,
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Button(
            onClick = onStart,
            enabled = !isSessionActive,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("start_today_plan_$itemTag"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isNext) urgencyColor else SurfaceElevated,
                contentColor = if (isNext) BlackFantasyBackground else TextWhite,
                disabledContainerColor = SurfaceElevated,
                disabledContentColor = TextMuted,
            ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isSessionActive) "SESSION ACTIVE" else "START FOCUS",
                fontWeight = FontWeight.Black,
            )
        }
    }
}
