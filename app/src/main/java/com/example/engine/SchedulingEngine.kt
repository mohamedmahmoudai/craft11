package com.example.engine

import com.example.data.local.entity.Task
import com.example.model.BlockColor
import com.example.model.DailyCapacityState
import com.example.model.DayCapacityLevel
import com.example.model.DayItem
import com.example.model.RoutineContextBlock
import com.example.model.TimeBlock
import com.example.model.TimelineItem
import java.util.Calendar
import java.util.Locale

class CapacityExceededException(message: String) : Exception(message)

object SchedulingEngine {
    const val TOTAL_DAILY_AWAKE_MINUTES = 960 // 16 hours (e.g. 07:00 to 23:00)
    const val TIMELINE_START_HOUR = 6.0f // 06:00
    const val TIMELINE_END_HOUR = 24.0f // 24:00
    const val MIN_FOCUS_GAP_MINUTES = 45 // Focus gap threshold

    /**
     * Calculates the daily capacity state for a given list of tasks for the day,
     * automatically subtracting routine context blocks from total available focus capacity.
     */
    fun calculateDailyCapacity(
        tasks: List<Task>,
        routineBlocks: List<RoutineContextBlock> = emptyList()
    ): DailyCapacityState {
        // Exclude parking lot / later items from capacity
        val scheduledTasks = tasks.filter { !it.scheduledTime.contains("لاحقاً") }
        val totalBookedMinutes = scheduledTasks.sumOf { it.durationMinutes }

        // Automatically subtract routine context blocks from total available focus capacity
        val enabledRoutineBlocks = routineBlocks.filter { it.isEnabled }
        val routineMinutes = enabledRoutineBlocks.sumOf { it.durationMinutes }
        val effectiveCapacityMinutes = (TOTAL_DAILY_AWAKE_MINUTES - routineMinutes).coerceAtLeast(60)

        val bookedRatio = if (effectiveCapacityMinutes > 0) {
            (totalBookedMinutes.toFloat() / effectiveCapacityMinutes.toFloat()).coerceIn(0f, 2f)
        } else 0f
        val remainingMinutes = (effectiveCapacityMinutes - totalBookedMinutes).coerceAtLeast(0)

        val bookedHours = totalBookedMinutes / 60
        val bookedMins = totalBookedMinutes % 60
        val bookedTimeFormatted = "${bookedHours}h ${String.format(Locale.US, "%02dm", bookedMins)}"

        val freeHours = remainingMinutes / 60
        val freeMins = remainingMinutes % 60
        val freeTimeFormatted = "${freeHours}h ${String.format(Locale.US, "%02dm", freeMins)}"

        val isOverbooked = totalBookedMinutes > effectiveCapacityMinutes

        return DailyCapacityState(
            bookedMinutes = totalBookedMinutes,
            totalAwakeMinutes = TOTAL_DAILY_AWAKE_MINUTES,
            bookedRatio = bookedRatio,
            bookedTimeFormatted = bookedTimeFormatted,
            freeTimeFormatted = freeTimeFormatted,
            isOverbooked = isOverbooked,
            routineMinutes = routineMinutes,
            effectiveCapacityMinutes = effectiveCapacityMinutes
        )
    }

    /**
     * Conflict detection: checks whether adding additionalMinutes exceeds effective capacity
     * after routine context windows are deducted.
     */
    fun canSchedule(
        currentTasks: List<Task>,
        additionalMinutes: Int,
        routineBlocks: List<RoutineContextBlock> = emptyList()
    ): Boolean {
        val routineMins = routineBlocks.filter { it.isEnabled }.sumOf { it.durationMinutes }
        val effectiveCapacity = (TOTAL_DAILY_AWAKE_MINUTES - routineMins).coerceAtLeast(60)
        val currentBooked = currentTasks.sumOf { it.durationMinutes }
        return (currentBooked + additionalMinutes) <= effectiveCapacity
    }

    /**
     * Validates and throws CapacityExceededException if conflict occurs.
     */
    @Throws(CapacityExceededException::class)
    fun validateScheduleCapacity(
        currentTasks: List<Task>,
        additionalMinutes: Int,
        routineBlocks: List<RoutineContextBlock> = emptyList()
    ) {
        val routineMins = routineBlocks.filter { it.isEnabled }.sumOf { it.durationMinutes }
        val effectiveCapacity = (TOTAL_DAILY_AWAKE_MINUTES - routineMins).coerceAtLeast(60)
        val currentBooked = currentTasks.sumOf { it.durationMinutes }
        if (currentBooked + additionalMinutes > effectiveCapacity) {
            throw CapacityExceededException("اليوم ممتلئ بالفعل بعد احتساب الروتين اليومي ($routineMins دقيقة محجوزة). لا توجد سعة كافية لهذه المهمة.")
        }
    }

    /**
     * Parse time string to 24-hour decimal float (e.g. "09:30" -> 9.5f, "02:00 م" -> 14.0f)
     */
    fun parseTimeToHourFloat(timeStr: String?): Float? {
        if (timeStr.isNullOrBlank()) return null
        val clean = timeStr.trim()

        // 1. Try 24-hour "HH:mm"
        val colonParts = clean.split(":")
        if (colonParts.size >= 2) {
            val hourPart = colonParts[0].filter { it.isDigit() }.toIntOrNull()
            val minPart = colonParts[1].take(2).filter { it.isDigit() }.toIntOrNull() ?: 0

            if (hourPart != null) {
                var hour = hourPart.toFloat()
                val isPM = clean.contains("م") || clean.lowercase().contains("pm")
                val isAM = clean.contains("ص") || clean.lowercase().contains("am")

                if (isPM && hour < 12f) {
                    hour += 12f
                } else if (isAM && hour == 12f) {
                    hour = 0f
                }
                return (hour + minPart / 60f).coerceIn(0f, 24f)
            }
        }

        return null
    }

    /**
     * Formats duration into human readable Arabic string (e.g. 90 mins -> "1 ساعة و 30 دقيقة")
     */
    fun formatArabicDuration(minutes: Int): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 && remainingMinutes > 0 -> "$hours ساعة و $remainingMinutes دقيقة"
            hours > 0 -> if (hours == 1) "ساعة واحدة" else if (hours == 2) "ساعتان" else "$hours ساعات"
            else -> "$remainingMinutes دقيقة"
        }
    }

    /**
     * Formats hour float (e.g. 9.5f -> "09:30 ص" or "09:30")
     */
    fun formatHourToTimeRange(startHour: Float, durationHours: Float): String {
        val endHour = (startHour + durationHours).coerceAtMost(24f)

        val startH = startHour.toInt()
        val startM = ((startHour - startH) * 60).toInt()

        val endH = endHour.toInt()
        val endM = ((endHour - endH) * 60).toInt()

        return String.format(
            Locale.US,
            "%02d:%02d - %02d:%02d",
            startH, startM, endH, endM
        )
    }

    /**
     * Generates a complete 06:00 - 24:00 timeline containing scheduled TaskBlocks, routine windows, and FocusGaps (>= 45 min)
     */
    fun generateTimelineItems(
        tasks: List<Task>,
        routineBlocks: List<RoutineContextBlock> = emptyList()
    ): List<TimelineItem> {
        val result = mutableListOf<TimelineItem>()
        val scheduledTasks = tasks.filter { !it.scheduledTime.contains("لاحقاً") }
        val enabledRoutineBlocks = routineBlocks.filter { it.isEnabled }

        if (scheduledTasks.isEmpty() && enabledRoutineBlocks.isEmpty()) {
            // Entire day is a focus gap
            val totalMins = ((TIMELINE_END_HOUR - TIMELINE_START_HOUR) * 60).toInt()
            result.add(
                TimelineItem.FocusGap(
                    id = "gap-whole-day",
                    startHour = TIMELINE_START_HOUR,
                    durationHours = TIMELINE_END_HOUR - TIMELINE_START_HOUR,
                    timeRange = "06:00 - 24:00",
                    durationMinutes = totalMins,
                    formattedDuration = formatArabicDuration(totalMins)
                )
            )
            return result
        }

        // Build task blocks with start hours
        val taskBlocks = mutableListOf<TimelineItem.TaskBlock>()
        var fallbackHour = 9.0f // Fallback initial placement

        // 1. Add routine context commitments (commute, lunch, lecture prep, etc.)
        for (routine in enabledRoutineBlocks) {
            val startH = routine.startHourFloat
            val durHours = (routine.durationMinutes / 60f).coerceAtLeast(0.25f)
            taskBlocks.add(
                TimelineItem.TaskBlock(
                    id = "routine_${routine.id}",
                    title = "🔒 ${routine.title}",
                    subtitle = "التزام روتيني يومي",
                    timeRange = routine.timeRange,
                    startHour = startH,
                    durationHours = durHours,
                    colorType = BlockColor.ORANGE,
                    isFixed = true,
                    location = null,
                    isCompleted = false,
                    originalTaskId = null
                )
            )
        }

        // 2. Add scheduled tasks
        scheduledTasks.forEachIndexed { index, task ->
            val color = try {
                BlockColor.valueOf(task.categoryColor)
            } catch (e: Exception) {
                BlockColor.INDIGO
            }

            var startH = parseTimeToHourFloat(task.startTime)
                ?: parseTimeToHourFloat(task.scheduledTime.split("-").firstOrNull())
                ?: fallbackHour

            val durHours = (task.durationMinutes / 60f).coerceAtLeast(0.5f)

            // Avoid overflowing beyond 24:00
            if (startH + durHours > TIMELINE_END_HOUR) {
                startH = (TIMELINE_END_HOUR - durHours).coerceAtLeast(TIMELINE_START_HOUR)
            }

            val timeRangeStr = if (task.scheduledTime.isNotBlank()) {
                task.scheduledTime
            } else {
                formatHourToTimeRange(startH, durHours)
            }

            taskBlocks.add(
                TimelineItem.TaskBlock(
                    id = task.id,
                    title = task.title,
                    subtitle = task.subtitle,
                    timeRange = timeRangeStr,
                    startHour = startH,
                    durationHours = durHours,
                    colorType = color,
                    isFixed = task.isFixed,
                    location = if (task.isFixed) "القاعة 402" else null,
                    isCompleted = task.isCompleted,
                    originalTaskId = task.id
                )
            )

            fallbackHour = (startH + durHours + 0.5f).coerceAtMost(22.0f)
        }

        // Sort task blocks by startHour
        taskBlocks.sortBy { it.startHour }

        // Find gaps between TIMELINE_START_HOUR, task blocks, and TIMELINE_END_HOUR
        var currentPointer = TIMELINE_START_HOUR

        for (tb in taskBlocks) {
            val gapHours = tb.startHour - currentPointer
            val gapMinutes = (gapHours * 60).toInt()

            if (gapMinutes >= MIN_FOCUS_GAP_MINUTES) {
                result.add(
                    TimelineItem.FocusGap(
                        id = "gap-${currentPointer.toInt()}-${tb.startHour.toInt()}",
                        startHour = currentPointer,
                        durationHours = gapHours,
                        timeRange = formatHourToTimeRange(currentPointer, gapHours),
                        durationMinutes = gapMinutes,
                        formattedDuration = formatArabicDuration(gapMinutes)
                    )
                )
            }

            result.add(tb)
            val blockEnd = tb.startHour + tb.durationHours
            if (blockEnd > currentPointer) {
                currentPointer = blockEnd
            }
        }

        // Trailing gap until 24:00 if >= 45 mins
        val trailingGapHours = TIMELINE_END_HOUR - currentPointer
        val trailingGapMinutes = (trailingGapHours * 60).toInt()
        if (trailingGapMinutes >= MIN_FOCUS_GAP_MINUTES) {
            result.add(
                TimelineItem.FocusGap(
                    id = "gap-${currentPointer.toInt()}-24",
                    startHour = currentPointer,
                    durationHours = trailingGapHours,
                    timeRange = formatHourToTimeRange(currentPointer, trailingGapHours),
                    durationMinutes = trailingGapMinutes,
                    formattedDuration = formatArabicDuration(trailingGapMinutes)
                )
            )
        }

        return result
    }

    /**
     * Builds dynamic 7-day week items with capacity level and active selection
     */
    fun buildWeekDays(
        baseDateMillis: Long,
        selectedDateMillis: Long,
        allTasks: List<Task>,
        routineBlocks: List<RoutineContextBlock> = emptyList()
    ): List<DayItem> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = baseDateMillis
            // Align to start of week (e.g. Sunday or Saturday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Move to Saturday of current week
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        val todayCal = Calendar.getInstance()
        val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }

        val arabicDayNames = listOf(
            "السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة"
        )

        val days = mutableListOf<DayItem>()
        val routineMins = routineBlocks.filter { it.isEnabled }.sumOf { it.durationMinutes }
        val effectiveCapacity = (TOTAL_DAILY_AWAKE_MINUTES - routineMins).coerceAtLeast(60)

        for (i in 0..6) {
            val dayTimestamp = calendar.timeInMillis
            val dayNum = calendar.get(Calendar.DAY_OF_MONTH)
            val dayName = arabicDayNames[i]

            val isSelected = (calendar.get(Calendar.YEAR) == selCal.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == selCal.get(Calendar.DAY_OF_YEAR))

            val isToday = (calendar.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR))

            // Calculate tasks on this day
            val dayStart = dayTimestamp
            val dayEnd = dayTimestamp + (24 * 60 * 60 * 1000L) - 1L
            val dayTasks = allTasks.filter { it.date in dayStart..dayEnd }

            val bookedMinutes = dayTasks.sumOf { it.durationMinutes }
            val bookedRatio = (bookedMinutes.toFloat() / effectiveCapacity).coerceIn(0f, 1f)

            val level = when {
                bookedRatio > 0.8f -> DayCapacityLevel.PACKED
                bookedRatio > 0.4f -> DayCapacityLevel.BALANCED
                else -> DayCapacityLevel.LIGHT
            }

            days.add(
                DayItem(
                    dayName = dayName,
                    dayNumber = dayNum,
                    isSelected = isSelected,
                    timestamp = dayTimestamp,
                    bookedMinutes = bookedMinutes,
                    bookedRatio = bookedRatio,
                    capacityLevel = level,
                    isToday = isToday
                )
            )

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return days
    }

    /**
     * Legacy & direct helper for backward compatibility and simple card mapping
     */
    fun generateScheduleBlocksWithGaps(
        tasks: List<Task>,
        routineBlocks: List<RoutineContextBlock> = emptyList()
    ): List<TimeBlock> {
        val timeline = generateTimelineItems(tasks, routineBlocks)
        return timeline.map { item ->
            when (item) {
                is TimelineItem.TaskBlock -> TimeBlock(
                    id = item.id,
                    title = item.title,
                    timeRange = item.timeRange,
                    startHour = item.startHour,
                    durationHours = item.durationHours,
                    colorType = item.colorType,
                    location = item.location,
                    isFixed = item.isFixed,
                    isAvailableGap = false,
                    isCompleted = item.isCompleted,
                    originalTaskId = item.originalTaskId
                )
                is TimelineItem.FocusGap -> TimeBlock(
                    id = item.id,
                    title = "مساحة تركيز متاحة",
                    timeRange = item.timeRange,
                    startHour = item.startHour,
                    durationHours = item.durationHours,
                    colorType = BlockColor.INDIGO,
                    location = null,
                    isFixed = false,
                    isAvailableGap = true,
                    isCompleted = false,
                    originalTaskId = null
                )
            }
        }
    }

    /**
     * Computes the exact timestamp for a given Day of Week (e.g. Calendar.SATURDAY) in the target week.
     */
    fun computeDateForDayOfWeek(baseDateMillis: Long, targetCalendarDay: Int, weekOffset: Int = 0): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = baseDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        val daysFromSaturday = when (targetCalendarDay) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
        calendar.add(Calendar.DAY_OF_MONTH, daysFromSaturday + (weekOffset * 7))
        return calendar.timeInMillis
    }
}
