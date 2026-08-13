package com.amkumirab.solostudying.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amkumirab.solostudying.data.entity.RewardBalanceEntity
import com.amkumirab.solostudying.data.entity.RewardItemEntity
import com.amkumirab.solostudying.data.repository.RewardPurchaseStatus
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import com.amkumirab.solostudying.sound.RpgSoundManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShopViewModel(
    private val repository: SoloStudyingRepository,
) : ViewModel() {

    val rewards: StateFlow<List<RewardItemEntity>> = repository.allRewards.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val balances: StateFlow<List<RewardBalanceEntity>> = repository.allBalances.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            val list = repository.allRewards.first()
            if (list.isEmpty()) {
                createDefaultRewards()
            }
        }
    }

    private suspend fun createDefaultRewards() {
        val defaults = listOf(
            RewardItemEntity(name = "Gaming Time", description = "Allow yourself to play high-concentration games.", cost = 300, rewardType = "Time-Based", rewardValue = 1, isCustom = false),
            RewardItemEntity(name = "Watch Anime", description = "Watch 1 episode of your favorite anime series.", cost = 150, rewardType = "Time-Based", rewardValue = 1, isCustom = false),
            RewardItemEntity(name = "Coffee Break", description = "Grab a coffee from your favourite espresso bar.", cost = 60, rewardType = "One-Time", isCustom = false),
            RewardItemEntity(name = "Favorite Snack", description = "Reward yourself with chocolate or chips.", cost = 100, rewardType = "One-Time", isCustom = false),
            RewardItemEntity(name = "Movie Night", description = "Watch a full movie with popcorn.", cost = 250, rewardType = "One-Time", isCustom = false),
            RewardItemEntity(name = "Social Media Time", description = "30 mins of infinite scrolling guilt-free.", cost = 80, rewardType = "Time-Based", rewardValue = 1, isCustom = false)
        )
        defaults.forEach { repository.insertReward(it) }
    }

    fun createReward(name: String, description: String, cost: Int, rewardType: String, rewardValue: Int) {
        viewModelScope.launch {
            repository.insertReward(
                RewardItemEntity(
                    name = name,
                    description = description,
                    cost = cost,
                    rewardType = rewardType,
                    rewardValue = rewardValue,
                    isCustom = true
                )
            )
        }
    }

    fun deleteReward(reward: RewardItemEntity) {
        viewModelScope.launch {
            repository.deleteReward(reward)
        }
    }

    fun purchaseReward(reward: RewardItemEntity, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            when (repository.purchaseReward(reward)) {
                RewardPurchaseStatus.Purchased -> {
                    RpgSoundManager.playShopPurchaseSound()
                    onResult(true, "Successfully purchased ${reward.name}!")
                }
                RewardPurchaseStatus.InsufficientGold -> {
                    onResult(false, "Not enough Gold to purchase this reward!")
                }
                RewardPurchaseStatus.MissingProfile -> {
                    onResult(false, "Player profile is unavailable. Please reopen the app and try again.")
                }
            }
        }
    }

    fun useReward(rewardName: String, amountToUse: Float, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val used = repository.useReward(rewardName, amountToUse)
            onResult(used)
            if (used) RpgSoundManager.playGoldSpendSound()
        }
    }

    fun useRewardTime(rewardName: String, amountToUse: Float, onResult: (Boolean) -> Unit) {
        useReward(rewardName, amountToUse, onResult)
    }
}
