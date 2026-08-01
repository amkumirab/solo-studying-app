package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.DungeonEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.sound.RpgSoundManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DungeonViewModel(
    private val repository: SoloStudyingRepository,
    private val context: Context
) : ViewModel() {

    val bosses: StateFlow<List<BossEntity>> = repository.allBosses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dungeons: StateFlow<List<DungeonEntity>> = repository.allDungeons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Prepopulate default dungeons if empty
        viewModelScope.launch {
            val list = repository.allDungeons.first()
            if (list.isEmpty()) {
                prepopulateDefaultDungeons()
            }
        }
    }

    private suspend fun prepopulateDefaultDungeons() {
        val defaults = listOf(
            DungeonEntity(name = "Main Realm", description = "Standard daily learning quests.", status = "Unlocked"),
            DungeonEntity(name = "Language Colosseum", description = "Conquer linguistics and foreign tongues.", status = "Unlocked"),
            DungeonEntity(name = "Code Catacombs", description = "Crawl through compiler depths and algorithm dungeons.", status = "Unlocked"),
            DungeonEntity(name = "Academic Citadel", description = "Review major exams, lecture summaries, and grand texts.", status = "Unlocked")
        )
        defaults.forEach { repository.insertDungeon(it) }
    }

    fun createBoss(name: String, difficulty: String, durationMinutes: Int, imagePath: String?, dungeonName: String = "Main Realm", isRealBoss: Boolean = false) {
        viewModelScope.launch {
            repository.insertBoss(
                BossEntity(
                    name = name,
                    difficulty = difficulty,
                    requiredMinutes = durationMinutes,
                    imagePath = imagePath,
                    dungeonName = dungeonName,
                    isRealBoss = isRealBoss
                )
            )
        }
    }

    fun deleteBoss(boss: BossEntity) {
        viewModelScope.launch {
            repository.deleteBoss(boss)
        }
    }

    fun createDungeon(name: String, description: String, unlockedTitle: String) {
        viewModelScope.launch {
            repository.insertDungeon(
                DungeonEntity(
                    name = name,
                    description = description,
                    status = "Unlocked",
                    unlockedTitle = unlockedTitle
                )
            )
        }
    }

    fun activateRedDungeonXpBoost() {
        viewModelScope.launch {
            val profile = repository.getProfileSync() ?: return@launch
            if (profile.gold >= 100 && !profile.isRedDungeonBoostActive) {
                val updated = profile.copy(
                    gold = profile.gold - 100,
                    isRedDungeonBoostActive = true
                )
                repository.insertOrUpdateProfile(updated)
                RpgSoundManager.playGoldSpendSound()
            }
        }
    }
}
