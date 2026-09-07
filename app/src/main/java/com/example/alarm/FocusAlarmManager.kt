package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.Task
import java.util.Calendar

class FocusAlarmManager(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Initializes High Priority and Default Notification Channels for Alarms and Reminders
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val prefs = com.example.data.preferences.UserPreferencesManager(context)
            val alarmSoundUri = prefs.getEffectiveAlarmUri(context)
            val reminderSoundUri = prefs.getEffectiveNotificationUri(context)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val notifAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // 1. High-Priority Alarm Channel
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "منبهات المهام الذكية (Smart Alarms)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "منبهات تفاعلية وشاشات كاملة عند بدء المهام المجدولة"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300)
                setSound(alarmSoundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            // 2. Reminder Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER_ID,
                "تذكيرات المهام المسبقة (Reminders)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات تذكيرية قبل بدء المهمة بـ 15 دقيقة"
                enableVibration(true)
                setSound(reminderSoundUri, notifAudioAttributes)
                setShowBadge(true)
            }

            notificationManager?.createNotificationChannels(listOf(alarmChannel, reminderChannel))
        }
    }

    /**
     * Schedules alarms for a given task according to its alert type and scheduled start time.
     */
    fun scheduleTaskAlarm(task: Task, triggerAtMillis: Long? = null, isStartAlarm: Boolean = true) {
        if (task.isCompleted || task.alertType.equals("NONE", ignoreCase = true)) {
            cancelTaskAlarm(task.id)
            return
        }

        val targetStartMillis = triggerAtMillis ?: computeTriggerMillis(task)
        if (targetStartMillis == null || targetStartMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Task ${task.title} start time is in the past or invalid. Skipping alarm schedule.")
            return
        }

        // 1. Schedule exact start alarm
        scheduleStartAlarm(task, targetStartMillis)

        // 2. If alert type is SMART_ALARM or NOTIFICATION, schedule 15-minute prior reminder if applicable
        val reminderMillis = targetStartMillis - (15 * 60 * 1000L)
        if (reminderMillis > System.currentTimeMillis()) {
            scheduleReminderAlarm(task, reminderMillis)
        }
    }

    private fun scheduleStartAlarm(task: Task, triggerAtMillis: Long) {
        val intent = Intent(context, FocusAlarmReceiver::class.java).apply {
            action = ACTION_START_ALARM
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_SUBTITLE, task.subtitle)
            putExtra(EXTRA_TASK_TIME, task.scheduledTime)
            putExtra(EXTRA_ALERT_TYPE, task.alertType)
            putExtra(EXTRA_DURATION_MINUTES, task.durationMinutes)
        }

        val requestCode = task.id.hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        setAlarmExact(triggerAtMillis, pendingIntent)
        Log.d(TAG, "Scheduled START_ALARM for task '${task.title}' at $triggerAtMillis")
    }

    private fun scheduleReminderAlarm(task: Task, triggerAtMillis: Long) {
        val intent = Intent(context, FocusAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_NOTIFICATION
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_SUBTITLE, task.subtitle)
            putExtra(EXTRA_TASK_TIME, task.scheduledTime)
            putExtra(EXTRA_ALERT_TYPE, task.alertType)
        }

        val requestCode = (task.id + "_reminder").hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        setAlarmExact(triggerAtMillis, pendingIntent)
        Log.d(TAG, "Scheduled REMINDER for task '${task.title}' at $triggerAtMillis")
    }

    /**
     * Snoozes the alarm by specified minutes (default 10)
     */
    fun snoozeTaskAlarm(taskId: String, taskTitle: String = "المهمة المجدولة", taskSubtitle: String = "", snoozeMinutes: Int = 10) {
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val intent = Intent(context, FocusAlarmReceiver::class.java).apply {
            action = ACTION_START_ALARM
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_TASK_SUBTITLE, taskSubtitle.ifBlank { "تأجيل لمدة $snoozeMinutes دقائق" })
            putExtra(EXTRA_ALERT_TYPE, "SMART_ALARM")
        }

        val requestCode = taskId.hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        setAlarmExact(triggerAtMillis, pendingIntent)
        Log.d(TAG, "Snoozed alarm for task '$taskId' for $snoozeMinutes minutes")
    }

    /**
     * Cancels both start alarm and pre-reminder for the task
     */
    fun cancelTaskAlarm(taskId: String) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        // Cancel Start Alarm
        val startIntent = Intent(context, FocusAlarmReceiver::class.java).apply {
            action = ACTION_START_ALARM
        }
        val startPending = PendingIntent.getBroadcast(context, taskId.hashCode(), startIntent, flags or PendingIntent.FLAG_NO_CREATE)
        if (startPending != null) {
            alarmManager?.cancel(startPending)
            startPending.cancel()
        }

        // Cancel Reminder
        val reminderIntent = Intent(context, FocusAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_NOTIFICATION
        }
        val reminderPending = PendingIntent.getBroadcast(context, (taskId + "_reminder").hashCode(), reminderIntent, flags or PendingIntent.FLAG_NO_CREATE)
        if (reminderPending != null) {
            alarmManager?.cancel(reminderPending)
            reminderPending.cancel()
        }

        // Also dismiss any active notifications for this task
        notificationManager?.cancel(taskId.hashCode())
        FocusAlarmForegroundService.stop(context)
        FocusAlarmSoundManager.stopAlarm()
    }

    /**
     * Schedules background timer completion alarm to wake up system and fire completion notification & DB updates
     */
    fun scheduleTimerCompletion(taskId: String, taskTitle: String, plannedDurationMinutes: Int, completionMillis: Long) {
        val intent = Intent(context, FocusAlarmReceiver::class.java).apply {
            action = ACTION_TIMER_COMPLETED
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_DURATION_MINUTES, plannedDurationMinutes)
        }
        val requestCode = (taskId + "_timer_complete").hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        setAlarmExact(completionMillis, pendingIntent)
        Log.d(TAG, "Scheduled TIMER_COMPLETION for task '$taskTitle' at $completionMillis")
    }

    /**
     * Cancels any pending timer completion alarm
     */
    fun cancelTimerCompletion(taskId: String) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val intent = Intent(context, FocusAlarmReceiver::class.java).apply {
            action = ACTION_TIMER_COMPLETED
        }
        val requestCode = (taskId + "_timer_complete").hashCode()
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags or PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Snoozes an alarm by a specified number of minutes (default 10 minutes).
     */
    fun snoozeAlarm(taskId: String, taskTitle: String, taskSubtitle: String, snoozeMinutes: Int = 10) {
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val intent = Intent(context, FocusAlarmReceiver::class.java).apply {
            action = ACTION_START_ALARM
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_TASK_SUBTITLE, taskSubtitle)
            putExtra(EXTRA_TASK_TIME, "تأجيل $snoozeMinutes د")
            putExtra(EXTRA_ALERT_TYPE, "SMART_ALARM")
            putExtra(EXTRA_DURATION_MINUTES, 30)
        }
        val requestCode = (taskId + "_snooze").hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        setAlarmExact(triggerAtMillis, pendingIntent)
        Log.d(TAG, "Snoozed alarm for '$taskTitle' by $snoozeMinutes minutes")
    }

    /**
     * Triggers the local notification: "مبروك! لقد أنجزت مهمة [Task Title] 🎉"
     */
    fun showTimerCompletionNotification(taskId: String, taskTitle: String) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingContentIntent = PendingIntent.getActivity(context, taskId.hashCode() + 999, contentIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ALARM_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("إنجاز جلسة التركيز 🎉")
            .setContentText("مبروك! لقد أنجزت مهمة $taskTitle 🎉")
            .setStyle(NotificationCompat.BigTextStyle().bigText("مبروك! لقد أنجزت مهمة $taskTitle 🎉\nتم إكمال المهمة واحتساب وقت التركيز وتحديث التقدم بنجاح."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        notificationManager?.notify((taskId + "_completion").hashCode(), notification)
    }

    private fun setAlarmExact(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (alarmManager == null) return

        try {
            // Gold standard: setAlarmClock guarantees alarm firing in Doze / Silent mode
            val showIntent = Intent(context, com.example.ui.screens.SmartAlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                triggerAtMillis.hashCode(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            Log.w(TAG, "setAlarmClock not permitted or failed, falling back to setExactAndAllowWhileIdle: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to schedule alarm fallback", fallbackEx)
            }
        }
    }

    companion object {
        const val TAG = "FocusAlarmManager"
        const val CHANNEL_ALARM_ID = "focus_smart_alarm_channel"
        const val CHANNEL_REMINDER_ID = "focus_reminders_channel"

        const val ACTION_START_ALARM = "com.example.focuscraft.ACTION_START_ALARM"
        const val ACTION_REMINDER_NOTIFICATION = "com.example.focuscraft.ACTION_REMINDER_NOTIFICATION"
        const val ACTION_START_TASK = "com.example.focuscraft.ACTION_START_TASK"
        const val ACTION_SNOOZE_ALARM = "com.example.focuscraft.ACTION_SNOOZE_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.focuscraft.ACTION_DISMISS_ALARM"
        const val ACTION_TIMER_COMPLETED = "com.example.focuscraft.ACTION_TIMER_COMPLETED"

        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_TASK_SUBTITLE = "EXTRA_TASK_SUBTITLE"
        const val EXTRA_TASK_TIME = "EXTRA_TASK_TIME"
        const val EXTRA_ALERT_TYPE = "EXTRA_ALERT_TYPE"
        const val EXTRA_DURATION_MINUTES = "EXTRA_DURATION_MINUTES"
        const val EXTRA_TRIGGER_SMART_ALARM = "EXTRA_TRIGGER_SMART_ALARM"
        const val EXTRA_START_FOCUS_TASK_ID = "EXTRA_START_FOCUS_TASK_ID"

        /**
         * Parses task start hour & minute from startTime or scheduledTime and combines with task.date
         */
        fun computeTriggerMillis(task: Task): Long? {
            val (hour, minute) = parseTime(task.startTime, task.scheduledTime) ?: return null
            val cal = Calendar.getInstance().apply {
                timeInMillis = task.date
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        private fun parseTime(startTime: String?, scheduledTime: String): Pair<Int, Int>? {
            // 1. Try HH:mm 24-hr format first
            if (!startTime.isNullOrBlank()) {
                val parts = startTime.trim().split(":")
                if (parts.size >= 2) {
                    val h = parts[0].toIntOrNull()
                    val m = parts[1].toIntOrNull()
                    if (h != null && m != null && h in 0..23 && m in 0..59) {
                        return Pair(h, m)
                    }
                }
            }

            // 2. Parse from scheduledTime, e.g. "09:00 - 11:00 ص" or "02:00 م"
            if (scheduledTime.isNotBlank()) {
                val clean = scheduledTime.replace("–", "-")
                val firstPart = clean.split("-")[0].trim()
                val isPM = scheduledTime.contains("م") || scheduledTime.contains("PM", ignoreCase = true)
                val isAM = scheduledTime.contains("ص") || scheduledTime.contains("AM", ignoreCase = true)

                val digitsOnly = firstPart.filter { it.isDigit() || it == ':' }
                val timeParts = digitsOnly.split(":")
                if (timeParts.size >= 2) {
                    var h = timeParts[0].toIntOrNull() ?: return null
                    val m = timeParts[1].toIntOrNull() ?: 0
                    if (isPM && h < 12) h += 12
                    if (isAM && h == 12) h = 0
                    return Pair(h.coerceIn(0, 23), m.coerceIn(0, 59))
                }
            }
            return null
        }
    }
}
