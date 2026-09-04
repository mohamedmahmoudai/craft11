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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockColor
import com.example.model.DailyCapacityState
import com.example.model.TimelineItem
import com.example.ui.components.WhackaFlatTextField
import com.example.ui.theme.WhackaAmber
import com.example.ui.theme.WhackaEmerald
import com.example.ui.theme.WhackaPrimaryAccent
import com.example.ui.theme.WhackaPrimaryShadow
import com.example.ui.theme.WhackaRed
import com.example.ui.theme.WhackaSecondaryAccent
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    timelineItems: List<TimelineItem>,
    dailyCapacity: DailyCapacityState,
    selectedDateMillis: Long,
    onTriggerAlarm: (TimelineItem.TaskBlock) -> Unit,
    onToggleCompletion: (String) -> Unit = {},
    onDeleteTask: (String) -> Unit = {},
    onRescheduleTask: (String, String, String?, String?, Int) -> Unit = { _, _, _, _, _ -> },
    onAddNewTaskAtTime: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // State for bottom sheet
    var selectedTaskBlock by remember { mutableStateOf<TimelineItem.TaskBlock?>(null) }
    var showRescheduleDialog by remember { mutableStateOf(false) }

    // Dynamic current time in decimal hours
    var currentHourFloat by remember {
        val cal = Calendar.getInstance()
        mutableFloatStateOf(cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f)
    }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            currentHourFloat = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
            delay(30000)
        }
    }

    val dateHeaderStr = remember(selectedDateMillis) {
        val sdf = SimpleDateFormat("EEEE، d MMMM", Locale("ar"))
        sdf.format(selectedDateMillis)
    }

    val taskCount = timelineItems.count { it is TimelineItem.TaskBlock }
    val hourHeightDp = 84.dp
    val hourHeightVal = 84f
    val timelineStartHour = 6.0f
    val timelineEndHour = 24.0f
    val totalHours = (timelineEndHour - timelineStartHour).toInt()

    val hourLabels = remember {
        (6..24).map { h ->
            String.format(Locale.US, "%02d:00", if (h == 24) 0 else h)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الجدول اليومي",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Capacity status pill (50 shape)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (dailyCapacity.isOverbooked) WhackaAmber.copy(alpha = 0.15f)
                            else WhackaPrimaryAccent.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (dailyCapacity.isOverbooked) WhackaAmber.copy(alpha = 0.5f)
                            else WhackaPrimaryAccent.copy(alpha = 0.4f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "محجوز: ${dailyCapacity.bookedTimeFormatted}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (dailyCapacity.isOverbooked) WhackaAmber else WhackaPrimaryAccent
                    )
                }
            }

            Text(
                text = "$dateHeaderStr • $taskCount فترات مجدولة",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (taskCount == 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = WhackaPrimaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "يومك فارغ تماماً. لديك متسع كبير من الوقت المتاح.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Vertical Hour-by-Hour Timeline Card (WhackaCard 24dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 12.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = WhackaPrimaryShadow,
                    spotColor = WhackaPrimaryShadow
                )
                .testTag("schedule_timeline_container"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = 16.dp)
            ) {
                // Hour markers column (Right-aligned in RTL layout)
                Column(
                    modifier = Modifier
                        .width(62.dp)
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    hourLabels.forEach { label ->
                        Box(
                            modifier = Modifier
                                .height(hourHeightDp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Grid & Timeline items area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((hourHeightVal * totalHours + hourHeightVal).dp)
                        .padding(end = 12.dp)
                ) {
                    // 1. Divider lines for each hour
                    Column(modifier = Modifier.fillMaxWidth()) {
                        repeat(totalHours + 1) {
                            Box(
                                modifier = Modifier
                                    .height(hourHeightDp)
                                    .fillMaxWidth()
                                    .border(
                                        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    )
                            )
                        }
                    }

                    // 2. Focus Gaps & Task Blocks
                    timelineItems.forEach { item ->
                        when (item) {
                            is TimelineItem.FocusGap -> {
                                val topOffset = (hourHeightVal * (item.startHour - timelineStartHour).coerceAtLeast(0f)).dp
                                val blockHeight = ((hourHeightVal * item.durationHours) - 8f).coerceAtLeast(42f).dp

                                FocusGapCard(
                                    gap = item,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = topOffset + 4.dp)
                                        .height(blockHeight),
                                    onClick = {
                                        onAddNewTaskAtTime(item.startHour)
                                    }
                                )
                            }

                            is TimelineItem.TaskBlock -> {
                                val topOffset = (hourHeightVal * (item.startHour - timelineStartHour).coerceAtLeast(0f)).dp
                                val blockHeight = ((hourHeightVal * item.durationHours) - 8f).coerceAtLeast(54f).dp

                                TaskBlockCard(
                                    block = item,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = topOffset + 4.dp)
                                        .height(blockHeight),
                                    onClick = {
                                        selectedTaskBlock = item
                                    }
                                )
                            }
                        }
                    }

                    // 3. Current Time Indicator (Thin horizontal accent line with time tag)
                    if (currentHourFloat in timelineStartHour..timelineEndHour) {
                        val currentOffset = (hourHeightVal * (currentHourFloat - timelineStartHour)).dp
                        val currentCal = Calendar.getInstance()
                        val timeTag = String.format(
                            Locale.US,
                            "%02d:%02d",
                            currentCal.get(Calendar.HOUR_OF_DAY),
                            currentCal.get(Calendar.MINUTE)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = currentOffset)
                                .height(2.dp)
                                .background(WhackaRed.copy(alpha = 0.95f))
                                .testTag("current_time_line")
                        ) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(x = 6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(WhackaRed)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = timeTag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet for Task Details & Actions (24dp rounded corners)
    selectedTaskBlock?.let { block ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedTaskBlock = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val accentColor = when (block.colorType) {
                BlockColor.INDIGO, BlockColor.TEAL -> WhackaPrimaryAccent
                BlockColor.ORANGE -> WhackaSecondaryAccent
                BlockColor.GREEN -> WhackaEmerald
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header: Title, Category dot & Fixed badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (block.isFixed) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = WhackaEmerald.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, WhackaEmerald.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = WhackaEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "حدث ثابت",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = WhackaEmerald
                                )
                            }
                        }
                    }
                }

                // Subtitle & Time Info
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (block.subtitle.isNotBlank()) {
                        Text(
                            text = block.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = block.timeRange,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (block.isCompleted) WhackaEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (block.isCompleted) "مكتملة" else "قيد التنفيذ",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (block.isCompleted) WhackaEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons: Pill shapes (50)
                Button(
                    onClick = {
                        onTriggerAlarm(block)
                        selectedTaskBlock = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_start_focus_sheet"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بدء جلسة التركيز والمنبه الذكي",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        block.originalTaskId?.let { onToggleCompletion(it) }
                        selectedTaskBlock = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (block.isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (block.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else WhackaEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (block.isCompleted) "إلغاء الإنجاز" else "تعيين كمكتملة",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showRescheduleDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            tint = WhackaPrimaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تغيير الموعد",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            block.originalTaskId?.let { onDeleteTask(it) }
                            selectedTaskBlock = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, WhackaRed.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WhackaRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = WhackaRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حذف المهمة",
                            color = WhackaRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Reschedule Dialog (24dp rounded corners)
    if (showRescheduleDialog && selectedTaskBlock != null) {
        val block = selectedTaskBlock!!
        var newTimeText by remember { mutableStateOf(block.timeRange) }
        var newDurationMins by remember { mutableStateOf((block.durationHours * 60).toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showRescheduleDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "تغيير موعد المهمة",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "المهمة: ${block.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    WhackaFlatTextField(
                        value = newTimeText,
                        onValueChange = { newTimeText = it },
                        label = "الموعد الجديد",
                        placeholder = "مثال: 02:00 - 03:30 م"
                    )

                    WhackaFlatTextField(
                        value = newDurationMins,
                        onValueChange = { newDurationMins = it },
                        label = "المدة بالدقائق",
                        placeholder = "مثال: 90"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dur = newDurationMins.toIntOrNull() ?: 90
                        block.originalTaskId?.let { id ->
                            onRescheduleTask(id, newTimeText, null, null, dur)
                        }
                        showRescheduleDialog = false
                        selectedTaskBlock = null
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = WhackaPrimaryAccent)
                ) {
                    Text("حفظ التغييرات", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescheduleDialog = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Task Block card inside the timeline (Whacka shape & accents)
 */
@Composable
fun TaskBlockCard(
    block: TimelineItem.TaskBlock,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val baseColor = when (block.colorType) {
        BlockColor.INDIGO, BlockColor.TEAL -> WhackaPrimaryAccent
        BlockColor.ORANGE -> WhackaSecondaryAccent
        BlockColor.GREEN -> WhackaEmerald
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (block.isCompleted) WhackaEmerald.copy(alpha = 0.5f)
                    else baseColor.copy(alpha = if (block.isFixed) 0.8f else 0.45f)
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .testTag("timeline_task_${block.id}"),
        shape = RoundedCornerShape(16.dp),
        color = baseColor.copy(alpha = if (block.isFixed) 0.22f else 0.14f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (block.isCompleted) WhackaEmerald else baseColor)
                        )

                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (block.isFixed) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = WhackaEmerald.copy(alpha = 0.2f)
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

                if (block.subtitle.isNotBlank()) {
                    Text(
                        text = block.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = block.timeRange,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = baseColor
            )
        }
    }
}

/**
 * Focus Gap / Free Time block card
 */
@Composable
fun FocusGapCard(
    gap: TimelineItem.FocusGap,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("focus_gap_${gap.id}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    tint = WhackaPrimaryAccent.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "وقت متاح للتركيز (${(gap.durationHours * 60).toInt()} د)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = gap.timeRange,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
