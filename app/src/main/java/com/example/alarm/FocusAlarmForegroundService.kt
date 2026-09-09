package com.example.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class FocusAlarmForegroundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isRinging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FocusAlarmForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val action = intent.action ?: ACTION_START_ALARM
        val taskId = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_ID) ?: "default_task"
        val taskTitle = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_TITLE) ?: "مهمة مجدولة"
        val taskSubtitle = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE) ?: "حان موعد بدء المهمة"
        val notificationId = taskId.hashCode()

        Log.d(TAG, "Foreground service received action: $action for task: $taskTitle")

        when (action) {
            ACTION_START_ALARM -> {
                acquireWakeLock()
                val notification = buildAlarmNotification(taskId, taskTitle, taskSubtitle, notificationId)
                startForeground(notificationId, notification)
                startAlarmSoundAndVibration()
            }

            ACTION_DISMISS_ALARM -> {
                stopAlarmAndSelf()
            }

            ACTION_SNOOZE_ALARM -> {
                FocusAlarmManager(applicationContext).snoozeTaskAlarm(taskId, taskTitle, taskSubtitle, 10)
                stopAlarmAndSelf()
            }

            ACTION_START_TASK -> {
                val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(FocusAlarmManager.EXTRA_START_FOCUS_TASK_ID, taskId)
                    putExtra(FocusAlarmManager.EXTRA_TASK_TITLE, taskTitle)
                    putExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE, taskSubtitle)
                }
                applicationContext.startActivity(mainIntent)
                stopAlarmAndSelf()
            }

            else -> {
                stopAlarmAndSelf()
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                @Suppress("DEPRECATION")
                wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "FocusCraft:AlarmWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(5 * 60 * 1000L) // 5 minutes max
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock acquisition warning: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock release warning: ${e.message}")
        }
    }

    private fun startAlarmSoundAndVibration() {
        if (isRinging) return
        isRinging = true

        // Ensure any ongoing audio or ringtone is stopped immediately to prevent overlap
        FocusAlarmSoundManager.stopAlarm()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error resetting previous media player: ${e.message}")
        }

        val prefs = com.example.data.preferences.UserPreferencesManager(applicationContext)
        val customAlarmUri = prefs.alarmSoundUri

        if (!customAlarmUri.isNullOrBlank()) {
            // If custom system ringtone is selected, play it via FocusAlarmSoundManager exclusively
            Log.d(TAG, "Playing user selected custom ringtone: $customAlarmUri")
            FocusAlarmSoundManager.startAlarm(applicationContext)
        } else {
            // Otherwise fall back to built-in sound (res/raw/default_alarm.mp3)
            try {
                val alarmUri = prefs.getEffectiveAlarmUri(applicationContext)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, alarmUri)
                    setAudioAttributes(audioAttributes)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer error, falling back to RingtoneManager", e)
                FocusAlarmSoundManager.startAlarm(applicationContext)
            }
        }

        // 2. Pulse Vibration
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 600, 300, 600, 300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration warning: ${e.message}")
        }
    }

    private fun stopAlarmSoundAndVibration() {
        isRinging = false
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media player: ${e.message}")
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling vibration: ${e.message}")
        }

        FocusAlarmSoundManager.stopAlarm()
    }

    private fun stopAlarmAndSelf() {
        stopAlarmSoundAndVibration()
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun buildAlarmNotification(
        taskId: String,
        taskTitle: String,
        taskSubtitle: String,
        notificationId: Int
    ): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        // Full-screen Intent opening SmartAlarmActivity in isolated mode
        val fullScreenIntent = Intent(this, com.example.ui.screens.SmartAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(FocusAlarmManager.EXTRA_TRIGGER_SMART_ALARM, true)
            putExtra(FocusAlarmManager.EXTRA_TASK_ID, taskId)
            putExtra(FocusAlarmManager.EXTRA_TASK_TITLE, taskTitle)
            putExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE, taskSubtitle)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            fullScreenIntent,
            flags
        )

        // Action 1: Start Focus Task
        val startTaskIntent = Intent(this, FocusAlarmForegroundService::class.java).apply {
            action = ACTION_START_TASK
            putExtra(FocusAlarmManager.EXTRA_TASK_ID, taskId)
            putExtra(FocusAlarmManager.EXTRA_TASK_TITLE, taskTitle)
            putExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE, taskSubtitle)
        }
        val startTaskPendingIntent = PendingIntent.getService(
            this,
            (taskId + "_fg_start").hashCode(),
            startTaskIntent,
            flags
        )

        // Action 2: Snooze 10 minutes
        val snoozeIntent = Intent(this, FocusAlarmForegroundService::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(FocusAlarmManager.EXTRA_TASK_ID, taskId)
            putExtra(FocusAlarmManager.EXTRA_TASK_TITLE, taskTitle)
            putExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE, taskSubtitle)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            (taskId + "_fg_snooze").hashCode(),
            snoozeIntent,
            flags
        )

        // Action 3: Dismiss
        val dismissIntent = Intent(this, FocusAlarmForegroundService::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(FocusAlarmManager.EXTRA_TASK_ID, taskId)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            (taskId + "_fg_dismiss").hashCode(),
            dismissIntent,
            flags
        )

        return NotificationCompat.Builder(this, FocusAlarmManager.CHANNEL_ALARM_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ حان وقت: $taskTitle")
            .setContentText(taskSubtitle.ifBlank { "انقر لبدء جلسة التركيز الآن" })
            .setStyle(NotificationCompat.BigTextStyle().bigText("${taskSubtitle.ifBlank { "حان موعد بدء المهمة" }}\n\nانقر للبدء أو اختر أحد الإجراءات أدناه."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(Color.parseColor("#0F6E60"))
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.mipmap.ic_launcher, "▶️ بدء التركيز", startTaskPendingIntent)
            .addAction(R.mipmap.ic_launcher, "⏳ تأجيل 10 د", snoozePendingIntent)
            .addAction(R.mipmap.ic_launcher, "✖️ إيقاف", dismissPendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopAlarmSoundAndVibration()
        releaseWakeLock()
        super.onDestroy()
        Log.d(TAG, "FocusAlarmForegroundService destroyed")
    }

    companion object {
        private const val TAG = "FocusAlarmService"
        const val ACTION_START_ALARM = "com.example.focuscraft.ACTION_START_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.focuscraft.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.focuscraft.ACTION_SNOOZE_ALARM"
        const val ACTION_START_TASK = "com.example.focuscraft.ACTION_START_TASK"

        fun stop(context: Context) {
            try {
                val intent = Intent(context, FocusAlarmForegroundService::class.java).apply {
                    action = ACTION_DISMISS_ALARM
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Error requesting service stop: ${e.message}")
            }
        }
    }
}
