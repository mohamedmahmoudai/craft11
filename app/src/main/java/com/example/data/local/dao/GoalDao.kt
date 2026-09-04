package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun findGoalById(id: String): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<Goal>)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Query("SELECT COUNT(*) FROM goals")
    suspend fun getGoalCount(): Int

    @Query("UPDATE goals SET currentCount = :count, progress = :progress WHERE id = :id")
    suspend fun updateGoalProgress(id: String, count: Int, progress: Float)

    @Query("UPDATE goals SET status = :status WHERE id = :id")
    suspend fun updateGoalStatus(id: String, status: String)
}
