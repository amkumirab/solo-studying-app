package com.amkumirab.solostudying.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.entity.RecurringQuestEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.domain.quest.QuestPriority
import com.amkumirab.solostudying.domain.quest.RecurringQuestSchedule
import com.amkumirab.solostudying.domain.quest.buildRecurringQuestOccurrence
import com.amkumirab.solostudying.domain.quest.dailyQuestDateKey
import com.amkumirab.solostudying.domain.quest.selectVisibleDailyQuests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.time.LocalDate

class DailyQuestViewModel(
    private val repository: SoloStudyingRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val timeZone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val currentDate = MutableStateFlow(dateKey())

    val allQuests: StateFlow<List<DailyQuestEntity>> = repository.allDailyQuests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val recurringQuests: StateFlow<List<RecurringQuestEntity>> = repository.allRecurringQuests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val todayQuests: StateFlow<List<DailyQuestEntity>> = combine(
        repository.allDailyQuests,
        currentDate,
    ) { quests, today ->
        selectVisibleDailyQuests(quests, today)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val todayDate: String get() = currentDate.value

    init {
        ensureTodayOccurrences(currentDate.value)
    }

    fun refreshToday() {
        val today = dateKey()
        currentDate.value = today
        ensureTodayOccurrences(today)
    }

    fun createQuest(
        title: String,
        durationMinutes: Int,
        skillId: Int?,
        priority: QuestPriority,
        repeatSchedule: RecurringQuestSchedule? = null,
    ) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank() || durationMinutes !in 1..480) return
        viewModelScope.launch {
            val normalizedSkillId = skillId?.takeIf { it > 0 }
            if (repeatSchedule == null) {
                repository.insertDailyQuest(
                    DailyQuestEntity(
                        title = normalizedTitle,
                        durationMinutes = durationMinutes,
                        skillId = normalizedSkillId,
                        scheduledDate = dateKey(),
                        priority = priority.value,
                        createdAt = clock(),
                    ),
                )
            } else {
                val template = RecurringQuestEntity(
                    title = normalizedTitle,
                    durationMinutes = durationMinutes,
                    skillId = normalizedSkillId,
                    priority = priority.value,
                    weekdaysMask = repeatSchedule.weekdaysMask,
                    createdAt = clock(),
                )
                val id = repository.insertRecurringQuest(template).toInt()
                createOccurrenceIfDue(template.copy(id = id), LocalDate.parse(dateKey()))
            }
        }
    }

    fun setQuestCompleted(quest: DailyQuestEntity, completed: Boolean) {
        viewModelScope.launch {
            repository.updateDailyQuest(
                quest.copy(
                    isCompleted = completed,
                    completedAt = if (completed) clock() else null,
                ),
            )
        }
    }

    fun updateQuest(
        quest: DailyQuestEntity,
        title: String,
        durationMinutes: Int,
        skillId: Int?,
        priority: QuestPriority,
    ) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank() || durationMinutes !in 1..480) return
        viewModelScope.launch {
            repository.updateDailyQuest(
                quest.copy(
                    title = normalizedTitle,
                    durationMinutes = durationMinutes,
                    skillId = skillId?.takeIf { it > 0 },
                    priority = priority.value,
                ),
            )
        }
    }

    fun deleteQuest(quest: DailyQuestEntity) {
        viewModelScope.launch {
            if (quest.recurringQuestId == null) {
                repository.deleteDailyQuest(quest)
            } else {
                repository.updateDailyQuest(
                    quest.copy(isSkipped = true, isCompleted = false, completedAt = null),
                )
            }
        }
    }

    fun updateRecurringQuest(
        quest: RecurringQuestEntity,
        title: String,
        durationMinutes: Int,
        skillId: Int?,
        priority: QuestPriority,
        repeatSchedule: RecurringQuestSchedule,
    ) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank() || durationMinutes !in 1..480) return
        viewModelScope.launch {
            val updated = quest.copy(
                title = normalizedTitle,
                durationMinutes = durationMinutes,
                skillId = skillId?.takeIf { it > 0 },
                priority = priority.value,
                weekdaysMask = repeatSchedule.weekdaysMask,
            )
            repository.updateRecurringQuest(updated)
            createOccurrenceIfDue(updated, LocalDate.parse(dateKey()))
        }
    }

    fun setRecurringQuestActive(quest: RecurringQuestEntity, active: Boolean) {
        viewModelScope.launch {
            val updated = quest.copy(isActive = active)
            repository.updateRecurringQuest(updated)
            if (active) createOccurrenceIfDue(updated, LocalDate.parse(dateKey()))
        }
    }

    fun deleteRecurringQuest(quest: RecurringQuestEntity) {
        viewModelScope.launch {
            repository.deleteRecurringQuest(quest)
        }
    }

    private fun ensureTodayOccurrences(date: String) {
        viewModelScope.launch {
            val today = LocalDate.parse(date)
            repository.getActiveRecurringQuests().forEach { quest ->
                createOccurrenceIfDue(quest, today)
            }
        }
    }

    private suspend fun createOccurrenceIfDue(quest: RecurringQuestEntity, date: LocalDate) {
        buildRecurringQuestOccurrence(quest, date, clock())?.let { occurrence ->
            repository.insertDailyQuestIfAbsent(occurrence)
        }
    }

    private fun dateKey(): String = dailyQuestDateKey(clock(), timeZone)
}
