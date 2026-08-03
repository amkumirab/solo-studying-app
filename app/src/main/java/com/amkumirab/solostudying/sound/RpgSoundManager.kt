package com.amkumirab.solostudying.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.annotation.RawRes
import com.amkumirab.solostudying.R
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Plays short, low-latency feedback sounds for study and progression events. */
object RpgSoundManager {
    private const val TAG = "RpgSoundManager"
    private const val MAX_STREAMS = 4

    private val soundIds = EnumMap<Effect, Int>(Effect::class.java)
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val mutableSettings = MutableStateFlow(SoundSettings())

    val settings: StateFlow<SoundSettings> = mutableSettings.asStateFlow()

    @Volatile
    private var soundPool: SoundPool? = null
    private var settingsStore: SoundSettingsStore? = null

    /** Preloads every sound once using the application context. */
    @Synchronized
    fun initialize(context: Context) {
        val store = SoundSettingsStore(context.applicationContext)
        settingsStore = store
        mutableSettings.value = store.read()

        if (soundPool != null) return

        val pool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            } else {
                Log.w(TAG, "Unable to load sound sample $sampleId (status $status)")
            }
        }

        soundPool = pool
        Effect.entries.forEach { effect ->
            soundIds[effect] = pool.load(context.applicationContext, effect.resourceId, 1)
        }
    }

    fun playClickSound() = play(Effect.CLICK)

    fun playBeginBattleSound() = play(Effect.BATTLE_START)

    fun playPauseStudySound() = play(Effect.STUDY_PAUSE)

    fun playResumeStudySound() = play(Effect.STUDY_RESUME)

    fun playEndSessionSound() = play(Effect.SESSION_COMPLETE)

    fun playBossDefeatedSound() = play(Effect.BOSS_DEFEATED)

    fun playConquerSound() = play(Effect.BOSS_DEFEATED)

    fun playGoldSpendSound() = play(Effect.GOLD_SPEND)

    fun playShopPurchaseSound() = play(Effect.SHOP_PURCHASE)

    fun playSkillUnlockSound() = play(Effect.SKILL_UNLOCK)

    fun playLevelUpSound() = play(Effect.LEVEL_UP)

    fun playWarningAlarmSound() = play(Effect.WARNING)

    fun previewSound() = play(Effect.SESSION_COMPLETE)

    @Synchronized
    fun setSoundEnabled(enabled: Boolean) {
        updateSettings(mutableSettings.value.copy(enabled = enabled))
    }

    @Synchronized
    fun setVolume(volume: Float) {
        val normalizedVolume = if (volume.isFinite()) volume.coerceIn(0f, 1f) else 1f
        updateSettings(mutableSettings.value.copy(volume = normalizedVolume))
    }

    @Synchronized
    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        loadedSoundIds.clear()
    }

    private fun play(effect: Effect) {
        val currentSettings = mutableSettings.value
        if (!currentSettings.enabled || currentSettings.volume <= 0f) return

        val pool = soundPool ?: return
        val soundId = soundIds[effect] ?: return
        if (soundId !in loadedSoundIds) return

        val playbackVolume = effect.volume * currentSettings.volume
        val streamId = pool.play(
            soundId,
            playbackVolume,
            playbackVolume,
            1,
            0,
            effect.playbackRate,
        )
        if (streamId == 0) {
            Log.d(TAG, "No stream was available for ${effect.name}")
        }
    }

    private fun updateSettings(settings: SoundSettings) {
        mutableSettings.value = settings
        settingsStore?.write(settings)
    }

    private enum class Effect(
        @param:RawRes val resourceId: Int,
        val volume: Float = 0.72f,
        val playbackRate: Float = 1f,
    ) {
        CLICK(R.raw.system_click, volume = 0.48f),
        BATTLE_START(R.raw.battle_start, volume = 0.82f),
        STUDY_PAUSE(R.raw.study_pause, volume = 0.62f),
        STUDY_RESUME(R.raw.study_resume, volume = 0.68f),
        SESSION_COMPLETE(R.raw.session_complete, volume = 0.78f),
        BOSS_DEFEATED(R.raw.boss_defeated, volume = 0.88f),
        GOLD_SPEND(R.raw.gold_spend, volume = 0.58f),
        SHOP_PURCHASE(R.raw.shop_purchase, volume = 0.66f),
        SKILL_UNLOCK(R.raw.skill_unlock, volume = 0.82f),
        LEVEL_UP(R.raw.level_up, volume = 0.9f),
        WARNING(R.raw.system_warning, volume = 0.76f),
    }
}
