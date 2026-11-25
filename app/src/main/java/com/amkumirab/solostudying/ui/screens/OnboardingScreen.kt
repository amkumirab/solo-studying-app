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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amkumirab.solostudying.R
import com.amkumirab.solostudying.ui.theme.*

@Composable
fun OnboardingScreen(
    onFinish: (name: String, hunterClass: String, mainGoal: String, learningPath: String, scheduleDays: String, scheduleMinutes: Int, scheduleFlexibility: String, weekdayMinutes: String) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }

    // Step 0: Name & Class
    var userName by remember { mutableStateOf("") }
    val classes = listOf(
        Triple("Shadow Monarch", "Agile & focused coder/academic utilizing deep concentration spells.", Icons.Default.Cyclone),
        Triple("Academic Sage", "Lover of ancient books, historical texts, and deep academic research.", Icons.Default.School),
        Triple("Code Crusader", "Determined builder of algorithms and complex systems in the software realms.", Icons.Default.Terminal),
        Triple("Creative Mystic", "Visual thinker, UX alchemist, and designer of immersive experiences.", Icons.Default.Brush)
    )
    var selectedClass by remember { mutableStateOf("Shadow Monarch") }

    // Step 1: Main Goal
    val goals = listOf(
        Pair("Academic Mastery", "Slay final exams, maintain high GPA, and conquer grand lecture summaries."),
        Pair("Learn Coding", "Forge production-grade projects, master data structures, and crawl system stacks."),
        Pair("Master a Language", "Acquire polyglot rangers skills to decipher foreign runes and speech trials."),
        Pair("General Growth", "Cultivate regular focus alignments, reading routines, and lifestyle development.")
    )
    var selectedGoal by remember { mutableStateOf("Academic Mastery") }

    // Step 2: Learning Path
    val paths = listOf(
        Pair("Theoretical Sage Path", "Centered on absorbing lectures, conceptual diagrams, and text revision."),
        Pair("Practical Builder Path", "Focused on writing source code, labs, construction, and manual trials."),
        Pair("Linguistic Ranger Path", "Emphasizing persistent vocabulary drills, sentence construction, and speech."),
        Pair("Balanced Mystic Path", "A harmonious hybrid path blending conceptual study, coding, and regular exercises.")
    )
    var selectedPath by remember { mutableStateOf("Theoretical Sage Path") }

    // Step 3: Schedule days, minutes, flexibility
    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = remember { mutableStateListOf("Mon", "Wed", "Fri") }
    var targetMinutesPerDay by remember { mutableIntStateOf(45) }
    var selectedFlexibility by remember { mutableStateOf("Medium") }

    val maxStep = 4

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackFantasyBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (currentStep) {
                0 -> {
                    // STEP 0: Hunter Profile & Class
                    Image(
                        painter = painterResource(R.drawable.brand_onboarding),
                        contentDescription = stringResource(R.string.app_logo_description),
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = "DECLARE YOUR CHAMPION COVENANT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RpgGold,
                            letterSpacing = 1.5.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    Text(
                        text = "Enter your true pseudonym and select an RPG starting archetype class. This decides your path's thematic focus.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    )

                    OutlinedTextField(
                        value = userName,
                        onValueChange = { if (it.length <= 20) userName = it },
                        label = { Text("Champion Pseudonym / True Name") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedLabelColor = NeonBlueAccent,
                            focusedIndicatorColor = NeonBlueAccent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SELECT YOUR RPG CLASS archetype",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                    )

                    classes.forEach { item ->
                        val isSel = selectedClass == item.first
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) RpgGold else Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedClass = item.first }
                                .testTag("class_card_${item.first.replace(" ", "_").lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) DarkFantasySurface else Color.Black.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.third,
                                    contentDescription = item.first,
                                    tint = if (isSel) RpgGold else TextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column {
                                    Text(
                                        text = item.first,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) RpgGold else TextWhite,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = item.second,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // STEP 1: Select Main Goal
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Goal Icon",
                        tint = NeonBlueAccent,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = "SELECT YOUR GRAND QUEST",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonBlueAccent,
                            letterSpacing = 1.2.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    Text(
                        text = "What is the primary study realm you seek to conquer during your focus campaigns?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    )

                    goals.forEach { goal ->
                        val isSel = selectedGoal == goal.first
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) NeonBlueAccent else Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedGoal = goal.first }
                                .testTag("goal_card_${goal.first.replace(" ", "_").lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) DarkFantasySurface else Color.Black.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "⚔️  ${goal.first.uppercase()}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) NeonBlueAccent else TextWhite,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = goal.second,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // STEP 2: Choose Learning Path
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Path Icon",
                        tint = RpgRuby,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = "SELECT LEARNING ALIGNMENT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RpgRuby,
                            letterSpacing = 1.2.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    Text(
                        text = "Choose the methodology style of how you prefer to conquer educational dungeons.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    )

                    paths.forEach { path ->
                        val isSel = selectedPath == path.first
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) RpgRuby else Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedPath = path.first }
                                .testTag("path_card_${path.first.replace(" ", "_").lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) DarkFantasySurface else Color.Black.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "🌟  ${path.first.uppercase()}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) RpgRuby else TextWhite,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = path.second,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // STEP 3: Configure Schedule Commitments
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Schedule Icon",
                        tint = RpgGold,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = "SET COVENANT SCHEDULE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RpgGold,
                            letterSpacing = 1.2.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    Text(
                        text = "Select study days, daily target focus minutes, and flexibility constraints to bind your schedule.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("🗓️ CHOOSE COMMITMENT DAYS", fontWeight = FontWeight.Bold, color = RpgGold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                weekDays.forEach { day ->
                                    val isSel = selectedDays.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSel) RpgGold else Color.Black.copy(alpha = 0.6f))
                                            .border(1.dp, if (isSel) RpgGold else Color.Gray, CircleShape)
                                            .clickable {
                                                if (isSel) selectedDays.remove(day) else selectedDays.add(day)
                                            }
                                            .testTag("day_chip_$day"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.take(1),
                                            fontWeight = FontWeight.Black,
                                            color = if (isSel) BlackFantasyBackground else TextWhite,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⏱️ DAILY FOCUS TARGET", fontWeight = FontWeight.Bold, color = RpgGold, fontSize = 13.sp)
                                Text("$targetMinutesPerDay mins", fontWeight = FontWeight.ExtraBold, color = NeonBlueAccent, fontSize = 14.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(30, 45, 60, 90).forEach { mins ->
                                    val isSel = targetMinutesPerDay == mins
                                    Button(
                                        onClick = { targetMinutesPerDay = mins },
                                        modifier = Modifier.weight(1f).testTag("mins_btn_$mins"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) NeonBlueAccent else Color.Black.copy(alpha = 0.4f),
                                            contentColor = if (isSel) BlackFantasyBackground else TextWhite
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("${mins}m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))

                            Text("🛡️ COVENANT FLEXIBILITY", fontWeight = FontWeight.Bold, color = RpgGold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("Low", "Medium", "High").forEach { flex ->
                                    val isSel = selectedFlexibility == flex
                                    Button(
                                        onClick = { selectedFlexibility = flex },
                                        modifier = Modifier.weight(1f).testTag("flex_btn_$flex"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) RpgRuby else Color.Black.copy(alpha = 0.4f),
                                            contentColor = if (isSel) BlackFantasyBackground else TextWhite
                                        )
                                    ) {
                                        Text(flex, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // STEP 4: Review and Seal Covenant
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Review Icon",
                        tint = RpgEmerald,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = "SEAL THE COVENANT OF SAGES",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RpgEmerald,
                            letterSpacing = 1.2.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    Text(
                        text = "Review your character ledger attributes. Confirming these details seals your training bond in the realm of focus.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, RpgGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("👑 SAGE HERO IDENTITY CARD", fontWeight = FontWeight.Bold, color = RpgGold, fontSize = 14.sp)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Hero Name:", color = TextMuted, fontSize = 12.sp)
                                Text(userName.trim().uppercase(), color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Hero Archetype Class:", color = TextMuted, fontSize = 12.sp)
                                Text(selectedClass, color = RpgGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Quest Goal:", color = TextMuted, fontSize = 12.sp)
                                Text(selectedGoal, color = NeonBlueAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Learning Alignment Path:", color = TextMuted, fontSize = 12.sp)
                                Text(selectedPath, color = RpgRuby, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Focus Commit Days:", color = TextMuted, fontSize = 12.sp)
                                Text(selectedDays.joinToString(", "), color = TextWhite, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Daily Target Focus:", color = TextMuted, fontSize = 12.sp)
                                Text("$targetMinutesPerDay mins / day", color = RpgEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Covenant Constraints:", color = TextMuted, fontSize = 12.sp)
                                Text("$selectedFlexibility Flexibility", color = TextWhite, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    Button(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                        modifier = Modifier.testTag("onboarding_back_button")
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BACK")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Progress indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (0..maxStep).forEach { step ->
                        Box(
                            modifier = Modifier
                                .size(if (currentStep == step) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (currentStep == step) RpgGold else Color.Gray.copy(alpha = 0.5f))
                        )
                    }
                }

                val nextEnabled = when (currentStep) {
                    0 -> userName.trim().length >= 2
                    3 -> selectedDays.isNotEmpty()
                    else -> true
                }

                Button(
                    onClick = {
                        if (currentStep == maxStep) {
                            val daysStr = selectedDays.joinToString(",")
                            val weekdayMinutesList = List(7) { targetMinutesPerDay }.joinToString(",")
                            onFinish(
                                userName.trim(),
                                selectedClass,
                                selectedGoal,
                                selectedPath,
                                daysStr,
                                targetMinutesPerDay,
                                selectedFlexibility,
                                weekdayMinutesList
                            )
                        } else {
                            currentStep++
                        }
                    },
                    enabled = nextEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == maxStep) RpgEmerald else NeonBlueAccent,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    val label = if (currentStep == maxStep) "CONCLUDE COVENANT" else "FORWARD"
                    val iconVector = if (currentStep == maxStep) Icons.Default.CheckCircle else Icons.Default.ChevronRight
                    Text(label, color = if (nextEnabled) BlackFantasyBackground else Color.Gray, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (nextEnabled) BlackFantasyBackground else Color.Gray
                    )
                }
            }
        }
    }
}
