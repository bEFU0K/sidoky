package com.pixelsudoku

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import com.pixelsudoku.data.Settings

/** Короткие ретро-сигналы без внешних файлов и авторских прав (ToneGenerator). */
object Sfx {
    enum class T { TAP, PLACE, ERROR, WIN }

    fun play(s: Settings, t: T) {
        if (!s.sound) return
        try {
            val (tone, ms) = when (t) {
                T.TAP -> ToneGenerator.TONE_PROP_BEEP to 50
                T.PLACE -> ToneGenerator.TONE_PROP_ACK to 80
                T.ERROR -> ToneGenerator.TONE_PROP_NACK to 150
                T.WIN -> ToneGenerator.TONE_PROP_PROMPT to 400
            }
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, (s.volume * 100).toInt().coerceIn(1, 100))
            tg.startTone(tone, ms)
            Handler(Looper.getMainLooper()).postDelayed({ tg.release() }, ms + 200L)
        } catch (_: Exception) { }
    }

    fun vibrate(ctx: Context, s: Settings) {
        if (!s.vibration) return
        try {
            val v = ctx.getSystemService(Vibrator::class.java)
            if (v != null && v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) { }
    }
}
