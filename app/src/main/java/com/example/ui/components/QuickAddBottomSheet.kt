package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BlockColor
import com.example.model.QuickAddInitialType
import com.example.ui.theme.WhackaAmber
import com.example.ui.theme.WhackaEmerald
import com.example.ui.theme.WhackaRed
import com.example.viewmodel.FocusViewModel
import java.text.SimpleDateFormat
import java.util.Locale

fun parseDateStringToMillis(dateStr: String, fallbackMillis: Long): Long {
    if (dateStr.isBlank() || dateStr == "اليوم") return fallbackMillis
    return try {
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        sdf.parse(dateStr)?.time ?: fallbackMillis
    } catch (_: Exception) {
        fallbackMillis
    }
}

// =========================================================================
// Category Definition for Whacka Tasks
// =========================================================================

enum class WhackaCategory(val title: String, val color: Color, val blockColor: BlockColor) {
    DESIGN("تصميم", Color(0xFF0F6E60), BlockColor.TEAL),
    DEVELOPMENT("تطوير", Color(0xFF2563EB), BlockColor.INDIGO),
    MARKETING("تسويق", Color(0xFFC78222), BlockColor.ORANGE),
    GENERAL("عام", Color(0xFF6B7280), BlockColor.GREEN),
    MEETING("اجتماع", Color(0xFF7C3AED), BlockColor.INDIGO)
}

enum class TaskPriority(val title: String, val color: Color, val bgColor: Color) {
    LOW("منخفضة", Color(0xFF6B7280), Color(0xFFF3F4F6)),
    MEDIUM("متوسطة", Color(0xFFC78222), Color(0xFFFEF3C7)),
    HIGH("عالية", Color(0xFFDC2626), Color(0xFFFEE2E2))
}

data class MilestoneDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String = "",
    var deadline: String = "",
    var isCompleted: Boolean = false
)

// =========================================================================
// 1. Centered 2x2 Quick Add Modal ("إضافة جديدة")
// =========================================================================

@Composable
fun QuickAddBottomSheet(
    viewModel: FocusViewModel,
    initialType: QuickAddInitialType = QuickAddInitialType.NONE,
    onDismiss: () -> Unit
) {
    var showTaskDialog by remember { mutableStateOf(initialType == QuickAddInitialType.TASK) }
    var showEventDialog by remember { mutableStateOf(initialType == QuickAddInitialType.EVENT) }
    var showProjectDialog by remember { mutableStateOf(initialType == QuickAddInitialType.PROJECT) }
    var showGoalDialog by remember { mutableStateOf(initialType == QuickAddInitialType.GOAL) }

    if (initialType == QuickAddInitialType.NONE) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .testTag("quick_add_sheet"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar with Close (X) button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "إضافة جديدة",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    Text(
                        text = "اختر ما ترغب بإضافته إلى خطتك اليومية",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // 2x2 Grid of Minimalist Whacka Action Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        QuickActionCard(
                            title = "مهمة",
                            subtitle = "عمل أو دراسة مرنة",
                            icon = Icons.Outlined.CheckCircle,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_task_btn"),
                            onClick = { showTaskDialog = true }
                        )

                        QuickActionCard(
                            title = "موعد",
                            subtitle = "حدث ثابت ومجدول",
                            icon = Icons.Outlined.Event,
                            accentColor = WhackaAmber,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_event_btn"),
                            onClick = { showEventDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        QuickActionCard(
                            title = "مشروع",
                            subtitle = "مجموعة مهام كبرى",
                            icon = Icons.Outlined.Layers,
                            accentColor = Color(0xFF2563EB),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_project_btn"),
                            onClick = { showProjectDialog = true }
                        )

                        QuickActionCard(
                            title = "هدف",
                            subtitle = "غاية وعادة مستمرة",
                            icon = Icons.Outlined.Flag,
                            accentColor = WhackaEmerald,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_goal_btn"),
                            onClick = { showGoalDialog = true }
                        )
                    }
                }
            }
        }
    }
}

    if (showTaskDialog) {
        CreateTaskDialog(
            viewModel = viewModel,
            onDismiss = {
                showTaskDialog = false
                onDismiss()
            }
        )
    }

    if (showEventDialog) {
        CreateEventDialog(
            viewModel = viewModel,
            onDismiss = {
                showEventDialog = false
                onDismiss()
            }
        )
    }

    if (showProjectDialog) {
        CreateProjectDialog(
            viewModel = viewModel,
            onDismiss = {
                showProjectDialog = false
                onDismiss()
            }
        )
    }

    if (showGoalDialog) {
        CreateGoalDialog(
            viewModel = viewModel,
            onDismiss = {
                showGoalDialog = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1
            )
        }
    }
}

// =========================================================================
// 2. Custom Whacka Flat Text Field (16.dp corners, clean border, no underline)
// =========================================================================

@Composable
fun WhackaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
        )
    }
}

// =========================================================================
// 2b. Native Material 3 Date Picker Field
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhackaDatePickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    label: String,
    placeholder: String = "اختر التاريخ",
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = java.util.Calendar.getInstance()
                            calendar.timeInMillis = millis
                            val formatted = String.format(
                                java.util.Locale.US,
                                "%02d/%02d/%04d",
                                calendar.get(java.util.Calendar.MONTH) + 1,
                                calendar.get(java.util.Calendar.DAY_OF_MONTH),
                                calendar.get(java.util.Calendar.YEAR)
                            )
                            onDateSelected(formatted)
                        }
                        showDialog = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("date_picker_confirm_btn")
                ) {
                    Text("تم", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("date_picker_cancel_btn")
                ) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(24.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DatePicker(
                    state = datePickerState,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { showDialog = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = if (value.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "اختيار التاريخ",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// =========================================================================
// 3. Priority Pill Selector (منخفضة, متوسطة, عالية)
// =========================================================================

@Composable
fun PriorityPillSelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "الأولوية",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskPriority.entries.forEach { priority ->
                val isSelected = selectedPriority == priority
                val pillBorder = if (isSelected) priority.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                val pillBg = if (isSelected) priority.color else Color.Transparent
                val pillText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(pillBg)
                        .border(BorderStroke(1.dp, pillBorder), RoundedCornerShape(12.dp))
                        .clickable { onPrioritySelected(priority) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = priority.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = pillText
                        )
                    )
                }
            }
        }
    }
}

// =========================================================================
// 4. Create Task Modal Dialog (Exact Whacka Specifications)
// =========================================================================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    viewModel: FocusViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(WhackaCategory.DESIGN) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var selectedProject by remember { mutableStateOf("بدون مشروع") }
    var selectedGoal by remember { mutableStateOf("بدون هدف") }
    var scheduledDate by remember { mutableStateOf("اليوم") }
    var scheduledTime by remember { mutableStateOf("10:00 ص") }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var deadlineText by remember { mutableStateOf("") }
    var isFixed by remember { mutableStateOf(false) }
    var isLater by remember { mutableStateOf(false) }
    var isWeeklyRecurring by remember { mutableStateOf(false) }
    val selectedReminders = remember { mutableStateListOf("15 دقيقة قبل") }

    var projectMenuExpanded by remember { mutableStateOf(false) }
    var goalMenuExpanded by remember { mutableStateOf(false) }

    val reminderOptions = listOf(
        "5 دقائق قبل", "15 دقيقة قبل", "30 دقيقة قبل", "ساعة قبل",
        "ساعتين قبل", "يوم قبل", "3 أيام قبل", "أسبوع قبل"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with circular close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "مهمة جديدة",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    // 1. Task Title Input
                    WhackaTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "العنوان",
                        placeholder = "اكتب اسم المهمة",
                        modifier = Modifier.testTag("task_title_input")
                    )

                    // 2. Task Description Input (2 rows)
                    WhackaTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "الوصف",
                        placeholder = "تفاصيل إضافية (اختياري)",
                        singleLine = false,
                        minLines = 2
                    )

                    // 3. Category Pills (تصميم, تطوير, تسويق, عام, اجتماع)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "التصنيف",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WhackaCategory.entries.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                val catBg = if (isSelected) cat.color else Color.Transparent
                                val catText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                val catBorder = if (isSelected) cat.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(catBg)
                                        .border(BorderStroke(1.dp, catBorder), RoundedCornerShape(50))
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = catText
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 4. Priority Buttons
                    PriorityPillSelector(
                        selectedPriority = selectedPriority,
                        onPrioritySelected = { selectedPriority = it }
                    )

                    // 5. 2x2 Grid for Project & Goal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Project Dropdown Box
                        ExposedDropdownMenuBox(
                            expanded = projectMenuExpanded,
                            onExpandedChange = { projectMenuExpanded = !projectMenuExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            WhackaTextField(
                                value = selectedProject,
                                onValueChange = {},
                                label = "المشروع",
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = projectMenuExpanded,
                                onDismissRequest = { projectMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بدون مشروع") },
                                    onClick = {
                                        selectedProject = "بدون مشروع"
                                        projectMenuExpanded = false
                                    }
                                )
                                uiState.projects.forEach { proj ->
                                    DropdownMenuItem(
                                        text = { Text(proj.name) },
                                        onClick = {
                                            selectedProject = proj.name
                                            projectMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Goal Dropdown Box
                        ExposedDropdownMenuBox(
                            expanded = goalMenuExpanded,
                            onExpandedChange = { goalMenuExpanded = !goalMenuExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            WhackaTextField(
                                value = selectedGoal,
                                onValueChange = {},
                                label = "الهدف",
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = goalMenuExpanded,
                                onDismissRequest = { goalMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بدون هدف") },
                                    onClick = {
                                        selectedGoal = "بدون هدف"
                                        goalMenuExpanded = false
                                    }
                                )
                                uiState.goals.forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text(g.title) },
                                        onClick = {
                                            selectedGoal = g.title
                                            goalMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 6. 2-Column Row for Date & Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WhackaDatePickerField(
                            value = scheduledDate,
                            onDateSelected = { scheduledDate = it },
                            label = "التاريخ المجدول",
                            placeholder = "اليوم",
                            modifier = Modifier.weight(1f)
                        )
                        WhackaTimePickerField(
                            value = scheduledTime,
                            onTimeSelected = { scheduledTime = it },
                            label = "الوقت",
                            placeholder = "10:00 ص",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 7. 2-Column Row for Duration & Deadline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WhackaTextField(
                            value = "$durationMinutes",
                            onValueChange = { durationMinutes = it.toIntOrNull() ?: 45 },
                            label = "المدة (دقيقة)",
                            placeholder = "45",
                            modifier = Modifier.weight(1f)
                        )
                        WhackaDatePickerField(
                            value = deadlineText,
                            onDateSelected = { deadlineText = it },
                            label = "الموعد النهائي (اختياري)",
                            placeholder = "اختر تاريخ",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 8. Reminders Multi-Select Pills
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "التذكيرات",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            reminderOptions.forEach { rem ->
                                val isSelected = selectedReminders.contains(rem)
                                val remBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                val remText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                val remBorder = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(remBg)
                                        .border(BorderStroke(1.dp, remBorder), RoundedCornerShape(50))
                                        .clickable {
                                            if (isSelected) selectedReminders.remove(rem) else selectedReminders.add(rem)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rem,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = remText
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 9. Options Row ("موعد ثابت" & "تكرار أسبوعي" & "لاحقاً")
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    isFixed = !isFixed
                                    if (isFixed) isLater = false
                                }
                            ) {
                                Checkbox(
                                    checked = isFixed,
                                    onCheckedChange = { checked ->
                                        isFixed = checked
                                        if (checked) isLater = false
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "موعد ثابت",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    isWeeklyRecurring = !isWeeklyRecurring
                                }
                            ) {
                                Checkbox(
                                    checked = isWeeklyRecurring,
                                    onCheckedChange = { isWeeklyRecurring = it },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("task_weekly_recurring_checkbox")
                                )
                                Text(
                                    text = "تكرار أسبوعي",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isWeeklyRecurring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    isLater = !isLater
                                    if (isLater) isFixed = false
                                }
                            ) {
                                Checkbox(
                                    checked = isLater,
                                    onCheckedChange = { checked ->
                                        isLater = checked
                                        if (checked) isFixed = false
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "لاحقاً",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        if (isWeeklyRecurring) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "سيتم تكرار هذه المهمة أسبوعياً كل يوم بنفس التوقيت على مدار الأسابيع القادمة.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // 10. Whacka Submit Button (Full width, 52dp, Cairo Bold, 16dp rounded)
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val dateMillis = parseDateStringToMillis(scheduledDate, uiState.selectedDateTimestamp)
                                viewModel.addTask(
                                    title = title,
                                    subtitle = if (description.isNotBlank()) description else "تصنيف: ${selectedCategory.title}",
                                    time = if (isLater) "لاحقاً" else scheduledTime,
                                    durationMinutes = durationMinutes,
                                    colorType = selectedCategory.blockColor,
                                    isFixed = isFixed,
                                    dateMillis = dateMillis,
                                    isWeeklyRecurring = isWeeklyRecurring
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_task_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "حفظ المهمة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 5. Create Project Modal Dialog (Exact Whacka Specifications + AI Breakdown)
// =========================================================================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    viewModel: FocusViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("📁") }
    var selectedGoal by remember { mutableStateOf("بدون هدف") }
    var deadline by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableIntStateOf(10) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    val projectReminderOptions = listOf("يوم قبل الموعد", "3 أيام قبل", "أسبوع قبل")
    val selectedProjectReminders = remember { mutableStateListOf("يوم قبل الموعد") }

    val milestones = remember {
        mutableStateListOf(
            MilestoneDraft(title = "تخطيط المتطلبات والتصميم", deadline = "الأسبوع 1"),
            MilestoneDraft(title = "تطوير الواجهات والوظائف", deadline = "الأسبوع 2")
        )
    }

    var goalMenuExpanded by remember { mutableStateOf(false) }

    val iconOptions = listOf("📁", "💼", "🎯", "📊", "🚀", "🎨", "📣", "🛠️")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "مشروع جديد",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    // 1. AI Estimation Banner / Button ("ساعدني في تقدير المشروع")
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (name.isNotBlank()) {
                                    viewModel.generateTaskBreakdownForTitle(name, description)
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = WhackaAmber.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, WhackaAmber.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "تقدير الذكاء الاصطناعي",
                                tint = WhackaAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ساعدني في تقدير وتفكيك المشروع",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = WhackaAmber
                                    )
                                )
                                Text(
                                    text = "توليد مراحل ومهام مقترحة وساعات عمل بالذكاء الاصطناعي",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // 2. Project Name
                    WhackaTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "اسم المشروع",
                        placeholder = "مثال: بناء Portfolio"
                    )

                    // 3. Project Description
                    WhackaTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "الوصف",
                        placeholder = "تفاصيل إضافية (اختياري)",
                        singleLine = false,
                        minLines = 2
                    )

                    // 4. Icon Picker (8 emoji choices)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "الأيقونة",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            iconOptions.forEach { iconEmoji ->
                                val isSelected = selectedIcon == iconEmoji
                                val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                val bgCol = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgCol)
                                        .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(12.dp))
                                        .clickable { selectedIcon = iconEmoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = iconEmoji,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // 5. Linked Goal Dropdown
                    ExposedDropdownMenuBox(
                        expanded = goalMenuExpanded,
                        onExpandedChange = { goalMenuExpanded = !goalMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WhackaTextField(
                            value = selectedGoal,
                            onValueChange = {},
                            label = "الهدف المرتبط",
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = goalMenuExpanded,
                            onDismissRequest = { goalMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون هدف") },
                                onClick = {
                                    selectedGoal = "بدون هدف"
                                    goalMenuExpanded = false
                                }
                            )
                            uiState.goals.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.title) },
                                    onClick = {
                                        selectedGoal = g.title
                                        goalMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 6. 2-Column Row for Deadline & Estimated Hours
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WhackaDatePickerField(
                            value = deadline,
                            onDateSelected = { deadline = it },
                            label = "الموعد النهائي",
                            placeholder = "اختر تاريخ",
                            modifier = Modifier.weight(1f)
                        )
                        WhackaTextField(
                            value = "$estimatedHours",
                            onValueChange = { estimatedHours = it.toIntOrNull() ?: 10 },
                            label = "الساعات المقدّرة",
                            placeholder = "10",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 7. Priority Buttons
                    PriorityPillSelector(
                        selectedPriority = selectedPriority,
                        onPrioritySelected = { selectedPriority = it }
                    )

                    // 8. Reminders Multi-Select Pills
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "التذكيرات",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            projectReminderOptions.forEach { rem ->
                                val isSelected = selectedProjectReminders.contains(rem)
                                val remBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                val remText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                val remBorder = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(remBg)
                                        .border(BorderStroke(1.dp, remBorder), RoundedCornerShape(50))
                                        .clickable {
                                            if (isSelected) selectedProjectReminders.remove(rem) else selectedProjectReminders.add(rem)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rem,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = remText
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 9. Milestones Section (المراحل)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المراحل (Milestones)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            TextButton(
                                onClick = {
                                    val count = milestones.size + 1
                                    milestones.add(
                                        MilestoneDraft(
                                            title = "مرحلة جديدة $count",
                                            deadline = "قريباً"
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "إضافة مرحلة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إضافة مرحلة",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        if (milestones.isEmpty()) {
                            Text(
                                text = "لا توجد مراحل بعد. اضغط على إضافة مرحلة لتنظيم العمل.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        } else {
                            milestones.forEachIndexed { index, m ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}. ${m.title}",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                if (index > 0) {
                                                    IconButton(
                                                        onClick = {
                                                            val prev = milestones[index - 1]
                                                            milestones[index - 1] = milestones[index]
                                                            milestones[index] = prev
                                                        },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowUp,
                                                            contentDescription = "تحريك لأعلى",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                if (index < milestones.size - 1) {
                                                    IconButton(
                                                        onClick = {
                                                            val next = milestones[index + 1]
                                                            milestones[index + 1] = milestones[index]
                                                            milestones[index] = next
                                                        },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowDown,
                                                            contentDescription = "تحريك لأسفل",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { milestones.remove(m) },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف المرحلة",
                                                        tint = WhackaRed,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "الموعد: ${m.deadline}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (m.isCompleted) WhackaEmerald.copy(alpha = 0.15f) else Color(0x0D000000))
                                                    .clickable { m.isCompleted = !m.isCompleted }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (m.isCompleted) "مكتملة ✓" else "قيد الانتظار",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        color = if (m.isCompleted) WhackaEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 9. Whacka Submit Button (Full width, 52dp, Cairo Bold, 16dp rounded)
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                viewModel.addProject(
                                    name = "$selectedIcon $name",
                                    description = description,
                                    color = "TEAL",
                                    taskCount = milestones.size.coerceAtLeast(1)
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "حفظ المشروع",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 6. Create Event Modal Dialog (Whacka Clean Style)
// =========================================================================

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CreateEventDialog(
    viewModel: FocusViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("اليوم") }
    var startTime by remember { mutableStateOf("02:00 م") }
    var endTime by remember { mutableStateOf("03:00 م") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var isFixed by remember { mutableStateOf(true) }
    var isWeeklyRecurring by remember { mutableStateOf(false) }

    val eventReminderOptions = listOf("5 دقائق قبل", "15 دقيقة قبل", "30 دقيقة قبل", "ساعة قبل")
    val selectedEventReminders = remember { mutableStateListOf("15 دقيقة قبل") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "موعد جديد",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    WhackaTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "عنوان الموعد",
                        placeholder = "مثال: اجتماع فريق التطوير"
                    )

                    WhackaTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "الوصف",
                        placeholder = "تفاصيل الموعد أو الأجندة (اختياري)",
                        singleLine = false,
                        minLines = 2
                    )

                    WhackaDatePickerField(
                        value = eventDate,
                        onDateSelected = { eventDate = it },
                        label = "التاريخ",
                        placeholder = "اليوم"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WhackaTimePickerField(
                            value = startTime,
                            onTimeSelected = { startTime = it },
                            label = "وقت البدء",
                            placeholder = "02:00 م",
                            modifier = Modifier.weight(1f)
                        )
                        WhackaTimePickerField(
                            value = endTime,
                            onTimeSelected = { endTime = it },
                            label = "وقت الانتهاء",
                            placeholder = "03:00 م",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    WhackaTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = "الموقع أو الرابط (اختياري)",
                        placeholder = "مثال: Google Meet أو قاعة 3"
                    )

                    WhackaTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "ملاحظات إضافية (اختياري)",
                        placeholder = "أي تجهيزات أو مرفقات",
                        singleLine = false,
                        minLines = 2
                    )

                    PriorityPillSelector(
                        selectedPriority = selectedPriority,
                        onPrioritySelected = { selectedPriority = it }
                    )

                    // Reminders Multi-Select
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "التذكيرات",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            eventReminderOptions.forEach { rem ->
                                val isSelected = selectedEventReminders.contains(rem)
                                val remBg = if (isSelected) WhackaAmber else Color.Transparent
                                val remText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                val remBorder = if (isSelected) WhackaAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(remBg)
                                        .border(BorderStroke(1.dp, remBorder), RoundedCornerShape(50))
                                        .clickable {
                                            if (isSelected) selectedEventReminders.remove(rem) else selectedEventReminders.add(rem)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rem,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = remText
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Checkboxes for "موعد ثابت" & "تكرار أسبوعي"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isFixed = !isFixed }
                        ) {
                            Checkbox(
                                checked = isFixed,
                                onCheckedChange = { isFixed = it },
                                colors = CheckboxDefaults.colors(checkedColor = WhackaAmber)
                            )
                            Text(
                                text = "موعد ثابت (لا يقبل النقل أثناء الجدولة التكيفية)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isWeeklyRecurring = !isWeeklyRecurring }
                        ) {
                            Checkbox(
                                checked = isWeeklyRecurring,
                                onCheckedChange = { isWeeklyRecurring = it },
                                colors = CheckboxDefaults.colors(checkedColor = WhackaAmber),
                                modifier = Modifier.testTag("event_weekly_recurring_checkbox")
                            )
                            Text(
                                text = "تكرار أسبوعي (يتكرر كل أسبوع في نفس اليوم)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isWeeklyRecurring) WhackaAmber else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        if (isWeeklyRecurring) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WhackaAmber.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, WhackaAmber.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "سيظهر هذا الموعد تلقائياً في جدول هذا اليوم من كل أسبوع.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = WhackaAmber,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val sub = when {
                                    location.isNotBlank() && description.isNotBlank() -> "$location • $description"
                                    location.isNotBlank() -> "الموقع: $location"
                                    description.isNotBlank() -> description
                                    else -> "موعد ثابت"
                                }
                                val dateMillis = parseDateStringToMillis(eventDate, viewModel.uiState.value.selectedDateTimestamp)
                                viewModel.addTask(
                                    title = title,
                                    subtitle = sub,
                                    time = startTime,
                                    durationMinutes = 60,
                                    colorType = BlockColor.ORANGE,
                                    isFixed = isFixed,
                                    dateMillis = dateMillis,
                                    isWeeklyRecurring = isWeeklyRecurring
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhackaAmber,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "حفظ الموعد",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 7. Create Goal Modal Dialog (Whacka Clean Style)
// =========================================================================

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalDialog(
    viewModel: FocusViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("متوسطة") }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = java.util.Calendar.getInstance()
                            calendar.timeInMillis = millis
                            val formatted = String.format(
                                java.util.Locale.US,
                                "%02d/%02d/%04d",
                                calendar.get(java.util.Calendar.MONTH) + 1,
                                calendar.get(java.util.Calendar.DAY_OF_MONTH),
                                calendar.get(java.util.Calendar.YEAR)
                            )
                            deadline = formatted
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("اختيار", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("إلغاء")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header: Close Button (Left) & Centered Title "هدف جديد"
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "هدف جديد",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. Goal Name Field ("اسم الهدف")
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "اسم الهدف",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = {
                                Text(
                                    text = "مثال: بناء مصدر دخل جديد",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color(0xFF0F6E60),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = Color(0xFF0F6E60)
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )
                    }

                    // 2. Goal Description Field ("الوصف")
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "الوصف",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = {
                                Text(
                                    text = "لماذا هذا الهدف مهم لك؟",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp),
                            singleLine = false,
                            minLines = 3,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color(0xFF0F6E60),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = Color(0xFF0F6E60)
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )
                    }

                    // 3. Two Column Row: [الموعد النهائي] & [الأولوية]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Priority Selector Column ("الأولوية")
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "الأولوية",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { priorityMenuExpanded = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "قائمة الأولويات",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Text(
                                            text = priority,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = priorityMenuExpanded,
                                    onDismissRequest = { priorityMenuExpanded = false }
                                ) {
                                    listOf("عالية", "متوسطة", "منخفضة").forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = p,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (p == priority) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (p == "عالية") Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            },
                                            onClick = {
                                                priority = p
                                                priorityMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Deadline Date Selector Column ("الموعد النهائي")
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "الموعد النهائي",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showDatePicker = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = deadline.ifBlank { "mm/dd/yyyy" },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            color = if (deadline.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                        )
                                    )

                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = "اختيار التاريخ",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 4. Save Goal Button ("حفظ الهدف") - Cairo Bold, Full Width, Teal Green #0F6E60
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.addGoal(
                                    title = title,
                                    description = description,
                                    category = "عام",
                                    targetCount = 10,
                                    deadline = deadline.ifBlank { "مستمر" },
                                    color = if (priority == "عالية") "RED" else "TEAL",
                                    priority = priority
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F6E60),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "حفظ الهدف",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
