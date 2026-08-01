package com.amkumirab.solostudying.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Generates short interface sounds without bundled audio files. */
object RpgSoundManager {
    private const val TAG = "RpgSoundManager"
    private const val SAMPLE_RATE = 22_050
    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun playClickSound() = play(Tone(1_300f, 55, 0.16f), Tone(750f, 35, 0.10f))

    fun playBeginBattleSound() =
        play(Tone(330f, 90), Tone(494f, 90), Tone(659f, 150))

    fun playPauseStudySound() = play(Tone(700f, 80), Tone(420f, 110))

    fun playResumeStudySound() = play(Tone(420f, 80), Tone(820f, 120))

    fun playEndSessionSound() = play(Tone(523f, 90), Tone(659f, 90), Tone(784f, 160))

    fun playBossDefeatedSound() = playConquerSound()

    fun playConquerSound() =
        play(Tone(330f, 100), Tone(494f, 100), Tone(659f, 120), Tone(831f, 220))

    fun playGoldSpendSound() = play(Tone(880f, 65), Tone(660f, 65), Tone(440f, 100))

    fun playShopPurchaseSound() = play(Tone(660f, 70), Tone(990f, 130))

    fun playSkillUnlockSound() =
        play(Tone(392f, 70), Tone(523f, 70), Tone(659f, 70), Tone(988f, 180))

    fun playLevelUpSound() =
        play(
            Tone(262f, 65),
            Tone(330f, 65),
            Tone(392f, 65),
            Tone(523f, 65),
            Tone(659f, 65),
            Tone(784f, 65),
            Tone(1_047f, 220),
        )

    fun playWarningAlarmSound() =
        play(Tone(580f, 120, 0.24f), Tone(280f, 120, 0.24f), Tone(580f, 160, 0.24f))

    private fun play(vararg tones: Tone) {
        playbackScope.launch {
            val samples = synthesize(tones)
            val bufferSize = maxOf(
                AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                ),
                samples.size * Short.SIZE_BYTES,
            )

            val audioTrack = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } catch (error: IllegalArgumentException) {
                Log.w(TAG, "Unable to create an audio track", error)
                return@launch
            }

            try {
                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                delay(tones.sumOf { it.durationMs }.toLong() + 80L)
            } catch (error: IllegalStateException) {
                Log.w(TAG, "Unable to play interface sound", error)
            } finally {
                runCatching { audioTrack.stop() }
                audioTrack.release()
            }
        }
    }

    private fun synthesize(tones: Array<out Tone>): ShortArray {
        val sampleCount = tones.sumOf { tone ->
            (SAMPLE_RATE * tone.durationMs / 1_000f).toInt()
        }
        val samples = ShortArray(sampleCount)
        var offset = 0

        tones.forEach { tone ->
            val toneSamples = (SAMPLE_RATE * tone.durationMs / 1_000f).toInt()
            for (index in 0 until toneSamples) {
                val progress = index.toFloat() / toneSamples.coerceAtLeast(1)
                val attack = (progress / 0.08f).coerceAtMost(1f)
                val envelope = attack * (1f - progress)
                val time = index.toDouble() / SAMPLE_RATE
                val wave = sin(2.0 * PI * tone.frequencyHz.toDouble() * time)
                samples[offset + index] =
                    (wave * Short.MAX_VALUE * tone.volume * envelope).toInt().toShort()
            }
            offset += toneSamples
        }
        return samples
    }

    private data class Tone(
        val frequencyHz: Float,
        val durationMs: Int,
        val volume: Float = 0.20f,
    )
}
