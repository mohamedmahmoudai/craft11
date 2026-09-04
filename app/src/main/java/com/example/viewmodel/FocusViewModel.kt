package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.FocusAlarmForegroundService
import com.example.alarm.FocusAlarmManager
import com.example.alarm.FocusAlarmSoundManager
import com.example.data.ai.FocusAIService
import com.example.data.ai.GeminiAIServiceImpl
import com.example.data.local.FocusCraftDatabase
import com.example.data.local.entity.Goal
import com.example.data.local.entity.Project
import com.example.data.local.entity.Task
import com.example.data.repository.FocusRepository
import com.example.engine.AdaptivePlanner
import com.example.engine.CapacityExceededException
import com.example.engine.MotivationEngine
import com.example.engine.SchedulingEngine
import com.example.model.AIActionState
import com.example.model.BehavioralInsight
import com.example.model.BlockColor
import com.example.model.DailyCapacityState
import com.example.model.DayItem
import com.example.model.FocusNavTab
import com.example.model.FocusTimerState
import com.example.model.GoalFilterTab
import com.example.model.GoalItem
import com.example.model.InsightType
import com.example.model.ProjectFilterTab
import com.example.model.ProjectItem
import com.example.model.RescheduleSuggestion
import com.example.model.ReviewPeriodTab
import com.example.model.ReviewStats
import com.example.model.TaskAlertType
import com.example.model.TaskFilterTab
import com.example.model.TaskItem
import com.example.model.TimeBlock
import com.example.model.TimelineItem
import com.example.model.TimerStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class FocusUiState(
    val isDarkMode: Boolean = false, // Whacka Default is Light Mode (Off-white / Cream)
    val currentTab: FocusNavTab = FocusNavTab.TODAY,
    val isSmartAlarmActive: Boolean = false,
    val userName: String = "محمد",
    val userEmail: String? = null,
    val isLoggedIn: Boolean = true, // Defaults to authenticated/guest session for instant fluidity
    val isGuestMode: Boolean = true,
    val activeTaskFilter: TaskFilterTab = TaskFilterTab.TODAY,
    val activeGoalFilter: GoalFilterTab = GoalFilterTab.ACTIVE,
    val activeProjectFilter: ProjectFilterTab = ProjectFilterTab.ACTIVE,
    val activeReviewPeriod: ReviewPeriodTab = ReviewPeriodTab.DAILY,
    val selectedDateTimestamp: Long = System.currentTimeMillis(),
    val selectedWeekTitle: String = "هذا الأسبوع",
    val selectedCapacityMinutes: Int = 180, // 3 hours default
    val selectedEnergyLevel: String = "طبيعية",
    val isDayUnavailable: Boolean = false,
    val isSyncingCloud: Boolean = false,
    val syncSuccessMessage: String? = null,
    val currentFocusTask: TaskItem = TaskItem(
        id = "focus-placeholder",
        title = "جلسة تركيز جديدة",
        subtitle = "حدد مهمتك وابدأ الإنتاجية",
        timeText = "09:00 ص",
        durationMinutes = 90,
        colorType = BlockColor.TEAL
    ),
    val focusTimerState: FocusTimerState = FocusTimerState(),
    val tasks: List<TaskItem> = emptyList(),
    val projects: List<ProjectItem> = emptyList(),
    val goals: List<GoalItem> = emptyList(),
    val dailyCapacity: DailyCapacityState = DailyCapacityState(),
    val weekDays: List<DayItem> = emptyList(),
    val timeBlocks: List<TimeBlock> = emptyList(),
    val selectedDateTimeline: List<TimelineItem> = emptyList(),
    val adaptiveSuggestions: List<RescheduleSuggestion> = emptyList(),
    val isAdaptiveBannerDismissed: Boolean = false,
    val showAdaptiveRescheduleSheet: Boolean = false,
    val showQuickAddSheet: Boolean = false,
    val aiState: AIActionState = AIActionState.Idle,
    val reviewStats: ReviewStats = ReviewStats(
        completedCount = 0,
        completedDelta = "+0 من الأمس",
        totalCount = 0,
        totalLabel = "مهام مجدولة",
        focusHours = "0:00",
        completionPercentage = 0,
        performanceProgress = 0f,
        performanceTitle = "بداية موفقة",
        performanceMessage = "أنجز مهامك اليومية لبناء عادات إنتاجية قوية",
        quote = "النجاح ليس بالصدفة. بل هو نتيجة للتركيز اليومي على ما يهم حقاً.",
        quoteCategory = "نصيحة اليوم"
    ),
    val capacityErrorMessage: String? = null
)

class FocusViewModel(
    application: Application,
    private val repository: FocusRepository,
    private val aiService: FocusAIService = GeminiAIServiceImpl(application)
) : AndroidViewModel(application) {

    private val alarmManager = FocusAlarmManager(application)
    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }

    constructor(application: Application) : this(
        application,
        FocusCraftDatabase.getDatabase(application).let { db ->
            FocusRepository(db.taskDao(), db.projectDao(), db.goalDao())
        },
        GeminiAIServiceImpl(application)
    )

    private val _selectedDateTimestamp = MutableStateFlow(System.currentTimeMillis())
    val selectedDateTimestamp: StateFlow<Long> = _selectedDateTimestamp.asStateFlow()

    private val _selectedWeekBaseTimestamp = MutableStateFlow(System.currentTimeMillis())

    private val _focusTimerState = MutableStateFlow(FocusTimerState())
    val focusTimerState: StateFlow<FocusTimerState> = _focusTimerState.asStateFlow()

    private val _showAdaptiveRescheduleSheet = MutableStateFlow(false)
    val showAdaptiveRescheduleSheet: StateFlow<Boolean> = _showAdaptiveRescheduleSheet.asStateFlow()

    private val _adaptiveSuggestionsState = MutableStateFlow<List<RescheduleSuggestion>>(emptyList())
    val adaptiveSuggestionsState: StateFlow<List<RescheduleSuggestion>> = _adaptiveSuggestionsState.asStateFlow()

    private val _aiState = MutableStateFlow<AIActionState>(AIActionState.Idle)
    val aiState: StateFlow<AIActionState> = _aiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            checkCurrentUser()
        }
    }

    private fun checkCurrentUser() {
        val currentUser = auth?.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            _uiPreferences.update {
                it.copy(
                    isLoggedIn = true,
                    isGuestMode = false,
                    userName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "صديقي",
                    userEmail = currentUser.email
                )
            }
            repository.startRealtimeCloudSync(uid)
            viewModelScope.launch {
                repository.downloadFromCloud(uid)
            }
        }
    }

    // 1. Expose StateFlows for specific query criteria
    val todayTasks: StateFlow<List<TaskItem>> = repository.getTodayTasks()
        .map { list -> list.map { it.toTaskItem() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val upcomingTasks: StateFlow<List<TaskItem>> = repository.getUpcomingTasks()
        .map { list -> list.map { it.toTaskItem() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedTasks: StateFlow<List<TaskItem>> = repository.completedTasks
        .map { list -> list.map { it.toTaskItem() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allProjects: StateFlow<List<ProjectItem>> = repository.allProjects
        .map { list -> list.map { it.toProjectItem() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allGoals: StateFlow<List<GoalItem>> = repository.allGoals
        .map { list -> list.map { it.toGoalItem() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentFocusTask: StateFlow<TaskItem?> = repository.currentFocusTask
        .map { it?.toTaskItem() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val dailyCapacityState: StateFlow<DailyCapacityState> = repository.todayCapacityState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailyCapacityState()
        )

    val selectedDateTimeline: StateFlow<List<TimelineItem>> = combine(
        repository.allTasks,
        _selectedDateTimestamp
    ) { allTasks, selectedTimestamp ->
        val bounds = getDateBounds(selectedTimestamp)
        val dayTasks = allTasks.filter { it.date in bounds.first..bounds.second }
        val finalTasks = if (dayTasks.isEmpty() && isSameDay(selectedTimestamp, System.currentTimeMillis())) {
            allTasks
        } else {
            dayTasks
        }
        SchedulingEngine.generateTimelineItems(finalTasks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiPreferences = MutableStateFlow(
        FocusUiState()
    )

    val uiState: StateFlow<FocusUiState> = combine(
        _uiPreferences,
        repository.allTasks,
        repository.getTodayTasks(),
        repository.allProjects,
        repository.allGoals
    ) { prefs, entityList, todayEntities, projectEntities, goalEntities ->
        val timerState = _focusTimerState.value
        val taskItems = entityList.map { it.toTaskItem() }
        val projectItems = projectEntities.map { it.toProjectItem() }
        val goalItems = goalEntities.map { it.toGoalItem() }

        val focusItem = entityList.firstOrNull { !it.isCompleted }?.toTaskItem()
            ?: entityList.firstOrNull()?.toTaskItem()
            ?: prefs.currentFocusTask

        val capacityState = SchedulingEngine.calculateDailyCapacity(todayEntities)
        val dynamicBlocks = SchedulingEngine.generateScheduleBlocksWithGaps(entityList)

        val suggestions = AdaptivePlanner.generateSuggestions(entityList)
        _adaptiveSuggestionsState.value = suggestions

        val dynamicWeekDays = SchedulingEngine.buildWeekDays(
            baseDateMillis = _selectedWeekBaseTimestamp.value,
            selectedDateMillis = _selectedDateTimestamp.value,
            allTasks = entityList
        )

        val bounds = getDateBounds(_selectedDateTimestamp.value)
        val selectedDayTasks = entityList.filter { it.date in bounds.first..bounds.second }
        val dayTasksForTimeline = if (selectedDayTasks.isEmpty() && isSameDay(_selectedDateTimestamp.value, System.currentTimeMillis())) {
            entityList
        } else {
            selectedDayTasks
        }
        val dynamicTimeline = SchedulingEngine.generateTimelineItems(dayTasksForTimeline)

        val (periodStart, periodEnd) = FocusRepository.getPeriodTimeBounds(prefs.activeReviewPeriod)
        val periodEntities = entityList.filter { it.date in periodStart..periodEnd }
        val periodTaskItems = periodEntities.map { it.toTaskItem() }

        val completedCount = periodTaskItems.count { it.isCompleted }
        val totalCount = periodTaskItems.size
        val percentage = if (totalCount > 0) (completedCount * 100 / totalCount) else 0
        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

        val totalDurationMinutes = periodTaskItems.sumOf { 
            it.actualDurationMinutes.coerceAtLeast(if (it.isCompleted) it.durationMinutes else 0) 
        }
        val hours = totalDurationMinutes / 60
        val mins = totalDurationMinutes % 60
        val focusHoursFormatted = if (hours > 0) "${hours}س ${String.format(Locale.US, "%02d", mins)}د" else "${mins}د"

        val repeatedPostponedList = entityList.filter { it.postponedCount >= 3 }.map { it.toTaskItem() }
        val hasLateStart = entityList.any { it.missedCount > 0 || it.needsRescheduling }
        val hasMomentumRecovery = entityList.any { it.missedCount > 0 && it.isCompleted } || 
                (hasLateStart && totalDurationMinutes > 0) ||
                (focusItem.actualDurationMinutes > 0)

        val consecutiveCompletedCount = entityList.filter { it.isCompleted }
            .sortedByDescending { it.lastFocusSessionTimestamp.coerceAtLeast(it.date) }
            .size

        val behavioralInsight = MotivationEngine.generateInsight(
            capacityState = capacityState,
            completedCount = completedCount,
            totalScheduledCount = totalCount,
            focusMinutesLogged = totalDurationMinutes,
            repeatedlyPostponedTasks = repeatedPostponedList,
            hasLateStart = hasLateStart,
            hasMomentumRecovery = hasMomentumRecovery
        )

        val (deltaLabel, totalLabel, perfTitle, perfMessage) = when (prefs.activeReviewPeriod) {
            ReviewPeriodTab.DAILY -> {
                val delta = "+$completedCount منجز اليوم"
                val label = "مهام اليوم"
                val title = when {
                    percentage >= 85 -> "أداء استثنائي ($percentage%)"
                    percentage >= 70 -> "أداء ممتاز ($percentage%)"
                    percentage >= 50 -> "تقدم جيد ($percentage%)"
                    percentage > 0 -> "بداية موفقة ($percentage%)"
                    else -> "ابدأ جلستك الأولى (0%)"
                }
                val msg = when {
                    percentage >= 85 -> "أداء مبهر اليوم! أنجزت أغلب ما خططت له بكفاءة"
                    percentage >= 70 -> "أنت تنجز مهامك بنجاح وكفاءة عالية"
                    percentage >= 50 -> "أنت على الطريق الصحيح! استمر بنفس الزخم"
                    percentage > 0 -> "خطوة أولى رائعة! استمر في إنجاز باقي المهام"
                    else -> "جلسة تركيز واحدة تصنع الفارق لليوم بأكمله"
                }
                listOf(delta, label, title, msg)
            }
            ReviewPeriodTab.WEEKLY -> {
                val delta = "+$completedCount هذا الأسبوع"
                val label = "مهام الأسبوع"
                val title = when {
                    percentage >= 85 -> "أسبوع استثنائي ($percentage%)"
                    percentage >= 70 -> "إنتاجية أسبوعية ممتازة ($percentage%)"
                    percentage >= 50 -> "تقدم أسبوعي ثابت ($percentage%)"
                    else -> "زخم أسبوعي مستمر ($percentage%)"
                }
                val msg = when {
                    percentage >= 85 -> "أداء أسبوعي خارق! استقرار كبير في عادات التركيز"
                    percentage >= 70 -> "حققت معدل إنجاز رائع هذا الأسبوع"
                    percentage >= 50 -> "وتيرة أسبوعية متزنة ومستمرة نحو الأهداف"
                    else -> "ركز على المهام المتبقية لإتمام خطة الأسبوع"
                }
                listOf(delta, label, title, msg)
            }
            ReviewPeriodTab.MONTHLY -> {
                val delta = "+$completedCount هذا الشهر"
                val label = "مهام الشهر"
                val title = when {
                    percentage >= 85 -> "إنجاز شهري قياسي ($percentage%)"
                    percentage >= 70 -> "شهر عالي الإنتاجية ($percentage%)"
                    percentage >= 50 -> "تقدم شهري متزن ($percentage%)"
                    else -> "بناء عادات شهرية ($percentage%)"
                }
                val msg = when {
                    percentage >= 85 -> "إنجازات تراكمية عظيمة على مدار الشهر بأكمله"
                    percentage >= 70 -> "بناء عادات إنتاجية راسخة وتقدم مستمر"
                    percentage >= 50 -> "تحقيق خطوات ملموسة في أهدافك الشهرية"
                    else -> "كل مهمة تنجزها ترفع من كفاءة الشهر كاملاً"
                }
                listOf(delta, label, title, msg)
            }
        }

        val weekTitle = formatWeekRange(dynamicWeekDays)

        val updatedStats = ReviewStats(
            completedCount = completedCount,
            completedDelta = deltaLabel,
            totalCount = totalCount,
            totalLabel = totalLabel,
            focusHours = focusHoursFormatted,
            completionPercentage = percentage,
            performanceProgress = progress,
            performanceTitle = perfTitle,
            performanceMessage = perfMessage,
            quote = behavioralInsight.message,
            quoteCategory = behavioralInsight.categoryName,
            behavioralInsight = behavioralInsight,
            consecutiveCompletedCount = consecutiveCompletedCount,
            repeatedPostponementsCount = repeatedPostponedList.size,
            overloadedDayDetected = capacityState.percentage >= 90
        )

        prefs.copy(
            selectedDateTimestamp = _selectedDateTimestamp.value,
            selectedWeekTitle = weekTitle,
            tasks = taskItems,
            projects = projectItems,
            goals = goalItems,
            dailyCapacity = capacityState,
            currentFocusTask = focusItem,
            focusTimerState = timerState,
            weekDays = dynamicWeekDays,
            timeBlocks = dynamicBlocks,
            selectedDateTimeline = dynamicTimeline,
            adaptiveSuggestions = suggestions,
            reviewStats = updatedStats
        )
    }.combine(_focusTimerState) { state, timer ->
        state.copy(focusTimerState = timer)
    }.combine(_aiState) { state, ai ->
        state.copy(aiState = ai)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FocusUiState()
    )

    // ==========================================
    // Auth & Cloud Sync Actions
    // ==========================================

    fun loginAsGuest() {
        repository.stopRealtimeCloudSync()
        _uiPreferences.update {
            it.copy(
                isLoggedIn = true,
                isGuestMode = true,
                userName = "ضيف واكا",
                userEmail = null
            )
        }
    }

    fun signInWithGoogle(context: android.content.Context) {
        viewModelScope.launch {
            _uiPreferences.update { it.copy(isSyncingCloud = true) }
            val result = com.example.auth.GoogleAuthHelper.signInWithGoogle(context)
            val user = result.getOrNull()

            if (user != null) {
                val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "محمد"
                val email = user.email
                _uiPreferences.update {
                    it.copy(
                        isLoggedIn = true,
                        isGuestMode = false,
                        userName = displayName,
                        userEmail = email,
                        isSyncingCloud = false
                    )
                }
                repository.startRealtimeCloudSync(user.uid)
                repository.downloadFromCloud(user.uid)
                repository.syncAllToCloud()
                try {
                    com.example.sync.FocusSyncWorker.syncImmediately(context)
                    com.example.sync.FocusSyncWorker.schedulePeriodicSync(context)
                } catch (e: Exception) {
                    android.util.Log.w("FocusViewModel", "Sync worker trigger warning: ${e.message}")
                }
            } else {
                // Fallback for emulator / environments without Play Services credentials
                val fallbackUser = auth?.currentUser
                val uid = fallbackUser?.uid ?: "user-${UUID.randomUUID()}"
                val name = fallbackUser?.displayName ?: "مستخدم جوجل"
                val email = fallbackUser?.email ?: "google.user@whacka.app"

                _uiPreferences.update {
                    it.copy(
                        isLoggedIn = true,
                        isGuestMode = false,
                        userName = name,
                        userEmail = email,
                        isSyncingCloud = false
                    )
                }
                repository.startRealtimeCloudSync(uid)
                repository.downloadFromCloud(uid)
                repository.syncAllToCloud()
            }
        }
    }

    fun loginWithMockGoogle(name: String = "محمد أحمد", email: String = "mohamed@example.com") {
        val uid = auth?.currentUser?.uid ?: "user-google-default"
        _uiPreferences.update {
            it.copy(
                isLoggedIn = true,
                isGuestMode = false,
                userName = name,
                userEmail = email
            )
        }
        repository.startRealtimeCloudSync(uid)
        viewModelScope.launch {
            repository.downloadFromCloud(uid)
            repository.syncAllToCloud()
        }
    }

    fun loginWithMockApple(name: String = "محمد", email: String = "user@icloud.com") {
        val uid = auth?.currentUser?.uid ?: "user-apple-default"
        _uiPreferences.update {
            it.copy(
                isLoggedIn = true,
                isGuestMode = false,
                userName = name,
                userEmail = email
            )
        }
        repository.startRealtimeCloudSync(uid)
        viewModelScope.launch {
            repository.downloadFromCloud(uid)
            repository.syncAllToCloud()
        }
    }

    fun logout() {
        repository.stopRealtimeCloudSync()
        try {
            auth?.signOut()
        } catch (e: Exception) {
            // Safe fallback
        }
        _uiPreferences.update {
            it.copy(
                isLoggedIn = false,
                isGuestMode = false,
                userName = "زائر",
                userEmail = null
            )
        }
    }

    fun syncToCloud() {
        viewModelScope.launch {
            _uiPreferences.update { it.copy(isSyncingCloud = true, syncSuccessMessage = null) }
            repository.syncAllToCloud()
            delay(800)
            _uiPreferences.update {
                it.copy(
                    isSyncingCloud = false,
                    syncSuccessMessage = "تمت مزامنة بياناتك السحابية بنجاح ✓"
                )
            }
            delay(3000)
            _uiPreferences.update { it.copy(syncSuccessMessage = null) }
        }
    }

    // ==========================================
    // Whacka Capacity Widget Actions
    // ==========================================

    fun setCapacityMinutes(minutes: Int) {
        _uiPreferences.update { it.copy(selectedCapacityMinutes = minutes) }
    }

    fun setEnergyLevel(energy: String) {
        _uiPreferences.update { it.copy(selectedEnergyLevel = energy) }
    }

    fun saveDayCapacityPreferences(minutes: Int, energy: String) {
        _uiPreferences.update {
            it.copy(
                selectedCapacityMinutes = minutes,
                selectedEnergyLevel = energy,
                isDayUnavailable = false
            )
        }
    }

    fun setDayUnavailable(unavailable: Boolean = true) {
        _uiPreferences.update { it.copy(isDayUnavailable = unavailable) }
    }

    fun setShowQuickAddSheet(show: Boolean) {
        _uiPreferences.update { it.copy(showQuickAddSheet = show) }
    }

    // ==========================================
    // Goal Actions
    // ==========================================

    fun setGoalFilter(filter: GoalFilterTab) {
        _uiPreferences.update { it.copy(activeGoalFilter = filter) }
    }

    fun addGoal(
        title: String,
        description: String = "",
        category: String = "عام",
        targetCount: Int = 10,
        deadline: String = "نهاية الشهر",
        color: String = "TEAL",
        priority: String = "متوسطة"
    ) {
        if (title.isBlank()) return
        val goal = Goal(
            id = "goal-${UUID.randomUUID()}",
            title = title.trim(),
            description = description.trim(),
            category = category.trim().ifBlank { "عام" },
            targetCount = targetCount.coerceAtLeast(1),
            currentCount = 0,
            progress = 0f,
            deadline = deadline.ifBlank { "مستمر" },
            status = "ACTIVE",
            color = color,
            priority = priority,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertGoal(goal)
        }
    }

    fun incrementGoalProgress(goalId: String, amount: Int = 1) {
        viewModelScope.launch {
            repository.incrementGoalProgress(goalId, amount)
        }
    }

    fun updateGoalStatus(goalId: String, status: GoalFilterTab) {
        viewModelScope.launch {
            repository.updateGoalStatus(goalId, status)
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
        }
    }

    // ==========================================
    // Project Actions
    // ==========================================

    fun setProjectFilter(filter: ProjectFilterTab) {
        _uiPreferences.update { it.copy(activeProjectFilter = filter) }
    }

    fun addProject(
        name: String,
        description: String = "",
        color: String = "TEAL",
        taskCount: Int = 5,
        goalId: String? = null
    ) {
        if (name.isBlank()) return
        val project = Project(
            id = "project-${UUID.randomUUID()}",
            name = name.trim(),
            description = description.trim(),
            color = color,
            progress = 0f,
            status = "ACTIVE",
            taskCount = taskCount,
            completedTaskCount = 0,
            goalId = goalId,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertProject(project)
        }
    }

    fun updateProjectStatus(projectId: String, status: ProjectFilterTab) {
        viewModelScope.launch {
            repository.updateProjectStatus(projectId, status)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                val all = repository.allTasks.first()
                val tasksInProject = all.filter { it.projectId == projectId }
                tasksInProject.forEach { t ->
                    alarmManager.cancelTaskAlarm(t.id)
                }
            } catch (e: Exception) {
                android.util.Log.w("FocusViewModel", "Error cleaning up alarms for project: ${e.message}")
            }
            repository.deleteProject(projectId)
        }
    }

    // ==========================================
    // AI Actions
    // ==========================================

    fun generateTaskBreakdown(taskId: String) {
        viewModelScope.launch {
            _aiState.value = AIActionState.Loading("جارٍ تفكيك المهمة بالذكاء الاصطناعي...")
            val task = repository.getTaskById(taskId)
            if (task == null) {
                _aiState.value = AIActionState.Error("المهمة غير موجودة")
                return@launch
            }
            aiService.breakdownTask(task.title, task.subtitle).collect { subTasks ->
                _aiState.value = AIActionState.BreakdownSuccess(
                    parentTaskId = task.id,
                    parentTitle = task.title,
                    subTasks = subTasks
                )
            }
        }
    }

    fun generateTaskBreakdownForTitle(title: String, description: String = "") {
        if (title.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AIActionState.Loading("جارٍ تفكيك المهمة بالذكاء الاصطناعي...")
            aiService.breakdownTask(title, description).collect { subTasks ->
                _aiState.value = AIActionState.BreakdownSuccess(
                    parentTaskId = null,
                    parentTitle = title,
                    subTasks = subTasks
                )
            }
        }
    }

    fun applySubTasksToProject(parentTaskId: String?, subTasks: List<String>, selectedIndexes: Set<Int>? = null) {
        viewModelScope.launch {
            val selectedSubTasks = if (selectedIndexes != null) {
                subTasks.filterIndexed { index, _ -> selectedIndexes.contains(index) }
            } else {
                subTasks
            }

            if (selectedSubTasks.isEmpty()) {
                _aiState.value = AIActionState.Idle
                return@launch
            }

            val parentTask = if (!parentTaskId.isNullOrBlank()) repository.getTaskById(parentTaskId) else null
            val baseTimeMillis = parentTask?.date ?: _selectedDateTimestamp.value
            val parentDuration = parentTask?.durationMinutes ?: (selectedSubTasks.size * 25)
            val subTaskDuration = (parentDuration / selectedSubTasks.size).coerceIn(15, 60)

            selectedSubTasks.forEach { subTitle ->
                val newId = UUID.randomUUID().toString()
                val newTask = Task(
                    id = newId,
                    title = subTitle,
                    subtitle = if (parentTask != null) "فرعية من: ${parentTask.title}" else "خطوة فرعية بالذكاء الاصطناعي",
                    durationMinutes = subTaskDuration,
                    scheduledTime = "",
                    isCompleted = false,
                    date = baseTimeMillis,
                    categoryColor = parentTask?.categoryColor ?: "TEAL",
                    isFixed = false
                )
                repository.insertTask(newTask)
            }

            _aiState.value = AIActionState.Idle
        }
    }

    fun generateDeFrictionAdvice(taskId: String) {
        viewModelScope.launch {
            _aiState.value = AIActionState.Loading("جارٍ صياغة خطة التحفيز السلوكية...")
            val task = repository.getTaskById(taskId)
            if (task == null) {
                _aiState.value = AIActionState.Error("المهمة غير موجودة")
                return@launch
            }
            aiService.suggestDeFrictionPlan(task.title, task.postponedCount).collect { advice ->
                _aiState.value = AIActionState.DeFrictionSuccess(
                    taskId = task.id,
                    taskTitle = task.title,
                    advice = advice
                )
            }
        }
    }

    fun estimateTaskDuration(taskTitle: String, onResult: (Int) -> Unit) {
        if (taskTitle.isBlank()) return
        viewModelScope.launch {
            aiService.estimateDuration(taskTitle).collect { mins ->
                onResult(mins)
            }
        }
    }

    fun clearAiState() {
        _aiState.value = AIActionState.Idle
    }

    // ==========================================
    // UI Navigation & Preferences
    // ==========================================

    fun toggleDarkMode() {
        _uiPreferences.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun selectTab(tab: FocusNavTab) {
        _uiPreferences.update { it.copy(currentTab = tab) }
    }

    fun setSmartAlarmActive(active: Boolean) {
        _uiPreferences.update { it.copy(isSmartAlarmActive = active) }
    }

    fun setTaskFilter(filter: TaskFilterTab) {
        _uiPreferences.update { it.copy(activeTaskFilter = filter) }
    }

    fun setReviewPeriod(period: ReviewPeriodTab) {
        _uiPreferences.update { it.copy(activeReviewPeriod = period) }
    }

    fun selectDate(timestamp: Long) {
        _selectedDateTimestamp.value = timestamp
    }

    fun selectDay(day: DayItem) {
        if (day.timestamp > 0L) {
            _selectedDateTimestamp.value = day.timestamp
        }
    }

    fun changeWeek(deltaWeeks: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedWeekBaseTimestamp.value
            add(Calendar.WEEK_OF_YEAR, deltaWeeks)
        }
        _selectedWeekBaseTimestamp.value = cal.timeInMillis
    }

    fun checkCanAddTask(durationMinutes: Int): Boolean {
        val currentBooked = uiState.value.dailyCapacity.bookedMinutes
        val total = uiState.value.dailyCapacity.totalAwakeMinutes
        return (currentBooked + durationMinutes) <= total
    }

    fun addTask(
        title: String,
        subtitle: String = "",
        time: String = "12:00 م",
        durationMinutes: Int = 90,
        colorType: BlockColor = BlockColor.TEAL,
        isFixed: Boolean = false,
        startTime: String? = null,
        endTime: String? = null,
        alertType: TaskAlertType = TaskAlertType.SMART_ALARM,
        projectId: String? = null
    ): Boolean {
        if (title.isBlank()) return false

        val currentBooked = uiState.value.dailyCapacity.bookedMinutes
        val total = uiState.value.dailyCapacity.totalAwakeMinutes
        if (currentBooked + durationMinutes > total) {
            _uiPreferences.update {
                it.copy(capacityErrorMessage = "⚠️ اليوم ممتلئ بالفعل. لا توجد سعة كافية لهذه المهمة.")
            }
            return false
        }

        val newEntity = Task(
            id = "task-${UUID.randomUUID()}",
            title = title.trim(),
            subtitle = subtitle.ifBlank { if (isFixed) "حدث ثابت" else "مهمة مرنة" },
            durationMinutes = durationMinutes,
            scheduledTime = time,
            isCompleted = false,
            date = _selectedDateTimestamp.value,
            categoryColor = colorType.name,
            isFixed = isFixed,
            startTime = startTime,
            endTime = endTime,
            alertType = alertType.name,
            projectId = projectId
        )

        viewModelScope.launch {
            try {
                repository.addTaskWithCapacityCheck(newEntity)
                alarmManager.scheduleTaskAlarm(newEntity)
                _uiPreferences.update { it.copy(capacityErrorMessage = null) }
            } catch (e: CapacityExceededException) {
                _uiPreferences.update { it.copy(capacityErrorMessage = e.message) }
            }
        }
        return true
    }

    fun rescheduleTask(
        taskId: String,
        newScheduledTime: String,
        newStartTime: String? = null,
        newEndTime: String? = null,
        newDurationMinutes: Int = 90
    ) {
        viewModelScope.launch {
            val all = repository.allTasks.first()
            val task = all.find { it.id == taskId } ?: return@launch
            val updated = task.copy(
                scheduledTime = newScheduledTime,
                startTime = newStartTime ?: task.startTime,
                endTime = newEndTime ?: task.endTime,
                durationMinutes = newDurationMinutes,
                needsRescheduling = false
            )
            repository.updateTask(updated)
            alarmManager.scheduleTaskAlarm(updated)
        }
    }

    fun clearCapacityError() {
        _uiPreferences.update { it.copy(capacityErrorMessage = null) }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId)
            val all = repository.allTasks.first()
            val task = all.find { it.id == taskId }
            if (task != null) {
                if (task.isCompleted) {
                    alarmManager.cancelTaskAlarm(taskId)
                    FocusAlarmForegroundService.stop(getApplication())
                    FocusAlarmSoundManager.stopAlarm()
                } else {
                    alarmManager.scheduleTaskAlarm(task)
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        alarmManager.cancelTaskAlarm(taskId)
        FocusAlarmForegroundService.stop(getApplication())
        FocusAlarmSoundManager.stopAlarm()
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    // ==========================================
    // Smart Alarm Trigger & Management
    // ==========================================

    fun triggerSmartAlarm(taskId: String, title: String? = null, subtitle: String? = null) {
        viewModelScope.launch {
            val all = repository.allTasks.first()
            val task = all.find { it.id == taskId }
            val focusItem = task?.toTaskItem() ?: uiState.value.tasks.find { it.id == taskId } ?: TaskItem(
                id = taskId,
                title = title ?: "مهمة مجدولة",
                subtitle = subtitle ?: "حان وقت بدء المهمة",
                durationMinutes = 45
            )
            _uiPreferences.update {
                it.copy(
                    isSmartAlarmActive = true,
                    currentFocusTask = focusItem
                )
            }
        }
    }

    fun triggerTestSmartAlarm() {
        val sampleTask = uiState.value.tasks.firstOrNull() ?: TaskItem(
            id = "test-alarm-${UUID.randomUUID()}",
            title = "اختبار التنبيه الذكي",
            subtitle = "جلسة تركيز تجريبية للتأكد من الصوت والإشعارات",
            timeText = "الآن",
            durationMinutes = 25
        )
        _uiPreferences.update {
            it.copy(
                isSmartAlarmActive = true,
                currentFocusTask = sampleTask
            )
        }
    }

    fun snoozeSmartAlarm(taskId: String? = null, snoozeMinutes: Int = 10) {
        FocusAlarmForegroundService.stop(getApplication())
        FocusAlarmSoundManager.stopAlarm()
        val id = taskId ?: uiState.value.currentFocusTask.id
        val title = uiState.value.currentFocusTask.title
        val subtitle = uiState.value.currentFocusTask.subtitle
        alarmManager.snoozeTaskAlarm(id, title, subtitle, snoozeMinutes)
        _uiPreferences.update { it.copy(isSmartAlarmActive = false) }
    }

    fun dismissSmartAlarm() {
        FocusAlarmForegroundService.stop(getApplication())
        FocusAlarmSoundManager.stopAlarm()
        _uiPreferences.update { it.copy(isSmartAlarmActive = false) }
    }

    fun startFocusTimerById(taskId: String) {
        FocusAlarmForegroundService.stop(getApplication())
        FocusAlarmSoundManager.stopAlarm()
        _uiPreferences.update { it.copy(isSmartAlarmActive = false) }
        viewModelScope.launch {
            val all = repository.allTasks.first()
            val task = all.find { it.id == taskId }
            val item = task?.toTaskItem() ?: uiState.value.tasks.find { it.id == taskId }
            if (item != null) {
                startFocusTimer(item)
            } else {
                startFocusTimer()
            }
        }
    }

    fun markTaskNeedsRescheduling(taskId: String, needsReschedule: Boolean = true) {
        viewModelScope.launch {
            repository.updateNeedsRescheduling(taskId, needsReschedule)
            checkAdaptivePlan()
        }
    }

    // ==========================================
    // Adaptive Planning Engine Methods
    // ==========================================

    fun setShowAdaptiveSheet(show: Boolean) {
        _showAdaptiveRescheduleSheet.value = show
        _uiPreferences.update { it.copy(showAdaptiveRescheduleSheet = show) }
    }

    fun dismissAdaptiveBanner() {
        _uiPreferences.update { it.copy(isAdaptiveBannerDismissed = true) }
    }

    fun checkAdaptivePlan() {
        viewModelScope.launch {
            val all = repository.allTasks.first()
            val suggestions = AdaptivePlanner.generateSuggestions(all)
            _adaptiveSuggestionsState.value = suggestions
        }
    }

    fun applyAdaptiveReschedule(suggestions: List<RescheduleSuggestion> = uiState.value.adaptiveSuggestions) {
        viewModelScope.launch {
            for (suggestion in suggestions) {
                if (suggestion.isPostponeToTomorrow) {
                    repository.postponeTaskToTomorrow(
                        taskId = suggestion.task.id,
                        newStartTime = suggestion.newStartTime,
                        newEndTime = suggestion.newEndTime ?: "10:30"
                    )
                } else if (suggestion.isMoveToLater) {
                    repository.moveToLater(suggestion.task.id)
                    alarmManager.cancelTaskAlarm(suggestion.task.id)
                } else {
                    repository.updateTaskSchedule(
                        taskId = suggestion.task.id,
                        newDate = suggestion.task.date,
                        newStartTime = suggestion.newStartTime,
                        newEndTime = suggestion.newEndTime,
                        newScheduledTime = suggestion.newScheduledTime,
                        newDurationMinutes = suggestion.newDurationMinutes
                    )
                    val updatedTask = suggestion.task.copy(
                        startTime = suggestion.newStartTime,
                        endTime = suggestion.newEndTime,
                        scheduledTime = suggestion.newScheduledTime,
                        durationMinutes = suggestion.newDurationMinutes,
                        needsRescheduling = false
                    )
                    alarmManager.scheduleTaskAlarm(updatedTask)
                }
            }
            _showAdaptiveRescheduleSheet.value = false
            _adaptiveSuggestionsState.value = emptyList()
        }
    }

    fun applySingleSuggestion(suggestion: RescheduleSuggestion) {
        viewModelScope.launch {
            if (suggestion.isPostponeToTomorrow) {
                repository.postponeTaskToTomorrow(
                    taskId = suggestion.task.id,
                    newStartTime = suggestion.newStartTime,
                    newEndTime = suggestion.newEndTime ?: "10:30"
                )
            } else if (suggestion.isMoveToLater) {
                repository.moveToLater(suggestion.task.id)
                alarmManager.cancelTaskAlarm(suggestion.task.id)
            } else {
                repository.updateTaskSchedule(
                    taskId = suggestion.task.id,
                    newDate = suggestion.task.date,
                    newStartTime = suggestion.newStartTime,
                    newEndTime = suggestion.newEndTime,
                    newScheduledTime = suggestion.newScheduledTime,
                    newDurationMinutes = suggestion.newDurationMinutes
                )
                val updatedTask = suggestion.task.copy(
                    startTime = suggestion.newStartTime,
                    endTime = suggestion.newEndTime,
                    scheduledTime = suggestion.newScheduledTime,
                    durationMinutes = suggestion.newDurationMinutes,
                    needsRescheduling = false
                )
                alarmManager.scheduleTaskAlarm(updatedTask)
            }
            val remaining = _adaptiveSuggestionsState.value.filter { it.task.id != suggestion.task.id }
            _adaptiveSuggestionsState.value = remaining
            if (remaining.isEmpty()) {
                _showAdaptiveRescheduleSheet.value = false
            }
        }
    }

    fun postponeTaskToTomorrow(taskId: String) {
        viewModelScope.launch {
            repository.postponeTaskToTomorrow(taskId)
            val all = repository.allTasks.first()
            val updated = all.find { it.id == taskId }
            if (updated != null) {
                alarmManager.scheduleTaskAlarm(updated)
            }
            checkAdaptivePlan()
        }
    }

    fun moveToLater(taskId: String) {
        viewModelScope.launch {
            repository.moveToLater(taskId)
            alarmManager.cancelTaskAlarm(taskId)
            checkAdaptivePlan()
        }
    }

    // ==========================================
    // Focus Timer Engine Methods
    // ==========================================

    fun startFocusTimer(task: TaskItem? = null, customDurationMinutes: Int? = null) {
        val targetTask = task ?: uiState.value.currentFocusTask
        val durationMins = customDurationMinutes ?: targetTask.durationMinutes.coerceAtLeast(15)
        val totalSecs = durationMins * 60

        _focusTimerState.update {
            it.copy(
                taskId = targetTask.id,
                taskTitle = targetTask.title,
                taskSubtitle = targetTask.subtitle,
                plannedDurationMinutes = durationMins,
                initialTotalSeconds = totalSecs,
                remainingSeconds = totalSecs,
                elapsedSeconds = 0,
                status = TimerStatus.RUNNING,
                isFocusModeFullscreen = true,
                colorType = targetTask.colorType
            )
        }
        startTicker()
    }

    fun togglePlayPauseFocusTimer() {
        val current = _focusTimerState.value
        when (current.status) {
            TimerStatus.RUNNING -> pauseFocusTimer()
            TimerStatus.PAUSED -> resumeFocusTimer()
            TimerStatus.IDLE, TimerStatus.COMPLETED -> {
                if (current.remainingSeconds <= 0) {
                    resetFocusTimer()
                }
                resumeFocusTimer()
            }
        }
    }

    fun pauseFocusTimer() {
        timerJob?.cancel()
        timerJob = null
        _focusTimerState.update { it.copy(status = TimerStatus.PAUSED) }
    }

    fun resumeFocusTimer() {
        _focusTimerState.update { it.copy(status = TimerStatus.RUNNING) }
        startTicker()
    }

    fun setFocusFullscreen(fullscreen: Boolean) {
        _focusTimerState.update { it.copy(isFocusModeFullscreen = fullscreen) }
    }

    fun resetFocusTimer() {
        timerJob?.cancel()
        timerJob = null
        _focusTimerState.update { current ->
            val totalSecs = current.plannedDurationMinutes * 60
            current.copy(
                remainingSeconds = totalSecs,
                initialTotalSeconds = totalSecs,
                elapsedSeconds = 0,
                status = TimerStatus.IDLE
            )
        }
    }

    fun stopAndSaveFocusTimer() {
        timerJob?.cancel()
        timerJob = null

        val current = _focusTimerState.value
        val minutesSpent = current.actualMinutesSpent
        val taskId = current.taskId

        if (!taskId.isNullOrBlank() && minutesSpent > 0) {
            viewModelScope.launch {
                repository.addActualTime(taskId, minutesSpent)
                if (minutesSpent < current.plannedDurationMinutes) {
                    repository.updateNeedsRescheduling(taskId, true)
                }
                checkAdaptivePlan()
            }
        }

        _focusTimerState.update {
            it.copy(
                status = TimerStatus.IDLE,
                isFocusModeFullscreen = false
            )
        }
    }

    fun completeFocusTimer() {
        timerJob?.cancel()
        timerJob = null

        val current = _focusTimerState.value
        val minutesSpent = current.actualMinutesSpent.coerceAtLeast(current.plannedDurationMinutes)
        val taskId = current.taskId

        if (!taskId.isNullOrBlank()) {
            alarmManager.cancelTaskAlarm(taskId)
            FocusAlarmForegroundService.stop(getApplication())
            FocusAlarmSoundManager.stopAlarm()
            viewModelScope.launch {
                repository.completeFocusSession(
                    taskId = taskId,
                    minutesSpent = minutesSpent,
                    markCompleted = true
                )
            }
        }

        _focusTimerState.update {
            it.copy(
                status = TimerStatus.COMPLETED,
                remainingSeconds = 0,
                isFocusModeFullscreen = false
            )
        }
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val current = _focusTimerState.value
                if (current.status != TimerStatus.RUNNING) break

                val newRemaining = current.remainingSeconds - 1
                val newElapsed = current.elapsedSeconds + 1

                if (newRemaining <= 0) {
                    _focusTimerState.update {
                        it.copy(
                            remainingSeconds = 0,
                            elapsedSeconds = newElapsed,
                            status = TimerStatus.COMPLETED
                        )
                    }
                    val taskId = current.taskId
                    if (!taskId.isNullOrBlank()) {
                        repository.completeFocusSession(
                            taskId = taskId,
                            minutesSpent = (newElapsed + 30) / 60,
                            markCompleted = true
                        )
                    }
                    break
                } else {
                    _focusTimerState.update {
                        it.copy(
                            remainingSeconds = newRemaining,
                            elapsedSeconds = newElapsed
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        fun getDateBounds(timestamp: Long): Pair<Long, Long> {
            val startCal = Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return Pair(startCal.timeInMillis, endCal.timeInMillis)
        }

        fun isSameDay(t1: Long, t2: Long): Boolean {
            val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
            val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }

        fun formatWeekRange(weekDays: List<DayItem>): String {
            if (weekDays.isEmpty()) return "هذا الأسبوع"
            val first = weekDays.first()
            val last = weekDays.last()
            return "${first.dayNumber} - ${last.dayNumber} هذا الشهر"
        }
    }
}
