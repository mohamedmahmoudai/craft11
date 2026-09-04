package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FocusCraftDatabase
import com.example.data.local.dao.GoalDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.Project
import com.example.data.local.entity.Task
import com.example.data.repository.FocusRepository
import com.example.engine.CapacityExceededException
import com.example.engine.SchedulingEngine
import com.example.model.TimelineItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusCraftRoomTest {

    private lateinit var db: FocusCraftDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var projectDao: ProjectDao
    private lateinit var goalDao: GoalDao
    private lateinit var repository: FocusRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FocusCraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = db.taskDao()
        projectDao = db.projectDao()
        goalDao = db.goalDao()
        repository = FocusRepository(taskDao, projectDao, goalDao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadTask() = runBlocking {
        val task = Task(
            id = "test-task-1",
            title = "جلسة برمجة الغرفة المحلية",
            subtitle = "إعداد قاعدة البيانات",
            durationMinutes = 45,
            scheduledTime = "10:00 ص",
            isCompleted = false,
            date = System.currentTimeMillis(),
            categoryColor = "INDIGO",
            isFixed = false
        )
        repository.insertTask(task)

        val allTasks = repository.allTasks.first()
        assertEquals(1, allTasks.size)
        assertEquals("جلسة برمجة الغرفة المحلية", allTasks[0].title)
        assertFalse(allTasks[0].isCompleted)
    }

    @Test
    fun toggleTaskCompletion() = runBlocking {
        val task = Task(
            id = "test-task-2",
            title = "مهمة للتجربة",
            subtitle = "قيد الانتظار",
            durationMinutes = 60,
            scheduledTime = "02:00 م",
            isCompleted = false,
            date = System.currentTimeMillis(),
            categoryColor = "ORANGE"
        )
        repository.insertTask(task)

        repository.toggleTaskCompletion("test-task-2")
        val updatedTasks = repository.allTasks.first()
        assertTrue(updatedTasks[0].isCompleted)

        repository.toggleTaskCompletion("test-task-2")
        val revertedTasks = repository.allTasks.first()
        assertFalse(revertedTasks[0].isCompleted)
    }

    @Test
    fun deleteTask() = runBlocking {
        val task = Task(
            id = "test-task-3",
            title = "مهمة قابلة للحذف",
            subtitle = "مؤقتة",
            durationMinutes = 30,
            scheduledTime = "04:00 م",
            isCompleted = false,
            date = System.currentTimeMillis(),
            categoryColor = "GREEN"
        )
        repository.insertTask(task)
        assertEquals(1, repository.allTasks.first().size)

        repository.deleteTask("test-task-3")
        assertEquals(0, repository.allTasks.first().size)
    }

    @Test
    fun testSchedulingEngineCapacityCalculation() {
        val tasks = listOf(
            Task(id = "1", title = "Lecture", durationMinutes = 120, isFixed = true),
            Task(id = "2", title = "Study", durationMinutes = 180, isFixed = false)
        )
        val capacity = SchedulingEngine.calculateDailyCapacity(tasks)
        assertEquals(300, capacity.bookedMinutes)
        assertEquals(960, capacity.totalAwakeMinutes)
        assertEquals("5h 00m", capacity.bookedTimeFormatted)
        assertEquals("11h 00m", capacity.freeTimeFormatted)
        assertFalse(capacity.isOverbooked)
    }

    @Test(expected = CapacityExceededException::class)
    fun testSchedulingEngineOverbookingConflict() {
        val tasks = listOf(
            Task(id = "1", title = "Full Day", durationMinutes = 900)
        )
        SchedulingEngine.validateScheduleCapacity(tasks, 100) // 900 + 100 = 1000 > 960
    }

    @Test
    fun testScheduleBlocksWithGapsGeneration() {
        val tasks = listOf(
            Task(id = "1", title = "Fixed Event", durationMinutes = 90, isFixed = true)
        )
        val blocks = SchedulingEngine.generateScheduleBlocksWithGaps(tasks)
        assertTrue(blocks.any { it.isFixed })
        assertTrue(blocks.any { it.isAvailableGap })
    }

    @Test
    fun testTimelineItemsGenerationWithFocusGaps() {
        val tasks = listOf(
            Task(
                id = "t1",
                title = "محاضرة",
                scheduledTime = "09:00 - 10:30",
                durationMinutes = 90,
                isFixed = true,
                startTime = "09:00",
                endTime = "10:30"
            ),
            Task(
                id = "t2",
                title = "برمجة",
                scheduledTime = "13:00 - 15:00",
                durationMinutes = 120,
                isFixed = false,
                startTime = "13:00",
                endTime = "15:00"
            )
        )

        val timeline = SchedulingEngine.generateTimelineItems(tasks)
        assertTrue(timeline.any { it is TimelineItem.TaskBlock && it.isFixed })
        assertTrue(timeline.any { it is TimelineItem.TaskBlock && !it.isFixed })
        assertTrue(timeline.any { it is TimelineItem.FocusGap })
    }

    @Test
    fun testBuildWeekDaysWithCapacity() {
        val now = System.currentTimeMillis()
        val tasks = listOf(
            Task(id = "w1", title = "Task 1", durationMinutes = 120, date = now)
        )
        val weekDays = SchedulingEngine.buildWeekDays(now, now, tasks)
        assertEquals(7, weekDays.size)
        assertTrue(weekDays.any { it.isSelected })
    }

    @Test
    fun testAdaptivePlannerGeneratesSuggestionsForMissedTask() = runBlocking {
        val missedTask = Task(
            id = "missed-1",
            title = "مذاكرة هياكل البيانات",
            durationMinutes = 60,
            startTime = "08:00",
            endTime = "09:00",
            scheduledTime = "08:00 - 09:00",
            needsRescheduling = true,
            isCompleted = false,
            date = System.currentTimeMillis()
        )
        val suggestions = com.example.engine.AdaptivePlanner.generateSuggestions(
            tasks = listOf(missedTask)
        )
        assertEquals(1, suggestions.size)
        assertEquals("missed-1", suggestions[0].task.id)
        assertTrue(suggestions[0].newStartTime.isNotEmpty())
    }

    @Test
    fun testPostponeTaskToTomorrow() = runBlocking {
        val task = Task(
            id = "postpone-1",
            title = "مشروع التخرج",
            durationMinutes = 90,
            scheduledTime = "14:00 - 15:30",
            isCompleted = false,
            date = System.currentTimeMillis()
        )
        repository.insertTask(task)
        repository.postponeTaskToTomorrow("postpone-1")

        val all = repository.allTasks.first()
        val updated = all.find { it.id == "postpone-1" }
        assertNotNull(updated)
        assertEquals(1, updated!!.postponedCount)
        assertFalse(updated.needsRescheduling)
    }

    @Test
    fun testMotivationEngine_repeatedPostponementRule() {
        val taskItem = com.example.model.TaskItem(
            id = "t-postponed",
            title = "كتابة تقرير الذكاء الاصطناعي",
            postponedCount = 3
        )
        val insight = com.example.engine.MotivationEngine.generateInsight(
            capacityState = com.example.model.DailyCapacityState(bookedRatio = 0.5f),
            completedCount = 1,
            totalScheduledCount = 4,
            focusMinutesLogged = 25,
            repeatedlyPostponedTasks = listOf(taskItem)
        )
        assertEquals(com.example.model.InsightType.REPEATED_POSTPONEMENT, insight.insightType)
        assertTrue(insight.message.contains("3"))
    }

    @Test
    fun testMotivationEngine_overloadedDayRule() {
        val insight = com.example.engine.MotivationEngine.generateInsight(
            capacityState = com.example.model.DailyCapacityState(bookedRatio = 0.95f),
            completedCount = 0,
            totalScheduledCount = 8,
            focusMinutesLogged = 0
        )
        assertEquals(com.example.model.InsightType.OVERLOADED_DAY, insight.insightType)
        assertTrue(insight.message.contains("الخطة كانت أكبر من وقتك"))
    }

    @Test
    fun testMotivationEngine_highExecutionRule() {
        val insight = com.example.engine.MotivationEngine.generateInsight(
            capacityState = com.example.model.DailyCapacityState(bookedRatio = 0.6f),
            completedCount = 4,
            totalScheduledCount = 5,
            focusMinutesLogged = 120
        )
        assertEquals(com.example.model.InsightType.HIGH_EXECUTION, insight.insightType)
        assertTrue(insight.message.contains("ممتاز"))
    }

    @Test
    fun testMotivationEngine_momentumRecoveryRule() {
        val insight = com.example.engine.MotivationEngine.generateInsight(
            capacityState = com.example.model.DailyCapacityState(bookedRatio = 0.4f),
            completedCount = 1,
            totalScheduledCount = 3,
            focusMinutesLogged = 25,
            hasMomentumRecovery = true
        )
        assertEquals(com.example.model.InsightType.MOMENTUM_RECOVERY, insight.insightType)
        assertTrue(insight.message.contains("المهم إنك بدأت الآن"))
    }
}
