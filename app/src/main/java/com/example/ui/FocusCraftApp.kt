@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AIActionState
import com.example.model.FocusNavTab
import com.example.model.TimerStatus
import com.example.ui.components.AITaskBreakdownDialog
import com.example.ui.components.AdaptiveRescheduleBottomSheet
import com.example.ui.components.DeFrictionAdviceDialog
import com.example.ui.components.PermissionRationaleDialog
import com.example.ui.components.PermissionType
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.components.SystemPermissionsHandler
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SmartAlarmScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.FocusCraftTheme
import com.example.util.BatteryOptimizationHelper
import com.example.viewmodel.FocusViewModel

@Composable
fun FocusCraftApp(
    viewModel: FocusViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBatteryRationaleDialog by remember { mutableStateOf(false) }
    var showSplashScreen by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // System Permissions & Hardening Handler on App Startup
    SystemPermissionsHandler()

    FocusCraftTheme(darkTheme = uiState.isDarkMode) {
        if (showSplashScreen) {
            SplashScreen(onSplashFinished = { showSplashScreen = false })
        } else if (!uiState.isLoggedIn) {
            // Firebase Auth / Login Screen
            LoginScreen(
                onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                onAppleSignIn = { viewModel.loginWithMockApple() },
                onContinueAsGuest = { viewModel.loginAsGuest() }
            )
        } else if (uiState.focusTimerState.isFocusModeFullscreen) {
            // Fullscreen Focus Timer Mode
            FocusTimerScreen(
                timerState = uiState.focusTimerState,
                onPlayPause = { viewModel.togglePlayPauseFocusTimer() },
                onStopAndSave = { viewModel.stopAndSaveFocusTimer() },
                onComplete = { viewModel.completeFocusTimer() },
                onMinimize = { viewModel.setFocusFullscreen(false) },
                onReset = { viewModel.resetFocusTimer() }
            )
        } else if (uiState.isSmartAlarmActive) {
            // Fullscreen Smart Alarm Mode
            SmartAlarmScreen(
                timeText = uiState.currentFocusTask.timeText.ifBlank { "09:00" },
                taskTitle = uiState.currentFocusTask.title,
                taskSubtitle = uiState.currentFocusTask.subtitle,
                onStartTask = {
                    viewModel.startFocusTimer(uiState.currentFocusTask)
                },
                onSnooze = { viewModel.snoozeSmartAlarm(snoozeMinutes = 10) },
                onOpenTask = {
                    viewModel.dismissSmartAlarm()
                    viewModel.selectTab(FocusNavTab.TODAY)
                }
            )
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Floating Mini-Timer bar if timer is active in background
                        if (uiState.focusTimerState.status == TimerStatus.RUNNING || uiState.focusTimerState.status == TimerStatus.PAUSED) {
                            FloatingMiniTimerBar(
                                timerState = uiState.focusTimerState,
                                onExpand = { viewModel.setFocusFullscreen(true) },
                                onPlayPause = { viewModel.togglePlayPauseFocusTimer() }
                            )
                        }

                        WhackaBottomBar(
                            currentTab = uiState.currentTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = uiState.currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tabTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            FocusNavTab.TODAY -> {
                                HomeScreen(
                                    userName = uiState.userName,
                                    isDarkMode = uiState.isDarkMode,
                                    onToggleTheme = { viewModel.toggleDarkMode() },
                                    currentFocusTask = uiState.currentFocusTask,
                                    tasks = uiState.tasks,
                                    dailyCapacity = uiState.dailyCapacity,
                                    selectedCapacityMinutes = uiState.selectedCapacityMinutes,
                                    selectedEnergyLevel = uiState.selectedEnergyLevel,
                                    isDayUnavailable = uiState.isDayUnavailable,
                                    adaptiveSuggestions = uiState.adaptiveSuggestions,
                                    isAdaptiveBannerDismissed = uiState.isAdaptiveBannerDismissed,
                                    onSaveCapacityPreferences = { mins, energy ->
                                        viewModel.saveDayCapacityPreferences(mins, energy)
                                    },
                                    onSetDayUnavailable = { unavailable ->
                                        viewModel.setDayUnavailable(unavailable)
                                    },
                                    onOpenAdaptiveSheet = { viewModel.setShowAdaptiveSheet(true) },
                                    onDismissAdaptiveBanner = { viewModel.dismissAdaptiveBanner() },
                                    onToggleTask = { viewModel.toggleTaskCompletion(it) },
                                    onStartFocus = { viewModel.startFocusTimer(uiState.currentFocusTask) },
                                    onRequestBatteryOptimization = {
                                        showBatteryRationaleDialog = true
                                    }
                                )
                            }
                            FocusNavTab.SCHEDULE -> {
                                ScheduleScreen(
                                    timelineItems = uiState.selectedDateTimeline,
                                    dailyCapacity = uiState.dailyCapacity,
                                    selectedDateMillis = uiState.selectedDateTimestamp,
                                    onTriggerAlarm = { viewModel.startFocusTimer(uiState.currentFocusTask) },
                                    onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
                                    onDeleteTask = { viewModel.deleteTask(it) },
                                    onRescheduleTask = { id, time, s, e, dur ->
                                        viewModel.rescheduleTask(id, time, s, e, dur)
                                    },
                                    onAddNewTaskAtTime = {
                                        viewModel.setShowQuickAddSheet(true)
                                    },
                                    onImportTask = { title, time, dur, fixed ->
                                        viewModel.addTask(
                                            title = title,
                                            time = time,
                                            durationMinutes = dur,
                                            isFixed = fixed
                                        )
                                    }
                                )
                            }
                            FocusNavTab.GOALS -> {
                                GoalsScreen(
                                    viewModel = viewModel,
                                    goals = uiState.goals,
                                    activeFilter = uiState.activeGoalFilter,
                                    onSelectFilter = { viewModel.setGoalFilter(it) },
                                    onIncrementGoal = { viewModel.incrementGoalProgress(it) },
                                    onUpdateStatus = { id, status -> viewModel.updateGoalStatus(id, status) },
                                    onDeleteGoal = { viewModel.deleteGoal(it) }
                                )
                            }
                            FocusNavTab.PROJECTS -> {
                                ProjectsScreen(
                                    viewModel = viewModel,
                                    projects = uiState.projects,
                                    activeFilter = uiState.activeProjectFilter,
                                    onSelectFilter = { viewModel.setProjectFilter(it) },
                                    onUpdateStatus = { id, status -> viewModel.updateProjectStatus(id, status) },
                                    onDeleteProject = { viewModel.deleteProject(it) }
                                )
                            }
                            FocusNavTab.MORE -> {
                                MoreScreen(
                                    userName = uiState.userName,
                                    userEmail = uiState.userEmail,
                                    userPhotoUrl = uiState.userPhotoUrl,
                                    isGuestMode = uiState.isGuestMode,
                                    isDarkMode = uiState.isDarkMode,
                                    isSyncingCloud = uiState.isSyncingCloud,
                                    syncSuccessMessage = uiState.syncSuccessMessage,
                                    reviewStats = uiState.reviewStats,
                                    tasks = uiState.tasks,
                                    sleepBedtime = uiState.sleepBedtime,
                                    sleepWakeTime = uiState.sleepWakeTime,
                                    workStartTime = uiState.workStartTime,
                                    workEndTime = uiState.workEndTime,
                                    onUpdateSleepSchedule = { bedtime, wakeTime ->
                                        viewModel.updateSleepSchedule(bedtime, wakeTime)
                                    },
                                    onUpdateWorkWindow = { startTime, endTime ->
                                        viewModel.updateWorkWindow(startTime, endTime)
                                    },
                                    onUpdateUserName = { viewModel.updateUserName(it) },
                                    onToggleTheme = { viewModel.toggleDarkMode() },
                                    onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                                    onSyncToCloud = { viewModel.syncToCloud() },
                                    onOpenAdaptiveSheet = { viewModel.setShowAdaptiveSheet(true) },
                                    onOpenLoginScreen = { viewModel.logout() },
                                    onLogout = { viewModel.logout() },
                                    onScheduleParkingLotTask = { task ->
                                        viewModel.rescheduleTask(task.id, "11:00", "11:00", "12:00", task.durationMinutes)
                                    },
                                    onStartFocusTask = { task ->
                                        viewModel.startFocusTimer(task)
                                    },
                                    onDeleteTask = { taskId ->
                                        viewModel.deleteTask(taskId)
                                    },
                                    onTestAlarm = {
                                        viewModel.triggerTestSmartAlarm()
                                    }
                                )
                            }
                        }
                    }

                    // Whacka Floating Action Button (FAB) - Fixed at bottom-left corner
                    if (uiState.currentTab == FocusNavTab.TODAY || uiState.currentTab == FocusNavTab.SCHEDULE) {
                        FloatingActionButton(
                            onClick = { viewModel.setShowQuickAddSheet(true) },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, bottom = 18.dp)
                                .size(56.dp)
                                .testTag("whacka_fab_btn"),
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 10.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "إضافة جديدة",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Quick Add 2x2 Bottom Sheet
            if (uiState.showQuickAddSheet) {
                QuickAddBottomSheet(
                    viewModel = viewModel,
                    onDismiss = { viewModel.setShowQuickAddSheet(false) }
                )
            }

            // AI Breakdown & Loading Dialog
            if (uiState.aiState is AIActionState.Loading ||
                uiState.aiState is AIActionState.BreakdownSuccess ||
                uiState.aiState is AIActionState.Error
            ) {
                AITaskBreakdownDialog(
                    aiState = uiState.aiState,
                    onDismiss = { viewModel.clearAiState() },
                    onApplySubTasks = { parentId, subTasks, selectedIndexes ->
                        viewModel.applySubTasksToProject(parentId, subTasks, selectedIndexes)
                    }
                )
            }

            // Behavioral De-friction Micro-Advice Dialog
            if (uiState.aiState is AIActionState.DeFrictionSuccess) {
                val deFriction = uiState.aiState as AIActionState.DeFrictionSuccess
                DeFrictionAdviceDialog(
                    taskTitle = deFriction.taskTitle,
                    adviceText = deFriction.advice,
                    onStartFiveMinuteFocus = {
                        val task = uiState.tasks.find { it.id == deFriction.taskId }
                        viewModel.clearAiState()
                        if (task != null) {
                            viewModel.startFocusTimer(task.copy(durationMinutes = 5))
                        }
                    },
                    onDismiss = { viewModel.clearAiState() }
                )
            }

            // Adaptive Planning Bottom Sheet
            if (uiState.showAdaptiveRescheduleSheet && uiState.adaptiveSuggestions.isNotEmpty()) {
                AdaptiveRescheduleBottomSheet(
                    suggestions = uiState.adaptiveSuggestions,
                    dailyCapacity = uiState.dailyCapacity,
                    onApplyAll = { viewModel.applyAdaptiveReschedule() },
                    onApplySingle = { viewModel.applySingleSuggestion(it) },
                    onPostponeToTomorrow = { viewModel.postponeTaskToTomorrow(it) },
                    onMoveToLater = { viewModel.moveToLater(it) },
                    onDismiss = { viewModel.setShowAdaptiveSheet(false) }
                )
            }

            // Battery Optimization Hardening Dialog
            if (showBatteryRationaleDialog) {
                val context = androidx.compose.ui.platform.LocalContext.current
                PermissionRationaleDialog(
                    permissionType = PermissionType.BATTERY_OPTIMIZATION,
                    onConfirm = {
                        showBatteryRationaleDialog = false
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                    },
                    onDismiss = {
                        showBatteryRationaleDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun FloatingMiniTimerBar(
    timerState: com.example.model.FocusTimerState,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
            .testTag("floating_mini_timer_bar"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onExpand() }
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (timerState.status == TimerStatus.RUNNING) MaterialTheme.colorScheme.primary else Color(0xFFF59E0B))
                )
                Column {
                    Text(
                        text = timerState.taskTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "المتبقي: ${timerState.formattedRemainingTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = if (timerState.status == TimerStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "تشغيل أو إيقاف",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun WhackaBottomBar(
    currentTab: FocusNavTab,
    onTabSelected: (FocusNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tab 1: اليوم (Today)
            WhackaNavItem(
                tab = FocusNavTab.TODAY,
                isSelected = currentTab == FocusNavTab.TODAY,
                selectedIcon = Icons.Filled.Today,
                unselectedIcon = Icons.Outlined.Today,
                onClick = { onTabSelected(FocusNavTab.TODAY) },
                modifier = Modifier.weight(1f)
            )

            // Tab 2: الجدول (Schedule)
            WhackaNavItem(
                tab = FocusNavTab.SCHEDULE,
                isSelected = currentTab == FocusNavTab.SCHEDULE,
                selectedIcon = Icons.Filled.CalendarMonth,
                unselectedIcon = Icons.Outlined.CalendarMonth,
                onClick = { onTabSelected(FocusNavTab.SCHEDULE) },
                modifier = Modifier.weight(1f)
            )

            // Tab 3: المشاريع (Projects)
            WhackaNavItem(
                tab = FocusNavTab.PROJECTS,
                isSelected = currentTab == FocusNavTab.PROJECTS,
                selectedIcon = Icons.Filled.Layers,
                unselectedIcon = Icons.Outlined.Layers,
                onClick = { onTabSelected(FocusNavTab.PROJECTS) },
                modifier = Modifier.weight(1f)
            )

            // Tab 4: الأهداف (Goals)
            WhackaNavItem(
                tab = FocusNavTab.GOALS,
                isSelected = currentTab == FocusNavTab.GOALS,
                selectedIcon = Icons.Filled.Flag,
                unselectedIcon = Icons.Outlined.Flag,
                onClick = { onTabSelected(FocusNavTab.GOALS) },
                modifier = Modifier.weight(1f)
            )

            // Tab 5: المزيد (More)
            WhackaNavItem(
                tab = FocusNavTab.MORE,
                isSelected = currentTab == FocusNavTab.MORE,
                selectedIcon = Icons.Filled.MoreHoriz,
                unselectedIcon = Icons.Outlined.MoreHoriz,
                onClick = { onTabSelected(FocusNavTab.MORE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WhackaNavItem(
    tab: FocusNavTab,
    isSelected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("nav_tab_${tab.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = tab.title,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
