package com.example.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.FocusCraftDatabase
import com.example.data.repository.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "FocusAlarmReceiver received action: $action")

        val taskId = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_ID) ?: "default_task"
        val taskTitle = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_TITLE) ?: "مهمة مجدولة"
        val taskSubtitle = intent.getStringExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE) ?: ""
        val alertType = intent.getStringExtra(FocusAlarmManager.EXTRA_ALERT_TYPE) ?: "SMART_ALARM"
        val notificationId = taskId.hashCode()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val alarmManager = FocusAlarmManager(context)

        when (action) {
            FocusAlarmManager.ACTION_START_ALARM -> {
                // Exact start time reached
                handleStartAlarm(context, notificationManager, taskId, taskTitle, taskSubtitle, alertType, notificationId)
            }

            FocusAlarmManager.ACTION_REMINDER_NOTIFICATION -> {
                // 15-minute prior reminder
                handleReminderNotification(context, notificationManager, taskId, taskTitle, taskSubtitle, notificationId)
            }

            FocusAlarmManager.ACTION_START_TASK -> {
                // User pressed "بدأت المهمة" from notification
                notificationManager?.cancel(notificationId)
                FocusAlarmForegroundService.stop(context)
                FocusAlarmSoundManager.stopAlarm()

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(FocusAlarmManager.EXTRA_START_FOCUS_TASK_ID, taskId)
                    putExtra(FocusAlarmManager.EXTRA_TASK_TITLE, taskTitle)
                    putExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE, taskSubtitle)
                }
                context.startActivity(mainIntent)
            }

            FocusAlarmManager.ACTION_SNOOZE_ALARM -> {
                // User pressed "تأجيل 10 د" from notification
                notificationManager?.cancel(notificationId)
                FocusAlarmForegroundService.stop(context)
                FocusAlarmSoundManager.stopAlarm()
                alarmManager.snoozeTaskAlarm(taskId, taskTitle, taskSubtitle, 10)
            }

            FocusAlarmManager.ACTION_DISMISS_ALARM -> {
                notificationManager?.cancel(notificationId)
                FocusAlarmForegroundService.stop(context)
                FocusAlarmSoundManager.stopAlarm()
            }

            FocusAlarmManager.ACTION_TIMER_COMPLETED -> {
                handleTimerCompleted(context, notificationManager, taskId, taskTitle)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                // Device rebooted -> Reschedule all active uncompleted tasks
                rescheduleAllAlarms(context)
            }
        }
    }

    private fun handleStartAlarm(
        context: Context,
        notificationManager: NotificationManager?,
        taskId: String,
        taskTitle: String,
        taskSubtitle: String,
        alertType: String,
        notificationId: Int
    ) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        // 1. Full-screen / content Intent opening MainActivity in Smart Alarm mode
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(FocusAlarmManager.EXTRA_TRIGGER_SMART_ALARM, true)
            putExtra(FocusAlarmManager.EXTRA_TASK_ID, taskId)
            putExtra(FocusAlarmManager.EXTRA_TASK_TITLE, taskTitle)
            putExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE, taskSubtitle)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            fullScreenIntent,
            flags
        )

        // 2. Action: Start Task ("بدأت المهمة")
        val startTaskIntent = Intent(context, FocusAlarmReceiver::class.java).apply {
            action = FocusAlarmManager.ACTION_START_TASK
            putExtra(FocusAlarmManager.EXTRA_TASK_ID, taskId)
            putExtra(FocusAlarmManager.EXTRA_TASK_TITLE, taskTitle)
            putExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE, taskSubtitle)
        }
        val startTaskPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + "_start_act").hashCode(),
            startTaskIntent,
            flags
        )

        // 3. Action: Snooze 10 mins ("تأجيل 10 د")
        val snoozeIntent = Intent(context, FocusAlarmReceiver::class.java).apply {
            action = FocusAlarmManager.ACTION_SNOOZE_ALARM
            putExtra(FocusAlarmManager.EXTRA_TASK_ID, taskId)
            putExtra(FocusAlarmManager.EXTRA_TASK_TITLE, taskTitle)
            putExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE, taskSubtitle)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + "_snooze_act").hashCode(),
            snoozeIntent,
            flags
        )

        val builder = NotificationCompat.Builder(context, FocusAlarmManager.CHANNEL_ALARM_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ حان وقت: $taskTitle")
            .setContentText(taskSubtitle.ifBlank { "انقر لبدء جلسة التركيز الآن" })
            .setStyle(NotificationCompat.BigTextStyle().bigText("${taskSubtitle.ifBlank { "حان موعد بدء المهمة" }}\n\nانقر للبدء أو اضغط على أحد الإجراءات السريعة أدناه."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(Color.parseColor("#0F6E60"))
            .setAutoCancel(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.mipmap.ic_launcher, "▶️ بدأت المهمة", startTaskPendingIntent)
            .addAction(R.mipmap.ic_launcher, "⏳ تأجيل 10 د", snoozePendingIntent)

        val isFullSmartAlarm = !alertType.equals("NOTIFICATION", ignoreCase = true)

        if (isFullSmartAlarm) {
            try {
                val serviceIntent = Intent(context, FocusAlarmForegroundService::class.java).apply {
                    action = FocusAlarmForegroundService.ACTION_START_ALARM
                    putExtra(FocusAlarmManager.EXTRA_TASK_ID, taskId)
                    putExtra(FocusAlarmManager.EXTRA_TASK_TITLE, taskTitle)
                    putExtra(FocusAlarmManager.EXTRA_TASK_SUBTITLE, taskSubtitle)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start FocusAlarmForegroundService, falling back to notification", e)
                notificationManager?.notify(notificationId, builder.build())
                FocusAlarmSoundManager.startAlarm(context)
            }
        } else {
            notificationManager?.notify(notificationId, builder.build())
        }
    }

    private fun handleReminderNotification(
        context: Context,
        notificationManager: NotificationManager?,
        taskId: String,
        taskTitle: String,
        taskSubtitle: String,
        notificationId: Int
    ) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(FocusAlarmManager.EXTRA_START_FOCUS_TASK_ID, taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 100,
            openIntent,
            flags
        )

        val builder = NotificationCompat.Builder(context, FocusAlarmManager.CHANNEL_REMINDER_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 تذكير: $taskTitle")
            .setContentText("ستبدأ المهمة بعد 15 دقيقة (${taskSubtitle})")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(Color.parseColor("#4338CA"))
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        notificationManager?.notify(notificationId + 100, builder.build())
    }

    private fun handleTimerCompleted(
        context: Context,
        notificationManager: NotificationManager?,
        taskId: String,
        taskTitle: String
    ) {
        val alarmManager = FocusAlarmManager(context)
        alarmManager.showTimerCompletionNotification(taskId, taskTitle)

        // Clear active timer in UserPreferencesManager
        val prefs = com.example.data.preferences.UserPreferencesManager(context)
        prefs.clearActiveTimer()

        // Automatically mark the task status as COMPLETED, log actual spent time, and update project/goal
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FocusCraftDatabase.getDatabase(context)
                val repository = FocusRepository(db.taskDao(), db.projectDao(), db.goalDao())
                val task = db.taskDao().findTaskById(taskId)
                val minutesSpent = task?.durationMinutes?.coerceAtLeast(1) ?: 25
                repository.completeFocusSession(
                    taskId = taskId,
                    minutesSpent = minutesSpent,
                    markCompleted = true
                )
                Log.d(TAG, "Successfully completed task '$taskTitle' upon timer finish")
            } catch (e: Exception) {
                Log.e(TAG, "Error completing task on timer finish: ${e.message}")
            }
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FocusCraftDatabase.getDatabase(context)
                val repository = FocusRepository(db.taskDao(), db.projectDao(), db.goalDao())
                val allTasks = repository.allTasks.first()
                val alarmManager = FocusAlarmManager(context)

                for (task in allTasks) {
                    if (!task.isCompleted && !task.alertType.equals("NONE", ignoreCase = true)) {
                        alarmManager.scheduleTaskAlarm(task)
                    }
                }
                Log.d(TAG, "Successfully rescheduled ${allTasks.size} tasks on boot.")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms on boot", e)
            }
        }
    }

    companion object {
        private const val TAG = "FocusAlarmReceiver"
    }
}
