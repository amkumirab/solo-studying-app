package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.entity.RecurringQuestEntity
import com.amkumirab.solostudying.data.entity.SkillEntity
import com.amkumirab.solostudying.domain.quest.QuestPriority
import com.amkumirab.solostudying.domain.quest.ALL_WEEKDAYS_MASK
import com.amkumirab.solostudying.domain.quest.RecurringQuestSchedule
import com.amkumirab.solostudying.domain.quest.isCarriedDailyQuest
import com.amkumirab.solostudying.domain.quest.nextRecurringQuestDate
import com.amkumirab.solostudying.domain.quest.recurringQuestScheduleLabel
import com.amkumirab.solostudying.domain.quest.weekdayMask
import com.amkumirab.solostudying.quickstart.QUICK_START_PRESET_MINUTES
import com.amkumirab.solostudying.ui.theme.BlackFantasyBackground
import com.amkumirab.solostudying.ui.theme.DarkCardBorder
import com.amkumirab.solostudying.ui.theme.DarkFantasySurface
import com.amkumirab.solostudying.ui.theme.NeonBlueAccent
import com.amkumirab.solostudying.ui.theme.RpgEmerald
import com.amkumirab.solostudying.ui.theme.RpgGold
import com.amkumirab.solostudying.ui.theme.RpgRuby
import com.amkumirab.solostudying.ui.theme.TextMuted
import com.amkumirab.solostudying.ui.theme.TextWhite
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun DailyQuestBoard(
    quests: List<DailyQuestEntity>,
    allQuests: List<DailyQuestEntity>,
    recurringQuests: List<RecurringQuestEntity>,
    skills: List<SkillEntity>,
    today: String,
    activeQuestId: Int?,
    isSessionActive: Boolean,
    onCreateQuest: (String, Int, Int?, QuestPriority, RecurringQuestSchedule?) -> Unit,
    onUpdateQuest: (DailyQuestEntity, String, Int, Int?, QuestPriority) -> Unit,
    onSetCompleted: (DailyQuestEntity, Boolean) -> Unit,
    onDeleteQuest: (DailyQuestEntity) -> Unit,
    onUpdateRecurringQuest: (
        RecurringQuestEntity,
        String,
        Int,
        Int?,
        QuestPriority,
        RecurringQuestSchedule,
    ) -> Unit,
    onSetRecurringQuestActive: (RecurringQuestEntity, Boolean) -> Unit,
    onDeleteRecurringQuest: (RecurringQuestEntity) -> Unit,
    onStartQuest: (DailyQuestEntity) -> Unit,
) {
    var showEditor by remember { mutableStateOf(false) }
    var editingQuest by remember { mutableStateOf<DailyQuestEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<DailyQuestEntity?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var showRecurringQuests by remember { mutableStateOf(false) }
    var editingRecurringQuest by remember { mutableStateOf<RecurringQuestEntity?>(null) }
    val completedCount = quests.count { it.isCompleted }
    val progress = if (quests.isEmpty()) 0f else completedCount.toFloat() / quests.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_quest_board")
            .semantics { isTraversalGroup = true },
        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
        border = BorderStroke(1.5.dp, RpgGold.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Today, contentDescription = null, tint = RpgGold)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "DAILY QUEST BOARD",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = "$completedCount / ${quests.size} completed",
                            color = if (quests.isNotEmpty() && completedCount == quests.size) {
                                RpgEmerald
                            } else {
                                TextMuted
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick = { showRecurringQuests = true },
                        modifier = Modifier.testTag("recurring_quests_button"),
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Manage recurring quests",
                            tint = TextMuted,
                        )
                    }
                    IconButton(
                        onClick = { showHistory = true },
                        modifier = Modifier.testTag("daily_quest_history_button"),
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Open quest history", tint = TextMuted)
                    }
                    IconButton(
                        onClick = {
                            editingQuest = null
                            editingRecurringQuest = null
                            showEditor = true
                        },
                        modifier = Modifier.testTag("add_daily_quest_button"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add daily quest", tint = NeonBlueAccent)
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .testTag("daily_quest_progress")
                    .semantics {
                        contentDescription = "$completedCount of ${quests.size} daily quests completed"
                    },
                color = if (progress >= 1f && quests.isNotEmpty()) RpgEmerald else RpgGold,
                trackColor = Color(0xFF171C2A),
            )

            if (quests.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No quests planned for today.", color = TextMuted)
                    TextButton(
                        onClick = {
                            editingQuest = null
                            editingRecurringQuest = null
                            showEditor = true
                        },
                    ) {
                        Text("CREATE YOUR FIRST QUEST", color = NeonBlueAccent, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                quests.forEach { quest ->
                    DailyQuestRow(
                        quest = quest,
                        skill = skills.firstOrNull { it.id == quest.skillId },
                        isCarried = isCarriedDailyQuest(quest, today),
                        isActive = activeQuestId == quest.id,
                        isSessionActive = isSessionActive,
                        onToggle = { onSetCompleted(quest, !quest.isCompleted) },
                        onEdit = {
                            editingQuest = quest
                            showEditor = true
                        },
                        onDelete = { pendingDelete = quest },
                        onStart = { onStartQuest(quest) },
                    )
                }
            }

            if (quests.isNotEmpty() && completedCount == quests.size) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = RpgEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, RpgEmerald.copy(alpha = 0.6f)),
                ) {
                    Text(
                        text = "DAILY CLEAR — ALL QUESTS COMPLETE",
                        color = RpgEmerald,
                        fontWeight = FontWeight.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }

    if (showEditor) {
        DailyQuestEditorDialog(
            quest = editingQuest,
            recurringQuest = editingRecurringQuest,
            skills = skills,
            today = today,
            onDismiss = { showEditor = false },
            onSave = { title, duration, skillId, priority, repeatSchedule ->
                val existing = editingQuest
                val existingRecurring = editingRecurringQuest
                if (existingRecurring != null && repeatSchedule != null) {
                    onUpdateRecurringQuest(
                        existingRecurring,
                        title,
                        duration,
                        skillId,
                        priority,
                        repeatSchedule,
                    )
                } else if (existing == null) {
                    onCreateQuest(title, duration, skillId, priority, repeatSchedule)
                } else {
                    onUpdateQuest(existing, title, duration, skillId, priority)
                }
                showEditor = false
                editingRecurringQuest = null
            },
        )
    }

    pendingDelete?.let { quest ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete quest?") },
            text = {
                Text(
                    if (quest.recurringQuestId == null) {
                        "${quest.title} will be removed from your quest history."
                    } else {
                        "Only this occurrence will be skipped. Its recurring schedule will continue."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteQuest(quest)
                        pendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_daily_quest"),
                ) {
                    Text(
                        if (quest.recurringQuestId == null) "DELETE" else "SKIP OCCURRENCE",
                        color = RpgRuby,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("CANCEL") }
            },
            containerColor = DarkFantasySurface,
        )
    }

    if (showHistory) {
        DailyQuestHistoryDialog(
            quests = allQuests,
            today = today,
            onDismiss = { showHistory = false },
        )
    }

    if (showRecurringQuests) {
        RecurringQuestManagerDialog(
            quests = recurringQuests,
            today = LocalDate.parse(today),
            onDismiss = { showRecurringQuests = false },
            onEdit = { recurringQuest ->
                editingQuest = null
                editingRecurringQuest = recurringQuest
                showRecurringQuests = false
                showEditor = true
            },
            onSetActive = onSetRecurringQuestActive,
            onDelete = onDeleteRecurringQuest,
        )
    }
}

@Composable
private fun DailyQuestRow(
    quest: DailyQuestEntity,
    skill: SkillEntity?,
    isCarried: Boolean,
    isActive: Boolean,
    isSessionActive: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
) {
    val priority = QuestPriority.fromValue(quest.priority)
    val accent = when (priority) {
        QuestPriority.High -> RpgRuby
        QuestPriority.Normal -> RpgGold
        QuestPriority.Low -> NeonBlueAccent
    }
    val summary = buildString {
        append(quest.title)
        append(", ${quest.durationMinutes} minutes")
        skill?.let { append(", skill ${it.name}") }
        append(if (quest.isCompleted) ", completed" else ", not completed")
        if (isCarried) append(", carried over")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_quest_${quest.id}")
            .semantics { contentDescription = summary },
        colors = CardDefaults.cardColors(
            containerColor = if (quest.isCompleted) Color(0xFF11151D) else Color(0xFF171C2A),
        ),
        border = BorderStroke(1.dp, if (isActive) NeonBlueAccent else accent.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("toggle_daily_quest_${quest.id}"),
                ) {
                    Icon(
                        imageVector = if (quest.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (quest.isCompleted) "Mark ${quest.title} incomplete" else "Mark ${quest.title} complete",
                        tint = if (quest.isCompleted) RpgEmerald else accent,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.title,
                        color = if (quest.isCompleted) TextMuted else TextWhite,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (quest.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${quest.durationMinutes} MIN", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        skill?.let {
                            Text(it.name, color = NeonBlueAccent, fontSize = 10.sp, maxLines = 1)
                        }
                        if (isCarried) {
                            Text("CARRIED", color = RpgRuby, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        if (quest.recurringQuestId != null) {
                            Text("REPEATS", color = RpgEmerald, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_daily_quest_${quest.id}")) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${quest.title}", tint = TextMuted)
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_daily_quest_${quest.id}")) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${quest.title}", tint = RpgRuby)
                }
            }

            if (!quest.isCompleted) {
                Button(
                    onClick = onStart,
                    enabled = !isSessionActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("start_daily_quest_${quest.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) RpgEmerald else NeonBlueAccent,
                        contentColor = BlackFantasyBackground,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            isActive -> "QUEST ACTIVE"
                            isSessionActive -> "SESSION ACTIVE"
                            else -> "START QUEST"
                        },
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyQuestEditorDialog(
    quest: DailyQuestEntity?,
    recurringQuest: RecurringQuestEntity?,
    skills: List<SkillEntity>,
    today: String,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int?, QuestPriority, RecurringQuestSchedule?) -> Unit,
) {
    val editorKey = quest?.id ?: recurringQuest?.id?.let { -it }
    val initialTitle = quest?.title ?: recurringQuest?.title.orEmpty()
    val initialDuration = quest?.durationMinutes ?: recurringQuest?.durationMinutes ?: 25
    val initialSkillId = quest?.skillId ?: recurringQuest?.skillId
    val initialPriority = quest?.priority ?: recurringQuest?.priority ?: QuestPriority.Normal.value
    val initialMask = recurringQuest?.weekdaysMask ?: weekdayMask(LocalDate.parse(today).dayOfWeek)
    var title by remember(editorKey) { mutableStateOf(initialTitle) }
    var durationInput by remember(editorKey) { mutableStateOf(initialDuration.toString()) }
    var selectedSkillId by remember(editorKey) { mutableStateOf(initialSkillId) }
    var selectedPriority by remember(editorKey) {
        mutableStateOf(QuestPriority.fromValue(initialPriority))
    }
    var repeatChoice by remember(editorKey) {
        mutableStateOf(
            when {
                quest != null -> QuestRepeatChoice.Once
                recurringQuest?.weekdaysMask == ALL_WEEKDAYS_MASK -> QuestRepeatChoice.Daily
                recurringQuest != null -> QuestRepeatChoice.SelectedDays
                else -> QuestRepeatChoice.Once
            },
        )
    }
    var repeatDaysMask by remember(editorKey) { mutableStateOf(initialMask) }
    var skillMenuExpanded by remember { mutableStateOf(false) }
    val duration = durationInput.toIntOrNull()
    val repeatIsValid = repeatChoice != QuestRepeatChoice.SelectedDays || repeatDaysMask != 0
    val isValid = title.trim().isNotEmpty() && duration != null && duration in 1..480 && repeatIsValid
    val selectedSkill = skills.firstOrNull { it.id == selectedSkillId }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .testTag("daily_quest_editor"),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.5.dp, RpgGold),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = when {
                        recurringQuest != null -> "EDIT RECURRING QUEST"
                        quest == null -> "CREATE DAILY QUEST"
                        else -> "EDIT DAILY QUEST"
                    },
                    color = RpgGold,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() },
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_quest_title_input"),
                    label = { Text("What will you study?") },
                    singleLine = true,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    QUICK_START_PRESET_MINUTES.forEach { minutes ->
                        OutlinedButton(
                            onClick = { durationInput = minutes.toString() },
                            modifier = Modifier.testTag("daily_quest_duration_$minutes"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (duration == minutes) NeonBlueAccent else Color.Transparent,
                                contentColor = if (duration == minutes) BlackFantasyBackground else TextWhite,
                            ),
                        ) {
                            Text("$minutes M", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = durationInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all(Char::isDigit)) durationInput = input.take(3)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_quest_duration_input"),
                    label = { Text("Duration (1–480 minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = durationInput.isNotEmpty() && duration !in 1..480,
                )

                Box {
                    OutlinedButton(
                        onClick = { skillMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("daily_quest_skill_selector"),
                    ) {
                        Text(
                            text = selectedSkill?.name ?: "No skill allocation",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(Icons.Default.ExpandMore, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = skillMenuExpanded,
                        onDismissRequest = { skillMenuExpanded = false },
                        modifier = Modifier.background(DarkFantasySurface),
                    ) {
                        DropdownMenuItem(
                            text = { Text("No skill allocation", color = TextWhite) },
                            onClick = {
                                selectedSkillId = null
                                skillMenuExpanded = false
                            },
                        )
                        skills.forEach { skill ->
                            DropdownMenuItem(
                                text = { Text(skill.name, color = TextWhite) },
                                onClick = {
                                    selectedSkillId = skill.id
                                    skillMenuExpanded = false
                                },
                                modifier = Modifier.testTag("daily_quest_skill_${skill.id}"),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("PRIORITY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        QuestPriority.entries.forEach { priority ->
                            val selected = selectedPriority == priority
                            OutlinedButton(
                                onClick = { selectedPriority = priority },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("daily_quest_priority_${priority.name}"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) priorityColor(priority) else Color.Transparent,
                                    contentColor = if (selected) BlackFantasyBackground else TextWhite,
                                ),
                            ) {
                                Text(priority.displayName, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (quest == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("REPEAT", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            QuestRepeatChoice.entries.forEach { choice ->
                                val selected = repeatChoice == choice
                                OutlinedButton(
                                    onClick = { repeatChoice = choice },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quest_repeat_${choice.name}"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) NeonBlueAccent else Color.Transparent,
                                        contentColor = if (selected) BlackFantasyBackground else TextWhite,
                                    ),
                                ) {
                                    Text(choice.label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (repeatChoice == QuestRepeatChoice.SelectedDays) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                DayOfWeek.entries.forEach { day ->
                                    val bit = weekdayMask(day)
                                    val selected = repeatDaysMask and bit != 0
                                    OutlinedButton(
                                        onClick = {
                                            repeatDaysMask = if (selected) {
                                                repeatDaysMask and bit.inv()
                                            } else {
                                                repeatDaysMask or bit
                                            }
                                        },
                                        modifier = Modifier.testTag("quest_repeat_day_${day.name}"),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (selected) RpgEmerald else Color.Transparent,
                                            contentColor = if (selected) BlackFantasyBackground else TextWhite,
                                        ),
                                    ) {
                                        Text(day.name.take(3), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (repeatDaysMask == 0) {
                                Text(
                                    "Select at least one day.",
                                    color = RpgRuby,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Text(
                            text = when (repeatChoice) {
                                QuestRepeatChoice.Once -> "This quest appears only today."
                                QuestRepeatChoice.Daily -> "A new quest appears every day."
                                QuestRepeatChoice.SelectedDays -> "A new quest appears on the selected days."
                            },
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (recurringQuest != null) {
                            Text(
                                "Updates apply to future occurrences. Today's quest remains unchanged.",
                                color = RpgGold,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                } else if (quest.recurringQuestId != null) {
                    Text(
                        "Changes apply only to this occurrence. Manage its schedule from Recurring Quests.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("CANCEL", color = TextMuted)
                    }
                    Button(
                        onClick = {
                            val selectedDuration = duration ?: return@Button
                            val schedule = when (repeatChoice) {
                                QuestRepeatChoice.Once -> null
                                QuestRepeatChoice.Daily -> RecurringQuestSchedule(ALL_WEEKDAYS_MASK)
                                QuestRepeatChoice.SelectedDays -> RecurringQuestSchedule(repeatDaysMask)
                            }
                            onSave(
                                title.trim(),
                                selectedDuration,
                                selectedSkillId,
                                selectedPriority,
                                schedule,
                            )
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_daily_quest_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RpgGold),
                    ) {
                        Text(
                            if (quest == null && recurringQuest == null) "ADD QUEST" else "SAVE",
                            color = BlackFantasyBackground,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

private enum class QuestRepeatChoice(val label: String) {
    Once("ONCE"),
    Daily("DAILY"),
    SelectedDays("DAYS"),
}

@Composable
private fun RecurringQuestManagerDialog(
    quests: List<RecurringQuestEntity>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onEdit: (RecurringQuestEntity) -> Unit,
    onSetActive: (RecurringQuestEntity, Boolean) -> Unit,
    onDelete: (RecurringQuestEntity) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<RecurringQuestEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .testTag("recurring_quest_manager"),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.5.dp, RpgEmerald.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "RECURRING QUESTS",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text("Manage repeating study routines", color = TextMuted, fontSize = 11.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("CLOSE") }
                }

                if (quests.isEmpty()) {
                    Text(
                        "No recurring quests yet. Create one from the Daily Quest editor.",
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quests, key = { it.id }) { quest ->
                            val nextDate = nextRecurringQuestDate(quest, today)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF171C2A)),
                                border = BorderStroke(
                                    1.dp,
                                    if (quest.isActive) RpgEmerald.copy(alpha = 0.5f) else DarkCardBorder,
                                ),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        quest.title,
                                        color = if (quest.isActive) TextWhite else TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${recurringQuestScheduleLabel(quest.weekdaysMask)} · ${quest.durationMinutes} min",
                                        color = RpgEmerald,
                                        fontSize = 11.sp,
                                    )
                                    Text(
                                        if (nextDate == null) "Paused" else "Next: $nextDate",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        IconButton(
                                            onClick = { onEdit(quest) },
                                            modifier = Modifier.testTag("edit_recurring_quest_${quest.id}"),
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit ${quest.title} schedule",
                                                tint = TextMuted,
                                            )
                                        }
                                        IconButton(
                                            onClick = { onSetActive(quest, !quest.isActive) },
                                            modifier = Modifier.testTag("toggle_recurring_quest_${quest.id}"),
                                        ) {
                                            Icon(
                                                if (quest.isActive) {
                                                    Icons.Default.PauseCircleOutline
                                                } else {
                                                    Icons.Default.PlayCircleOutline
                                                },
                                                contentDescription = if (quest.isActive) {
                                                    "Pause ${quest.title} schedule"
                                                } else {
                                                    "Resume ${quest.title} schedule"
                                                },
                                                tint = if (quest.isActive) RpgGold else RpgEmerald,
                                            )
                                        }
                                        IconButton(
                                            onClick = { pendingDelete = quest },
                                            modifier = Modifier.testTag("delete_recurring_quest_${quest.id}"),
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete ${quest.title} schedule",
                                                tint = RpgRuby,
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
    }

    pendingDelete?.let { quest ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete recurring quest?") },
            text = {
                Text("Future occurrences of ${quest.title} will stop. Existing history will be kept.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(quest)
                        pendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_recurring_quest"),
                ) {
                    Text("DELETE SCHEDULE", color = RpgRuby)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("CANCEL") }
            },
            containerColor = DarkFantasySurface,
        )
    }
}

@Composable
private fun DailyQuestHistoryDialog(
    quests: List<DailyQuestEntity>,
    today: String,
    onDismiss: () -> Unit,
) {
    val history = remember(quests, today) {
        quests.filter { !it.isSkipped && (it.scheduledDate < today || it.isCompleted) }
            .sortedWith(compareByDescending<DailyQuestEntity> { it.scheduledDate }.thenByDescending { it.createdAt })
            .take(50)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .testTag("daily_quest_history_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.dp, DarkCardBorder),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("QUEST HISTORY", color = TextWhite, fontWeight = FontWeight.Black)
                    TextButton(onClick = onDismiss) { Text("CLOSE") }
                }
                if (history.isEmpty()) {
                    Text("Completed and past quests will appear here.", color = TextMuted)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(history, key = { it.id }) { quest ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF171C2A), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .background(if (quest.isCompleted) RpgEmerald else RpgRuby, CircleShape),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(quest.title, color = TextWhite, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(
                                        "${quest.scheduledDate} · ${quest.durationMinutes} min",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                    )
                                }
                                Text(
                                    if (quest.isCompleted) "DONE" else "PENDING",
                                    color = if (quest.isCompleted) RpgEmerald else RpgRuby,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun priorityColor(priority: QuestPriority): Color = when (priority) {
    QuestPriority.Low -> NeonBlueAccent
    QuestPriority.Normal -> RpgGold
    QuestPriority.High -> RpgRuby
}
