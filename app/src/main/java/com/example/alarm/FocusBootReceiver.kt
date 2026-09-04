package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.FocusCraftDatabase
import com.example.data.repository.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * FocusBootReceiver ensures high-reliability survival across device reboots and app updates.
 * Listens for ACTION_BOOT_COMPLETED, ACTION_MY_PACKAGE_REPLACED, and QUICKBOOT_POWERON.
 */
class FocusBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "FocusBootReceiver received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            rescheduleAllActiveTasks(context)
        }
    }

    private fun rescheduleAllActiveTasks(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FocusCraftDatabase.getDatabase(context)
                val repository = FocusRepository(db.taskDao(), db.projectDao(), db.goalDao())
                val alarmManager = FocusAlarmManager(context)

                // Ensure notification channels exist after reboot
                alarmManager.createNotificationChannels()

                val allTasks = repository.allTasks.first()
                var scheduledCount = 0
                val now = System.currentTimeMillis()

                for (task in allTasks) {
                    if (!task.isCompleted && !task.alertType.equals("NONE", ignoreCase = true)) {
                        val triggerMillis = FocusAlarmManager.computeTriggerMillis(task)
                        if (triggerMillis != null && triggerMillis > now) {
                            alarmManager.scheduleTaskAlarm(task, triggerMillis)
                            scheduledCount++
                        }
                    }
                }
                Log.d(TAG, "Reboot recovery: successfully restored $scheduledCount alarms.")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring alarms in FocusBootReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "FocusBootReceiver"
    }
}
