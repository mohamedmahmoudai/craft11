package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.FocusCraftDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class FocusSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        val user = auth?.currentUser

        // In Guest Mode or offline unauthenticated, succeed immediately without crashing
        if (user == null) {
            Log.d(TAG, "No authenticated user. Skipping cloud sync (Offline-First Guest Mode).")
            return@withContext Result.success()
        }

        val uid = user.uid
        val firestore = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
        if (firestore == null) {
            Log.w(TAG, "FirebaseFirestore instance unavailable.")
            return@withContext Result.success()
        }

        try {
            val db = FocusCraftDatabase.getDatabase(applicationContext)

            // 1. Sync Tasks
            val tasks = db.taskDao().getAllTasks().first()
            for (task in tasks) {
                val taskData = hashMapOf(
                    "id" to task.id,
                    "title" to task.title,
                    "subtitle" to task.subtitle,
                    "durationMinutes" to task.durationMinutes,
                    "scheduledTime" to task.scheduledTime,
                    "isCompleted" to task.isCompleted,
                    "date" to task.date,
                    "categoryColor" to task.categoryColor,
                    "isFixed" to task.isFixed,
                    "startTime" to (task.startTime ?: ""),
                    "endTime" to (task.endTime ?: ""),
                    "deadline" to (task.deadline ?: 0L),
                    "actualDurationMinutes" to task.actualDurationMinutes,
                    "alertType" to task.alertType,
                    "needsRescheduling" to task.needsRescheduling,
                    "missedCount" to task.missedCount,
                    "postponedCount" to task.postponedCount,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid)
                    .collection("tasks").document(task.id)
                    .set(taskData, SetOptions.merge())
                    .await()
            }

            // 2. Sync Projects
            val projects = db.projectDao().getAllProjects().first()
            for (project in projects) {
                val projectData = hashMapOf(
                    "id" to project.id,
                    "name" to project.name,
                    "description" to project.description,
                    "color" to project.color,
                    "progress" to project.progress,
                    "status" to project.status,
                    "taskCount" to project.taskCount,
                    "completedTaskCount" to project.completedTaskCount,
                    "createdAt" to project.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid)
                    .collection("projects").document(project.id)
                    .set(projectData, SetOptions.merge())
                    .await()
            }

            // 3. Sync Goals
            val goals = db.goalDao().getAllGoals().first()
            for (goal in goals) {
                val goalData = hashMapOf(
                    "id" to goal.id,
                    "title" to goal.title,
                    "description" to goal.description,
                    "category" to goal.category,
                    "targetCount" to goal.targetCount,
                    "currentCount" to goal.currentCount,
                    "progress" to goal.progress,
                    "deadline" to goal.deadline,
                    "status" to goal.status,
                    "color" to goal.color,
                    "priority" to goal.priority,
                    "createdAt" to goal.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid)
                    .collection("goals").document(goal.id)
                    .set(goalData, SetOptions.merge())
                    .await()
            }

            // 4. Sync Settings / Profile Sync Metadata
            val metaData = hashMapOf(
                "lastSyncTimestamp" to System.currentTimeMillis(),
                "totalTasks" to tasks.size,
                "totalProjects" to projects.size,
                "totalGoals" to goals.size,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: "")
            )
            firestore.collection("users").document(uid)
                .collection("settings").document("preferences")
                .set(metaData, SetOptions.merge())
                .await()

            Log.d(TAG, "FocusSyncWorker successfully synced data to Firestore for user: $uid")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "FocusSyncWorker encountered an error during sync", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "FocusSyncWorker"
        private const val UNIQUE_PERIODIC_WORK_NAME = "focus_periodic_sync_worker"
        private const val UNIQUE_ONE_TIME_WORK_NAME = "focus_immediate_sync_worker"

        fun schedulePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = PeriodicWorkRequestBuilder<FocusSyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
                Log.d(TAG, "Periodic sync scheduled successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule periodic sync: ${e.message}")
            }
        }

        fun syncImmediately(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val immediateRequest = OneTimeWorkRequestBuilder<FocusSyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_ONE_TIME_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    immediateRequest
                )
                Log.d(TAG, "Immediate sync enqueued successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enqueue immediate sync: ${e.message}")
            }
        }
    }
}
