package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.BlockColor
import com.example.model.TaskAlertType
import com.example.model.TaskFilterTab
import com.example.model.TaskItem

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String,
    val title: String,
    val subtitle: String = "",
    val durationMinutes: Int = 90,
    val scheduledTime: String = "",
    val isCompleted: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val categoryColor: String = "INDIGO",
    val isFixed: Boolean = false,
    val startTime: String? = null,
    val endTime: String? = null,
    val deadline: Long? = null,
    val actualDurationMinutes: Int = 0,
    val lastFocusSessionTimestamp: Long = 0L,
    val alertType: String = "SMART_ALARM",
    val needsRescheduling: Boolean = false,
    val missedCount: Int = 0,
    val postponedCount: Int = 0,
    val projectId: String? = null
) {
    fun toTaskItem(): TaskItem {
        val color = when (categoryColor.uppercase()) {
            "ORANGE" -> BlockColor.ORANGE
            "GREEN" -> BlockColor.GREEN
            else -> BlockColor.INDIGO
        }
        val tab = when {
            isCompleted -> TaskFilterTab.COMPLETED
            else -> TaskFilterTab.TODAY
        }
        val mappedAlert = when (alertType.uppercase()) {
            "NONE" -> TaskAlertType.NONE
            "NOTIFICATION" -> TaskAlertType.NOTIFICATION
            else -> TaskAlertType.SMART_ALARM
        }
        return TaskItem(
            id = id,
            title = title,
            subtitle = subtitle,
            timeText = scheduledTime,
            isCompleted = isCompleted,
            filterTab = tab,
            colorType = color,
            durationMinutes = durationMinutes,
            isFixed = isFixed,
            startTime = startTime,
            endTime = endTime,
            deadline = deadline,
            date = date,
            actualDurationMinutes = actualDurationMinutes,
            lastFocusSessionTimestamp = lastFocusSessionTimestamp,
            alertType = mappedAlert,
            needsRescheduling = needsRescheduling,
            missedCount = missedCount,
            postponedCount = postponedCount,
            projectId = projectId
        )
    }

    companion object {
        fun fromTaskItem(item: TaskItem, timestamp: Long = System.currentTimeMillis()): Task {
            return Task(
                id = item.id,
                title = item.title,
                subtitle = item.subtitle,
                durationMinutes = item.durationMinutes,
                scheduledTime = item.timeText,
                isCompleted = item.isCompleted,
                date = timestamp,
                categoryColor = item.colorType.name,
                isFixed = item.isFixed,
                startTime = item.startTime,
                endTime = item.endTime,
                deadline = item.deadline,
                actualDurationMinutes = item.actualDurationMinutes,
                lastFocusSessionTimestamp = item.lastFocusSessionTimestamp,
                alertType = item.alertType.name,
                needsRescheduling = item.needsRescheduling,
                missedCount = item.missedCount,
                postponedCount = item.postponedCount,
                projectId = item.projectId
            )
        }
    }
}
