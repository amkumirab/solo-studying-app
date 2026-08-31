package com.amkumirab.solostudying.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.domain.quest.QuestPriority
import com.amkumirab.solostudying.domain.quest.dailyQuestDateKey
import com.amkumirab.solostudying.domain.quest.selectVisibleDailyQuests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone

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

    fun refreshToday() {
        currentDate.value = dateKey()
    }

    fun createQuest(
        title: String,
        durationMinutes: Int,
        skillId: Int?,
        priority: QuestPriority,
    ) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank() || durationMinutes !in 1..480) return
        viewModelScope.launch {
            repository.insertDailyQuest(
                DailyQuestEntity(
                    title = normalizedTitle,
                    durationMinutes = durationMinutes,
                    skillId = skillId?.takeIf { it > 0 },
                    scheduledDate = dateKey(),
                    priority = priority.value,
                    createdAt = clock(),
                ),
            )
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
            repository.deleteDailyQuest(quest)
        }
    }

    private fun dateKey(): String = dailyQuestDateKey(clock(), timeZone)
}
