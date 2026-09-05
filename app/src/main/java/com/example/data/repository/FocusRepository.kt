package com.example.data.repository

import android.util.Log
import com.example.data.local.dao.GoalDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.Goal
import com.example.data.local.entity.Project
import com.example.data.local.entity.Task
import com.example.engine.CapacityExceededException
import com.example.engine.SchedulingEngine
import com.example.model.DailyCapacityState
import com.example.model.GoalFilterTab
import com.example.model.ProjectFilterTab
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class FocusRepository(
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val goalDao: GoalDao
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val completedTasks: Flow<List<Task>> = taskDao.getCompletedTasks()
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()

    fun getTodayTasks(): Flow<List<Task>> {
        val (start, end) = getTodayTimeBounds()
        return taskDao.getTodayTasks(start, end)
    }

    fun getUpcomingTasks(): Flow<List<Task>> {
        val (start, end) = getTodayTimeBounds()
        return taskDao.getUpcomingTasks(start, end)
    }

    val currentFocusTask: Flow<Task?> = allTasks.map { tasks ->
        tasks.firstOrNull { !it.isCompleted } ?: tasks.firstOrNull()
    }

    fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<Task>> {
        return taskDao.getTasksInDateRange(startDate, endDate)
    }

    fun getRepeatedlyPostponedTasks(minCount: Int = 3): Flow<List<Task>> {
        return taskDao.getRepeatedlyPostponedTasks(minCount)
    }

    val todayCapacityState: Flow<DailyCapacityState> = getTodayTasks().map { tasks ->
        SchedulingEngine.calculateDailyCapacity(tasks)
    }

    // ==========================================
    // Task Operations
    // ==========================================

    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
        syncTaskToCloud(task)
    }

    @Throws(CapacityExceededException::class)
    suspend fun addTaskWithCapacityCheck(task: Task) {
        val (start, end) = getTodayTimeBounds()
        val isToday = task.date in start..end
        if (isToday) {
            val todayTasksList = taskDao.getTodayTasks(start, end).first()
            SchedulingEngine.validateScheduleCapacity(todayTasksList, task.durationMinutes)
        }
        taskDao.insertTask(task)
        syncTaskToCloud(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
        syncTaskToCloud(task)
    }

    suspend fun deleteTask(taskId: String) {
        taskDao.deleteTaskById(taskId)
        deleteTaskFromCloud(taskId)
    }

    suspend fun toggleTaskCompletion(taskId: String) {
        val task = taskDao.findTaskById(taskId) ?: return
        val newCompleted = !task.isCompleted
        val newSubtitle = if (newCompleted) {
            "تم الإنجاز - ${task.scheduledTime}"
        } else {
            task.subtitle
        }
        taskDao.updateCompletionStatus(
            id = taskId,
            completed = newCompleted,
            subtitle = newSubtitle
        )
        val updated = task.copy(isCompleted = newCompleted, subtitle = newSubtitle)
        syncTaskToCloud(updated)
    }

    suspend fun addActualTime(taskId: String, minutesSpent: Int, timestamp: Long = System.currentTimeMillis()) {
        if (minutesSpent <= 0) return
        taskDao.addActualTime(taskId, minutesSpent, timestamp)
        val task = taskDao.findTaskById(taskId)
        if (task != null) syncTaskToCloud(task)
    }

    suspend fun updateNeedsRescheduling(taskId: String, needsRescheduling: Boolean) {
        taskDao.updateNeedsRescheduling(taskId, needsRescheduling)
    }

    suspend fun markTaskMissed(taskId: String) {
        taskDao.markTaskMissed(taskId)
    }

    suspend fun postponeTaskToTomorrow(taskId: String, newStartTime: String = "09:00", newEndTime: String = "10:30") {
        val tomorrowCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrowMillis = tomorrowCal.timeInMillis
        val scheduledTimeStr = "$newStartTime - $newEndTime"
        taskDao.postponeTask(
            id = taskId,
            newDate = tomorrowMillis,
            newStartTime = newStartTime,
            newEndTime = newEndTime,
            newScheduledTime = scheduledTimeStr
        )
    }

    suspend fun moveToLater(taskId: String) {
        val task = taskDao.findTaskById(taskId) ?: return
        val updated = task.copy(
            scheduledTime = "لاحقاً",
            startTime = null,
            endTime = null,
            needsRescheduling = false,
            postponedCount = task.postponedCount + 1
        )
        taskDao.updateTask(updated)
        syncTaskToCloud(updated)
    }

    suspend fun updateTaskSchedule(
        taskId: String,
        newDate: Long,
        newStartTime: String?,
        newEndTime: String?,
        newScheduledTime: String,
        newDurationMinutes: Int
    ) {
        taskDao.updateTaskSchedule(
            id = taskId,
            newDate = newDate,
            newStartTime = newStartTime,
            newEndTime = newEndTime,
            newScheduledTime = newScheduledTime,
            newDurationMinutes = newDurationMinutes
        )
    }

    suspend fun completeFocusSession(
        taskId: String,
        minutesSpent: Int,
        markCompleted: Boolean = true,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val task = taskDao.findTaskById(taskId) ?: return
        val newSubtitle = if (markCompleted) {
            "تم الإنجاز - ${task.scheduledTime}"
        } else {
            task.subtitle
        }
        taskDao.completeFocusSession(
            id = taskId,
            minutesSpent = minutesSpent.coerceAtLeast(0),
            isCompleted = markCompleted,
            subtitle = newSubtitle,
            timestamp = timestamp
        )
        val updated = task.copy(
            isCompleted = markCompleted,
            subtitle = newSubtitle,
            actualDurationMinutes = task.actualDurationMinutes + minutesSpent
        )
        syncTaskToCloud(updated)

        // Real-time linkage: Update associated Project progress and Goal progress
        try {
            val targetProjectId = task.projectId
            if (!targetProjectId.isNullOrBlank()) {
                val project = projectDao.findProjectById(targetProjectId)
                if (project != null) {
                    val allTasks = taskDao.getAllTasks().first().filter { it.projectId == targetProjectId }
                    val totalTasks = allTasks.size.coerceAtLeast(project.taskCount).coerceAtLeast(1)
                    val completedTasks = allTasks.count { it.isCompleted || (it.id == taskId && markCompleted) }
                    val newProgress = (completedTasks.toFloat() / totalTasks.toFloat()).coerceIn(0f, 1f)
                    val updatedProject = project.copy(
                        completedTaskCount = completedTasks,
                        taskCount = totalTasks,
                        progress = newProgress
                    )
                    projectDao.updateProject(updatedProject)
                    syncProjectToCloud(updatedProject)

                    val targetGoalId = project.goalId
                    if (!targetGoalId.isNullOrBlank()) {
                        val goal = goalDao.findGoalById(targetGoalId)
                        if (goal != null) {
                            val newGoalCount = (goal.currentCount + (if (markCompleted) 1 else 0)).coerceAtLeast(0)
                            val goalTarget = goal.targetCount.coerceAtLeast(1)
                            val newGoalProgress = (newGoalCount.toFloat() / goalTarget.toFloat()).coerceIn(0f, 1f)
                            val updatedGoal = goal.copy(
                                currentCount = newGoalCount,
                                progress = newGoalProgress
                            )
                            goalDao.updateGoal(updatedGoal)
                            syncGoalToCloud(updatedGoal)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update linked project/goal: ${e.message}")
        }
    }

    suspend fun getTaskById(taskId: String): Task? {
        return taskDao.findTaskById(taskId)
    }

    // ==========================================
    // Project Operations
    // ==========================================

    suspend fun insertProject(project: Project) {
        projectDao.insertProject(project)
        syncProjectToCloud(project)
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
        syncProjectToCloud(project)
    }

    suspend fun deleteProject(projectId: String) {
        projectDao.deleteProjectById(projectId)
        deleteProjectFromCloud(projectId)
    }

    // ==========================================
    // Goal Operations
    // ==========================================

    suspend fun insertGoal(goal: Goal) {
        goalDao.insertGoal(goal)
        syncGoalToCloud(goal)
    }

    suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal)
        syncGoalToCloud(goal)
    }

    suspend fun deleteGoal(goalId: String) {
        goalDao.deleteGoalById(goalId)
        deleteGoalFromCloud(goalId)
    }

    suspend fun incrementGoalProgress(goalId: String, amount: Int = 1) {
        val goal = goalDao.findGoalById(goalId) ?: return
        val newCount = (goal.currentCount + amount).coerceAtLeast(0)
        val newProgress = if (goal.targetCount > 0) {
            (newCount.toFloat() / goal.targetCount.toFloat()).coerceIn(0f, 1f)
        } else 1f
        val newStatus = if (newProgress >= 1f) "COMPLETED" else goal.status
        val updated = goal.copy(currentCount = newCount, progress = newProgress, status = newStatus)
        goalDao.updateGoal(updated)
        syncGoalToCloud(updated)
    }

    suspend fun updateGoalStatus(goalId: String, status: GoalFilterTab) {
        val statusStr = when (status) {
            GoalFilterTab.ACTIVE -> "ACTIVE"
            GoalFilterTab.PAUSED -> "PAUSED"
            GoalFilterTab.COMPLETED -> "COMPLETED"
            GoalFilterTab.ARCHIVED -> "ARCHIVED"
        }
        goalDao.updateGoalStatus(goalId, statusStr)
    }

    suspend fun updateProjectStatus(projectId: String, status: ProjectFilterTab) {
        val project = projectDao.getAllProjects().first().find { it.id == projectId } ?: return
        val statusStr = when (status) {
            ProjectFilterTab.ACTIVE -> "ACTIVE"
            ProjectFilterTab.PAUSED -> "PAUSED"
            ProjectFilterTab.COMPLETED -> "COMPLETED"
            ProjectFilterTab.ARCHIVED -> "ARCHIVED"
        }
        val updated = project.copy(status = statusStr)
        projectDao.updateProject(updated)
        syncProjectToCloud(updated)
    }

    // ==========================================
    // Firebase Cloud Sync Engine (Realtime + Bidirectional)
    // ==========================================

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }

    private var taskListenerRegistration: ListenerRegistration? = null
    private var projectListenerRegistration: ListenerRegistration? = null
    private var goalListenerRegistration: ListenerRegistration? = null

    fun startRealtimeCloudSync(uid: String) {
        stopRealtimeCloudSync()
        val db = firestore ?: return

        try {
            // Realtime listener for Tasks
            taskListenerRegistration = db.collection("users").document(uid).collection("tasks")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("FocusRepository", "Tasks listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val remoteTasks = snapshot.documents.mapNotNull { doc ->
                                try {
                                    Task(
                                        id = doc.getString("id") ?: doc.id,
                                        title = doc.getString("title") ?: "",
                                        subtitle = doc.getString("subtitle") ?: "",
                                        durationMinutes = (doc.getLong("durationMinutes") ?: 90L).toInt(),
                                        scheduledTime = doc.getString("scheduledTime") ?: "",
                                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                                        date = doc.getLong("date") ?: System.currentTimeMillis(),
                                        categoryColor = doc.getString("categoryColor") ?: "TEAL",
                                        isFixed = doc.getBoolean("isFixed") ?: false,
                                        startTime = doc.getString("startTime"),
                                        endTime = doc.getString("endTime"),
                                        alertType = doc.getString("alertType") ?: "SMART_ALARM",
                                        postponedCount = (doc.getLong("postponedCount") ?: 0L).toInt(),
                                        needsRescheduling = doc.getBoolean("needsRescheduling") ?: false,
                                        actualDurationMinutes = (doc.getLong("actualDurationMinutes") ?: 0L).toInt()
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (remoteTasks.isNotEmpty()) {
                                taskDao.insertTasks(remoteTasks)
                            }
                        }
                    }
                }

            // Realtime listener for Projects
            projectListenerRegistration = db.collection("users").document(uid).collection("projects")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("FocusRepository", "Projects listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val remoteProjects = snapshot.documents.mapNotNull { doc ->
                                try {
                                    Project(
                                        id = doc.getString("id") ?: doc.id,
                                        name = doc.getString("name") ?: "",
                                        description = doc.getString("description") ?: "",
                                        color = doc.getString("color") ?: "TEAL",
                                        progress = (doc.getDouble("progress") ?: 0.0).toFloat(),
                                        status = doc.getString("status") ?: "ACTIVE",
                                        taskCount = (doc.getLong("taskCount") ?: 0L).toInt(),
                                        completedTaskCount = (doc.getLong("completedTaskCount") ?: 0L).toInt(),
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (remoteProjects.isNotEmpty()) {
                                projectDao.insertProjects(remoteProjects)
                            }
                        }
                    }
                }

            // Realtime listener for Goals
            goalListenerRegistration = db.collection("users").document(uid).collection("goals")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("FocusRepository", "Goals listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val remoteGoals = snapshot.documents.mapNotNull { doc ->
                                try {
                                    Goal(
                                        id = doc.getString("id") ?: doc.id,
                                        title = doc.getString("title") ?: "",
                                        description = doc.getString("description") ?: "",
                                        category = doc.getString("category") ?: "عام",
                                        targetCount = (doc.getLong("targetCount") ?: 10L).toInt(),
                                        currentCount = (doc.getLong("currentCount") ?: 0L).toInt(),
                                        progress = (doc.getDouble("progress") ?: 0.0).toFloat(),
                                        deadline = doc.getString("deadline") ?: "نهاية الشهر",
                                        status = doc.getString("status") ?: "ACTIVE",
                                        color = doc.getString("color") ?: "TEAL",
                                        priority = doc.getString("priority") ?: "متوسطة",
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (remoteGoals.isNotEmpty()) {
                                goalDao.insertGoals(remoteGoals)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("FocusRepository", "Could not attach Firestore listeners: ${e.message}")
        }
    }

    fun stopRealtimeCloudSync() {
        try {
            taskListenerRegistration?.remove()
            projectListenerRegistration?.remove()
            goalListenerRegistration?.remove()
        } catch (e: Exception) {
            // Ignored
        } finally {
            taskListenerRegistration = null
            projectListenerRegistration = null
            goalListenerRegistration = null
        }
    }

    suspend fun downloadFromCloud(uid: String) {
        val db = firestore ?: return
        try {
            val tasksSnap = db.collection("users").document(uid).collection("tasks").get().await()
            val tasks = tasksSnap.documents.mapNotNull { doc ->
                try {
                    Task(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "",
                        subtitle = doc.getString("subtitle") ?: "",
                        durationMinutes = (doc.getLong("durationMinutes") ?: 90L).toInt(),
                        scheduledTime = doc.getString("scheduledTime") ?: "",
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        date = doc.getLong("date") ?: System.currentTimeMillis(),
                        categoryColor = doc.getString("categoryColor") ?: "TEAL",
                        isFixed = doc.getBoolean("isFixed") ?: false,
                        startTime = doc.getString("startTime"),
                        endTime = doc.getString("endTime"),
                        alertType = doc.getString("alertType") ?: "SMART_ALARM",
                        postponedCount = (doc.getLong("postponedCount") ?: 0L).toInt(),
                        needsRescheduling = doc.getBoolean("needsRescheduling") ?: false,
                        actualDurationMinutes = (doc.getLong("actualDurationMinutes") ?: 0L).toInt()
                    )
                } catch (e: Exception) { null }
            }
            if (tasks.isNotEmpty()) taskDao.insertTasks(tasks)

            val projectsSnap = db.collection("users").document(uid).collection("projects").get().await()
            val projects = projectsSnap.documents.mapNotNull { doc ->
                try {
                    Project(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        color = doc.getString("color") ?: "TEAL",
                        progress = (doc.getDouble("progress") ?: 0.0).toFloat(),
                        status = doc.getString("status") ?: "ACTIVE",
                        taskCount = (doc.getLong("taskCount") ?: 0L).toInt(),
                        completedTaskCount = (doc.getLong("completedTaskCount") ?: 0L).toInt(),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) { null }
            }
            if (projects.isNotEmpty()) projectDao.insertProjects(projects)

            val goalsSnap = db.collection("users").document(uid).collection("goals").get().await()
            val goals = goalsSnap.documents.mapNotNull { doc ->
                try {
                    Goal(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "عام",
                        targetCount = (doc.getLong("targetCount") ?: 10L).toInt(),
                        currentCount = (doc.getLong("currentCount") ?: 0L).toInt(),
                        progress = (doc.getDouble("progress") ?: 0.0).toFloat(),
                        deadline = doc.getString("deadline") ?: "نهاية الشهر",
                        status = doc.getString("status") ?: "ACTIVE",
                        color = doc.getString("color") ?: "TEAL",
                        priority = doc.getString("priority") ?: "متوسطة",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) { null }
            }
            if (goals.isNotEmpty()) goalDao.insertGoals(goals)
        } catch (e: Exception) {
            Log.w("FocusRepository", "Firestore download failed / offline: ${e.message}")
        }
    }

    fun syncAllToCloud() {
        val uid = auth?.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = taskDao.getAllTasks().first()
                val projects = projectDao.getAllProjects().first()
                val goals = goalDao.getAllGoals().first()

                tasks.forEach { syncTaskToCloud(it, uid) }
                projects.forEach { syncProjectToCloud(it, uid) }
                goals.forEach { syncGoalToCloud(it, uid) }
            } catch (e: Exception) {
                Log.w("FocusRepository", "Cloud sync ignored/offline: ${e.message}")
            }
        }
    }

    private fun syncTaskToCloud(task: Task, specificUid: String? = null) {
        val uid = specificUid ?: auth?.currentUser?.uid ?: return
        try {
            val taskMap = hashMapOf(
                "id" to task.id,
                "title" to task.title,
                "subtitle" to task.subtitle,
                "durationMinutes" to task.durationMinutes,
                "scheduledTime" to task.scheduledTime,
                "isCompleted" to task.isCompleted,
                "date" to task.date,
                "categoryColor" to task.categoryColor,
                "isFixed" to task.isFixed,
                "startTime" to task.startTime,
                "endTime" to task.endTime,
                "alertType" to task.alertType,
                "postponedCount" to task.postponedCount,
                "needsRescheduling" to task.needsRescheduling,
                "actualDurationMinutes" to task.actualDurationMinutes
            )
            firestore?.collection("users")?.document(uid)
                ?.collection("tasks")?.document(task.id)
                ?.set(taskMap, SetOptions.merge())

            if (task.isFixed) {
                firestore?.collection("users")?.document(uid)
                    ?.collection("events")?.document(task.id)
                    ?.set(taskMap, SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.d("FocusRepository", "Firestore write skipped: ${e.message}")
        }
    }

    private fun deleteTaskFromCloud(taskId: String) {
        val uid = auth?.currentUser?.uid ?: return
        try {
            firestore?.collection("users")?.document(uid)
                ?.collection("tasks")?.document(taskId)
                ?.delete()
            firestore?.collection("users")?.document(uid)
                ?.collection("events")?.document(taskId)
                ?.delete()
        } catch (e: Exception) {
            Log.d("FocusRepository", "Firestore delete skipped: ${e.message}")
        }
    }

    private fun syncProjectToCloud(project: Project, specificUid: String? = null) {
        val uid = specificUid ?: auth?.currentUser?.uid ?: return
        try {
            val projectMap = hashMapOf(
                "id" to project.id,
                "name" to project.name,
                "description" to project.description,
                "color" to project.color,
                "progress" to project.progress,
                "status" to project.status,
                "taskCount" to project.taskCount,
                "completedTaskCount" to project.completedTaskCount,
                "createdAt" to project.createdAt
            )
            firestore?.collection("users")?.document(uid)
                ?.collection("projects")?.document(project.id)
                ?.set(projectMap, SetOptions.merge())
        } catch (e: Exception) {
            Log.d("FocusRepository", "Firestore write skipped: ${e.message}")
        }
    }

    private fun deleteProjectFromCloud(projectId: String) {
        val uid = auth?.currentUser?.uid ?: return
        try {
            firestore?.collection("users")?.document(uid)
                ?.collection("projects")?.document(projectId)
                ?.delete()
        } catch (e: Exception) {
            Log.d("FocusRepository", "Firestore delete skipped: ${e.message}")
        }
    }

    private fun syncGoalToCloud(goal: Goal, specificUid: String? = null) {
        val uid = specificUid ?: auth?.currentUser?.uid ?: return
        try {
            val goalMap = hashMapOf(
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
                "createdAt" to goal.createdAt
            )
            firestore?.collection("users")?.document(uid)
                ?.collection("goals")?.document(goal.id)
                ?.set(goalMap, SetOptions.merge())
        } catch (e: Exception) {
            Log.d("FocusRepository", "Firestore write skipped: ${e.message}")
        }
    }

    private fun deleteGoalFromCloud(goalId: String) {
        val uid = auth?.currentUser?.uid ?: return
        try {
            firestore?.collection("users")?.document(uid)
                ?.collection("goals")?.document(goalId)
                ?.delete()
        } catch (e: Exception) {
            Log.d("FocusRepository", "Firestore delete skipped: ${e.message}")
        }
    }

    fun syncSettingsToCloud(
        uid: String,
        userName: String,
        email: String? = null,
        photoUrl: String? = null
    ) {
        try {
            val settingsMap = hashMapOf(
                "userName" to userName,
                "email" to (email ?: ""),
                "photoUrl" to (photoUrl ?: ""),
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("users")?.document(uid)
                ?.collection("settings")?.document("profile")
                ?.set(settingsMap, SetOptions.merge())
        } catch (e: Exception) {
            Log.d("FocusRepository", "Firestore settings write skipped: ${e.message}")
        }
    }

    suspend fun checkAndSeedInitialData() {
        // Zero dummy data: Clean initial state
        // Purge legacy mock data if present from older versions
        try {
            val legacyTaskIds = listOf("t1", "t2", "t3", "t4", "t5")
            for (id in legacyTaskIds) {
                taskDao.deleteTaskById(id)
            }
            val legacyProjectIds = listOf("p1", "p2", "p3")
            for (id in legacyProjectIds) {
                projectDao.deleteProjectById(id)
            }
            val legacyGoalIds = listOf("g1", "g2", "g3")
            for (id in legacyGoalIds) {
                goalDao.deleteGoalById(id)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Legacy mock data cleanup skipped: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "FocusRepository"

        fun getTodayTimeBounds(): Pair<Long, Long> {
            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return Pair(startCal.timeInMillis, endCal.timeInMillis)
        }

        fun getPeriodTimeBounds(period: com.example.model.ReviewPeriodTab): Pair<Long, Long> {
            return when (period) {
                com.example.model.ReviewPeriodTab.DAILY -> getTodayTimeBounds()
                com.example.model.ReviewPeriodTab.WEEKLY -> getWeekTimeBounds()
                com.example.model.ReviewPeriodTab.MONTHLY -> getMonthTimeBounds()
            }
        }

        fun getWeekTimeBounds(): Pair<Long, Long> {
            val startCal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.SATURDAY
                set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = (startCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return Pair(startCal.timeInMillis, endCal.timeInMillis)
        }

        fun getMonthTimeBounds(): Pair<Long, Long> {
            val startCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = (startCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return Pair(startCal.timeInMillis, endCal.timeInMillis)
        }
    }
}
