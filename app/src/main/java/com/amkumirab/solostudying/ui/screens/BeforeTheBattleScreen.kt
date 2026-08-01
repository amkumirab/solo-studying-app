package com.amkumirab.solostudying.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.SkillEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.sound.RpgSoundManager
import com.amkumirab.solostudying.ui.theme.*

@Composable
fun BeforeTheBattleScreen(
    boss: BossEntity?,
    freeStudyMins: Int?,
    selectedSkill: SkillEntity?,
    userProfile: UserProfileEntity,
    onBeginBattle: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // 1. Quick Focus Tips Collection
    val tips = remember {
        listOf(
            "Remove distractions before starting.",
            "Put your phone on silent mode.",
            "Focus on understanding, not memorizing.",
            "One focused session beats three distracted ones.",
            "Stay hydrated during long sessions.",
            "Don't chase perfection; chase progress.",
            "Take a deep breath before the battle.",
            "Your future self will thank you.",
            "Finish this session before checking notifications.",
            "Small progress every day becomes massive growth.",
            "Steel your focus. The boss awaits your challenge.",
            "Consistency is the ultimate weapon of legends.",
            "Silence the noise. Ignite your inner focus fire.",
            "A quiet room is the battlefield of geniuses.",
            "One block of pure focus changes everything."
        )
    }
    val selectedTip = remember { tips.random() }

    // 4. Mental Preparation Sentence
    val encouragements = remember {
        listOf(
            "Another step toward your goal.",
            "Every minute weakens the Boss.",
            "Stay focused. Victory comes one session at a time.",
            "Great hunters are built through consistency.",
            "Your future starts with this session.",
            "Prepare your mind. Steel your resolve.",
            "The path to academic excellence is forged in focus.",
            "No obstacle is insurmountable for a prepared mind."
        )
    }
    val selectedEncouragement = remember { encouragements.random() }

    // Music category items
    val musicCategories = remember {
        listOf(
            MusicCategory(
                title = "Relaxed Focus (Lo-Fi)",
                icon = "🌙",
                trackName = "Lo-fi Hip Hop",
                url = "https://www.youtube.com/watch?v=X4VbdwhkE10",
                recs = listOf("Reading", "Note taking", "Long study sessions"),
                color = Color(0xFF2E3E5C)
            ),
            MusicCategory(
                title = "Deep Focus (Alpha Waves)",
                icon = "🧠",
                trackName = "Study Music - Alpha Waves",
                url = "https://www.youtube.com/watch?v=WPni755-Krg",
                recs = listOf("Memorization", "Deep concentration", "Problem solving"),
                color = Color(0xFF1E3A8A)
            ),
            MusicCategory(
                title = "Epic Focus",
                icon = "⚔️",
                trackName = "Epic Instrumental Focus Music",
                url = "https://www.youtube.com/watch?v=AKWNytjwxLQ",
                recs = listOf("Boss battles", "Difficult subjects", "Motivation"),
                color = Color(0xFF581C87)
            ),
            MusicCategory(
                title = "High Energy Focus",
                icon = "🔥",
                trackName = "Best Music Mix for Leveling Up",
                url = "https://www.youtube.com/watch?v=f-I0PmRt0yM",
                recs = listOf("Low motivation", "Staying energized", "Intense study sessions"),
                color = Color(0xFF7F1D1D)
            )
        )
    }

    // Calculations for summary estimates
    val isFreeStudy = boss == null
    val targetMins = freeStudyMins ?: boss?.requiredMinutes ?: 30
    
    // Penalties & Boosters check
    val redDungeonDays = userProfile.redDungeonDays
    val isXpBoostActive = userProfile.isRedDungeonBoostActive
    
    // Multipliers
    val xpMultiplier = (if (redDungeonDays > 0) 0.8f else 1.0f) * (if (isXpBoostActive) 2.0f else 1.0f)
    val goldMultiplier = if (redDungeonDays > 0) 0.8f else 1.0f

    val baseRewardXp: Int
    val baseRewardGold: Int

    if (isFreeStudy) {
        baseRewardXp = (targetMins * 1.5f).toInt().coerceAtLeast(1)
        baseRewardGold = (targetMins * 0.8f).toInt()
    } else {
        val difficulty = boss?.difficulty ?: "Medium"
        val difficultyRewards = when (difficulty) {
            "Easy" -> Pair(75, 40)
            "Medium" -> Pair(150, 80)
            "Hard" -> Pair(350, 180)
            "Legendary" -> Pair(750, 400)
            else -> Pair(100, 50)
        }
        baseRewardXp = difficultyRewards.first
        baseRewardGold = difficultyRewards.second
    }

    val estimatedXp = (baseRewardXp * xpMultiplier).toInt()
    val estimatedGold = (baseRewardGold * goldMultiplier).toInt()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackFantasyBackground),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header: Aesthetic RPG Encounter Prep
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚔️ PREPARING THE MIND FOR COMBAT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isFreeStudy) NeonBlueAccent else RpgRuby,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFreeStudy) "ASTRAL TRANSITION" else "DUNGEON RITUAL OF BATTLE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = TextWhite,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // 1. Quick Focus Tips Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131422)),
                border = BorderStroke(1.5.dp, NeonBlueAccent.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonBlueAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TipsAndUpdates,
                            contentDescription = "Focus Tip Icon",
                            tint = NeonBlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "QUICK STRATEGY FOCUS TIP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonBlueAccent,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "\"$selectedTip\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // 3. Battle Session Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                border = BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🛡️ COMBAT ENGAGEMENT PROSPECTUS",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = RpgGold,
                            letterSpacing = 1.sp
                        )
                    )
                    
                    Divider(color = DarkCardBorder.copy(alpha = 0.5f), thickness = 1.dp)

                    // Stats grid representation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SummaryItem("TARGET", if (isFreeStudy) "Astral Free Study" else boss?.name ?: "Encounter")
                            SummaryItem("LOCATION", if (isFreeStudy) "Astral Plane Portal" else "Dungeon: ${boss?.dungeonName ?: "Realm"}")
                            SummaryItem("FOCUS DURATION", "$targetMins Minutes")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SummaryItem("EST. XP REWARD", "✨ $estimatedXp XP")
                            SummaryItem("EST. GOLD REWARD", "🪙 $estimatedGold Gold")
                            SummaryItem("STREAK MULTIPLIER", "🔥 ${userProfile.currentStreak} Day Streak")
                        }
                    }

                    // Display active multipliers visually if applicable
                    if (redDungeonDays > 0 || isXpBoostActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (redDungeonDays > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RpgRuby.copy(alpha = 0.15f))
                                        .border(0.5.dp, RpgRuby.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⚠️ RED DUNGEON PENALTY (-20%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = RpgRuby,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp
                                        )
                                    )
                                }
                            }
                            if (isXpBoostActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RpgEmerald.copy(alpha = 0.15f))
                                        .border(0.5.dp, RpgEmerald.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⚡ 2X XP BOOSTER ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = RpgEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    selectedSkill?.let { sk ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeonBlueAccent.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, NeonBlueAccent.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonBlueAccent, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Skill focus: ${sk.name.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = NeonBlueAccent, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Study Music Playlist Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🔊 WOULD YOU LIKE BACKGROUND MUSIC FOR THIS BATTLE?",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                // Cards of music selection
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    musicCategories.forEach { music ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    RpgSoundManager.playClickSound()
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(music.url))
                                    context.startActivity(intent)
                                }
                                .testTag("music_${music.title.replace(" ", "_")}"),
                            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                            border = BorderStroke(1.dp, DarkCardBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Dynamic theme music glow slot box
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(music.color, Color.Black),
                                                radius = 90f
                                            )
                                        )
                                        .border(1.dp, music.color.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = music.icon, fontSize = 22.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = music.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite
                                            )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .border(0.5.dp, RpgGold.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = RpgGold, modifier = Modifier.size(10.dp))
                                                Text("TUNE IN", color = RpgGold, style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Black))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = music.trackName,
                                        style = MaterialTheme.typography.bodySmall.copy(color = RpgGold, fontSize = 11.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Recommendations tags
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Best for: ${music.recs.joinToString(" • ")}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Mental Preparation Sentence Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color.Black)))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isFreeStudy) NeonBlueAccent.copy(alpha = 0.4f) else RpgRuby.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedEncouragement.uppercase(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = RpgGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontFamily = FontFamily.Serif
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5. Start Battle Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        RpgSoundManager.playClickSound()
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E191D)),
                    border = BorderStroke(1.dp, RpgRuby.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("RETREAT", color = RpgRuby, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Button(
                    onClick = {
                        RpgSoundManager.playClickSound()
                        onBeginBattle()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isFreeStudy) NeonBlueSecondary else RpgRuby),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.8f)
                        .testTag("begin_battle_button"),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Begin Battle Flash Icon",
                            tint = Color.Black
                        )
                        Text(
                            text = "BEGIN BATTLE",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextWhite,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class MusicCategory(
    val title: String,
    val icon: String,
    val trackName: String,
    val url: String,
    val recs: List<String>,
    val color: Color
)
