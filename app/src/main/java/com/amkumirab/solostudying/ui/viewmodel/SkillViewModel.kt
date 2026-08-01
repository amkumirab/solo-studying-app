package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.data.entity.BossSkillEntity
import com.amkumirab.solostudying.data.entity.SkillEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SkillViewModel(
    private val repository: SoloStudyingRepository,
    private val context: Context
) : ViewModel() {

    val skills: StateFlow<List<SkillEntity>> = repository.allSkills.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Prepopulate default expert study skills if empty
        viewModelScope.launch {
            val list = repository.allSkills.first()
            if (list.isEmpty()) {
                prepopulateDefaultSkills()
            }
        }
    }

    private suspend fun prepopulateDefaultSkills() {
        val defaults = listOf(
            SkillEntity(name = "Pomodoro Concentration", targetMinutes = 150, isUnlocked = false, suggestion = "Enhance focus loops using high concentration pomodoro alignments."),
            SkillEntity(name = "Deep Learning Sage", targetMinutes = 300, isUnlocked = false, suggestion = "Study complex topics without drifting into procrastination."),
            SkillEntity(name = "Memory Shield", targetMinutes = 200, isUnlocked = false, suggestion = "Safeguard memorized terms and facts with flashcard spells."),
            SkillEntity(name = "Critical Thinking Strike", targetMinutes = 250, isUnlocked = false, suggestion = "Break down difficult formulas and multi-layered challenges.")
        )
        defaults.forEach { repository.insertSkill(it) }
    }

    fun createSkill(name: String, targetMinutes: Int, suggestion: String = "Estimated study duration required") {
        viewModelScope.launch {
            repository.insertSkill(
                SkillEntity(
                    name = name,
                    targetMinutes = targetMinutes,
                    suggestion = suggestion,
                    isUnlocked = false
                )
            )
        }
    }

    fun deleteSkill(skill: SkillEntity) {
        viewModelScope.launch {
            repository.deleteSkill(skill)
        }
    }

    fun associateSkillWithBoss(bossId: Int, skillId: Int) {
        viewModelScope.launch {
            repository.insertBossSkill(BossSkillEntity(bossId = bossId, skillId = skillId))
        }
    }

    fun removeSkillsFromBoss(bossId: Int) {
        viewModelScope.launch {
            repository.deleteBossSkillsForBoss(bossId)
        }
    }

    fun getSkillsForBoss(bossId: Int) = repository.getSkillsForBoss(bossId)
}
