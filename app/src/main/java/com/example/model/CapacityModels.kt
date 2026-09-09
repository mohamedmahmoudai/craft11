package com.example.model

enum class TaskType {
    FIXED_EVENT,   // Strict startTime and endTime (e.g. University lectures, fixed meetings)
    FLEXIBLE_TASK  // Duration and deadline, can be placed in available gaps
}

enum class DailyFeasibility(val label: String, val threshold: Float) {
    COMFORTABLE("مريحة", 0.6f),
    BALANCED("متوازنة", 0.9f),
    INTENSIVE("مكثفة", 1.15f),
    UNREALISTIC("غير واقعية", Float.MAX_VALUE)
}

data class RoutineContextBlock(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val isEnabled: Boolean = true
) {
    val startHourFloat: Float
        get() {
            val mins = parseTimeMinutes(startTime)
            return (mins / 60f).coerceIn(0f, 24f)
        }

    val endHourFloat: Float
        get() {
            val mins = parseTimeMinutes(endTime)
            return (mins / 60f).coerceIn(0f, 24f)
        }

    val durationMinutes: Int
        get() {
            val startMins = parseTimeMinutes(startTime)
            val endMins = parseTimeMinutes(endTime)
            return if (endMins >= startMins) {
                (endMins - startMins).coerceAtLeast(0)
            } else {
                (24 * 60 - startMins + endMins).coerceAtLeast(0)
            }
        }

    val timeRange: String
        get() = "$startTime - $endTime"
}

data class DailyCapacityState(
    val bookedMinutes: Int = 0,
    val totalAwakeMinutes: Int = 960, // 16 hours
    val bookedRatio: Float = 0f,       // Ratio against total available focus capacity
    val bookedTimeFormatted: String = "0h 00m",
    val freeTimeFormatted: String = "16h 00m",
    val isOverbooked: Boolean = false,
    val routineMinutes: Int = 0,
    val effectiveCapacityMinutes: Int = 960
) {
    val percentage: Int
        get() = (bookedRatio * 100).toInt().coerceIn(0, 100)

    val feasibility: DailyFeasibility
        get() = when {
            bookedRatio <= 0.6f -> DailyFeasibility.COMFORTABLE
            bookedRatio <= 0.9f -> DailyFeasibility.BALANCED
            bookedRatio <= 1.15f -> DailyFeasibility.INTENSIVE
            else -> DailyFeasibility.UNREALISTIC
        }
}

data class ScheduleReminderItem(
    val minutes: Int,
    val triggerAt: Long,
    val scheduleId: String,
    val enabled: Boolean = true,
    val delivered: Boolean = false,
    val skippedPast: Boolean = false
)

data class ScheduleBlockItem(
    val id: String,
    val title: String,
    val timeRange: String,
    val startHour: Float,
    val durationHours: Float,
    val colorType: BlockColor = BlockColor.INDIGO,
    val location: String? = null,
    val isFixed: Boolean = false,
    val isAvailableGap: Boolean = false
)
