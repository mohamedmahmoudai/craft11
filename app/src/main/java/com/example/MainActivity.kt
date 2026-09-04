package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.alarm.FocusAlarmManager
import com.example.ui.FocusCraftApp
import com.example.viewmodel.FocusViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure notification channels exist on app launch
        FocusAlarmManager(this).createNotificationChannels()

        handleIntent(intent)

        setContent {
            FocusCraftApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val triggerAlarm = intent.getBooleanExtra(FocusAlarmManager.EXTRA_TRIGGER_SMART_ALARM, false)
        val taskId = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_ID)
            ?: intent.getStringExtra(FocusAlarmManager.EXTRA_START_FOCUS_TASK_ID)
        val title = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_TITLE)
        val subtitle = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE)

        if (triggerAlarm && !taskId.isNullOrBlank()) {
            viewModel.triggerSmartAlarm(taskId, title, subtitle)
        } else if (!taskId.isNullOrBlank() && intent.hasExtra(FocusAlarmManager.EXTRA_START_FOCUS_TASK_ID)) {
            viewModel.startFocusTimerById(taskId)
        }
    }
}


