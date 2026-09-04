package com.example.engine

import com.example.model.BehavioralInsight
import com.example.model.DailyCapacityState
import com.example.model.InsightType
import com.example.model.TaskItem

object MotivationEngine {

    /**
     * Generates a context-aware behavioral insight based on real activity data,
     * capacity levels, task completion rates, and delay patterns.
     */
    fun generateInsight(
        capacityState: DailyCapacityState,
        completedCount: Int,
        totalScheduledCount: Int,
        focusMinutesLogged: Int,
        repeatedlyPostponedTasks: List<TaskItem> = emptyList(),
        hasLateStart: Boolean = false,
        hasMomentumRecovery: Boolean = false
    ): BehavioralInsight {
        // 1. Repeated Postponement Rule (Task postponed 3+ times)
        val severePostponedTask = repeatedlyPostponedTasks.firstOrNull { it.postponedCount >= 3 }
        if (severePostponedTask != null) {
            val taskLabel = if (severePostponedTask.title.isNotBlank()) " \"${severePostponedTask.title}\"" else ""
            return BehavioralInsight(
                title = "تأجيل متكرر للمهمة",
                message = "هذه المهمة$taskLabel اتأجلت ${severePostponedTask.postponedCount} مرات. حاول تقسيمها لمهمتين أصغر لتسهيل البدء.",
                insightType = InsightType.REPEATED_POSTPONEMENT,
                categoryName = "تحليل سلوكي للتأجيل"
            )
        }

        // 2. Overloaded Day Rule (Daily capacity > 90% or packed beyond safety threshold)
        if (capacityState.percentage >= 90 || capacityState.isOverbooked) {
            return BehavioralInsight(
                title = "يوم عالي الكثافة (${capacityState.percentage}%)",
                message = "المشكلة لم تكن فيك. الخطة كانت أكبر من وقتك المتاح. خفف المهام غير العاجلة لحماية طاقتك من الإرهاق.",
                insightType = InsightType.OVERLOADED_DAY,
                categoryName = "حماية من الإرهاق"
            )
        }

        // 3. Momentum Recovery Rule (Started task after delay or resuming paused flow)
        if (hasMomentumRecovery || (hasLateStart && (focusMinutesLogged > 0 || completedCount > 0))) {
            return BehavioralInsight(
                title = "استعادة الزخم",
                message = "المهم إنك بدأت الآن. استمر من هنا وبخطوات تركيز متتالية.",
                insightType = InsightType.MOMENTUM_RECOVERY,
                categoryName = "زخم التركيز"
            )
        }

        // 4. High Execution Rule (Completed 3+ tasks)
        if (completedCount >= 3) {
            return BehavioralInsight(
                title = "أداء وإنجاز ممتاز",
                message = "ممتاز! أنت أنجزت ما كان مخططاً له اليوم وحافظت على إنتاجيتك العالية.",
                insightType = InsightType.HIGH_EXECUTION,
                categoryName = "تحفيز الإنجاز"
            )
        }

        // 5. Significant Focus Time Logged
        if (focusMinutesLogged >= 45) {
            val hours = focusMinutesLogged / 60
            val mins = focusMinutesLogged % 60
            val timeText = if (hours > 0) "${hours}س ${mins}د" else "${mins}د"
            return BehavioralInsight(
                title = "ساعات تركيز عميق ($timeText)",
                message = "استثمارك $timeText في التركيز الحقيقي يصنع فارقاً تراكمياً كبيراً. استمر بنفس النسق.",
                insightType = InsightType.HIGH_EXECUTION,
                categoryName = "زخم التركيز"
            )
        }

        // 6. Default / Balanced Progress Rule
        return BehavioralInsight(
            title = "خطوات ثابتة وتركيز مستمر",
            message = "النجاح ليس بالصدفة. بل هو نتيجة للتركيز اليومي على ما يهم حقاً. ابدأ بجلسة تركيز قصيرة.",
            insightType = InsightType.BALANCED_PROGRESS,
            categoryName = "محرك التحفيز السلوكي"
        )
    }
}
