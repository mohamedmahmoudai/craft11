package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.GoalFilterTab
import com.example.model.GoalItem

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val category: String = "عام",
    val targetCount: Int = 10,
    val currentCount: Int = 0,
    val progress: Float = 0f,
    val deadline: String = "",
    val status: String = "ACTIVE",
    val color: String = "TEAL",
    val priority: String = "متوسطة",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toGoalItem(): GoalItem {
        val tab = when (status.uppercase()) {
            "PAUSED" -> GoalFilterTab.PAUSED
            "COMPLETED" -> GoalFilterTab.COMPLETED
            "ARCHIVED" -> GoalFilterTab.ARCHIVED
            else -> GoalFilterTab.ACTIVE
        }
        val calculatedProgress = if (targetCount > 0) {
            (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
        } else progress.coerceIn(0f, 1f)

        return GoalItem(
            id = id,
            title = title,
            description = description,
            category = category,
            targetCount = targetCount,
            currentCount = currentCount,
            progress = calculatedProgress,
            deadline = deadline,
            filterTab = tab,
            color = color,
            priority = priority,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromGoalItem(item: GoalItem): Goal {
            val statusStr = when (item.filterTab) {
                GoalFilterTab.PAUSED -> "PAUSED"
                GoalFilterTab.COMPLETED -> "COMPLETED"
                GoalFilterTab.ARCHIVED -> "ARCHIVED"
                GoalFilterTab.ACTIVE -> "ACTIVE"
            }
            return Goal(
                id = item.id,
                title = item.title,
                description = item.description,
                category = item.category,
                targetCount = item.targetCount,
                currentCount = item.currentCount,
                progress = item.progress,
                deadline = item.deadline,
                status = statusStr,
                color = item.color,
                priority = item.priority,
                createdAt = item.createdAt
            )
        }
    }
}
