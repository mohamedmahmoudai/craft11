package com.example.engine

import com.example.data.local.entity.Task
import com.example.model.DailyCapacityState
import com.example.model.RescheduleSuggestion
import com.example.model.TimelineItem
import java.util.Calendar
import java.util.Locale

object AdaptivePlanner {

    const val MIN_RESCHEDULE_BUFFER_HOURS = 0.25f // 15 min buffer from now
    const val CAPACITY_OVERFLOW_THRESHOLD = 0.90f // Day is considered overcrowded if >= 90% booked

    /**
     * Analyzes current tasks and generates realistic adaptive suggestions for missed,
     * delayed, or partially completed tasks without overwhelming the user.
     */
    fun generateSuggestions(
        tasks: List<Task>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<RescheduleSuggestion> {
        val (todayStart, todayEnd) = getDayTimeBounds(currentTimeMillis)
        val todayTasks = tasks.filter { it.date in todayStart..todayEnd || (!it.isCompleted && it.date < todayStart) }

        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE) / 60f)

        // 1. Calculate today's capacity
        val capacityState = SchedulingEngine.calculateDailyCapacity(todayTasks)
        val isOvercrowded = capacityState.bookedRatio >= CAPACITY_OVERFLOW_THRESHOLD

        // 2. Identify candidate tasks that need adaptive rescheduling
        val candidateTasks = findTasksNeedingReschedule(todayTasks, currentHour, todayStart)

        if (candidateTasks.isEmpty()) {
            return emptyList()
        }

        // 3. Find available timeline gaps starting from current time
        val timelineItems = SchedulingEngine.generateTimelineItems(
            todayTasks.filter { it !in candidateTasks && !it.isCompleted }
        )
        val availableGaps = timelineItems.filterIsInstance<TimelineItem.FocusGap>()
            .filter { (it.startHour + it.durationHours) > currentHour + MIN_RESCHEDULE_BUFFER_HOURS }
            .sortedBy { it.startHour }

        val suggestions = mutableListOf<RescheduleSuggestion>()
        var gapPointerHour = (Math.ceil(((currentHour + MIN_RESCHEDULE_BUFFER_HOURS) * 4).toDouble()) / 4.0).toFloat().coerceAtLeast(6f)

        for (task in candidateTasks) {
            // Fixed events (like lectures/exams) shouldn't be automatically moved unless user requested
            if (task.isFixed && !task.needsRescheduling) {
                continue
            }

            // Determine remaining required duration
            val remainingDurationMinutes = if (task.actualDurationMinutes in 1 until task.durationMinutes) {
                (task.durationMinutes - task.actualDurationMinutes).coerceAtLeast(20)
            } else {
                task.durationMinutes
            }
            val remainingDurationHours = remainingDurationMinutes / 60f

            // Check if day is full (Capacity Overflow Protection)
            if (isOvercrowded && !task.isFixed) {
                suggestions.add(
                    RescheduleSuggestion(
                        task = task,
                        newStartTime = "09:00",
                        newEndTime = "10:30",
                        newScheduledTime = "09:00 - 10:30 ص (غداً)",
                        newDurationMinutes = remainingDurationMinutes,
                        reason = "سعة اليوم ممتلئة (${capacityState.percentage}%) - اقتراح تأجيل المهمة إلى الغد لتجنب الإرهاق",
                        isPostponeToTomorrow = true,
                        isMoveToLater = false,
                        availableGapMinutes = null
                    )
                )
                continue
            }

            // Look for a suitable focus gap today
            val matchingGap = availableGaps.firstOrNull { gap ->
                val gapEffectiveStart = maxOf(gap.startHour, gapPointerHour)
                val gapEffectiveEnd = gap.startHour + gap.durationHours
                (gapEffectiveEnd - gapEffectiveStart) >= (remainingDurationHours * 0.75f) // At least 75% fits
            }

            if (matchingGap != null) {
                val startH = maxOf(matchingGap.startHour, gapPointerHour)
                val endH = (startH + remainingDurationHours).coerceAtMost(24f)
                val startTimeFormatted = formatHourToArabicTime(startH)
                val endTimeFormatted = formatHourToArabicTime(endH)
                val scheduledTimeRange = "$startTimeFormatted - $endTimeFormatted"

                val isPartial = task.actualDurationMinutes in 1 until task.durationMinutes
                val reasonText = if (isPartial) {
                    "جلسة غير مكتملة (متبقي $remainingDurationMinutes دقيقة) - إعادة توزيع المتبقي في فترة تركيز متاحة اليوم"
                } else if (task.needsRescheduling) {
                    "طلب إعادة جدولة - تم تخصيص أقرب نافذة تركيز متاحة ($startTimeFormatted)"
                } else {
                    "تجاوز وقت البدء المحدد - نقل المهمة إلى وقت متاح اليوم ($startTimeFormatted)"
                }

                suggestions.add(
                    RescheduleSuggestion(
                        task = task,
                        newStartTime = formatHourTo24h(startH),
                        newEndTime = formatHourTo24h(endH),
                        newScheduledTime = scheduledTimeRange,
                        newDurationMinutes = remainingDurationMinutes,
                        reason = reasonText,
                        isPostponeToTomorrow = false,
                        isMoveToLater = false,
                        availableGapMinutes = matchingGap.durationMinutes
                    )
                )

                // Advance gap pointer for next task
                gapPointerHour = (endH + 0.25f).coerceAtMost(24f)
            } else {
                // No room left today -> propose moving to tomorrow
                suggestions.add(
                    RescheduleSuggestion(
                        task = task,
                        newStartTime = "09:00",
                        newEndTime = "10:30",
                        newScheduledTime = "09:00 - 10:30 ص (غداً)",
                        newDurationMinutes = remainingDurationMinutes,
                        reason = "لا توجد فترات تركيز كافية متبقية اليوم - اقتراح النقل إلى الغد الساعة 09:00 ص",
                        isPostponeToTomorrow = true,
                        isMoveToLater = false,
                        availableGapMinutes = null
                    )
                )
            }
        }

        return suggestions
    }

    /**
     * Identifies tasks that missed their scheduled window, were partially done,
     * or are explicitly flagged as needing rescheduling.
     */
    private fun findTasksNeedingReschedule(
        todayTasks: List<Task>,
        currentHour: Float,
        todayStart: Long
    ): List<Task> {
        return todayTasks.filter { task ->
            if (task.isCompleted) return@filter false

            // Explicitly flagged
            if (task.needsRescheduling) return@filter true

            // Past unfinished task from previous days
            if (task.date < todayStart) return@filter true

            // Partial session that stopped earlier
            if (task.actualDurationMinutes in 1 until task.durationMinutes) {
                return@filter true
            }

            // Check if scheduled end time has passed
            val endHourFloat = SchedulingEngine.parseTimeToHourFloat(task.endTime)
                ?: task.startTime?.let { SchedulingEngine.parseTimeToHourFloat(it)?.plus(task.durationMinutes / 60f) }
                ?: task.scheduledTime.split("-").lastOrNull()?.let { SchedulingEngine.parseTimeToHourFloat(it) }

            if (endHourFloat != null && currentHour > (endHourFloat + 0.25f)) {
                return@filter true
            }

            false
        }
    }

    /**
     * Format decimal hour to 24h format (e.g. 14.5f -> "14:30")
     */
    fun formatHourTo24h(hourFloat: Float): String {
        val h = hourFloat.toInt().coerceIn(0, 23)
        val m = (((hourFloat - h) * 60).toInt() / 5 * 5).coerceIn(0, 59)
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    /**
     * Format decimal hour to Arabic 12h format (e.g. 14.5f -> "02:30 م", 9.0f -> "09:00 ص")
     */
    fun formatHourToArabicTime(hourFloat: Float): String {
        val totalMinutes = (hourFloat * 60).toInt()
        val rawHour = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60

        val isPM = rawHour >= 12
        val displayHour = when {
            rawHour == 0 -> 12
            rawHour > 12 -> rawHour - 12
            else -> rawHour
        }
        val period = if (isPM) "م" else "ص"

        return String.format(Locale.US, "%02d:%02d %s", displayHour, minutes, period)
    }

    private fun getDayTimeBounds(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }
}
