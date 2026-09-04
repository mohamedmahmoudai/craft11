package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BlockColor
import com.example.model.DailyCapacityState
import com.example.model.TaskFilterTab
import com.example.model.TaskItem
import com.example.ui.components.WhackaCard
import com.example.ui.components.WhackaFlatTextField
import com.example.ui.components.WhackaPillButton
import com.example.ui.theme.WhackaAmber
import com.example.ui.theme.WhackaEmerald
import com.example.ui.theme.WhackaPrimaryAccent
import com.example.ui.theme.WhackaPrimaryShadow
import com.example.ui.theme.WhackaRed
import com.example.ui.theme.WhackaSecondaryAccent

@Composable
fun TasksScreen(
    tasks: List<TaskItem>,
    activeFilter: TaskFilterTab,
    dailyCapacity: DailyCapacityState = DailyCapacityState(),
    onFilterChanged: (TaskFilterTab) -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTask: (String, String, String, Int, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit = {},
    onBreakdownTask: (String) -> Unit = {},
    onBreakdownTaskForInput: (String, String) -> Unit = { _, _ -> },
    onGetDeFrictionAdvice: (String) -> Unit = {},
    onEstimateDuration: ((String, (Int) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredTasks = when (activeFilter) {
        TaskFilterTab.TODAY -> tasks.filter { it.filterTab == TaskFilterTab.TODAY }
        TaskFilterTab.UPCOMING -> tasks.filter { it.filterTab == TaskFilterTab.UPCOMING || !it.isCompleted }
        TaskFilterTab.LATER -> tasks.filter { it.filterTab == TaskFilterTab.LATER || it.isLater }
        TaskFilterTab.COMPLETED -> tasks.filter { it.isCompleted }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Headline: "المهام"
        Text(
            text = "المهام",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Whacka TabRow: (اليوم, قادمة, مكتملة) - Fully rounded pills (50)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TaskFilterTab.entries.forEach { tab ->
                val isSelected = activeFilter == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onFilterChanged(tab) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) WhackaPrimaryAccent else Color.Transparent
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // List of tasks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskItemCard(
                        task = task,
                        onToggle = { onToggleTask(task.id) },
                        onDelete = { onDeleteTask(task.id) },
                        onBreakdown = { onBreakdownTask(task.id) },
                        onGetDeFrictionAdvice = { onGetDeFrictionAdvice(task.id) }
                    )
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = WhackaPrimaryAccent.copy(alpha = 0.12f),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.TaskAlt,
                                            contentDescription = null,
                                            tint = WhackaPrimaryAccent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (tasks.isEmpty()) "لا توجد مهام حالياً. أضف مهمتك الأولى وابدأ التركيز." else "لا توجد مهام في هذا القسم",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // FAB with fully rounded shape
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp)
                    .testTag("add_task_fab"),
                shape = RoundedCornerShape(50),
                containerColor = WhackaPrimaryAccent,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة مهمة جديدة",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            dailyCapacity = dailyCapacity,
            onDismiss = { showAddDialog = false },
            onBreakdownAi = { inputTitle, inputDesc ->
                showAddDialog = false
                onBreakdownTaskForInput(inputTitle, inputDesc)
            },
            onEstimateDuration = onEstimateDuration,
            onConfirm = { title, subtitle, time, duration, isFixed ->
                onAddTask(title, subtitle, time, duration, isFixed)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TaskItemCard(
    task: TaskItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit = {},
    onBreakdown: () -> Unit = {},
    onGetDeFrictionAdvice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val categoryColor = when (task.colorType) {
        BlockColor.INDIGO, BlockColor.TEAL -> WhackaPrimaryAccent
        BlockColor.ORANGE -> WhackaSecondaryAccent
        BlockColor.GREEN -> WhackaEmerald
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = WhackaPrimaryShadow,
                spotColor = WhackaPrimaryShadow
            )
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onToggle() }
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 4.dp Category Accent Border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(84.dp)
                    .background(categoryColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onToggle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = if (task.isCompleted) "مكتمل" else "غير مكتمل",
                                tint = if (task.isCompleted) WhackaPrimaryAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                                if (task.isFixed) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = WhackaEmerald.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "ثابت",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = WhackaEmerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (task.subtitle.isNotBlank()) {
                                Text(
                                    text = task.subtitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                if (task.timeText.isNotBlank()) {
                                    Text(
                                        text = task.timeText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "${task.durationMinutes}د",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (task.actualDurationMinutes > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = WhackaPrimaryAccent.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "⏳ ${task.actualDurationMinutes}/${task.durationMinutes} د",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = WhackaPrimaryAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("تفكيك بالذكاء الاصطناعي ✨") },
                                onClick = {
                                    showMenu = false
                                    onBreakdown()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = WhackaPrimaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف المهمة", color = WhackaRed) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = WhackaRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // De-friction hint card for tasks postponed >= 3 times
                if (task.postponedCount >= 3 && !task.isCompleted) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onGetDeFrictionAdvice() },
                        shape = RoundedCornerShape(16.dp),
                        color = WhackaAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, WhackaAmber.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TipsAndUpdates,
                                    contentDescription = null,
                                    tint = WhackaAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "مهمة مؤجلة ${task.postponedCount} مرات: تحليل العقبة بالذكاء الاصطناعي",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = WhackaAmber
                                    )
                                    Text(
                                        text = "اضغط للحصول على اقتراح تقليل الاحتكاك وبدء العمل بسهولة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    dailyCapacity: DailyCapacityState,
    onDismiss: () -> Unit,
    onBreakdownAi: (String, String) -> Unit,
    onEstimateDuration: ((String, (Int) -> Unit) -> Unit)? = null,
    onConfirm: (String, String, String, Int, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("11:00 ص") }
    var durationText by remember { mutableStateOf("45") }
    var isFixed by remember { mutableStateOf(false) }

    val durationMinutes = durationText.toIntOrNull() ?: 0
    val isExceedingCapacity = (dailyCapacity.bookedMinutes + durationMinutes) > dailyCapacity.totalAwakeMinutes
    val canSave = title.isNotBlank() && durationMinutes > 0 && !isExceedingCapacity

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "إضافة مهمة جديدة",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.size(32.dp))
                    }

                    // Flat Inputs with 16dp shapes
                    WhackaFlatTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "عنوان المهمة",
                        placeholder = "مثال: مراجعة العرض التقديمي"
                    )

                    WhackaFlatTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = "الوصف أو التفاصيل (اختياري)",
                        placeholder = "مثال: تجهيز النقاط الرئيسية والأرقام"
                    )

                    // AI Sparkle Breakdown Action
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = title.isNotBlank()) {
                                onBreakdownAi(title, subtitle)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (title.isNotBlank()) WhackaPrimaryAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (title.isNotBlank()) WhackaPrimaryAccent.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (title.isNotBlank()) WhackaPrimaryAccent else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تفكيك المهمة بالذكاء الاصطناعي ✨",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (title.isNotBlank()) WhackaPrimaryAccent else MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "تقسيم الهدف المعقد إلى 3-5 خطوات فرعية قابلة للتنفيذ",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            WhackaFlatTextField(
                                value = time,
                                onValueChange = { time = it },
                                label = "الوقت",
                                placeholder = "11:00 ص"
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            WhackaFlatTextField(
                                value = durationText,
                                onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                                label = "المدة (د)",
                                placeholder = "45",
                                trailingIcon = {
                                    if (onEstimateDuration != null && title.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                onEstimateDuration(title) { est ->
                                                    durationText = est.toString()
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "تقدير المدة",
                                                tint = WhackaSecondaryAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Fixed Event Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFixed) "موعد ثابت" else "مهمة مرنة",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isFixed) "محمي من التعديل التلقائي الذكي" else "يقبل إعادة الجدولة التكيفية",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isFixed,
                            onCheckedChange = { isFixed = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WhackaEmerald
                            )
                        )
                    }

                    // Capacity Validation Warning UI State
                    if (isExceedingCapacity) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WhackaAmber.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, WhackaAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = WhackaAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "اليوم ممتلئ بالفعل. لا توجد سعة كافية لهذه المهمة.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    color = WhackaAmber
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pill Submit Button
                    Button(
                        onClick = {
                            val trimmedTitle = title.trim()
                            val trimmedDesc = subtitle.trim()
                            if (trimmedTitle.isNotBlank() && durationMinutes > 0) {
                                onConfirm(trimmedTitle, trimmedDesc, time.trim(), durationMinutes, isFixed)
                            }
                        },
                        enabled = canSave && title.trim().isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_task_btn"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhackaPrimaryAccent,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "إضافة المهمة",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
