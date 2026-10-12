package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.ui.theme.*

import java.util.Calendar

internal fun LazyListScope.profileOverview(nonNullProfile: UserProfileEntity, sessions: List<StudySessionEntity>) {
    val rankText = getRankLabel(nonNullProfile.level)
    // Player Profile Card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, NeonBlueAccent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(NeonBlueAccent.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                        .border(2.dp, NeonBlueAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${nonNullProfile.level}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                fontSize = 32.sp
                            )
                        )
                        Text(
                            text = "LEVEL",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonBlueAccent,
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = rankText,
                    style = MaterialTheme.typography.titleMedium.copy(
                         fontWeight = FontWeight.ExtraBold,
                         color = RpgGold,
                         letterSpacing = 1.sp
                    )
                )

                Text(
                    text = nonNullProfile.name,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )

                val nextLevelXp = (nonNullProfile.level * 150).coerceAtLeast(1)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (nonNullProfile.xp.toFloat() / nextLevelXp).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${nonNullProfile.xp} / $nextLevelXp XP", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                Spacer(modifier = Modifier.height(16.dp))

                // Streaks summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CURRENT STREAK",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                            Text(
                                text = "${nonNullProfile.currentStreak} Days",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = RpgRuby,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "LONGEST STREAK",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                            Text(
                                text = "${nonNullProfile.longestStreak} Days",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = RpgEmerald,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // --- TODAY'S DAILY COMBAT REPORT ---
    item {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todaySessions = sessions.filter { it.timestamp >= startOfToday }
        val totalDurationToday = todaySessions.sumOf { it.durationSeconds }
        val totalMinutesToday = totalDurationToday / 60
        val totalXpToday = todaySessions.sumOf { it.xpEarned }
        val totalGoldToday = todaySessions.sumOf { it.goldEarned }

        val currentCalendar = java.util.Calendar.getInstance()
        val todayDayOfWeek = currentCalendar.get(java.util.Calendar.DAY_OF_WEEK)
        val dailyTargetMinutes = nonNullProfile.getTargetMinutesForCalendarDay(todayDayOfWeek).coerceAtLeast(1)
        val todayProgress = (totalMinutesToday.toFloat() / dailyTargetMinutes).coerceIn(0f, 1f)

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "⚔️ TODAY'S CAMPAIGN REPORT",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonBlueAccent,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                border = BorderStroke(1.dp, if (todayProgress >= 1f) RpgGold else DarkCardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (todayProgress >= 1f) "✨ DAILY PACT CONQUERED!" else "🛡️ DAILY ACTIVE QUEST",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (todayProgress >= 1f) RpgGold else TextWhite
                                )
                            )
                            Text(
                                text = "Target: $dailyTargetMinutes minutes focus budget",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (todayProgress >= 1f) RpgGold.copy(alpha = 0.15f) else Color.Black)
                                .border(1.dp, if (todayProgress >= 1f) RpgGold else DarkCardBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${(todayProgress * 100).toInt()}% DONE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (todayProgress >= 1f) RpgGold else NeonBlueAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Linear progress bar for daily focus target
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(todayProgress)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        if (todayProgress >= 1f) listOf(RpgGold, Color(0xFFFCD34D)) else listOf(NeonBlueAccent, Color(0xFFC582FF))
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary numbers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "CONQUEST TIME", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                            Text(text = "$totalMinutesToday mins", style = MaterialTheme.typography.titleMedium.copy(color = TextWhite, fontWeight = FontWeight.Black))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "COMBAT LOOT", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                            Text(text = "+$totalXpToday XP / +$totalGoldToday Gold", style = MaterialTheme.typography.bodyMedium.copy(color = RpgGold, fontWeight = FontWeight.Black))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkCardBorder.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "BATTLE RECORDS FOR TODAY:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonBlueAccent,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (todaySessions.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your weapons are resting. Enter a dungeon or cross the free study portal to log today's focus chronicles!",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    } else {
                        val grouped = todaySessions.groupBy { it.bossName ?: "Free Study Zone" }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            grouped.forEach { (bossName, list) ->
                                val totalDurationSec = list.sumOf { it.durationSeconds }
                                val mins = totalDurationSec / 60
                                val secs = totalDurationSec % 60
                                val durationStr = if (mins > 0) "$mins min${if (mins != 1L) "s" else ""} $secs sec" else "$secs sec"
                                val xpGained = list.sumOf { it.xpEarned }
                                val goldGained = list.sumOf { it.goldEarned }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F101A), RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkCardBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "⚔️",
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Column {
                                            Text(
                                                text = bossName.uppercase(),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = TextWhite
                                                )
                                            )
                                            Text(
                                                text = "Fought for $durationStr",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextMuted,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "+$xpGained XP",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = NeonBlueAccent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "+$goldGained G",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = RpgGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    item {
        StudyInsightsCard(
            sessions = sessions,
            profile = nonNullProfile,
        )
    }

    // Broad analytical values
    item {
        Text(
            text = "⚔️ STUDY HISTORIC STATISTICS",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = NeonBlueAccent,
                letterSpacing = 1.sp
            )
        )
    }

    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatRow(label = "Total Focus Time", value = formatStudyTimeFriendly(nonNullProfile.totalStudyTimeSeconds))
                StatRow(label = "Completed Session Count", value = "${nonNullProfile.totalSessionCount}")
                StatRow(label = "Bosses Slain", value = "${nonNullProfile.totalBossesDefeated}")
                StatRow(label = "Total Gold Earned All-Time", value = "${nonNullProfile.totalGoldEarned} G")
                StatRow(label = "Total XP Claimed All-Time", value = "${nonNullProfile.totalXpEarned} XP")
            }
        }
    }

}
