package de.bcycle.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager

/** Completion only, including a finished measurement with poor quality; never abort/history/retry. */
internal object HrvCompletionSignal {
    fun play(context:Context,config:HrvMeasurementConfig) {
        if(config.vibrateAtEnd) runCatching {
            val vibrator=context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            if(vibrator?.hasVibrator()==true) vibrator.vibrate(VibrationEffect.createOneShot(250,VibrationEffect.DEFAULT_AMPLITUDE),
                VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_NOTIFICATION).build())
        }
        if(config.soundAtEnd) runCatching {
            val tone=ToneGenerator(AudioManager.STREAM_NOTIFICATION,65)
            try { tone.startTone(ToneGenerator.TONE_PROP_ACK,250) }
            finally { Handler(Looper.getMainLooper()).postDelayed({ tone.release() },500) }
        }
    }
}
