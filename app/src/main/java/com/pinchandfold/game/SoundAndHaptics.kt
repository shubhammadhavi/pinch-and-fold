package com.pinchandfold.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages SoundPool SFX playback and Android haptic feedback.
 */
class SoundAndHaptics(context: Context) {

    var muted: Boolean = false

    private val soundPool: SoundPool
    private val sfxSnap: Int
    private val sfxHit: Int
    private val sfxRopeBounce: Int
    private val sfxStretch: Int
    private val sfxRingOut: Int
    private val sfxWin: Int
    private val sfxUiTap: Int

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()

        sfxSnap = soundPool.load(context, R.raw.sfx_snap, 1)
        sfxHit = soundPool.load(context, R.raw.sfx_hit, 1)
        sfxRopeBounce = soundPool.load(context, R.raw.sfx_rope_bounce, 1)
        sfxStretch = soundPool.load(context, R.raw.sfx_stretch, 1)
        sfxRingOut = soundPool.load(context, R.raw.sfx_ring_out, 1)
        sfxWin = soundPool.load(context, R.raw.sfx_win, 1)
        sfxUiTap = soundPool.load(context, R.raw.sfx_ui_tap, 1)
    }

    fun playSnap(volume: Float = 0.9f) = play(sfxSnap, volume, 0.95f + Math.random().toFloat() * 0.12f)

    fun playHit(intensity: Float = 1f) {
        val vol = intensity.coerceIn(0.25f, 1.0f)
        val rate = 0.88f + Math.random().toFloat() * 0.24f
        play(sfxHit, vol, rate)
    }

    fun playRopeBounce(intensity: Float = 0.8f) =
        play(sfxRopeBounce, intensity.coerceIn(0.2f, 0.95f), 0.95f + Math.random().toFloat() * 0.12f)

    fun playStretchTick() = play(sfxStretch, 0.32f, 0.92f + Math.random().toFloat() * 0.2f)

    fun playRingOut() = play(sfxRingOut, 0.9f, 1.0f)

    fun playWin() = play(sfxWin, 0.85f, 1.0f)

    fun playUiTap() = play(sfxUiTap, 0.75f, 1.0f)

    private fun play(soundId: Int, volume: Float, rate: Float) {
        if (muted) return
        soundPool.play(soundId, volume, volume, 1, 0, rate)
    }

    fun vibrate(durationMs: Long, amplitude: Int = 140) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }

    fun release() {
        soundPool.release()
    }
}
