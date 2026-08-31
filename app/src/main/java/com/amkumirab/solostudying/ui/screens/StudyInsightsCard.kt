package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.domain.insights.InsightBucket
import com.amkumirab.solostudying.domain.insights.InsightRange
import com.amkumirab.solostudying.domain.insights.StudyInsights
import com.amkumirab.solostudying.domain.insights.calculateStudyInsights
import com.amkumirab.solostudying.ui.theme.DarkCardBorder
import com.amkumirab.solostudying.ui.theme.DarkFantasySurface
import com.amkumirab.solostudying.ui.theme.NeonBlueAccent
import com.amkumirab.solostudying.ui.theme.RpgEmerald
import com.amkumirab.solostudying.ui.theme.RpgGold
import com.amkumirab.solostudying.ui.theme.TextMuted
import com.amkumirab.solostudying.ui.theme.TextWhite
import kotlin.math.roundToInt

@Composable
fun StudyInsightsCard(
    sessions: List<StudySessionEntity>,
    profile: UserProfileEntity,
) {
    var selectedRange by remember { mutableStateOf(InsightRange.Last7Days) }
    val targets = remember(profile.scheduleWeekdayMinutes, profile.scheduleMinutesPerDay) {
        profile.scheduleWeekdayMinutes.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .takeIf { it.size == 7 }
            ?: List(7) { profile.scheduleMinutesPerDay }
    }
    val insights = remember(sessions, selectedRange, targets) {
        calculateStudyInsights(
            sessions = sessions,
            range = selectedRange,
            weekdayTargetMinutes = targets,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study_insights_card")
            .semantics { isTraversalGroup = true },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AutoGraph,
                contentDescription = null,
                tint = NeonBlueAccent,
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "STUDY INSIGHTS",
                    color = NeonBlueAccent,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "Focus patterns and progress against your schedule",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            InsightRange.entries.forEach { range ->
                val isSelected = selectedRange == range
                Button(
                    onClick = { selectedRange = range },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("insights_range_${range.name}")
                        .semantics {
                            selected = isSelected
                            stateDescription = if (isSelected) "Selected" else "Not selected"
                            contentDescription = "Show ${range.displayName.lowercase()} insights"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) NeonBlueAccent else Color(0xFF171C2A),
                        contentColor = if (isSelected) Color.Black else TextWhite,
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                ) {
                    Text(range.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, DarkCardBorder),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                InsightChart(insights = insights)
                Spacer(Modifier.height(14.dp))
                InsightMetrics(insights = insights)

                if (insights.sessionCount == 0) {
                    Text(
                        text = "No study sessions in this range yet.",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                    )
                } else {
                    Spacer(Modifier.height(14.dp))
                    InsightHighlights(insights = insights)
                }
            }
        }
    }
}

@Composable
private fun InsightChart(insights: StudyInsights) {
    val chartDescription = if (insights.buckets.isEmpty()) {
        "No chart data"
    } else {
        "${insights.buckets.size} focus periods. ${insights.targetCompletionPercent} percent of target completed"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("FOCUS CHART", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(
                "${insights.targetCompletionPercent}% OF TARGET",
                color = if (insights.targetCompletionPercent >= 100) RpgEmerald else RpgGold,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .testTag("insights_chart")
                .semantics { contentDescription = chartDescription },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            items(insights.buckets) { bucket ->
                InsightBar(bucket = bucket)
            }
        }
    }
}

@Composable
private fun InsightBar(bucket: InsightBucket) {
    val target = bucket.targetSeconds.coerceAtLeast(1L)
    val progress = (bucket.studySeconds.toFloat() / target).coerceIn(0f, 1f)
    val minutes = (bucket.studySeconds / 60L).toInt()
    val targetMinutes = (bucket.targetSeconds / 60L).toInt()
    val metTarget = bucket.targetSeconds > 0L && bucket.studySeconds >= bucket.targetSeconds

    Column(
        modifier = Modifier
            .width(38.dp)
            .semantics {
                contentDescription = "${bucket.label}: $minutes minutes studied, $targetMinutes minute target"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (minutes > 0) "${minutes}m" else "–",
            color = if (minutes > 0) TextWhite else TextMuted,
            fontSize = 9.sp,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(96.dp)
                .background(Color(0xFF0A0D15), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .background(
                        if (metTarget) RpgEmerald else NeonBlueAccent,
                        RoundedCornerShape(6.dp),
                    ),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = bucket.label,
            color = TextMuted,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun InsightMetrics(insights: StudyInsights) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InsightMetric("FOCUS", formatInsightDuration(insights.totalStudySeconds), Modifier.weight(1f))
        InsightMetric("SESSIONS", insights.sessionCount.toString(), Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InsightMetric("AVERAGE", formatInsightDuration(insights.averageSessionSeconds), Modifier.weight(1f))
        InsightMetric("LOOT", "+${insights.totalXp} XP / +${insights.totalGold} G", Modifier.weight(1f))
    }
}

@Composable
private fun InsightMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF171C2A), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(
            value,
            color = TextWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InsightHighlights(insights: StudyInsights) {
    val comparisonText = when {
        insights.range == InsightRange.AllTime -> "Lifetime study overview"
        insights.comparisonPercent == null -> "No activity in the previous period"
        insights.comparisonPercent >= 0 -> "${insights.comparisonPercent}% more focus than the previous period"
        else -> "${-insights.comparisonPercent}% less focus than the previous period"
    }
    val comparisonColor = when {
        insights.comparisonPercent == null -> TextMuted
        insights.comparisonPercent >= 0 -> RpgEmerald
        else -> RpgGold
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoGraph, contentDescription = null, tint = comparisonColor)
            Spacer(Modifier.width(8.dp))
            Text(comparisonText, color = comparisonColor, fontWeight = FontWeight.Bold)
        }
        insights.bestDayLabel?.let { label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = RpgGold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Best day: $label · ${formatInsightDuration(insights.bestDaySeconds)}",
                    color = TextWhite,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        insights.topSubject?.let { subject ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = NeonBlueAccent)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Top subject: $subject · ${formatInsightDuration(insights.topSubjectSeconds)}",
                    color = TextWhite,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatInsightDuration(seconds: Long): String {
    val minutes = (seconds.coerceAtLeast(0L) / 60.0).roundToInt()
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (remainingMinutes == 0) "${hours}h" else "${hours}h ${remainingMinutes}m"
}
