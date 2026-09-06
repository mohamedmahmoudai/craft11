package com.example.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.MainActivity
import com.example.alarm.FocusAlarmForegroundService
import com.example.alarm.FocusAlarmManager
import com.example.alarm.FocusAlarmSoundManager
import com.example.ui.theme.WhackaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmartAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on and show above lockscreen strictly for this alarm screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val taskId = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_ID) ?: ""
        val taskTitle = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_TITLE) ?: "جلسة تركيز جديدة"
        val taskSubtitle = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE) ?: "حان موعد بدء المهمة المجدولة"
        val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        setContent {
            WhackaTheme(darkTheme = true) {
                SmartAlarmScreen(
                    timeText = timeFormatted,
                    taskTitle = taskTitle,
                    taskSubtitle = taskSubtitle,
                    onStartTask = {
                        FocusAlarmForegroundService.stop(this@SmartAlarmActivity)
                        FocusAlarmSoundManager.stopAlarm()
                        val mainIntent = Intent(this@SmartAlarmActivity, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(FocusAlarmManager.EXTRA_START_FOCUS_TASK_ID, taskId)
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onSnooze = {
                        FocusAlarmForegroundService.stop(this@SmartAlarmActivity)
                        FocusAlarmSoundManager.stopAlarm()
                        FocusAlarmManager(this@SmartAlarmActivity).snoozeAlarm(taskId, taskTitle, taskSubtitle, 10)
                        finish()
                    },
                    onOpenTask = {
                        FocusAlarmForegroundService.stop(this@SmartAlarmActivity)
                        FocusAlarmSoundManager.stopAlarm()
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FocusAlarmSoundManager.stopAlarm()
    }
}
