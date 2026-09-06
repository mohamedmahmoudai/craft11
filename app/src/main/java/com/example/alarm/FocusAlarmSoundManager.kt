package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object FocusAlarmSoundManager {
    private const val TAG = "FocusAlarmSoundManager"
    private var activeRingtone: Ringtone? = null
    private var activeVibrator: Vibrator? = null
    private var isPlaying = false

    fun startAlarm(context: Context) {
        if (isPlaying) return
        isPlaying = true

        try {
            // 1. Play Alarm Sound
            val alarmUri = com.example.data.preferences.UserPreferencesManager(context)
                .getEffectiveAlarmUri(context)

            val ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            ringtone?.play()
            activeRingtone = ringtone
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
        }

        try {
            // 2. Trigger Pulsing Vibration
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            activeVibrator = vibrator
            val pattern = longArrayOf(0, 600, 300, 600, 300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }

    fun stopAlarm() {
        isPlaying = false
        try {
            activeRingtone?.stop()
            activeRingtone = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ringtone", e)
        }

        try {
            activeVibrator?.cancel()
            activeVibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration", e)
        }
    }
}
