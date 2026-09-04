package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ProjectFilterTab
import com.example.model.ProjectItem

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val color: String = "TEAL",
    val progress: Float = 0f,
    val status: String = "ACTIVE",
    val taskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val goalId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toProjectItem(): ProjectItem {
        val tab = when (status.uppercase()) {
            "PAUSED" -> ProjectFilterTab.PAUSED
            "COMPLETED" -> ProjectFilterTab.COMPLETED
            "ARCHIVED" -> ProjectFilterTab.ARCHIVED
            else -> ProjectFilterTab.ACTIVE
        }
        val calculatedProgress = if (taskCount > 0) {
            (completedTaskCount.toFloat() / taskCount.toFloat()).coerceIn(0f, 1f)
        } else progress.coerceIn(0f, 1f)

        return ProjectItem(
            id = id,
            name = name,
            description = description,
            color = color,
            progress = calculatedProgress,
            filterTab = tab,
            taskCount = taskCount,
            completedTaskCount = completedTaskCount,
            goalId = goalId,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromProjectItem(item: ProjectItem): Project {
            val statusStr = when (item.filterTab) {
                ProjectFilterTab.PAUSED -> "PAUSED"
                ProjectFilterTab.COMPLETED -> "COMPLETED"
                ProjectFilterTab.ARCHIVED -> "ARCHIVED"
                ProjectFilterTab.ACTIVE -> "ACTIVE"
            }
            return Project(
                id = item.id,
                name = item.name,
                description = item.description,
                color = item.color,
                progress = item.progress,
                status = statusStr,
                taskCount = item.taskCount,
                completedTaskCount = item.completedTaskCount,
                goalId = item.goalId,
                createdAt = item.createdAt
            )
        }
    }
}
