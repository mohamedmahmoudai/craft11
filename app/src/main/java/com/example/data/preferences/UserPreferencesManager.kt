package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

data class SavedTimerInfo(
    val taskId: String,
    val taskTitle: String,
    val taskSubtitle: String,
    val plannedDurationMinutes: Int,
    val initialTotalSeconds: Int,
    val targetEndTimeMillis: Long,
    val startTimeMillis: Long,
    val isRunning: Boolean,
    val pausedRemainingSeconds: Int
)

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("adaptive_user_prefs", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userPhotoUrl: String?
        get() = prefs.getString(KEY_USER_PHOTO_URL, null)
        set(value) = prefs.edit().putString(KEY_USER_PHOTO_URL, value).apply()

    var userUid: String?
        get() = prefs.getString(KEY_USER_UID, null)
        set(value) = prefs.edit().putString(KEY_USER_UID, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var isGuestMode: Boolean
        get() = prefs.getBoolean(KEY_IS_GUEST_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_GUEST_MODE, value).apply()

    // Sleep Schedule & Work Window preferences
    var sleepBedtime: String
        get() = prefs.getString(KEY_SLEEP_BEDTIME, "23:00") ?: "23:00"
        set(value) = prefs.edit().putString(KEY_SLEEP_BEDTIME, value).apply()

    var sleepWakeTime: String
        get() = prefs.getString(KEY_SLEEP_WAKETIME, "07:00") ?: "07:00"
        set(value) = prefs.edit().putString(KEY_SLEEP_WAKETIME, value).apply()

    var workStartTime: String
        get() = prefs.getString(KEY_WORK_START_TIME, "09:00") ?: "09:00"
        set(value) = prefs.edit().putString(KEY_WORK_START_TIME, value).apply()

    var workEndTime: String
        get() = prefs.getString(KEY_WORK_END_TIME, "17:00") ?: "17:00"
        set(value) = prefs.edit().putString(KEY_WORK_END_TIME, value).apply()

    // Sound and Notification preferences
    var alarmSoundUri: String?
        get() = prefs.getString(KEY_ALARM_SOUND_URI, null)
        set(value) = prefs.edit().putString(KEY_ALARM_SOUND_URI, value).apply()

    var alarmSoundTitle: String
        get() = prefs.getString(KEY_ALARM_SOUND_TITLE, "نغمة التطبيق الافتراضية (Default)") ?: "نغمة التطبيق الافتراضية (Default)"
        set(value) = prefs.edit().putString(KEY_ALARM_SOUND_TITLE, value).apply()

    var notificationSoundUri: String?
        get() = prefs.getString(KEY_NOTIF_SOUND_URI, null)
        set(value) = prefs.edit().putString(KEY_NOTIF_SOUND_URI, value).apply()

    var notificationSoundTitle: String
        get() = prefs.getString(KEY_NOTIF_SOUND_TITLE, "نغمة الإشعار الافتراضية (Default)") ?: "نغمة الإشعار الافتراضية (Default)"
        set(value) = prefs.edit().putString(KEY_NOTIF_SOUND_TITLE, value).apply()

    fun getEffectiveAlarmUri(context: android.content.Context): android.net.Uri {
        val saved = alarmSoundUri
        return if (!saved.isNullOrBlank()) {
            android.net.Uri.parse(saved)
        } else {
            android.net.Uri.parse("android.resource://${context.packageName}/${com.example.R.raw.default_alarm}")
        }
    }

    fun getEffectiveNotificationUri(context: android.content.Context): android.net.Uri {
        val saved = notificationSoundUri
        return if (!saved.isNullOrBlank()) {
            android.net.Uri.parse(saved)
        } else {
            android.net.Uri.parse("android.resource://${context.packageName}/${com.example.R.raw.default_notification}")
        }
    }

    fun saveUserSession(uid: String, name: String, email: String?, photoUrl: String?) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_IS_GUEST_MODE, false)
            .putString(KEY_USER_UID, uid)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PHOTO_URL, photoUrl)
            .apply()
    }

    fun setGuestSession(guestName: String = "") {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_IS_GUEST_MODE, true)
            .putString(KEY_USER_NAME, guestName)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHOTO_URL)
            .remove(KEY_USER_UID)
            .apply()
    }

    fun clearSession() {
        setGuestSession("")
    }

    // Active Focus Timer Persistence
    fun saveActiveTimer(
        taskId: String,
        taskTitle: String,
        taskSubtitle: String,
        plannedDurationMinutes: Int,
        initialTotalSeconds: Int,
        targetEndTimeMillis: Long,
        startTimeMillis: Long,
        isRunning: Boolean,
        pausedRemainingSeconds: Int = initialTotalSeconds
    ) {
        prefs.edit()
            .putString(KEY_TIMER_TASK_ID, taskId)
            .putString(KEY_TIMER_TASK_TITLE, taskTitle)
            .putString(KEY_TIMER_TASK_SUBTITLE, taskSubtitle)
            .putInt(KEY_TIMER_PLANNED_MINUTES, plannedDurationMinutes)
            .putInt(KEY_TIMER_INITIAL_TOTAL_SECS, initialTotalSeconds)
            .putLong(KEY_TIMER_TARGET_END_MILLIS, targetEndTimeMillis)
            .putLong(KEY_TIMER_START_MILLIS, startTimeMillis)
            .putBoolean(KEY_TIMER_IS_RUNNING, isRunning)
            .putInt(KEY_TIMER_PAUSED_REMAINING_SECS, pausedRemainingSeconds)
            .apply()
    }

    fun savePausedTimer(remainingSeconds: Int) {
        prefs.edit()
            .putBoolean(KEY_TIMER_IS_RUNNING, false)
            .putInt(KEY_TIMER_PAUSED_REMAINING_SECS, remainingSeconds)
            .apply()
    }

    fun clearActiveTimer() {
        prefs.edit()
            .remove(KEY_TIMER_TASK_ID)
            .remove(KEY_TIMER_TASK_TITLE)
            .remove(KEY_TIMER_TASK_SUBTITLE)
            .remove(KEY_TIMER_PLANNED_MINUTES)
            .remove(KEY_TIMER_INITIAL_TOTAL_SECS)
            .remove(KEY_TIMER_TARGET_END_MILLIS)
            .remove(KEY_TIMER_START_MILLIS)
            .remove(KEY_TIMER_IS_RUNNING)
            .remove(KEY_TIMER_PAUSED_REMAINING_SECS)
            .apply()
    }

    fun getSavedTimer(): SavedTimerInfo? {
        val taskId = prefs.getString(KEY_TIMER_TASK_ID, null) ?: return null
        val taskTitle = prefs.getString(KEY_TIMER_TASK_TITLE, "") ?: ""
        val taskSubtitle = prefs.getString(KEY_TIMER_TASK_SUBTITLE, "") ?: ""
        val plannedMinutes = prefs.getInt(KEY_TIMER_PLANNED_MINUTES, 25)
        val initialSecs = prefs.getInt(KEY_TIMER_INITIAL_TOTAL_SECS, plannedMinutes * 60)
        val targetEndMillis = prefs.getLong(KEY_TIMER_TARGET_END_MILLIS, 0L)
        val startMillis = prefs.getLong(KEY_TIMER_START_MILLIS, 0L)
        val isRunning = prefs.getBoolean(KEY_TIMER_IS_RUNNING, false)
        val pausedRemainingSeconds = prefs.getInt(KEY_TIMER_PAUSED_REMAINING_SECS, initialSecs)
        return SavedTimerInfo(
            taskId = taskId,
            taskTitle = taskTitle,
            taskSubtitle = taskSubtitle,
            plannedDurationMinutes = plannedMinutes,
            initialTotalSeconds = initialSecs,
            targetEndTimeMillis = targetEndMillis,
            startTimeMillis = startMillis,
            isRunning = isRunning,
            pausedRemainingSeconds = pausedRemainingSeconds
        )
    }

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHOTO_URL = "user_photo_url"
        private const val KEY_USER_UID = "user_uid"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_GUEST_MODE = "is_guest_mode"

        private const val KEY_SLEEP_BEDTIME = "sleep_bedtime"
        private const val KEY_SLEEP_WAKETIME = "sleep_waketime"
        private const val KEY_WORK_START_TIME = "work_start_time"
        private const val KEY_WORK_END_TIME = "work_end_time"

        private const val KEY_ALARM_SOUND_URI = "alarm_sound_uri"
        private const val KEY_ALARM_SOUND_TITLE = "alarm_sound_title"
        private const val KEY_NOTIF_SOUND_URI = "notif_sound_uri"
        private const val KEY_NOTIF_SOUND_TITLE = "notif_sound_title"

        private const val KEY_TIMER_TASK_ID = "timer_task_id"
        private const val KEY_TIMER_TASK_TITLE = "timer_task_title"
        private const val KEY_TIMER_TASK_SUBTITLE = "timer_task_subtitle"
        private const val KEY_TIMER_PLANNED_MINUTES = "timer_planned_minutes"
        private const val KEY_TIMER_INITIAL_TOTAL_SECS = "timer_initial_total_secs"
        private const val KEY_TIMER_TARGET_END_MILLIS = "timer_target_end_millis"
        private const val KEY_TIMER_START_MILLIS = "timer_start_millis"
        private const val KEY_TIMER_IS_RUNNING = "timer_is_running"
        private const val KEY_TIMER_PAUSED_REMAINING_SECS = "timer_paused_remaining_secs"
    }
}

