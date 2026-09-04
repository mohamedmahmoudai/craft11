package com.example.data.ai

import kotlinx.coroutines.flow.Flow

interface FocusAIService {
    /**
     * Deconstructs a high-level task into 3-5 clear, actionable sub-tasks.
     */
    fun breakdownTask(taskTitle: String, taskDescription: String = ""): Flow<List<String>>

    /**
     * Estimates the realistic duration in minutes for the given task.
     */
    fun estimateDuration(taskTitle: String): Flow<Int>

    /**
     * Generates a 2-sentence psychological de-friction plan for a delayed or postponed task.
     */
    fun suggestDeFrictionPlan(taskTitle: String, postponedCount: Int): Flow<String>
}
