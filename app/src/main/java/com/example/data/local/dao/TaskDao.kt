package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, date ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY isCompleted ASC, date ASC")
    fun getTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE (date > :endOfDay OR (date < :startOfDay AND isCompleted = 0)) ORDER BY isCompleted ASC, date ASC")
    fun getUpcomingTasks(startOfDay: Long, endOfDay: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY date DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE postponedCount >= :minCount ORDER BY postponedCount DESC")
    fun getRepeatedlyPostponedTasks(minCount: Int = 3): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: String): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun findTaskById(id: String): Task?

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Update
    suspend fun updateTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :completed, subtitle = :subtitle WHERE id = :id")
    suspend fun updateCompletionStatus(id: String, completed: Boolean, subtitle: String)

    @Query("UPDATE tasks SET actualDurationMinutes = actualDurationMinutes + :minutesSpent, lastFocusSessionTimestamp = :timestamp WHERE id = :id")
    suspend fun addActualTime(id: String, minutesSpent: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET actualDurationMinutes = actualDurationMinutes + :minutesSpent, lastFocusSessionTimestamp = :timestamp, isCompleted = :isCompleted, subtitle = :subtitle WHERE id = :id")
    suspend fun completeFocusSession(id: String, minutesSpent: Int, isCompleted: Boolean, subtitle: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET needsRescheduling = :needsRescheduling WHERE id = :id")
    suspend fun updateNeedsRescheduling(id: String, needsRescheduling: Boolean)

    @Query("UPDATE tasks SET missedCount = missedCount + 1, needsRescheduling = 1 WHERE id = :id")
    suspend fun markTaskMissed(id: String)

    @Query("UPDATE tasks SET postponedCount = postponedCount + 1, date = :newDate, startTime = :newStartTime, endTime = :newEndTime, scheduledTime = :newScheduledTime, needsRescheduling = 0 WHERE id = :id")
    suspend fun postponeTask(id: String, newDate: Long, newStartTime: String?, newEndTime: String?, newScheduledTime: String)

    @Query("UPDATE tasks SET date = :newDate, startTime = :newStartTime, endTime = :newEndTime, scheduledTime = :newScheduledTime, durationMinutes = :newDurationMinutes, needsRescheduling = 0 WHERE id = :id")
    suspend fun updateTaskSchedule(id: String, newDate: Long, newStartTime: String?, newEndTime: String?, newScheduledTime: String, newDurationMinutes: Int)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}
