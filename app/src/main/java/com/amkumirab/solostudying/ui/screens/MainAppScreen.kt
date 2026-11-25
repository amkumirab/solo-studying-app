package com.amkumirab.solostudying.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.domain.dungeon.calculateDungeonStudyPlan
import com.amkumirab.solostudying.domain.streak.buildWeeklyStreakSnapshot
import com.amkumirab.solostudying.quickstart.QuickStartPreferences
import com.amkumirab.solostudying.quickstart.QuickStartSelection
import com.amkumirab.solostudying.quickstart.normalizeQuickStartSelection
import com.amkumirab.solostudying.sound.RpgSoundManager
import com.amkumirab.solostudying.ui.theme.*
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModel
import com.amkumirab.solostudying.widget.TodayWidgetProvider
import com.amkumirab.solostudying.widget.WeeklyStreakWidgetProvider
import com.amkumirab.solostudying.widget.buildTodayWidgetSnapshot

enum class Tab(val title: String, val icon: ImageVector) {
    Today("Today", Icons.Default.Today),
    Dungeons("Study", Icons.Default.Shield),
    Battle("Session", Icons.Default.Timer),
    Shop("Rewards", Icons.Default.LocalMall),
    Stats("Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: SoloStudyingViewModel) {
    val bosses by viewModel.bosses.collectAsState()
    val dungeons by viewModel.dungeons.collectAsStateWithLifecycle()
    val bossSteps by viewModel.bossSteps.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val rewards by viewModel.rewards.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val skills by viewModel.skills.collectAsState()
    val dailyQuests by viewModel.dailyQuests.collectAsState()
    val reminderSettings by viewModel.reminderSettings.collectAsState()
    val soundSettings by RpgSoundManager.settings.collectAsState()

    var currentTab by rememberSaveable { mutableStateOf(Tab.Today) }
    var studySection by rememberSaveable { mutableStateOf(DungeonSection.Goals) }
    var profileSection by rememberSaveable { mutableStateOf(ProfileSection.Overview) }
    val tabState = rememberSaveableStateHolder()
    var showCreateBossDialog by remember { mutableStateOf(false) }
    var showCreateRewardDialog by remember { mutableStateOf(false) }
    var showFreeStudyDialog by remember { mutableStateOf(false) }
    var showFocusCycleDialog by remember { mutableStateOf(false) }
    var pendingTabAfterFocusWarning by remember { mutableStateOf<Tab?>(null) }
    var showExitFocusDialog by remember { mutableStateOf(false) }
    val navigateTo: (Tab) -> Unit = { destination ->
        if (currentTab == Tab.Battle && viewModel.isBattleActive && destination != Tab.Battle) {
            pendingTabAfterFocusWarning = destination
        } else currentTab = destination
    }

    // Before the Battle preparation states
    var prepBoss by remember { mutableStateOf<BossEntity?>(null) }
    var prepStudyStep by remember { mutableStateOf<BossStepEntity?>(null) }
    var prepFreeStudyMins by remember { mutableStateOf<Int?>(null) }
    var prepSelectedSkill by remember { mutableStateOf<SkillEntity?>(null) }


    // Navigation fallback: If a battle is active, user can stay in any tab, but we flash the timer tab.
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val quickStartPreferences = remember(context) { QuickStartPreferences(context) }
    var quickStartSelection by remember { mutableStateOf(quickStartPreferences.read()) }
    var handledQuickFocusRequest by remember { mutableIntStateOf(0) }
    val updateQuickStartSelection: (QuickStartSelection) -> Unit = { selection ->
        val normalized = normalizeQuickStartSelection(
            durationMinutes = selection.durationMinutes,
            skillId = selection.skillId,
        )
        quickStartSelection = normalized
        quickStartPreferences.save(normalized)
    }

    LaunchedEffect(userProfile, sessions, dailyQuests, bosses, bossSteps, quickStartSelection) {
        TodayWidgetProvider.updateAll(
            context = context,
            snapshot = buildTodayWidgetSnapshot(
                profile = userProfile,
                sessions = sessions,
                quests = dailyQuests,
                bosses = bosses,
                steps = bossSteps,
                quickStart = quickStartSelection,
            ),
        )
        WeeklyStreakWidgetProvider.updateAll(
            context = context,
            snapshot = buildWeeklyStreakSnapshot(
                profile = userProfile,
                sessions = sessions,
            ),
        )
    }

    FocusSessionLifecycleEffect(viewModel = viewModel)
    StrictFocusPinningEffect(activity = activity, viewModel = viewModel)

    LaunchedEffect(viewModel.isBattleActive) {
        if (viewModel.isBattleActive && prepBoss == null && prepFreeStudyMins == null) {
            currentTab = Tab.Battle
        }
    }

    LaunchedEffect(viewModel.focusNavigationRequest) {
        if (viewModel.focusNavigationRequest > 0) {
            currentTab = Tab.Battle
        }
    }

    LaunchedEffect(viewModel.statsNavigationRequest) {
        if (viewModel.statsNavigationRequest > 0) {
            profileSection = ProfileSection.Overview
            currentTab = Tab.Stats
        }
    }

    LaunchedEffect(viewModel.quickFocusRequest, userProfile) {
        val request = viewModel.quickFocusRequest
        val profile = userProfile
        if (request <= handledQuickFocusRequest || profile == null) return@LaunchedEffect
        handledQuickFocusRequest = request

        if (!profile.hasCompletedOnboarding || !profile.hasCompletedTutorial) return@LaunchedEffect
        if (viewModel.isBattleActive) {
            currentTab = Tab.Battle
            return@LaunchedEffect
        }

        val selection = quickStartPreferences.read()
        updateQuickStartSelection(selection)
        viewModel.selectAndStartQuickFocus(
            minutes = selection.durationMinutes,
            skillId = selection.skillId,
        )
        currentTab = Tab.Battle
    }

    BackHandler(enabled = viewModel.isBattleActive || currentTab != Tab.Today) {
        when {
            currentTab == Tab.Stats && profileSection != ProfileSection.Overview -> profileSection = ProfileSection.Overview
            viewModel.isBattleActive -> showExitFocusDialog = true
            else -> {
                prepBoss = null
                prepStudyStep = null
                prepFreeStudyMins = null
                prepSelectedSkill = null
                currentTab = Tab.Today
            }
        }
    }

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
            AppHeader(
                title = if (currentTab == Tab.Stats && profileSection == ProfileSection.Settings) "Settings" else currentTab.title,
                playerSummary = "Lv. ${userProfile?.level ?: 1} · ${userProfile?.gold ?: 0} Gold",
                onSettings = {
                    profileSection = ProfileSection.Settings
                    navigateTo(Tab.Stats)
                },
                onBack = if (currentTab == Tab.Stats && profileSection == ProfileSection.Settings) {
                    { profileSection = ProfileSection.Overview }
                } else null,
            )
        },
        bottomBar = {
            Column {
            if (viewModel.isBattleActive && currentTab != Tab.Battle) {
                ActiveSessionBar(
                    paused = viewModel.isBattlePaused,
                    title = viewModel.activeBoss?.name ?: "Focus study",
                    onReturn = { currentTab = Tab.Battle },
                )
            }
            RPGBottomBar(
                currentTab = currentTab,
                onTabSelected = navigateTo,
                isBattleActive = viewModel.isBattleActive
            )
            }
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
            NotificationOverlays(
                viewModel = viewModel,
                onStartAnotherSession = { currentTab = Tab.Dungeons },
            )

            tabState.SaveableStateProvider(currentTab.name) {
                when (currentTab) {
                    Tab.Today, Tab.Dungeons -> DungeonTab(
                        selectedSection = if (currentTab == Tab.Today) DungeonSection.Today else studySection,
                        onSectionSelected = { studySection = it },
                        bosses = bosses,
                        bossSteps = bossSteps,
                        sessions = sessions,
                        activeBoss = viewModel.activeBoss,
                        activeStepId = viewModel.activeBossStepId,
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
                            prepStudyStep = null
                            prepFreeStudyMins = null
                            prepSelectedSkill = viewModel.selectedSkillToTrain
                            currentTab = Tab.Battle
                        },
                        onConquerRealBoss = { boss ->
                            RpgSoundManager.playClickSound()
                            viewModel.conquerRealBossManual(boss)
                        },
                        quickStartSelection = quickStartSelection,
                        onQuickStartSelectionChange = updateQuickStartSelection,
                        onQuickStart = { selection ->
                            if (!viewModel.isBattleActive) {
                                val selectedSkill = skills.firstOrNull {
                                    it.id == selection.skillId
                                }
                                updateQuickStartSelection(selection.copy(skillId = selectedSkill?.id))
                                viewModel.selectedSkillToTrain = selectedSkill
                                viewModel.selectAndStartFreeStudy(selection.durationMinutes)
                                currentTab = Tab.Battle
                            }
                        },
                        onCustomQuickStart = {
                            RpgSoundManager.playClickSound()
                            showFreeStudyDialog = true
                        },
                        onFocusCycles = {
                            RpgSoundManager.playClickSound()
                            showFocusCycleDialog = true
                        },
                        onDailyQuestStarted = {
                            currentTab = Tab.Battle
                        },
                        onPlannedSessionStarted = { boss, minutes ->
                            if (!viewModel.isBattleActive) {
                                prepBoss = null
                                prepStudyStep = null
                                prepFreeStudyMins = null
                                prepSelectedSkill = null
                                viewModel.selectedSkillToTrain = null
                                viewModel.selectAndStartBattle(boss, minutes)
                                currentTab = Tab.Battle
                            }
                        },
                        onStartBossStep = { boss, step ->
                            RpgSoundManager.playClickSound()
                            prepBoss = boss
                            prepStudyStep = step
                            prepFreeStudyMins = null
                            prepSelectedSkill = viewModel.selectedSkillToTrain
                            currentTab = Tab.Battle
                        },
                    )
                    Tab.Battle -> {
                        if (prepBoss != null || prepFreeStudyMins != null) {
                            BeforeTheBattleScreen(
                                boss = prepBoss,
                                studyStep = prepStudyStep,
                                freeStudyMins = prepFreeStudyMins,
                                selectedSkill = prepSelectedSkill,
                                userProfile = userProfile ?: UserProfileEntity(),
                                suggestedDailyMinutes = prepBoss?.let { boss ->
                                    dungeons.firstOrNull { it.name == boss.dungeonName }?.let { dungeon ->
                                        calculateDungeonStudyPlan(dungeon, bosses)?.minutesPerStudyDay
                                    }
                                },
                                onBeginBattle = { sessionMinutes ->
                                    if (prepBoss != null) {
                                        viewModel.selectedSkillToTrain = prepSelectedSkill
                                        if (prepStudyStep != null) {
                                            viewModel.selectAndStartBossStep(prepBoss!!, prepStudyStep!!)
                                        } else {
                                            viewModel.selectAndStartBattle(prepBoss!!, sessionMinutes)
                                        }
                                    } else if (prepFreeStudyMins != null) {
                                        viewModel.selectedSkillToTrain = prepSelectedSkill
                                        viewModel.selectAndStartFreeStudy(prepFreeStudyMins!!)
                                    }
                                    prepBoss = null
                                    prepStudyStep = null
                                    prepFreeStudyMins = null
                                    prepSelectedSkill = null
                                },
                                onCancel = {
                                    prepBoss = null
                                    prepStudyStep = null
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
                        showSettingsHeader = false,
                        selectedSection = profileSection,
                        onSectionSelected = { profileSection = it },
                        profile = userProfile,
                        sessions = sessions,
                        skills = skills,
                        onCreateSkill = { name, hrs, sug -> viewModel.createSkill(name, hrs, sug) },
                        onDeleteSkill = { skill -> viewModel.deleteSkill(skill) },
                        onUpdateSchedule = { days, mins, flex, weekdayMins -> viewModel.updateScheduleWithWeekdays(days, mins, flex, weekdayMins) },
                        onTriggerSimulatedNotification = { action -> viewModel.simulateCompanionNotification(action) },
                        reminderSettings = reminderSettings,
                        onReminderSettingsChange = viewModel::updateReminderSettings,
                        soundSettings = soundSettings,
                        onSoundEnabledChange = { enabled ->
                            RpgSoundManager.setSoundEnabled(enabled)
                            if (enabled) RpgSoundManager.playClickSound()
                        },
                        onSoundVolumeChange = RpgSoundManager::setVolume,
                        onPreviewSound = RpgSoundManager::previewSound,
                        onReplayTutorial = { viewModel.tutorialViewModel.replayTutorial() },
                        breakSuggestionsEnabled = viewModel.breakSuggestionsEnabled,
                        onBreakSuggestionsEnabledChange = viewModel::setBreakSuggestionsEnabled,
                        focusShieldEnabled = viewModel.focusShieldEnabled,
                        focusShieldHasAccess = viewModel.focusShieldHasAccess,
                        onFocusShieldEnabledChange = viewModel::setFocusShieldEnabled,
                        onRequestFocusShieldAccess = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        },
                        onUpdateSessionNote = viewModel::updateSessionNote,
                    )
                }
            }
        }
    }

    if (showCreateBossDialog) {
        CreateBossDialog(
            onDismiss = { showCreateBossDialog = false },
            onConfirm = { name, difficulty, minutesOption, dungeonName, isRealBoss, deadlineDate ->
                viewModel.createBoss(
                    name = name,
                    difficulty = difficulty,
                    durationMinutes = minutesOption,
                    imagePath = null,
                    dungeonName = dungeonName,
                    isRealBoss = isRealBoss,
                    deadlineDate = deadlineDate,
                )
                showCreateBossDialog = false
            }
        )
    }

    if (showFocusCycleDialog) {
        FocusCycleSetupDialog(
            skills = skills,
            initialSkillId = quickStartSelection.skillId,
            onDismiss = { showFocusCycleDialog = false },
            onStart = { plan ->
                updateQuickStartSelection(
                    quickStartSelection.copy(
                        durationMinutes = plan.focusMinutes,
                        skillId = plan.skillId,
                    ),
                )
                viewModel.startFocusCycle(plan)
                showFocusCycleDialog = false
                currentTab = Tab.Battle
            },
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
        var durationInput by remember {
            mutableStateOf(quickStartSelection.durationMinutes.toString())
        }
        var dropdownExpanded by remember { mutableStateOf(false) }
        var studySkillSelected by remember {
            mutableStateOf(skills.firstOrNull { it.id == quickStartSelection.skillId })
        }

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

                        val fMins = durationInput.toIntOrNull()
                        val isValidDuration = fMins != null &&
                            fMins in QuickStartPreferences.MIN_DURATION_MINUTES..QuickStartPreferences.MAX_DURATION_MINUTES
                        Button(
                            onClick = {
                                if (isValidDuration) {
                                    updateQuickStartSelection(
                                        QuickStartSelection(
                                            durationMinutes = fMins,
                                            skillId = studySkillSelected?.id,
                                        ),
                                    )
                                    prepBoss = null
                                    prepStudyStep = null
                                    prepFreeStudyMins = fMins
                                    prepSelectedSkill = studySkillSelected
                                    showFreeStudyDialog = false
                                    currentTab = Tab.Battle
                                }
                            },
                            enabled = isValidDuration,
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

    pendingTabAfterFocusWarning?.let { destination ->
        AlertDialog(
            onDismissRequest = { pendingTabAfterFocusWarning = null },
            modifier = Modifier.testTag("focus_navigation_warning_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = NeonBlueAccent,
                )
            },
            title = { Text("Leave the focus screen?") },
            text = {
                Text("Your study timer will keep running. Use the session bar to return at any time.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentTab = destination
                        pendingTabAfterFocusWarning = null
                    },
                    modifier = Modifier.testTag("confirm_leave_focus_screen_button"),
                ) {
                    Text("LEAVE SCREEN")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingTabAfterFocusWarning = null },
                    modifier = Modifier.testTag("keep_focusing_button"),
                ) {
                    Text("KEEP FOCUSING")
                }
            },
            containerColor = DarkFantasySurface,
        )
    }

    if (showExitFocusDialog) {
        AlertDialog(
            onDismissRequest = { showExitFocusDialog = false },
            modifier = Modifier.testTag("focus_exit_warning_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Default.PauseCircle,
                    contentDescription = null,
                    tint = RpgGold,
                )
            },
            title = { Text("Pause and exit?") },
            text = {
                Text("The session will be paused and restored when you open the app again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitFocusDialog = false
                        viewModel.pauseBattle {
                            activity?.finish()
                        }
                    },
                    modifier = Modifier.testTag("pause_and_exit_button"),
                ) {
                    Text("PAUSE AND EXIT")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitFocusDialog = false },
                    modifier = Modifier.testTag("cancel_focus_exit_button"),
                ) {
                    Text("CONTINUE SESSION")
                }
            },
            containerColor = DarkFantasySurface,
        )
    }
}
}
