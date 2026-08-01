package com.amkumirab.solostudying.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.amkumirab.solostudying.ui.theme.*
import com.amkumirab.solostudying.ui.viewmodel.TutorialViewModel
import com.amkumirab.solostudying.ui.viewmodel.TutorialUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TutorialScreen(viewModel: TutorialViewModel) {
    val state by viewModel.uiState.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var userNameInput by remember { mutableStateOf("") }
    var userClassInput by remember { mutableStateOf("Shadow Monarch") }

    // Synchronize initial user name from profile if available
    LaunchedEffect(profile) {
        profile?.let {
            if (userNameInput.isEmpty() && it.name != "Solo Hero") {
                userNameInput = it.name
            }
            userClassInput = it.hunterClass
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackFantasyBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonBlueSecondary.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation & Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.prevStep() },
                    enabled = state.currentStep > 1,
                    modifier = Modifier.testTag("tutorial_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (state.currentStep > 1) RpgGold else TextMuted.copy(alpha = 0.3f)
                    )
                }

                // Progress Step Dots
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AWAKENING RITUAL: STEP ${state.currentStep}/${state.totalSteps}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = RpgGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..state.totalSteps) {
                            val isActive = i <= state.currentStep
                            val isCurrent = i == state.currentStep
                            val color = when {
                                isCurrent -> NeonBlueAccent
                                isActive -> NeonBlueSecondary
                                else -> DarkCardBorder
                            }
                            val width = if (isCurrent) 12.dp else 6.dp
                            Box(
                                modifier = Modifier
                                    .size(width = width, height = 6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                        }
                    }
                }

                // Skip Button
                TextButton(
                    onClick = { viewModel.showSkipConfirmation(true) },
                    modifier = Modifier.testTag("tutorial_skip_button")
                ) {
                    Text(
                        text = "SKIP",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = RpgRuby,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // Central Content Area with smooth step transitions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "TutorialStepTransition"
                ) { step ->
                    when (step) {
                        1 -> StepAwakening(
                            nameInput = userNameInput,
                            onNameChange = { userNameInput = it }
                        )
                        2 -> StepProfileExplanation()
                        3 -> StepClassesExplanation(
                            selectedClass = userClassInput,
                            onClassSelected = { userClassInput = it }
                        )
                        4 -> StepDungeonsExplanation()
                        5 -> StepBossBattlesExplanation(
                            state = state,
                            onSimulateAttack = { viewModel.simulateAttack() },
                            onReset = { viewModel.resetFakeBoss() }
                        )
                        6 -> StepStudyModesExplanation()
                        7 -> StepBattlePrepExplanation()
                        8 -> StepSkillsGrimoireExplanation()
                        9 -> StepRewardsShopExplanation()
                        10 -> StepRedDungeonExplanation()
                        11 -> StepFirstMissionWizard(
                            state = state,
                            onDungeonNameChange = { viewModel.updateDungeonName(it) },
                            onBossNameChange = { viewModel.updateBossName(it) },
                            onHoursChange = { viewModel.updateEstimatedHours(it) },
                            onSkillChange = { viewModel.updateSelectedSkill(it) },
                            onDayToggle = { viewModel.toggleScheduleDay(it) },
                            onMinutesChange = { viewModel.updateScheduleMinutes(it) }
                        )
                        12 -> StepFirstBattleCelebration()
                    }
                }
            }

            // Bottom Navigation Buttons
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (state.currentStep == 1) {
                        // Persist username & move on
                        val currentName = userNameInput.trim().ifEmpty { "New Hunter" }
                        viewModel.nextStep()
                    } else if (state.currentStep == 11) {
                        viewModel.createFirstMissionAndCelebrate()
                    } else {
                        viewModel.nextStep()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.currentStep == 11 || state.currentStep == 12) RpgEmerald else NeonBlueAccent,
                    contentColor = BlackFantasyBackground
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("tutorial_next_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val btnText = when (state.currentStep) {
                        1 -> "Begin Awakening"
                        11 -> "Consecrate Quest & Start"
                        12 -> "Awaken Now"
                        else -> "Continue Journey"
                    }
                    Text(
                        text = btnText.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (state.currentStep == 12) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next"
                    )
                }
            }
        }

        // Skip Confirmation Dialog
        if (state.isSkipConfirmationVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.showSkipConfirmation(false) },
                title = {
                    Text(
                        text = "BYPASS AWAKENING?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = RpgRuby,
                            letterSpacing = 1.sp
                        )
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you wish to bypass the Hunter Awakening Ceremony? Your starting covenant will be established automatically.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.showSkipConfirmation(false)
                            viewModel.completeTutorial()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RpgRuby)
                    ) {
                        Text("Bypass Ceremony", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showSkipConfirmation(false) }) {
                        Text("Return to Ritual", color = TextMuted)
                    }
                },
                containerColor = DarkFantasySurface,
                textContentColor = TextWhite,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// ==========================================
// STEP 1: HUNTER AWAKENING
// ==========================================
@Composable
fun StepAwakening(
    nameInput: String,
    onNameChange: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowSize by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .drawWithContent {
                    drawCircle(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonBlueAccent.copy(alpha = 0.3f * glowSize),
                                Color.Transparent
                            )
                        ),
                        radius = 70.dp.toPx()
                    )
                    drawContent()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cyclone,
                contentDescription = "Awakening Sigil",
                tint = NeonBlueAccent,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        rotationZ = (System.currentTimeMillis() / 20 % 360).toFloat()
                    }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "WELCOME, HUNTER",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = TextWhite,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your real life is now your battlefield.\nEvery hour of learning strengthens your power.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = TextMuted,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, DarkCardBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ENTER YOUR PSEUDONYM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = RpgGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = onNameChange,
                    singleLine = true,
                    placeholder = { Text("e.g. Alex", color = TextMuted.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonBlueAccent,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = BlackFantasyBackground,
                        unfocusedContainerColor = BlackFantasyBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tutorial_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This name will be inscribed upon your cosmic status plate.",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }
        }
    }
}

// ==========================================
// STEP 2: PROFILE EXPLANATION
// ==========================================
@Composable
fun StepProfileExplanation() {
    var activeSpotlight by remember { mutableStateOf(0) }

    // Automatic cyclic spotlight animation for educational visual anchors
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            activeSpotlight = (activeSpotlight + 1) % 5
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "COSMIC STATUS PLATE",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = RpgGold,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your profile tracks your lifework as measurable RPG statistics. Consistency fuels your scaling.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Profile spotlight visual stack
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileStatItem(
                label = "Level",
                value = "LVL 12",
                desc = "Represents your lifetime growth and total knowledge accumulated.",
                isSpotlight = activeSpotlight == 0,
                icon = Icons.Default.TrendingUp,
                color = NeonBlueAccent
            )

            ProfileStatItem(
                label = "XP (Experience Points)",
                value = "1,840 / 2,200",
                desc = "Earned through real study sessions. Overflows level up your system.",
                isSpotlight = activeSpotlight == 1,
                icon = Icons.Default.AutoAwesome,
                color = RpgGold
            )

            ProfileStatItem(
                label = "Gold Coins",
                value = "450 G",
                desc = "Your hard-earned focus currency. Spent on real-life custom rewards.",
                isSpotlight = activeSpotlight == 2,
                icon = Icons.Default.MonetizationOn,
                color = RpgGold
            )

            ProfileStatItem(
                label = "Rank",
                value = "B-RANK HUNTER",
                desc = "Grows based on the quantity and intensity of Dungeons completed.",
                isSpotlight = activeSpotlight == 3,
                icon = Icons.Default.Shield,
                color = NeonBlueAccent
            )

            ProfileStatItem(
                label = "Streak Counter",
                value = "7 DAYS",
                desc = "Rewards consistency, not perfect layouts. Keeps the focus momentum alive.",
                isSpotlight = activeSpotlight == 4,
                icon = Icons.Default.LocalFireDepartment,
                color = RpgRuby
            )
        }
    }
}

@Composable
fun ProfileStatItem(
    label: String,
    value: String,
    desc: String,
    isSpotlight: Boolean,
    icon: ImageVector,
    color: Color
) {
    val borderAlpha by animateFloatAsState(if (isSpotlight) 1f else 0.2f, label = "border")
    val scale by animateFloatAsState(if (isSpotlight) 1.02f else 1.0f, label = "scale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSpotlight) DarkFantasySurface.copy(alpha = 0.9f) else DarkFantasySurface.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            1.dp,
            if (isSpotlight) color.copy(alpha = borderAlpha) else DarkCardBorder.copy(alpha = borderAlpha)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSpotlight) color else TextMuted,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSpotlight) color else TextWhite
                        )
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = color
                        )
                    )
                }
                if (isSpotlight) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 3: EXPLAIN CLASSES
// ==========================================
@Composable
fun StepClassesExplanation(
    selectedClass: String,
    onClassSelected: (String) -> Unit
) {
    val classes = listOf(
        Triple("Shadow Monarch", "For those who conquer impossible goals with absolute focus.", Icons.Default.Cyclone),
        Triple("Academic Sage", "For those who seek knowledge mastery, deep logic, and wisdom.", Icons.Default.School),
        Triple("Code Crusader", "For builders, programmers, and software engineering creators.", Icons.Default.Terminal),
        Triple("Creative Mystic", "For designers, artists, creators, and visual innovators.", Icons.Default.Brush)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CHOOSE YOUR COVENANT CLASS",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Classes represent your aesthetic styling identity and learning theme, providing a cosmetic RPG frame without unfair power advantages.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        classes.forEach { (name, desc, icon) ->
            val isSelected = selectedClass == name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onClassSelected(name) }
                    .testTag("class_card_$name"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DarkFantasySurface else DarkFantasySurface.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) NeonBlueAccent else DarkCardBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = if (isSelected) NeonBlueAccent else TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonBlueAccent else TextWhite
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = NeonBlueAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// STEP 4: EXPLAIN DUNGEONS
// ==========================================
@Composable
fun StepDungeonsExplanation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Castle,
            contentDescription = "Dungeon",
            tint = NeonBlueAccent,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "UNDERSTAND DUNGEONS",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "\"A Dungeon represents a major chapter of your life.\"",
            style = MaterialTheme.typography.titleMedium.copy(
                color = RpgGold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Instead of plain categories, frame your major commitments as Unlocked Dungeons. Each Dungeon represents a long-term goal consisting of minor battles:",
            style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Dungeon examples grid/list
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DungeonExampleCard(title = "University Semester", icon = Icons.Default.School, subtitle = "Contains modules, lecture schedules, and grading targets.")
            DungeonExampleCard(title = "Coding Grimoire", icon = Icons.Default.Terminal, subtitle = "Focuses on portfolio creation, algorithm mastery, and building apps.")
            DungeonExampleCard(title = "Polyglot Runes", icon = Icons.Default.Translate, subtitle = "Acquires language paths and vocabulary study consistency.")
        }
    }
}

@Composable
fun DungeonExampleCard(title: String, icon: ImageVector, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(NeonBlueSecondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = NeonBlueAccent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextWhite))
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
            }
        }
    }
}

// ==========================================
// STEP 5: BOSS BATTLES (INTERACTIVE)
// ==========================================
@Composable
fun StepBossBattlesExplanation(
    state: TutorialUiState,
    onSimulateAttack: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ENGAGE BOSS BATTLES",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = RpgRuby,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Every major milestone is a Boss containing HP (Hit Points). Every minute of focused study in real life inflicts equivalent physical damage to the Boss.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Simulated Interactive Boss Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("tutorial_boss_card"),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, if (state.fakeBossHp == 0) RpgEmerald else RpgRuby)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Machine Learning Exam".uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "DIFFICULTY: HARD",
                            style = MaterialTheme.typography.labelSmall.copy(color = RpgGold)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CrisisAlert,
                        contentDescription = "Boss",
                        tint = if (state.fakeBossHp == 0) RpgEmerald else RpgRuby,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // HP Bar
                val hpPercentage = state.fakeBossHp.toFloat() / state.fakeBossMaxHp.toFloat()
                LinearProgressIndicator(
                    progress = hpPercentage,
                    color = if (state.fakeBossHp == 0) RpgEmerald else RpgRuby,
                    trackColor = DarkCardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${state.fakeBossHp} / ${state.fakeBossMaxHp} HP",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (state.fakeBossHp == 0) RpgEmerald else TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Floating simulation output
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(BlackFantasyBackground, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.damageNumbers.isNotEmpty()) {
                        Text(
                            text = "SIMULATED FOCUS DEALT: ${state.damageNumbers.last()}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = NeonBlueAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Text(
                            text = "Tap button below to simulate studying",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSimulateAttack,
                        enabled = state.fakeBossHp > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = RpgRuby),
                        modifier = Modifier.testTag("tutorial_simulate_study_button")
                    ) {
                        Text("SIMULATE 30m STUDY", color = TextWhite, fontWeight = FontWeight.Black)
                    }

                    if (state.fakeBossHp == 0) {
                        Button(
                            onClick = onReset,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder)
                        ) {
                            Text("RESET BOSS", color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// STEP 6: STUDY MODES
// ==========================================
@Composable
fun StepStudyModesExplanation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Timeline,
            contentDescription = "Study Modes",
            tint = NeonBlueAccent,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SELECT YOUR MODE",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, NeonBlueAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MilitaryTech, contentDescription = "Boss Mode", tint = RpgRuby, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "BOSS STUDY MODE", style = MaterialTheme.typography.titleMedium.copy(color = RpgRuby, fontWeight = FontWeight.Black))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Allows you to duel a pre-declared Boss (exam, assignment). Study seconds reduce Boss HP directly, granting premium rewards and leveling up associated skills upon final defeat.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Timeline, contentDescription = "Free Mode", tint = NeonBlueAccent, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "FREE STUDY MODE", style = MaterialTheme.typography.titleMedium.copy(color = NeonBlueAccent, fontWeight = FontWeight.Black))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A versatile focus timer for reading, review, and auxiliary tasks. Gives standard XP/Gold, maintaining flexible study pathways without targeting a specific active dungeon Boss.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }
    }
}

// ==========================================
// STEP 7: BATTLE PREPARATION
// ==========================================
@Composable
fun StepBattlePrepExplanation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShieldMoon,
            contentDescription = "Prep",
            tint = NeonBlueAccent,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "BATTLE PREPARATION",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "\"Before every battle, prepare your mind.\"",
            style = MaterialTheme.typography.titleSmall.copy(
                color = RpgGold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Every session is entered through a dedicated Prep Chamber where you configure study aids, select instrumental RPG background combat tracks, and adjust focus objectives. Alignment boosts efficiency.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrepFeatureCard(title = "Focus Music", desc = "Ambient tracks", icon = Icons.Default.MusicNote, modifier = Modifier.weight(1f))
            PrepFeatureCard(title = "Study Tactics", desc = "Technique cards", icon = Icons.Default.Analytics, modifier = Modifier.weight(1f))
            PrepFeatureCard(title = "Skills Align", desc = "Train sub-skills", icon = Icons.Default.Psychology, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PrepFeatureCard(title: String, desc: String, icon: ImageVector, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = NeonBlueAccent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold))
            Text(text = desc, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted), textAlign = TextAlign.Center)
        }
    }
}

// ==========================================
// STEP 8: SKILLS GRIMOIRE
// ==========================================
@Composable
fun StepSkillsGrimoireExplanation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = "Grimoire",
            tint = RpgGold,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SKILLS GRIMOIRE",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\"Every subject becomes a permanent skill.\"",
            style = MaterialTheme.typography.titleSmall.copy(
                color = RpgGold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Track your learning achievements across specialized disciplines. Build your catalog of real-world knowledge spells and scale their levels through active study sessions:",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Grid-like list of skill examples
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkillPreviewItem(name = "Kotlin Programming", lvl = 5, progress = 0.8f)
            SkillPreviewItem(name = "Database Systems", lvl = 3, progress = 0.4f)
            SkillPreviewItem(name = "Machine Learning", lvl = 4, progress = 0.6f)
        }
    }
}

@Composable
fun SkillPreviewItem(name: String, lvl: Int, progress: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextWhite))
                Text(text = "LVL $lvl", style = MaterialTheme.typography.labelMedium.copy(color = RpgGold, fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                color = RpgGold,
                trackColor = BlackFantasyBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

// ==========================================
// STEP 9: GOLD & REWARDS
// ==========================================
@Composable
fun StepRewardsShopExplanation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalMall,
            contentDescription = "Shop",
            tint = RpgGold,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "REWARD SHOP",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = RpgGold,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "\"Gold represents effort you earned.\"",
            style = MaterialTheme.typography.titleSmall.copy(
                color = NeonBlueAccent,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Do not purchase items with actual money. Instead, buy custom-made real-life entertainment tokens using the gold acquired from your study sessions. Reclaim your rest guilt-free!",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShopExampleCard(title = "1 Hr Gaming", cost = 80, icon = Icons.Default.SportsEsports)
            ShopExampleCard(title = "Anime Episode", cost = 40, icon = Icons.Default.Tv)
            ShopExampleCard(title = "Favorite Snack", cost = 60, icon = Icons.Default.Fastfood)
        }
    }
}

@Composable
fun ShopExampleCard(title: String, cost: Int, icon: ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.width(110.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = RpgGold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = RpgGold, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = "$cost G", style = MaterialTheme.typography.labelSmall.copy(color = RpgGold, fontWeight = FontWeight.Bold))
            }
        }
    }
}

// ==========================================
// STEP 10: RED DUNGEON
// ==========================================
@Composable
fun StepRedDungeonExplanation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = RpgRuby,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "THE RED DUNGEON PENALTY",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = RpgRuby,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "\"Even Hunters fall sometimes.\"",
            style = MaterialTheme.typography.titleSmall.copy(
                color = RpgGold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Missing planned study sessions does not destroy your hard-earned progress. Instead, it triggers a safe Red Dungeon state where gold and XP yields are temporarily lowered to encourage return-to-action.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, RpgRuby.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "RECOVERY ALIGNMENT",
                    style = MaterialTheme.typography.titleSmall.copy(color = RpgRuby, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Simply perform any active study session to clear the penalty immediately. Use the Rest and Shelter system to schedule safe breaks without triggering penalty states.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }
    }
}

// ==========================================
// STEP 11: CREATE FIRST MISSION (WIZARD)
// ==========================================
@Composable
fun StepFirstMissionWizard(
    state: TutorialUiState,
    onDungeonNameChange: (String) -> Unit,
    onBossNameChange: (String) -> Unit,
    onHoursChange: (Int) -> Unit,
    onSkillChange: (String) -> Unit,
    onDayToggle: (String) -> Unit,
    onMinutesChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CONJURE YOUR FIRST QUEST",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = RpgGold,
                letterSpacing = 1.5.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Define your very first real-world study target, which we will materialize instantly as your starting Dungeon Realm.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Input forms
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dungeon Name
                Column {
                    Text(text = "1. DUNGEON NAME (Long-term context)", style = MaterialTheme.typography.labelSmall.copy(color = RpgGold, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.dungeonName,
                        onValueChange = onDungeonNameChange,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonBlueAccent,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_dungeon_name")
                    )
                }

                // Boss Name
                Column {
                    Text(text = "2. FIRST BOSS (Short-term major target)", style = MaterialTheme.typography.labelSmall.copy(color = RpgGold, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.bossName,
                        onValueChange = onBossNameChange,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonBlueAccent,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_boss_name")
                    )
                }

                // Estimated study hours -> slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "3. ESTIMATED STUDY HOURS", style = MaterialTheme.typography.labelSmall.copy(color = RpgGold, fontWeight = FontWeight.Bold))
                        Text(text = "${state.estimatedHours} Hours", style = MaterialTheme.typography.labelSmall.copy(color = NeonBlueAccent, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = state.estimatedHours.toFloat(),
                        onValueChange = { onHoursChange(it.toInt()) },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonBlueAccent,
                            activeTrackColor = NeonBlueAccent,
                            inactiveTrackColor = DarkCardBorder
                        ),
                        modifier = Modifier.testTag("wizard_hours_slider")
                    )
                }

                // Related Skill
                Column {
                    Text(text = "4. ASSOCIATED GRIMOIRE SKILL", style = MaterialTheme.typography.labelSmall.copy(color = RpgGold, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    val skills = listOf("Pomodoro Concentration", "Deep Learning Sage", "Memory Shield", "Critical Thinking Strike")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        skills.forEach { skill ->
                            val isSelected = state.selectedSkillName == skill
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) NeonBlueSecondary.copy(alpha = 0.2f) else BlackFantasyBackground,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonBlueAccent else DarkCardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSkillChange(skill) }
                                    .padding(horizontal = 12.dp)
                                    .height(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = skill,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) NeonBlueAccent else TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // Weekly Schedule Days
                Column {
                    Text(text = "5. WEEKLY SCHEDULE DAYS", style = MaterialTheme.typography.labelSmall.copy(color = RpgGold, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { day ->
                            val isSelected = state.scheduleDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (isSelected) NeonBlueSecondary.copy(alpha = 0.2f) else BlackFantasyBackground,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonBlueAccent else DarkCardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onDayToggle(day) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.take(1),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) NeonBlueAccent else TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // Daily Minutes
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "6. DAILY FOCUS GOAL", style = MaterialTheme.typography.labelSmall.copy(color = RpgGold, fontWeight = FontWeight.Bold))
                        Text(text = "${state.scheduleMinutes} Minutes", style = MaterialTheme.typography.labelSmall.copy(color = NeonBlueAccent, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = state.scheduleMinutes.toFloat(),
                        onValueChange = { onMinutesChange(it.toInt()) },
                        valueRange = 15f..180f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonBlueAccent,
                            activeTrackColor = NeonBlueAccent,
                            inactiveTrackColor = DarkCardBorder
                        ),
                        modifier = Modifier.testTag("wizard_minutes_slider")
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 12: BATTLE CELEBRATION
// ==========================================
@Composable
fun StepFirstBattleCelebration() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebration_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            // Shiny circle background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RpgGold.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Trophy",
                tint = RpgGold,
                modifier = Modifier.size(90.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "YOUR FIRST QUEST HAS BEGUN!",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = RpgGold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "You have consecrated your starting covenant, materializing your first custom Dungeon realm and Boss target inside your system grimoire.",
            style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Rewards Unlocked List
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, RpgGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "UNLOCKED SYSTEM BONUSES",
                    style = MaterialTheme.typography.labelSmall.copy(color = RpgGold, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                RewardCelebrationRow(icon = Icons.Default.Badge, text = "First Title Unlocked: \"Newborn Hunter\"")
                RewardCelebrationRow(icon = Icons.Default.AutoAwesome, text = "+100 Experience Points (XP)")
                RewardCelebrationRow(icon = Icons.Default.MonetizationOn, text = "+50 Dungeon Gold Coins")
            }
        }
    }
}

@Composable
fun RewardCelebrationRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = RpgGold, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite, fontWeight = FontWeight.Bold))
    }
}
