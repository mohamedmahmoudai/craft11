package com.example.model

enum class FocusNavTab(val title: String) {
    TODAY("اليوم"),
    SCHEDULE("الجدول"),
    GOALS("الأهداف"),
    PROJECTS("المشاريع"),
    MORE("المزيد");

    companion object {
        val HOME = TODAY
        val TASKS = PROJECTS
        val WEEK = SCHEDULE
    }
}

enum class TaskFilterTab(val title: String) {
    TODAY("اليوم"),
    UPCOMING("قادمة"),
    LATER("لاحقاً"),
    COMPLETED("مكتملة")
}

enum class GoalFilterTab(val title: String) {
    ACTIVE("نشط"),
    PAUSED("متوقف"),
    COMPLETED("مكتمل"),
    ARCHIVED("مؤرشف")
}

enum class ProjectFilterTab(val title: String) {
    ACTIVE("نشط"),
    PAUSED("متوقف"),
    COMPLETED("مكتمل"),
    ARCHIVED("مؤرشف")
}

enum class ReviewPeriodTab(val title: String) {
    DAILY("اليومي"),
    WEEKLY("الأسبوعي"),
    MONTHLY("الشهري")
}

enum class BlockColor {
    INDIGO,
    ORANGE,
    GREEN,
    TEAL
}

enum class DayCapacityLevel {
    LIGHT,      // < 40%
    BALANCED,   // 40% - 80%
    PACKED      // > 80%
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

enum class TaskAlertType(val title: String, val subtitle: String) {
    NONE("بدون تنبيه", "بدون إشعار أو منبه"),
    NOTIFICATION("إشعار صامت", "إشعار في شريط التنبيهات"),
    SMART_ALARM("منبه ذكي كامل", "شاشة منبه تفاعلية بصوت واهتزاز")
}

data class FocusTimerState(
    val taskId: String? = null,
    val taskTitle: String = "جلسة تركيز عميق",
    val taskSubtitle: String = "ركز على ما يهمك الآن",
    val plannedDurationMinutes: Int = 25,
    val initialTotalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val status: TimerStatus = TimerStatus.IDLE,
    val isFocusModeFullscreen: Boolean = false,
    val colorType: BlockColor = BlockColor.TEAL
) {
    val progress: Float
        get() = if (initialTotalSeconds > 0) {
            (elapsedSeconds.toFloat() / initialTotalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val formattedRemainingTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }

    val formattedElapsedTime: String
        get() {
            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }

    val actualMinutesSpent: Int
        get() = (elapsedSeconds + 30) / 60
}

data class TaskItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val timeText: String = "",
    val isCompleted: Boolean = false,
    val filterTab: TaskFilterTab = TaskFilterTab.TODAY,
    val colorType: BlockColor = BlockColor.TEAL,
    val durationMinutes: Int = 90,
    val location: String? = null,
    val isFixed: Boolean = false,
    val startTime: String? = null,
    val endTime: String? = null,
    val deadline: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val actualDurationMinutes: Int = 0,
    val lastFocusSessionTimestamp: Long = 0L,
    val alertType: TaskAlertType = TaskAlertType.SMART_ALARM,
    val needsRescheduling: Boolean = false,
    val missedCount: Int = 0,
    val postponedCount: Int = 0,
    val projectId: String? = null,
    val energyLevel: String = "طبيعية"
) {
    val isLater: Boolean
        get() = timeText == "لاحقاً" || filterTab == TaskFilterTab.LATER
}

data class GoalItem(
    val id: String,
    val title: String,
    val description: String = "",
    val category: String = "عام",
    val targetCount: Int = 10,
    val currentCount: Int = 0,
    val progress: Float = 0f,
    val deadline: String = "",
    val filterTab: GoalFilterTab = GoalFilterTab.ACTIVE,
    val color: String = "TEAL",
    val priority: String = "متوسطة",
    val createdAt: Long = System.currentTimeMillis()
) {
    val percentage: Int
        get() = (progress * 100).toInt().coerceIn(0, 100)
}

data class ProjectItem(
    val id: String,
    val name: String,
    val description: String = "",
    val color: String = "TEAL",
    val progress: Float = 0f,
    val filterTab: ProjectFilterTab = ProjectFilterTab.ACTIVE,
    val taskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val goalId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val percentage: Int
        get() = (progress * 100).toInt().coerceIn(0, 100)
}

data class RescheduleSuggestion(
    val task: com.example.data.local.entity.Task,
    val newStartTime: String,
    val newEndTime: String? = null,
    val newScheduledTime: String = "",
    val newDurationMinutes: Int = task.durationMinutes,
    val reason: String,
    val isPostponeToTomorrow: Boolean = false,
    val isMoveToLater: Boolean = false,
    val availableGapMinutes: Int? = null
)

data class TimeBlock(
    val id: String,
    val title: String,
    val timeRange: String,
    val startHour: Float,
    val durationHours: Float,
    val colorType: BlockColor,
    val location: String? = null,
    val isFixed: Boolean = false,
    val isAvailableGap: Boolean = false,
    val isCompleted: Boolean = false,
    val originalTaskId: String? = null
)

sealed class TimelineItem {
    abstract val id: String
    abstract val startHour: Float
    abstract val durationHours: Float

    data class TaskBlock(
        override val id: String,
        val title: String,
        val subtitle: String = "",
        val timeRange: String,
        override val startHour: Float,
        override val durationHours: Float,
        val colorType: BlockColor,
        val isFixed: Boolean = false,
        val location: String? = null,
        val isCompleted: Boolean = false,
        val originalTaskId: String? = null,
        val taskItem: TaskItem? = null
    ) : TimelineItem()

    data class FocusGap(
        override val id: String,
        override val startHour: Float,
        override val durationHours: Float,
        val timeRange: String,
        val durationMinutes: Int,
        val formattedDuration: String
    ) : TimelineItem()
}

data class DayItem(
    val dayName: String,
    val dayNumber: Int,
    val isSelected: Boolean = false,
    val timestamp: Long = 0L,
    val bookedMinutes: Int = 0,
    val bookedRatio: Float = 0f,
    val capacityLevel: DayCapacityLevel = DayCapacityLevel.LIGHT,
    val isToday: Boolean = false
)

enum class InsightType {
    HIGH_EXECUTION,
    OVERLOADED_DAY,
    REPEATED_POSTPONEMENT,
    MOMENTUM_RECOVERY,
    BALANCED_PROGRESS,
    CONSISTENCY
}

data class BehavioralInsight(
    val title: String,
    val message: String,
    val insightType: InsightType,
    val categoryName: String = "محرك التحفيز السلوكي"
)

data class ReviewStats(
    val completedCount: Int = 3,
    val completedDelta: String = "+2 اليوم",
    val totalCount: Int = 6,
    val totalLabel: String = "مهام مجدولة",
    val focusHours: String = "3س 30د",
    val completionPercentage: Int = 60,
    val performanceProgress: Float = 0.6f,
    val performanceTitle: String = "تقدم ممتاز",
    val performanceMessage: String = "أنت على الطريق الصحيح! استمر بنفس الزخم",
    val quote: String = "النجاح ليس بالصدفة. بل هو نتيجة للتركيز اليومي على ما يهم حقاً.",
    val quoteCategory: String = "محرك التحفيز السلوكي",
    val behavioralInsight: BehavioralInsight = BehavioralInsight(
        title = "تحليل الأداء السلوكي",
        message = "النجاح ليس بالصدفة. بل هو نتيجة للتركيز اليومي على ما يهم حقاً.",
        insightType = InsightType.BALANCED_PROGRESS
    ),
    val consecutiveCompletedCount: Int = 0,
    val repeatedPostponementsCount: Int = 0,
    val overloadedDayDetected: Boolean = false
) {
    val completionRatePercent: Int get() = completionPercentage
    val focusHoursThisWeek: String get() = focusHours
    val completedTasksCount: Int get() = completedCount
    val streakDays: Int get() = consecutiveCompletedCount.coerceAtLeast(3)
}

sealed class AIActionState {
    object Idle : AIActionState()
    data class Loading(val message: String = "جارٍ المعالجة بالذكاء الاصطناعي...") : AIActionState()
    data class BreakdownSuccess(
        val parentTaskId: String?,
        val parentTitle: String,
        val subTasks: List<String>,
        val isOfflineFallback: Boolean = false
    ) : AIActionState()
    data class DeFrictionSuccess(
        val taskId: String,
        val taskTitle: String,
        val advice: String,
        val isOfflineFallback: Boolean = false
    ) : AIActionState()
    data class Error(val message: String) : AIActionState()
}
