package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.domain.dungeon.calculateDungeonStudyPlan
import com.amkumirab.solostudying.domain.today.TodayPlanItemType
import com.amkumirab.solostudying.domain.today.buildTodayPlan
import com.amkumirab.solostudying.quickstart.QuickStartSelection
import com.amkumirab.solostudying.sound.RpgSoundManager
import com.amkumirab.solostudying.ui.theme.*
import com.amkumirab.solostudying.ui.viewmodel.SoloStudyingViewModel
import java.time.LocalDate

internal enum class DungeonSection(val title: String, val subtitle: String, val icon: ImageVector) {
    Today("Today", "Plan and finish today's study", Icons.Default.Today),
    Focus("Focus", "Start a focused study session", Icons.Default.Bolt),
    Goals("Goals", "Manage long-term challenges", Icons.Default.Flag),
    Plan("Plan", "Build and adjust your weekly study plan", Icons.Default.DateRange),
}

@Composable
internal fun DungeonTab(
    bosses: List<BossEntity>,
    bossSteps: List<BossStepEntity>,
    activeBoss: BossEntity?,
    activeStepId: Int?,
    isBattleActive: Boolean,
    profile: UserProfileEntity?,
    skills: List<SkillEntity>,
    viewModel: SoloStudyingViewModel,
    onCreateBossClicked: () -> Unit,
    onFightBoss: (BossEntity) -> Unit,
    onConquerRealBoss: (BossEntity) -> Unit,
    quickStartSelection: QuickStartSelection,
    onQuickStartSelectionChange: (QuickStartSelection) -> Unit,
    onQuickStart: (QuickStartSelection) -> Unit,
    onCustomQuickStart: () -> Unit,
    onFocusCycles: () -> Unit,
    onDailyQuestStarted: () -> Unit,
    onPlannedSessionStarted: (BossEntity, Int) -> Unit,
    onStartBossStep: (BossEntity, BossStepEntity) -> Unit,
    selectedSection: DungeonSection = DungeonSection.Today,
    onSectionSelected: (DungeonSection) -> Unit = {},
) {
    var selectedDungeonCategory by rememberSaveable { mutableStateOf("All") }
    var showDungeonPlanDialog by rememberSaveable { mutableStateOf(false) }
    var stepsBoss by remember { mutableStateOf<BossEntity?>(null) }
    var goalActionId by rememberSaveable { mutableStateOf<Int?>(null) }
    var goalAction by rememberSaveable { mutableStateOf(GoalAction.Edit) }
    var showDeletedGoals by rememberSaveable { mutableStateOf(false) }
    val deletedGoals by viewModel.dungeonViewModel.deletedGoals.collectAsStateWithLifecycle()
    val sectionState = rememberSaveableStateHolder()
    val isToday = selectedSection == DungeonSection.Today
    val dailyQuests = if (isToday) viewModel.dailyQuestViewModel.todayQuests.collectAsStateWithLifecycle().value
        else emptyList()
    val allDailyQuests = if (isToday) viewModel.dailyQuestViewModel.allQuests.collectAsStateWithLifecycle().value
        else emptyList()
    val recurringQuests = if (isToday) viewModel.dailyQuestViewModel.recurringQuests.collectAsStateWithLifecycle().value
        else emptyList()
    val dungeons by viewModel.dungeonViewModel.dungeons.collectAsStateWithLifecycle()
    val dashboardDate = remember(viewModel.dailyQuestViewModel.todayDate) {
        runCatching { LocalDate.parse(viewModel.dailyQuestViewModel.todayDate) }
            .getOrDefault(LocalDate.now())
    }
    val todayPlan = if (isToday) {
        val history by viewModel.statusViewModel.weekSessions.collectAsStateWithLifecycle()
        remember(profile, history, dailyQuests, bosses, bossSteps) {
            profile?.let {
                buildTodayPlan(
                    profile = it,
                    sessions = history.sessions,
                    quests = dailyQuests,
                    bosses = bosses,
                    steps = bossSteps,
                    today = history.period.today,
                    zoneId = history.period.zoneId,
                )
            }
        }
    } else null

    // Accumulate all distinct dungeon categories created by the player
    val dungeonsList = remember(bosses, dungeons) {
        listOf("All") + (bosses.map { it.dungeonName } + dungeons.filter { it.targetMinutes != null }.map { it.name })
            .distinct().filter { it.isNotBlank() && it != "All" }
    }

    val selectedDungeon = dungeons.firstOrNull { it.name == selectedDungeonCategory }
    val selectedStudyPlan = remember(selectedDungeon, bosses, dashboardDate) {
        selectedDungeon?.let { calculateDungeonStudyPlan(it, bosses, dashboardDate) }
    }

    val filteredBosses = remember(bosses, selectedDungeonCategory) {
        bosses.filter { it.secondGateOfId == null &&
            (selectedDungeonCategory == "All" || it.dungeonName == selectedDungeonCategory) }
    }
    val visibleSecondGates = remember(bosses) { bosses.filter { it.secondGateOfId != null }.associateBy { it.secondGateOfId } }
    val archivedSecondGates = remember(deletedGoals) { deletedGoals.mapNotNull { it.secondGateOfId }.toSet() }
    val manageGoal: (BossEntity, GoalAction) -> Unit = { boss, action ->
        goalActionId = boss.id
        goalAction = action
    }

    sectionState.SaveableStateProvider(selectedSection.name) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (selectedSection == DungeonSection.Today) "Your day" else selectedSection.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                    )
                    Text(
                        text = selectedSection.subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted
                        )
                    )
                }

                if (selectedSection == DungeonSection.Goals) {
                    FilledTonalButton(
                        onClick = onCreateBossClicked,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeonBlueAccent.copy(alpha = 0.14f),
                            contentColor = NeonBlueAccent,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("create_boss_button"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("New goal", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedSection != DungeonSection.Today) DungeonSectionTabs(
                selectedSection = selectedSection,
                onSelected = onSectionSelected,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedSection == DungeonSection.Plan) {
                StudyPlannerEntry(
                    bosses = bosses, today = dashboardDate,
                    weekdayDefaults = profile?.scheduleWeekdayMinutes?.split(",")?.map { it.toIntOrNull() ?: 0 } ?: List(7) { 60 },
                    isSessionActive = isBattleActive, onStart = onPlannedSessionStarted,
                    showDailySessions = false,
                )
            }
            if (selectedSection == DungeonSection.Today) {
                todayPlan?.let { plan ->
                    StudyPlannerEntry(
                        bosses = bosses,
                        today = dashboardDate,
                        weekdayDefaults = profile?.scheduleWeekdayMinutes?.split(",")?.map { it.toIntOrNull() ?: 0 }
                            ?: List(7) { 60 },
                        isSessionActive = isBattleActive,
                        onStart = onPlannedSessionStarted,
                        content = { weeklyPlan, plannedSessions ->
                            val managedGoals = weeklyPlan?.config?.courses?.map { it.bossId }.orEmpty().toSet()
                            TodayDashboardCard(
                                plan = plan.copy(items = plan.items.filterNot { item ->
                                    (item.type == TodayPlanItemType.Boss && item.sourceId in managedGoals) ||
                                        (item.type == TodayPlanItemType.BossStep && item.bossId in managedGoals)
                                }),
                                isSessionActive = isBattleActive,
                                today = dashboardDate,
                                hasWeeklyPlan = weeklyPlan != null,
                                plannedSessions = plannedSessions,
                                onStartItem = { item ->
                                    when (item.type) {
                                        TodayPlanItemType.DailyQuest -> {
                                            dailyQuests.firstOrNull { it.id == item.sourceId }?.let { quest ->
                                                if (!viewModel.isBattleActive) {
                                                    viewModel.selectAndStartDailyQuest(quest)
                                                    onDailyQuestStarted()
                                                }
                                            }
                                        }

                                        TodayPlanItemType.BossStep -> {
                                            val boss = bosses.firstOrNull { it.id == item.bossId }
                                            val step = bossSteps.firstOrNull { it.id == item.sourceId }
                                            if (boss != null && step != null && !viewModel.isBattleActive) {
                                                onStartBossStep(boss, step)
                                            }
                                        }

                                        TodayPlanItemType.Boss -> {
                                            bosses.firstOrNull { it.id == item.sourceId }?.let(onFightBoss)
                                        }

                                        TodayPlanItemType.QuickFocus -> {
                                            if (!viewModel.isBattleActive) {
                                                viewModel.selectedSkillToTrain = null
                                                viewModel.selectAndStartFreeStudy(item.durationMinutes)
                                                onDailyQuestStarted()
                                            }
                                        }
                                    }
                                },
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                ExpandableStudySection(
                    title = "Daily tasks",
                    summary = "${dailyQuests.count { !it.isCompleted }} remaining today",
                ) {
                DailyQuestBoard(
                quests = dailyQuests,
                allQuests = allDailyQuests,
                recurringQuests = recurringQuests,
                skills = skills,
                today = viewModel.dailyQuestViewModel.todayDate,
                activeQuestId = viewModel.activeDailyQuestId,
                isSessionActive = isBattleActive,
                onCreateQuest = viewModel::createDailyQuest,
                onUpdateQuest = viewModel::updateDailyQuest,
                onSetCompleted = viewModel::setDailyQuestCompleted,
                onDeleteQuest = viewModel::deleteDailyQuest,
                onUpdateRecurringQuest = viewModel::updateRecurringQuest,
                onSetRecurringQuestActive = viewModel::setRecurringQuestActive,
                onDeleteRecurringQuest = viewModel::deleteRecurringQuest,
                onStartQuest = { quest ->
                    if (!viewModel.isBattleActive) {
                        viewModel.selectAndStartDailyQuest(quest)
                        onDailyQuestStarted()
                    }
                },
                )
                }
            }

            if (selectedSection == DungeonSection.Focus) {

                QuickStartCard(
                skills = skills,
                selection = quickStartSelection,
                isSessionActive = isBattleActive,
                onSelectionChange = onQuickStartSelectionChange,
                onStart = onQuickStart,
                onCustomDuration = onCustomQuickStart,
                onFocusCycles = onFocusCycles,
                )

                Spacer(modifier = Modifier.height(12.dp))

                FocusProfilesCard(
                profiles = viewModel.focusProfiles,
                skills = skills,
                isSessionActive = isBattleActive,
                focusShieldHasAccess = viewModel.focusShieldHasAccess,
                onStart = { profileToStart ->
                    if (!viewModel.isBattleActive) {
                        viewModel.startFocusProfile(
                            profile = profileToStart,
                            validSkillIds = skills.mapTo(mutableSetOf()) { it.id },
                        )
                        onDailyQuestStarted()
                    }
                },
                onSave = viewModel::saveFocusProfile,
                onDelete = viewModel::deleteFocusProfile,
                )
            }

            if (selectedSection == DungeonSection.Goals) {

                DeadlineGoalsCard(
                bosses = bosses,
                scheduleDays = profile?.scheduleDays.orEmpty(),
                isSessionActive = isBattleActive,
                onCreateGoal = onCreateBossClicked,
                onStartGoal = onFightBoss,
                )

                Spacer(modifier = Modifier.height(12.dp))

            // System 7: RED DUNGEON (PENALTY SYSTEM) VISUAL STRESS INDICATOR
            val redDungeonDays = profile?.redDungeonDays ?: 0
            val isBoostActive = profile?.isRedDungeonBoostActive == true
            val totalCleared = profile?.totalRedDungeonsCleared ?: 0
            ExpandableStudySection(
                title = if (redDungeonDays > 0) "Recovery challenge active" else "Recovery challenges",
                summary = if (redDungeonDays > 0) "Rewards reduced. Complete a focus session to clear the gate."
                    else "$totalCleared cleared · no active penalty",
                tag = "gate_status_toggle",
            ) {
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
                            text = "Rewards are reduced until you complete a focus study session to clear this gate.",
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
                            text = "No active penalty. Keep studying on your scheduled days to maintain your streak.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }
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

                if (selectedStudyPlan != null) {
                    DungeonStudyPlanCard(plan = selectedStudyPlan, onEdit = { showDungeonPlanDialog = true })
                } else {
                    OutlinedButton(
                        onClick = { showDungeonPlanDialog = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_dungeon_study_plan"),
                    ) { Text(if (selectedDungeonCategory == "All") "Plan a dungeon" else "Set study target and deadline") }
                    Text(
                        "Plan a full course in hours and choose when to finish. Select a dungeon to see its progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showDeletedGoals = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("deleted_goals"),
                ) {
                    Text(if (deletedGoals.isEmpty()) "Deleted goals" else "Deleted goals (${deletedGoals.size})")
                }
                Spacer(Modifier.height(12.dp))

                if (filteredBosses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
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
                            modifier = Modifier.size(48.dp)
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredBosses.forEach { boss ->
                        key(boss.id) {
                            val gate = visibleSecondGates[boss.id]
                            val gateContent: (@Composable () -> Unit)? = if (gate != null) {
                                {
                                    Text("Second Gate", style = MaterialTheme.typography.titleSmall, color = NeonBlueAccent)
                                    ManagedGoalCard(gate, bossSteps, activeBoss, isBattleActive, onFightBoss,
                                        onConquerRealBoss, { stepsBoss = it }, manageGoal)
                                }
                            } else if (boss.id in archivedSecondGates) {
                                { Text("Restore the Second Gate from Deleted goals.", color = TextMuted) }
                            } else null
                            ManagedGoalCard(boss, bossSteps, activeBoss, isBattleActive, onFightBoss,
                                onConquerRealBoss, { stepsBoss = it }, manageGoal, gateContent)
                        }
                    }
                }
                }
            }
        }
    }

    if (showDeletedGoals) {
        DeletedGoalsDialog(
            goals = deletedGoals,
            onDismiss = { showDeletedGoals = false },
            onRestore = viewModel.dungeonViewModel::restoreGoal,
        )
    }

    bosses.firstOrNull { it.id == goalActionId }?.let { boss ->
        GoalManagementDialog(
            goal = boss,
            action = goalAction,
            onDismiss = { goalActionId = null },
            onCommit = { edit, extraMinutes ->
                if (viewModel.battleViewModel.hasActiveOrRecoveringSession()) false else when (goalAction) {
                    GoalAction.Edit -> viewModel.dungeonViewModel.editGoal(boss.id, checkNotNull(edit))
                    GoalAction.Archive -> viewModel.dungeonViewModel.archiveGoal(boss)
                    GoalAction.SecondGate -> viewModel.dungeonViewModel.openSecondGate(boss.id, checkNotNull(extraMinutes))
                }
            },
        )
    }

    if (showDungeonPlanDialog) {
        DungeonStudyPlanDialog(
            initialDungeon = if (selectedDungeonCategory == "All") null else selectedDungeon
                ?: DungeonEntity(name = selectedDungeonCategory, description = ""),
            existingDungeons = dungeons + bosses.map { DungeonEntity(name = it.dungeonName, description = "") },
            bosses = bosses,
            today = dashboardDate,
            onDismiss = { showDungeonPlanDialog = false },
            onSave = { input ->
                viewModel.dungeonViewModel.saveStudyPlan(input)
                selectedDungeonCategory = input.dungeonName
            },
            onRemove = { viewModel.dungeonViewModel.clearStudyPlan(selectedDungeonCategory) },
        )
    }

    stepsBoss?.let { boss ->
        BossStudyStepsDialog(
            boss = boss,
            steps = bossSteps.filter { it.bossId == boss.id },
            isSessionActive = isBattleActive,
            activeStepId = activeStepId,
            onDismiss = { stepsBoss = null },
            onCreateStep = { title, minutes ->
                viewModel.createBossStep(boss.id, title, minutes)
            },
            onUpdateStep = viewModel::updateBossStep,
            onSetCompleted = viewModel::setBossStepCompleted,
            onDeleteStep = viewModel::deleteBossStep,
            onStartStep = { step ->
                stepsBoss = null
                onStartBossStep(boss, step)
            },
        )
    }
}

@Composable
internal fun DungeonSectionTabs(
    selectedSection: DungeonSection,
    onSelected: (DungeonSection) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceSubtle)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(4.dp)
            .testTag("study_hub_sections"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(DungeonSection.Goals, DungeonSection.Focus, DungeonSection.Plan).forEach { section ->
            val isSelected = section == selectedSection
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) InfoContainer else Color.Transparent)
                    .clickable { onSelected(section) }
                    .testTag("study_hub_${section.name.lowercase()}")
                    .semantics {
                        selected = isSelected
                        contentDescription = "${section.title} study hub section"
                    }
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = if (isSelected) NeonBlueAccent else TextMuted,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = section.title,
                    color = if (isSelected) TextWhite else TextMuted,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
