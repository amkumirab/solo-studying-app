package com.amkumirab.solostudying.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.notification.NotificationReceiver
import com.amkumirab.solostudying.sound.RpgSoundManager
import com.amkumirab.solostudying.ui.theme.*
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

enum class Tab(val title: String, val icon: ImageVector) {
    Dungeons("Dungeons", Icons.Default.Shield),
    Battle("Battle", Icons.Default.Timer),
    Shop("Reward Shop", Icons.Default.LocalMall),
    Stats("Profile Stats", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: SoloStudyingViewModel) {
    val bosses by viewModel.bosses.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val rewards by viewModel.rewards.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val skills by viewModel.skills.collectAsState()

    var currentTab by remember { mutableStateOf(Tab.Dungeons) }
    var showCreateBossDialog by remember { mutableStateOf(false) }
    var showCreateRewardDialog by remember { mutableStateOf(false) }
    var showFreeStudyDialog by remember { mutableStateOf(false) }

    // Before the Battle preparation states
    var prepBoss by remember { mutableStateOf<BossEntity?>(null) }
    var prepFreeStudyMins by remember { mutableStateOf<Int?>(null) }
    var prepSelectedSkill by remember { mutableStateOf<SkillEntity?>(null) }


    // Navigation fallback: If a battle is active, user can stay in any tab, but we flash the timer tab.
    val context = LocalContext.current

    if (userProfile != null && !userProfile!!.hasCompletedTutorial) {
        TutorialScreen(viewModel = viewModel.tutorialViewModel)
    } else if (userProfile != null && !userProfile!!.hasCompletedOnboarding) {
        OnboardingScreen(
            onFinish = { name, hClass, goal, path, days, mins, flex, weekdayMins ->
                viewModel.finishDetailedOnboarding(name, hClass, goal, path, days, mins, flex, weekdayMins)
            }
        )
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            RPGTopBar(profile = userProfile)
        },
        bottomBar = {
            RPGBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                isBattleActive = viewModel.isBattleActive
            )
        },
        containerColor = BlackFantasyBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BlackFantasyBackground)
        ) {
            // Display active notification dialogs/banners
            NotificationOverlays(viewModel = viewModel)

            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    Tab.Dungeons -> DungeonTab(
                        bosses = bosses,
                        activeBoss = viewModel.activeBoss,
                        isBattleActive = viewModel.isBattleActive,
                        profile = userProfile,
                        skills = skills,
                        viewModel = viewModel,
                        onCreateBossClicked = {
                            RpgSoundManager.playClickSound()
                            showCreateBossDialog = true
                        },
                        onFightBoss = { boss ->
                            RpgSoundManager.playClickSound()
                            prepBoss = boss
                            prepFreeStudyMins = null
                            prepSelectedSkill = viewModel.selectedSkillToTrain
                            currentTab = Tab.Battle
                        },
                        onDeleteBoss = { boss ->
                            RpgSoundManager.playClickSound()
                            viewModel.deleteBoss(boss)
                        },
                        onConquerRealBoss = { boss ->
                            RpgSoundManager.playClickSound()
                            viewModel.conquerRealBossManual(boss)
                        },
                        onEnterFreeStudyClicked = {
                            RpgSoundManager.playClickSound()
                            showFreeStudyDialog = true
                        }
                    )
                    Tab.Battle -> {
                        if (prepBoss != null || prepFreeStudyMins != null) {
                            BeforeTheBattleScreen(
                                boss = prepBoss,
                                freeStudyMins = prepFreeStudyMins,
                                selectedSkill = prepSelectedSkill,
                                userProfile = userProfile ?: UserProfileEntity(),
                                onBeginBattle = {
                                    if (prepBoss != null) {
                                        viewModel.selectedSkillToTrain = prepSelectedSkill
                                        viewModel.selectAndStartBattle(prepBoss!!)
                                    } else if (prepFreeStudyMins != null) {
                                        viewModel.selectedSkillToTrain = prepSelectedSkill
                                        viewModel.selectAndStartFreeStudy(prepFreeStudyMins!!)
                                    }
                                    prepBoss = null
                                    prepFreeStudyMins = null
                                    prepSelectedSkill = null
                                },
                                onCancel = {
                                    prepBoss = null
                                    prepFreeStudyMins = null
                                    prepSelectedSkill = null
                                    currentTab = Tab.Dungeons
                                }
                            )
                        } else {
                            BattleTab(
                                viewModel = viewModel,
                                onGoToDungeons = { currentTab = Tab.Dungeons }
                            )
                        }
                    }
                    Tab.Shop -> ShopTab(
                        rewards = rewards,
                        balances = balances,
                        goldBalance = userProfile?.gold ?: 0,
                        onCreateRewardClicked = { showCreateRewardDialog = true },
                        onPurchaseReward = { reward, callback ->
                            viewModel.purchaseReward(reward, callback)
                        },
                        onUseReward = { rewardName, hours, callback ->
                            viewModel.useRewardTime(rewardName, hours, callback)
                        },
                        onDeleteReward = { reward -> viewModel.deleteReward(reward) }
                    )
                    Tab.Stats -> StatsTab(
                        profile = userProfile,
                        sessions = sessions,
                        skills = skills,
                        onCreateSkill = { name, hrs, sug -> viewModel.createSkill(name, hrs, sug) },
                        onDeleteSkill = { skill -> viewModel.deleteSkill(skill) },
                        onUpdateSchedule = { days, mins, flex, weekdayMins -> viewModel.updateScheduleWithWeekdays(days, mins, flex, weekdayMins) },
                        onTriggerSimulatedNotification = { action -> viewModel.simulateCompanionNotification(action) },
                        onReplayTutorial = { viewModel.tutorialViewModel.replayTutorial() }
                    )
                }
            }
        }
    }

    if (showCreateBossDialog) {
        CreateBossDialog(
            onDismiss = { showCreateBossDialog = false },
            onConfirm = { name, difficulty, minutesOption, dungeonName, isRealBoss ->
                viewModel.createBoss(name, difficulty, minutesOption, null, dungeonName, isRealBoss)
                showCreateBossDialog = false
            }
        )
    }

    if (showCreateRewardDialog) {
        CreateRewardDialog(
            onDismiss = { showCreateRewardDialog = false },
            onConfirm = { name, desc, cost, type, value ->
                viewModel.createReward(name, desc, cost, type, value)
                showCreateRewardDialog = false
            }
        )
    }

    if (showFreeStudyDialog) {
        var durationInput by remember { mutableStateOf("30") }
        var dropdownExpanded by remember { mutableStateOf(false) }
        var studySkillSelected by remember { mutableStateOf<SkillEntity?>(null) }

        Dialog(onDismissRequest = { showFreeStudyDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                border = BorderStroke(1.5.dp, NeonBlueAccent),
                modifier = Modifier.padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ENTER ASTRAL PLANE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = NeonBlueAccent,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Meditate in a fluid casual focus sanctuary. Earn relaxed Gold & XP, decay inactivity, and flex focus periods dynamically.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )

                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = {
                            if (it.isEmpty() || it.all { c -> c.isDigit() }) durationInput = it
                        },
                        label = { Text("Study Duration (Minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedLabelColor = NeonBlueAccent,
                            focusedIndicatorColor = NeonBlueAccent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Target Skill drop-down
                    if (skills.isNotEmpty()) {
                        Column {
                            Text("ALLOCATE PROGRESS TO SKILL:", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { dropdownExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = studySkillSelected?.name?.uppercase() ?: "NO SKILL SELECTED (NONE)",
                                    color = if (studySkillSelected != null) NeonBlueAccent else TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(DarkFantasySurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("NONE / NO ALLOCATION", color = TextWhite) },
                                    onClick = {
                                        studySkillSelected = null
                                        dropdownExpanded = false
                                    }
                                )
                                skills.forEach { sk ->
                                    DropdownMenuItem(
                                        text = { Text(sk.name.uppercase(), color = NeonBlueAccent) },
                                        onClick = {
                                            studySkillSelected = sk
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showFreeStudyDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("BACK")
                        }

                        val fMins = durationInput.toIntOrNull() ?: 30
                        Button(
                            onClick = {
                                if (fMins > 0) {
                                    prepBoss = null
                                    prepFreeStudyMins = fMins
                                    prepSelectedSkill = studySkillSelected
                                    showFreeStudyDialog = false
                                    currentTab = Tab.Battle
                                }
                            },
                            enabled = fMins > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlueAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(2f)
                        ) {
                            Text("BEGIN DUEL", color = BlackFantasyBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
}

// ==========================================
// COMPOSABLE: TOP BAR WITH HEALTH & PROGRESS
// ==========================================
@Composable
fun RPGTopBar(profile: UserProfileEntity?) {
    val nonNullProfile = profile ?: UserProfileEntity()
    val nextLevelXp = nonNullProfile.level * 150
    val xpProgress = if (nextLevelXp > 0) nonNullProfile.xp.toFloat() / nextLevelXp else 0f
    val rankLabel = getRankLabel(nonNullProfile.level)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkFantasySurface)
            .drawBehind {
                drawLine(
                    color = NeonBlueAccent.copy(alpha = 0.25f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Profile Info with circular level icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .border(2.dp, NeonBlueAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LV.${nonNullProfile.level}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonBlueAccent,
                            fontSize = 11.sp
                        )
                    )
                }

                Column {
                    Text(
                        text = rankLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.5.sp,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = nonNullProfile.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            // Streak & Gold Economy Indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (nonNullProfile.currentStreak > 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(RpgRuby.copy(alpha = 0.15f))
                            .border(1.dp, RpgRuby.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak Fire",
                            tint = RpgRuby,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${nonNullProfile.currentStreak}D",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = RpgRuby,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(RpgGold)
                    )
                    Text(
                        text = "${nonNullProfile.gold}G",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = RpgGold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.testTag("gold_counter")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // XP Progression Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "XP",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonBlueAccent,
                    fontSize = 10.sp
                )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(xpProgress)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(RpgRuby, NeonBlueAccent) // Red to Cyan gradient bar
                            )
                        )
                )
            }
            Text(
                text = "${nonNullProfile.xp}/${nextLevelXp} XP",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

// ==========================================
// COMPOSABLE: BOTTOM NAVIGATION BAR
// ==========================================
@Composable
fun RPGBottomBar(currentTab: Tab, onTabSelected: (Tab) -> Unit, isBattleActive: Boolean) {
    NavigationBar(
        containerColor = DarkFantasySurface,
        tonalElevation = 8.dp,
        modifier = Modifier.drawBehind {
            drawLine(
                color = NeonBlueAccent.copy(alpha = 0.25f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    ) {
        Tab.values().forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    RpgSoundManager.playClickSound()
                    onTabSelected(tab)
                },
                icon = {
                    Box {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) NeonBlueAccent else TextMuted
                        )
                        if (tab == Tab.Battle && isBattleActive) {
                            // Flash a red indicator on battle tab when studying
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(RpgRuby)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NeonBlueAccent else TextMuted
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF1C2237)
                )
            )
        }
    }
}

// ==========================================
// TAB: 1. DUNGEONS TAB
// ==========================================
@Composable
fun DungeonTab(
    bosses: List<BossEntity>,
    activeBoss: BossEntity?,
    isBattleActive: Boolean,
    profile: UserProfileEntity?,
    skills: List<SkillEntity>,
    viewModel: SoloStudyingViewModel,
    onCreateBossClicked: () -> Unit,
    onFightBoss: (BossEntity) -> Unit,
    onDeleteBoss: (BossEntity) -> Unit,
    onConquerRealBoss: (BossEntity) -> Unit,
    onEnterFreeStudyClicked: () -> Unit
) {
    var selectedDungeonCategory by remember { mutableStateOf("All") }
    
    // Accumulate all distinct dungeon categories created by the player
    val dungeonsList = remember(bosses) {
        listOf("All") + bosses.map { it.dungeonName }.distinct().filter { it.isNotBlank() }
    }

    val filteredBosses = remember(bosses, selectedDungeonCategory) {
        if (selectedDungeonCategory == "All") bosses 
        else bosses.filter { it.dungeonName == selectedDungeonCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Dungeons",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite
                    )
                )
                Text(
                    text = "Slay your study obstacles",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted
                    )
                )
            }

            Button(
                onClick = onCreateBossClicked,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlueSecondary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("create_boss_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Boss icon", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Summon Boss", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // System 7: RED DUNGEON (PENALTY SYSTEM) VISUAL STRESS INDICATOR
        val redDungeonDays = profile?.redDungeonDays ?: 0
        val isBoostActive = profile?.isRedDungeonBoostActive == true
        val totalCleared = profile?.totalRedDungeonsCleared ?: 0
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (redDungeonDays > 0) Color(0xFF2E090F) else Color(0xFF161012)
            ),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (redDungeonDays > 0) RpgRuby else Color(0xFF4C1D24)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (redDungeonDays > 0) "🔴 ACTIVE RED GATE" else "🛡️ RED GATES",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (redDungeonDays > 0) RpgRuby else TextWhite
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (redDungeonDays > 0) RpgRuby.copy(alpha = 0.2f) else Color(0xFF2E090F))
                                .border(1.dp, if (redDungeonDays > 0) RpgRuby else Color(0xFF4C1D24), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (redDungeonDays > 0) "BREACH LEVEL $redDungeonDays" else "SECURED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (redDungeonDays > 0) RpgRuby else RpgGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    
                    Text(
                        text = "⚔️ PURIFIED: $totalCleared",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = RpgGold,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (redDungeonDays > 0) {
                    Text(
                        text = "Your procrastination has let dark rifts breach the perimeter! Normal rewards are reduced by -20% XP/Gold. You need to complete any focus study session to seal this breach.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextWhite.copy(alpha = 0.85f))
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF4C1D24), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⚡ RED GATE 2X XP BOOSTER",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = RpgGold
                                    )
                                )
                                Text(
                                    text = "Finish faster: Double XP payout on purification!",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            if (isBoostActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RpgGold.copy(alpha = 0.15f))
                                        .border(1.dp, RpgGold, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🔥 2X XP ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = RpgGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        RpgSoundManager.playClickSound()
                                        viewModel.activateRedDungeonXpBoost()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RpgGold),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "BUY (100 G)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = BlackFantasyBackground,
                                            fontWeight = FontWeight.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "The barriers are holding perfectly. Keep up your daily study streak to prevent Crimson Gates from spawning.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Casually Entered Free Study Zone Portal banner/hub
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEnterFreeStudyClicked() }
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E3A)),
            border = BorderStroke(1.dp, NeonBlueAccent.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cyclone,
                            contentDescription = "Astral vortex portal",
                            tint = NeonBlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ASTRAL PORTAL (FREE STUDY)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = NeonBlueAccent,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Study casually with custom timer focus.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go portal",
                    tint = NeonBlueAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dungeon Categories/Folders Scrollable filters
        if (dungeonsList.size > 1) {
            Text(
                text = "📁 SELECT REALM DUNGEON",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonBlueAccent,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dungeonsList.forEach { dung ->
                    val isSel = selectedDungeonCategory == dung
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) NeonBlueAccent else DarkFantasySurface)
                            .border(1.dp, if (isSel) NeonBlueAccent else DarkCardBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedDungeonCategory = dung }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = dung.uppercase(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) BlackFantasyBackground else TextWhite
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (filteredBosses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkFantasySurface)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(listOf(DarkCardBorder, Color.Transparent)),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Empty dungeons shield",
                        tint = NeonBlueAccent.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "DUNGEON REALM CLEAR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No boss summons exist in this category. Summon an academic obstacle or change filters!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredBosses.forEach { boss ->
                    BossCard(
                        boss = boss,
                        activeBoss = activeBoss,
                        isBattleActive = isBattleActive,
                        onFightBoss = { onFightBoss(boss) },
                        onDeleteBoss = { onDeleteBoss(boss) },
                        onConquerRealBoss = { onConquerRealBoss(boss) }
                    )
                }
            }
        }
    }
}

// Boss rendering with customized linear themes
@Composable
fun BossCard(
    boss: BossEntity,
    activeBoss: BossEntity?,
    isBattleActive: Boolean,
    onFightBoss: () -> Unit,
    onDeleteBoss: () -> Unit,
    onConquerRealBoss: () -> Unit
) {
    val isCurrentlyFightingThis = activeBoss?.id == boss.id
    val difficultyColor = getDifficultyColor(boss.difficulty)
    val backgroundBrush = getBossCardBrush(boss.difficulty)
    val cardBorderColor = if (isCurrentlyFightingThis) NeonBlueAccent else if (boss.isRealBoss) RpgGold else DarkCardBorder

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("boss_item_${boss.name.replace(" ", "_")}")
            .border(
                width = if (isCurrentlyFightingThis || boss.isRealBoss) 2.dp else 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface)
    ) {
        Column {
            // Header Image Box with canvas styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(backgroundBrush)
            ) {
                // Procedural overlay grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = 8
                    val rows = 3
                    val gridW = size.width / cols
                    val gridH = size.height / rows
                    for (i in 1 until cols) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(i * gridW, 0f),
                            end = Offset(i * gridW, size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (i in 1 until rows) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(0f, i * gridH),
                            end = Offset(size.width, i * gridH),
                            strokeWidth = 1f
                        )
                    }
                }

                // Banner Details
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Difficulty Tag & Goal category
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .border(1.dp, difficultyColor.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = boss.difficulty.uppercase(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = difficultyColor
                                    )
                                )
                            }

                            if (boss.isRealBoss) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RpgGold.copy(alpha = 0.3f))
                                        .border(1.dp, RpgGold, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "👑 DELIVERABLE",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = RpgGold
                                        )
                                    )
                                }
                            }
                        }

                        // Completion indicator
                        if (boss.isCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(RpgEmerald.copy(alpha = 0.2f))
                                    .border(1.dp, RpgEmerald, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SLAIN",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = RpgEmerald
                                    )
                                )
                            }
                        }
                    }

                    // Boss Title
                    Column {
                        Text(
                            text = boss.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                letterSpacing = 1.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "REQUIRED: ${boss.requiredMinutes} Mins Focus",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextWhite.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (boss.dungeonName.isNotBlank() && boss.dungeonName != "Main Realm") {
                                Text(
                                    text = "📁 ${boss.dungeonName.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = NeonBlueAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Lower Info Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Info Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progressSeconds = boss.timeSpentSeconds
                    val totalTargetSeconds = boss.requiredMinutes * 60L
                    val completionPercent = if (totalTargetSeconds > 0) {
                        ((progressSeconds.toFloat() / totalTargetSeconds) * 100).coerceAtMost(100f)
                    } else 0f

                    Column {
                        Text(
                            text = "PROGRESS DAMAGE",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%% DMG", completionPercent),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = NeonBlueAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "INVESTED TIME",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = formatStudyTimeShort(boss.timeSpentSeconds),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDeleteBoss,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF231E29)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = RpgRuby)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete boss item"
                        )
                    }

                    // MANUAL CONQUEST: If it's a real-life deliverable exam/submission, OR if it's the currently active study boss, show Manual "COMPLETED" bypass
                    if ((boss.isRealBoss || isCurrentlyFightingThis) && !boss.isCompleted) {
                        Button(
                            onClick = onConquerRealBoss,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF332515), contentColor = RpgGold),
                            border = BorderStroke(1.dp, RpgGold)
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CONQUER", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    if (isCurrentlyFightingThis) {
                        Button(
                            onClick = onFightBoss,
                            colors = ButtonDefaults.buttonColors(containerColor = RpgRuby),
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = "Active battle indicator", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ACTIVE FIGHT", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onFightBoss,
                            enabled = true,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (boss.isCompleted) Color(0xFF1F2F2D) else NeonBlueAccent,
                                contentColor = if (boss.isCompleted) RpgEmerald else BlackFantasyBackground
                            ),
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (boss.isCompleted) Icons.Default.Replay else Icons.Default.Shield,
                                contentDescription = "Enter fight icon",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (boss.isCompleted) {
                                    if (isBattleActive) "SWITCH (FARM)" else "FIGHT AGAIN (FARM)"
                                } else {
                                    if (isBattleActive) "SWITCH TO THIS" else "BEGIN DEFY QUEST"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB: 2. BATTLE SCREEN (TIMER VIEW)
// ==========================================
@Composable
fun BattleTab(
    viewModel: SoloStudyingViewModel,
    onGoToDungeons: () -> Unit
) {
    val boss = viewModel.activeBoss
    val isFreeStudy = viewModel.isFreeStudyActive

    if (boss == null && !isFreeStudy) {
        // No Quest Empty State
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    tint = NeonBlueAccent.copy(alpha = 0.25f),
                    contentDescription = "Empty battle portrait",
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NO QUEST UNDERWAY",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        letterSpacing = 1.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter a dungeon challenge to summon and duel an academic boss, or step through the Astral Portal for casual study!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onGoToDungeons,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlueAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Enter Dungeons", color = BlackFantasyBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // Battle Screen Layout - supporting both Boss and Casual Meditation
        val targetSeconds = if (isFreeStudy) {
            (viewModel.battleTimeLeftSeconds + viewModel.battleTimeSpentSeconds).coerceAtLeast(60L)
        } else {
            (boss?.requiredMinutes ?: 30) * 60L
        }
        val hpPercent = if (targetSeconds > 0) {
            ((viewModel.battleTimeLeftSeconds.toFloat() / targetSeconds)).coerceIn(0f, 1f)
        } else 0f
        val damagePercent = 1f - hpPercent

        val categoryText = if (isFreeStudy) {
            "ASTRAL PLANE FOCUS"
        } else {
            "DUNGEON: ${(boss?.dungeonName ?: "Main Realm").uppercase()}"
        }

        val nameText = if (isFreeStudy) {
            "ASTRAL FREE STUDY ZONE"
        } else {
            boss?.name?.uppercase() ?: ""
        }

        val requiredMins = if (isFreeStudy) {
            (targetSeconds / 60).toInt()
        } else {
            boss?.requiredMinutes ?: 30
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = if (isFreeStudy) "🌌 CASUAL MEDITATION PORTAL" else "⚔️ ACTIVE BOSS ENCOUNTER",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isFreeStudy) NeonBlueAccent else RpgRuby,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = nameText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )

                // Trained Skill indicator
                viewModel.selectedSkillToTrain?.let { sk ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonBlueAccent.copy(alpha = 0.15f))
                            .border(1.dp, NeonBlueAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🎯 TRAINING ACADEMY SKILL: ${sk.name.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonBlueAccent,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Animated glowing landscape for active fight - THE RITUAL BATTLE RING
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Frame containing radial ambient glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        (if (isFreeStudy) NeonBlueAccent else RpgRuby).copy(alpha = 0.08f),
                                        Color.Transparent
                                    ),
                                    radius = 350f
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        ) {
                            // Boss Ritual Ring layout
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // 1. Dashed Ring on Canvas
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = (if (isFreeStudy) NeonBlueAccent else RpgRuby).copy(alpha = 0.15f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                        )
                                    )
                                }
                                // 2. Inner thin Solid Ring
                                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                    drawCircle(
                                        color = (if (isFreeStudy) NeonBlueAccent else RpgRuby).copy(alpha = 0.25f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                    )
                                }
                                // Active boss portal
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(18.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isFreeStudy) {
                                        // Meditative vortex icon
                                        Icon(
                                            imageVector = Icons.Default.Cyclone,
                                            contentDescription = null,
                                            tint = NeonBlueAccent,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    } else {
                                        // Three glowing neon pillar lines representing the Machine Learning Hydra core
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(6.dp)
                                                    .height(40.dp)
                                                    .clip(CircleShape)
                                                    .background(RpgRuby)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(6.dp)
                                                    .height(56.dp)
                                                    .clip(CircleShape)
                                                    .background(RpgRuby)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(6.dp)
                                                    .height(40.dp)
                                                    .clip(CircleShape)
                                                    .background(RpgRuby)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background((if (isFreeStudy) NeonBlueAccent else getDifficultyColor(boss?.difficulty ?: "Medium")).copy(alpha = 0.15f))
                                    .border(1.dp, (if (isFreeStudy) NeonBlueAccent else getDifficultyColor(boss?.difficulty ?: "Medium")).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isFreeStudy) "ASTRAL CHILL" else "LEVEL ${boss?.difficulty?.uppercase() ?: "MEDIUM"}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isFreeStudy) NeonBlueAccent else getDifficultyColor(boss?.difficulty ?: "Medium"),
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Health/Energy Bar of representation
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (isFreeStudy) "SOUL RESONANCE" else "BOSS VITALITY",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFreeStudy) NeonBlueAccent else RpgRuby,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (isFreeStudy) {
                                "${(viewModel.battleTimeLeftSeconds / 60) + 1} / $requiredMins MINS"
                            } else {
                                "${(viewModel.battleTimeLeftSeconds / 60) + 1} / ${boss?.requiredMinutes ?: 30} HP"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        // Health sliding Red to Cyan Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(hpPercent)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        if (isFreeStudy) listOf(Color(0xFF2B5C8F), NeonBlueAccent) else listOf(RpgRuby, NeonBlueAccent)
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = categoryText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                              )
                        )
                        Text(
                            text = if (isFreeStudy) {
                                "FOCUSED: ${viewModel.battleTimeSpentSeconds}S"
                            } else {
                                "DEALT ${String.format(Locale.getDefault(), "%.1f", damagePercent * 100f)}% DAMAGE"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFreeStudy) NeonBlueAccent else RpgRuby,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Battle Timer & Active Progress
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (viewModel.isBattlePaused) "ENGAGED - FIGHT PAUSED" else "ENGAGED IN COMBAT",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonBlueAccent,
                                letterSpacing = 3.sp,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        // Massive tab-digit font-mono countdown text
                        Text(
                            text = formatCountdown(viewModel.battleTimeLeftSeconds),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = TextWhite,
                                fontSize = 56.sp,
                                letterSpacing = (-1).sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "STUDIED SECONDS: ${viewModel.battleTimeSpentSeconds}S",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RpgGold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Real Interactive Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (viewModel.isBattleActive && !viewModel.isBattlePaused) {
                        Button(
                            onClick = {
                                RpgSoundManager.playClickSound()
                                viewModel.pauseBattle()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3047)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause fight")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PAUSE BATTLE", fontWeight = FontWeight.Bold)
                        }
                    } else if (viewModel.isBattlePaused) {
                        Button(
                            onClick = {
                                RpgSoundManager.playClickSound()
                                viewModel.resumeBattle()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlueSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume fight")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESUME BATTLE", fontWeight = FontWeight.Bold)
                        }
                    }

                    // CONQUER action directly on the Active Battle Screen
                    Button(
                        onClick = { viewModel.completeActiveBoss() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isFreeStudy) Color(0xFF1F2F2D) else Color(0xFF2E2315)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f),
                        border = BorderStroke(1.dp, if (isFreeStudy) RpgEmerald else RpgGold)
                    ) {
                        Icon(
                            imageVector = if (isFreeStudy) Icons.Default.CheckCircleOutline else Icons.Default.Celebration,
                            contentDescription = "Complete active session",
                            tint = if (isFreeStudy) RpgEmerald else RpgGold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFreeStudy) "FINISH STUDY" else "CONQUER",
                            color = if (isFreeStudy) RpgEmerald else RpgGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Retreat/Abandon action
                    Button(
                        onClick = {
                            RpgSoundManager.playClickSound()
                            viewModel.abandonActiveBoss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF321B21)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OutlinedFlag, contentDescription = "Flee battle", tint = RpgRuby)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RETREAT", color = RpgRuby, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Fast Emulator Simulation helpers
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131422)),
                    border = BorderStroke(1.dp, RpgGold.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚡ ADVENTURER REVELATION CODES (EMULATOR BYPASS)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = RpgGold,
                                fontSize = 9.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.simulateStudySeconds(300L) }, // +5 min
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B293A)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text("+5 MIN DEALT", color = NeonBlueAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.simulateStudySeconds(1200L) }, // +20 min
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C1E3F)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text("+20 MIN DEALT", color = Color(0xFFC582FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.completeActiveBoss() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B3024)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text("AUTO-SLAY", color = RpgEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB: 3. REWARD SHOP TAB
// ==========================================
@Composable
fun ShopTab(
    rewards: List<RewardItemEntity>,
    balances: List<RewardBalanceEntity>,
    goldBalance: Int,
    onCreateRewardClicked: () -> Unit,
    onPurchaseReward: (RewardItemEntity, (Boolean, String) -> Unit) -> Unit,
    onUseReward: (String, Float, (Boolean) -> Unit) -> Unit,
    onDeleteReward: (RewardItemEntity) -> Unit
) {
    var shopToastMessage by remember { mutableStateOf<String?>(null) }
    var shopToastSuccess by remember { mutableStateOf(true) }

    LaunchedEffect(shopToastMessage) {
        if (shopToastMessage != null) {
            delay(3500)
            shopToastMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Adventurer Shop",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite
                    )
                )
                Text(
                    text = "Deduct study-earned Gold for guild rewards",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onCreateRewardClicked,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlueSecondary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("create_reward_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Gavel, contentDescription = "Forge reward button", modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Forge Reward", fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Persistent custom styled Toast message
        AnimatedVisibility(
            visible = shopToastMessage != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            shopToastMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("shop_toast"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (shopToastSuccess) Color(0xFF142E25) else Color(0xFF34171E)
                    ),
                    border = BorderStroke(1.dp, if (shopToastSuccess) RpgEmerald else RpgRuby)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (shopToastSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = "Success check indicator",
                            tint = if (shopToastSuccess) RpgEmerald else RpgRuby
                        )
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Unlocked inventory balances (Time-Based)
            item {
                Text(
                    text = "🛡️ SECURED REWARD INVENTORY (AVAILABLE TO SPEND)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (balances.isEmpty() || balances.none { it.availableHours > 0f }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "No stored reward assets inside inventory. Complete study duels against bosses, buy rewards with gold below, and they will accumulate here safely!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        balances.filter { it.availableHours > 0f }.forEach { balance ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C30)),
                                border = BorderStroke(1.dp, NeonBlueAccent.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = balance.rewardName.uppercase(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite
                                            )
                                        )
                                        Text(
                                            text = "Available Amount: ${String.format(Locale.getDefault(), "%.1f", balance.availableHours)} Hours",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = RpgGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            onUseReward(balance.rewardName, 1f) { success ->
                                                if (success) {
                                                    shopToastSuccess = true
                                                    shopToastMessage = "Spent 1 Unit of ${balance.rewardName}! Bask in your reward time guilt-free!"
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RpgEmerald),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Spend 1 Unit", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Purchasing shop items
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "🪙 REWARD MERCHANT ITEMS",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(rewards) { reward ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_item_${reward.name.replace(" ", "_")}"),
                    colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Graphic Box representation of Reward
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (reward.rewardType == "Time-Based")
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)),
                                            radius = 120f
                                        )
                                    else
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFF581C87), Color(0xFF0F172A)),
                                            radius = 120f
                                        )
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (reward.rewardType == "Time-Based") NeonBlueAccent else Color(0xFFFF5CE2),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (reward.rewardType == "Time-Based") Icons.Default.HourglassEmpty else Icons.Default.EmojiEvents,
                                contentDescription = "Reward representation icon",
                                tint = if (reward.rewardType == "Time-Based") NeonBlueAccent else Color(0xFFFF5CE2),
                                modifier = Modifier.size(32.dp)
                            )
                            
                            // RPG item quantity/duration badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 12.dp)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = if (reward.rewardType == "Time-Based") NeonBlueAccent.copy(alpha = 0.5f) else Color(0xFFFF5CE2).copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 12.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (reward.rewardType == "Time-Based") "${reward.rewardValue}H" else "1X",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (reward.rewardType == "Time-Based") NeonBlueAccent else Color(0xFFFF5CE2)
                                    )
                                )
                            }
                        }

                        // Text content
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reward.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (reward.isCustom) {
                                    IconButton(
                                        onClick = { onDeleteReward(reward) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete reward item",
                                            tint = RpgRuby,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = reward.description,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Price
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = "Gold coin icon",
                                        tint = RpgGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${reward.cost} GOLD",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = RpgGold
                                        )
                                    )
                                }

                                Button(
                                    onClick = {
                                        onPurchaseReward(reward) { success, msg ->
                                            shopToastSuccess = success
                                            shopToastMessage = msg
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (goldBalance >= reward.cost) RpgGold else Color(0xFF24201A),
                                        contentColor = if (goldBalance >= reward.cost) Color.Black else TextMuted
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("buy_button_${reward.name.replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = "ACQUIRE",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Black
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

// ==========================================
// TAB: 4. RPG PLAYER PROFILE & STATS TAB
// ==========================================
@Composable
fun StatsTab(
    profile: UserProfileEntity?,
    sessions: List<StudySessionEntity>,
    skills: List<SkillEntity>,
    onCreateSkill: (String, Int, String) -> Unit,
    onDeleteSkill: (SkillEntity) -> Unit,
    onUpdateSchedule: (String, Int, String, String) -> Unit,
    onTriggerSimulatedNotification: (String) -> Unit,
    onReplayTutorial: () -> Unit
) {
    val nonNullProfile = profile ?: UserProfileEntity()
    val rankText = getRankLabel(nonNullProfile.level)

    var showSummonSkillDialog by remember { mutableStateOf(false) }
    var showScheduleContractDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                        text = "Guild Scholar Class Player",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

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

        // System 8: DYNAMIC STUDY SCHEDULE GUILD CONTRACT DISPLAY
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📜 GUILD STUDY CONTRACT",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    )
                )

                Text(
                    text = "EDIT CONTRACT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = RpgGold
                    ),
                    modifier = Modifier.clickable { showScheduleContractDialog = true }
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val daysPrint = if (nonNullProfile.scheduleDays.isBlank()) "None Selected" else nonNullProfile.scheduleDays
                    val flexPrint = if (nonNullProfile.scheduleFlexibility.isBlank()) "Adaptable Study Plan" else nonNullProfile.scheduleFlexibility
                    
                    Text(
                        text = "This sacred pact outlines your academic constraints for the current realm. Breaking alignment resets non-permanent milestones.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatRow(label = "Days of Duel Duty", value = daysPrint)
                    
                    val daysOfWeekList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val targetsList = nonNullProfile.scheduleWeekdayMinutes.split(",").map { it.trim().toIntOrNull() ?: 45 }
                    val weekdayTargetsFormatted = daysOfWeekList.mapIndexed { idx, day ->
                        val mins = if (idx < targetsList.size) targetsList[idx] else 45
                        "$day: ${mins}m"
                    }.joinToString(" | ")
                    
                    StatRow(label = "Custom Weekday Targets", value = weekdayTargetsFormatted)
                    StatRow(label = "Flex Class Covenant", value = flexPrint)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔊 RPG COMPANION MESSENGER",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Trigger context-aware RPG messages directly to your Android notifications based on your current schedule, active bosses, streaks, and goals.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                RpgSoundManager.playClickSound()
                                onTriggerSimulatedNotification(NotificationReceiver.ACTION_MORNING_QUEST)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF132238)),
                            border = BorderStroke(1.dp, NeonBlueAccent.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).testTag("sim_morning_button")
                        ) {
                            Text("☀️ MORNING", color = NeonBlueAccent, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        Button(
                            onClick = {
                                RpgSoundManager.playClickSound()
                                onTriggerSimulatedNotification(NotificationReceiver.ACTION_BEFORE_STUDY)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281C38)),
                            border = BorderStroke(1.dp, RpgGold.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).testTag("sim_before_button")
                        ) {
                            Text("🔔 RITUAL", color = RpgGold, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        Button(
                            onClick = {
                                RpgSoundManager.playClickSound()
                                onTriggerSimulatedNotification(NotificationReceiver.ACTION_EVENING_CAMPAIGN)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E191D)),
                            border = BorderStroke(1.dp, RpgRuby.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).testTag("sim_evening_button")
                        ) {
                            Text("⏳ TWILIGHT", color = RpgRuby, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Replay Tutorial / System Controls Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "⚙️ SYSTEM CONTROLS",
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
                    border = BorderStroke(1.dp, DarkCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Want to re-experience the immersive Hunter Awakening Ritual? You can replay the introductory covenant setup at any time.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                RpgSoundManager.playClickSound()
                                onReplayTutorial()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1B22)),
                            border = BorderStroke(1.dp, RpgGold.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("replay_tutorial_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = RpgGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("REPLAY AWAKENING CEREMONY", color = RpgGold, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
                            }
                        }
                    }
                }
            }
        }

        // System 6: SKILL ACADEMY INSTRUCTION SECTION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎓 HERO SKILL ACADEMY",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    )
                )

                Button(
                    onClick = { showSummonSkillDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131C30)),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonBlueAccent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SUMMON SKILL", fontSize = 10.sp, color = NeonBlueAccent, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (skills.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NO SKILLS MASTERED YET",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Summon customized educational skills (e.g., French Nouns, Physics Rigor) and train them during active focus sessions to elevate them!",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, textAlign = TextAlign.Center),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(skills) { sk ->
                val totalSeconds = sk.targetMinutes * 60L
                val skillProgress = if (totalSeconds > 0) {
                    (sk.spentSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
                } else 0f
                val activeLvl = (sk.spentSeconds / 3600) + 1
                val isUnlocked = sk.spentSeconds >= totalSeconds

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13141E)),
                    border = BorderStroke(1.dp, if (isUnlocked) RpgGold.copy(alpha = 0.4f) else DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = sk.name.uppercase(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isUnlocked) RpgGold else TextWhite
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isUnlocked) RpgGold.copy(alpha = 0.15f) else Color.Black)
                                            .border(1.dp, if (isUnlocked) RpgGold else DarkCardBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "LVL $activeLvl",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isUnlocked) RpgGold else NeonBlueAccent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = sk.suggestion,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }

                            // Disintegrate action
                            IconButton(
                                onClick = { onDeleteSkill(sk) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Disintegrate skill icon", tint = RpgRuby, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar to Unlock/Master Skill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUnlocked) "⭐ TRANSCENDED MASTER" else "UNLEASHING RESILIENCE",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isUnlocked) RpgGold else NeonBlueAccent,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "${sk.spentSeconds / 60}/${sk.targetMinutes} MINS",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(skillProgress)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            if (isUnlocked) listOf(RpgGold, Color(0xFFFCD34D)) else listOf(NeonBlueAccent, Color(0xFFC582FF))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
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

        // Section: Activity log list
        item {
            Text(
                text = "📜 HISTORICAL DUNGEON CHRONICLES",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonBlueAccent,
                    letterSpacing = 1.sp
                )
            )
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Your journal has no records. Battle bosses inside dungeons to log focus activities!",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(sessions) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10111B)),
                    border = BorderStroke(1.dp, if (session.wasCompleted) RpgEmerald.copy(alpha = 0.15f) else RpgRuby.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (session.wasCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = "Session result status icon",
                                    tint = if (session.wasCompleted) RpgEmerald else RpgRuby,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = (session.bossName ?: "Unknown Quest").uppercase(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                )
                            }
                            Text(
                                text = "Studied: ${formatStudyTimeShort(session.durationSeconds)} • ${formatSessionDate(session.timestamp)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+${session.xpEarned} XP",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonBlueAccent
                                )
                            )
                            Text(
                                text = "+${session.goldEarned} G",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RpgGold
                                )
                            )
                        }
                    }
                }
            }
        }

    }

    // Interactive Dialog 1: Summon custom skill
    if (showSummonSkillDialog) {
        var skillName by remember { mutableStateOf("") }
        var targetHrsInput by remember { mutableStateOf("10") } // Default target 10 hours
        var suggInput by remember { mutableStateOf("Specialized expertise training") }

        Dialog(onDismissRequest = { showSummonSkillDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                border = BorderStroke(1.5.dp, NeonBlueAccent),
                modifier = Modifier.padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "SUMMON ACADEMY SKILL",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = NeonBlueAccent,
                            letterSpacing = 1.sp
                        )
                    )

                    OutlinedTextField(
                        value = skillName,
                        onValueChange = { skillName = it },
                        label = { Text("Skill Name (e.g. Physics Rigor)") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedLabelColor = NeonBlueAccent,
                            focusedIndicatorColor = NeonBlueAccent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = targetHrsInput,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) targetHrsInput = it
                        },
                        label = { Text("Milestone Focus (Minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedLabelColor = NeonBlueAccent,
                            focusedIndicatorColor = NeonBlueAccent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = suggInput,
                        onValueChange = { suggInput = it },
                        label = { Text("Core Purpose Description") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedLabelColor = NeonBlueAccent,
                            focusedIndicatorColor = NeonBlueAccent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showSummonSkillDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCEL")
                        }

                        val valMinutes = targetHrsInput.toIntOrNull() ?: 600
                        Button(
                            onClick = {
                                if (skillName.isNotBlank() && valMinutes > 0) {
                                    onCreateSkill(skillName, valMinutes, suggInput)
                                    showSummonSkillDialog = false
                                }
                            },
                            enabled = skillName.isNotBlank() && valMinutes > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlueAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("CONSECRATE", color = BlackFantasyBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Interactive Dialog 2: Dynamic Study Schedule Guild Contract editor
    if (showScheduleContractDialog) {
        var flexState by remember { mutableStateOf(if (nonNullProfile.scheduleFlexibility.isNotBlank()) nonNullProfile.scheduleFlexibility else "Adaptable Study Plan") }
        
        // Days checkbox states
        val daysOfWeekList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val currentDaysSelected = remember {
            mutableStateMapOf<String, Boolean>().apply {
                daysOfWeekList.forEach { day ->
                    put(day, nonNullProfile.scheduleDays.contains(day))
                }
            }
        }

        // Initialize targets for each day from CSV scheduleWeekdayMinutes
        val weekdayTargets = remember {
            val initialList = nonNullProfile.scheduleWeekdayMinutes.split(",").map { it.trim().toIntOrNull() ?: 45 }
            mutableStateMapOf<String, String>().apply {
                daysOfWeekList.forEachIndexed { index, day ->
                    val mins = if (index < initialList.size) initialList[index] else nonNullProfile.scheduleMinutesPerDay
                    put(day, mins.toString())
                }
            }
        }

        val flexClasses = listOf("Casual Study", "Adaptable Study Plan", "Rigorous Schedule", "Elite Grindhouse")

        Dialog(onDismissRequest = { showScheduleContractDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                border = BorderStroke(1.5.dp, RpgGold),
                modifier = Modifier.padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "SIGN STUDY COVENANT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = RpgGold,
                            letterSpacing = 1.sp
                        )
                    )

                    Text(
                        text = "Customize study target focus budgets for each day. The system monitors alignment dynamically according to the current calendar day.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    // Daily custom targets list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "🛡️ DAILY FOCUS COMMITMENTS",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        daysOfWeekList.forEach { day ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = currentDaysSelected[day] ?: false,
                                        onCheckedChange = { currentDaysSelected[day] = it },
                                        colors = CheckboxDefaults.colors(checkedColor = NeonBlueAccent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = day.uppercase(),
                                        color = if (currentDaysSelected[day] == true) TextWhite else TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = weekdayTargets[day] ?: "0",
                                        onValueChange = { newValue ->
                                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                                weekdayTargets[day] = newValue
                                                if ((newValue.toIntOrNull() ?: 0) > 0) {
                                                    currentDaysSelected[day] = true
                                                }
                                            }
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier
                                            .width(70.dp)
                                            .background(Color.Black, RoundedCornerShape(4.dp))
                                            .border(1.dp, NeonBlueAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(vertical = 6.dp, horizontal = 8.dp)
                                    )
                                    Text(
                                        text = "MINS",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                    )
                                }
                            }
                        }
                    }

                    // Flexibility Dropdown
                    Column {
                        Text("FLEXIBILITY INTENSIVENESS", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        flexClasses.forEach { flexClass ->
                            val isSel = flexState == flexClass
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) RpgGold.copy(alpha = 0.2f) else Color.Black)
                                    .border(1.dp, if (isSel) RpgGold else DarkCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { flexState = flexClass }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = flexClass.uppercase(),
                                    color = if (isSel) RpgGold else TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showScheduleContractDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ABORT")
                        }

                        Button(
                            onClick = {
                                val selectedDaysCsv = daysOfWeekList.filter { currentDaysSelected[it] == true }.joinToString(",")
                                val weekdayMinutesCsv = daysOfWeekList.map { weekdayTargets[it]?.toIntOrNull() ?: 0 }.joinToString(",")
                                val averageMins = daysOfWeekList.map { weekdayTargets[it]?.toIntOrNull() ?: 0 }.filter { it > 0 }.average().toInt().coerceAtLeast(30)
                                onUpdateSchedule(selectedDaysCsv, averageMins, flexState, weekdayMinutesCsv)
                                showScheduleContractDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RpgGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("SEAL TREATY", color = BlackFantasyBackground, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite))
    }
}

// ==========================================
// COMPOSABLE: OVERLAYS (RPG REACTION DIALOGS)
// ==========================================
@Composable
fun NotificationOverlays(viewModel: SoloStudyingViewModel) {
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
                        Icon(Icons.Default.ArrowForward, contentDescription = "Level advancement arrow", tint = NeonBlueAccent, modifier = Modifier.padding(horizontal = 8.dp))
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

// ==========================================
// COMPOSABLE: CREATION DIALOG FOR BOSS SUMMON
// ==========================================
@Composable
fun CreateBossDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Medium") }
    var minutesInput by remember { mutableStateOf("30") }
    var dungeonNameInput by remember { mutableStateOf("Main Realm") }
    var isRealBoss by remember { mutableStateOf(false) }

    val options = listOf("Easy", "Medium", "Hard", "Legendary")
    val defaultMinutesPreset = listOf(15, 25, 30, 45, 60, 90, 120)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.5.dp, DarkCardBorder),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SUMMON ACADEMIC BOSS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    )
                )

                // Boss Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Enemy Name (e.g., Mathematics Exam)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("boss_name_input"),
                    singleLine = true
                )

                // Dungeon Name Folder Input
                OutlinedTextField(
                    value = dungeonNameInput,
                    onValueChange = { dungeonNameInput = it },
                    label = { Text("Dungeon Name (Category, e.g. Semester 5)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Real Deliverable toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRealBoss = !isRealBoss }
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRealBoss) RpgGold.copy(alpha = 0.15f) else Color.Black)
                        .border(1.dp, if (isRealBoss) RpgGold else DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = isRealBoss,
                        onCheckedChange = { isRealBoss = it },
                        colors = CheckboxDefaults.colors(checkedColor = RpgGold)
                    )
                    Column {
                        Text(
                            text = "👑 REAL DELIVERABLE BOSS",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = if (isRealBoss) RpgGold else TextWhite)
                        )
                        Text(
                            text = "Represents real Exams/Assignments. Enables Manual Pass capability bypass.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }

                // Duration focus Input
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            minutesInput = it
                        }
                    },
                    label = { Text("Required Study Duration (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("boss_duration_input"),
                    singleLine = true
                )

                // Quick presets
                Column {
                    Text(text = "QUICK TIME PRESETS (MINUTES)", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        defaultMinutesPreset.take(4).forEach { min ->
                            Button(
                                onClick = { minutesInput = min.toString() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (minutesInput == min.toString()) NeonBlueAccent else Color(0xFF1E2030),
                                    contentColor = if (minutesInput == min.toString()) Color.Black else TextWhite
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$min m", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        defaultMinutesPreset.drop(4).forEach { min ->
                            Button(
                                onClick = { minutesInput = min.toString() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (minutesInput == min.toString()) NeonBlueAccent else Color(0xFF1E2030),
                                    contentColor = if (minutesInput == min.toString()) Color.Black else TextWhite
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$min m", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // Difficulty selectors
                Column {
                    Text(text = "DIFFICULTY TIER", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        options.forEach { opt ->
                            val isSel = difficulty == opt
                            val col = getDifficultyColor(opt)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) col.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSel) col else DarkCardBorder, RoundedCornerShape(6.dp))
                                    .clickable { difficulty = opt }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) col else TextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Confirm/Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL")
                    }

                    val finalMinutes = minutesInput.toIntOrNull() ?: 30
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && finalMinutes > 0) {
                                onConfirm(name, difficulty, finalMinutes, dungeonNameInput.trim(), isRealBoss)
                            }
                        },
                        enabled = name.isNotEmpty() && finalMinutes > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlueSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("conclude_boss_summon_button")
                    ) {
                        Text("SUMMON", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPOSABLE: CREATION DIALOG FOR SHOP REWARDS
// ==========================================
@Composable
fun CreateRewardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("150") }
    var rewardType by remember { mutableStateOf("Time-Based") } // "Time-Based", "One-Time"
    var hoursValue by remember { mutableStateOf("1") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.5.dp, DarkCardBorder),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "FORGE GUILD REWARD",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    )
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Reward Name (e.g. Gaming Episode)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reward_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (e.g. Treat is 1 Episode of Anime)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reward_desc_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = costInput,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            costInput = it
                        }
                    },
                    label = { Text("Guild Cost (Gold Coins)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reward_cost_input"),
                    singleLine = true
                )

                Column {
                    Text(text = "REWARD STRUCTURE TYPE", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Time-Based", "One-Time").forEach { type ->
                            val isSel = rewardType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) NeonBlueAccent.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSel) NeonBlueAccent else DarkCardBorder, RoundedCornerShape(6.dp))
                                    .clickable { rewardType = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) NeonBlueAccent else TextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                if (rewardType == "Time-Based") {
                    OutlinedTextField(
                        value = hoursValue,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                hoursValue = it
                            }
                        },
                        label = { Text("Accumulated Hours Granted") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            focusedLabelColor = NeonBlueAccent,
                            focusedIndicatorColor = NeonBlueAccent
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("reward_value_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL")
                    }

                    val finalCost = costInput.toIntOrNull() ?: 150
                    val finalVal = hoursValue.toIntOrNull() ?: 1
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && finalCost > 0) {
                                onConfirm(name, desc, finalCost, rewardType, finalVal)
                            }
                        },
                        enabled = name.isNotEmpty() && finalCost > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlueSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("conclude_reward_forge_button")
                    ) {
                        Text("FORGE REWARD", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// HELPERS & STYLE ASSIGNMENTS
// ==========================================
private fun getRankLabel(level: Int): String {
    return when {
        level <= 2 -> "Novice Apprentice 🎓"
        level <= 5 -> "Apprentice Spellcaster 🔮"
        level <= 9 -> "Academic Vanguard ⚔️"
        level <= 14 -> "Dungeon Sage Lorekeeper 📖"
        else -> "Master Arch-Scholar Legend 🌟"
    }
}

private fun getDifficultyColor(difficulty: String): Color {
    return when (difficulty) {
        "Easy" -> RpgEmerald
        "Medium" -> NeonBlueAccent
        "Hard" -> Color(0xFFFA772C)
        "Legendary" -> Color(0xFFC582FF)
        else -> TextWhite
    }
}

private fun getBossCardBrush(difficulty: String): Brush {
    return when (difficulty) {
        "Easy" -> Brush.verticalGradient(listOf(Color(0xFF0D251A), Color(0xFF0F3A22)))
        "Medium" -> Brush.verticalGradient(listOf(Color(0xFF0E1A29), Color(0xFF082747)))
        "Hard" -> Brush.verticalGradient(listOf(Color(0xFF2E1712), Color(0xFF4C150A)))
        "Legendary" -> Brush.verticalGradient(listOf(Color(0xFF20112A), Color(0xFF38104E)))
        else -> Brush.verticalGradient(listOf(Color(0xFF131422), Color(0xFF1B1C2E)))
    }
}

private fun formatStudyTimeShort(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}

private fun formatStudyTimeFriendly(seconds: Long): String {
    val totalMins = seconds / 60
    val hrs = totalMins / 60
    val mins = totalMins % 60
    return when {
        seconds == 0L -> "0 seconds"
        hrs == 0L -> "$mins min${if (mins != 1L) "s" else ""}"
        mins == 0L -> "$hrs hr${if (hrs != 1L) "s" else ""}"
        else -> "$hrs hr${if (hrs != 1L) "s" else ""} and $mins min${if (mins != 1L) "s" else ""}"
    }
}

private fun formatSessionDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatCountdown(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}

// ==========================================
// COMPOSABLE: ONBOARDING & COMPLETE TUTORIAL
// ==========================================
@Composable
fun OnboardingScreen(
    onFinish: (name: String, hunterClass: String, mainGoal: String, learningPath: String, scheduleDays: String, scheduleMinutes: Int, scheduleFlexibility: String, weekdayMinutes: String) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    
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
    var targetMinutesPerDay by remember { mutableStateOf(45) }
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
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Hunter Icon",
                        tint = RpgGold,
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

                            Divider(color = Color.Gray.copy(alpha = 0.15f))

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

                            Divider(color = Color.Gray.copy(alpha = 0.15f))

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
