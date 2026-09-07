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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddPhotoAlternate
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import com.example.model.BlockColor
import com.example.model.DailyCapacityState
import com.example.model.DayItem
import com.example.model.TimelineItem
import com.example.viewmodel.FocusViewModel
import com.example.ui.components.ImportScheduleReviewDialog
import com.example.ui.components.WhackaFlatTextField
import com.example.ui.theme.WhackaAmber
import com.example.ui.theme.WhackaEmerald
import com.example.ui.theme.WhackaPrimaryAccent
import com.example.ui.theme.WhackaPrimaryShadow
import com.example.ui.theme.WhackaRed
import com.example.ui.theme.WhackaSecondaryAccent
import com.example.util.ImportedScheduleDraft
import com.example.util.ScheduleOcrParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    timelineItems: List<TimelineItem>,
    dailyCapacity: DailyCapacityState,
    selectedDateMillis: Long,
    days: List<DayItem> = emptyList(),
    onSelectDay: (DayItem) -> Unit = {},
    onSelectDateMillis: (Long) -> Unit = {},
    onQuickAddEvent: () -> Unit = {},
    onTriggerAlarm: (TimelineItem.TaskBlock) -> Unit,
    onToggleCompletion: (String) -> Unit = {},
    onDeleteTask: (String) -> Unit = {},
    onRescheduleTask: (String, String, String?, String?, Int) -> Unit = { _, _, _, _, _ -> },
    onAddNewTaskAtTime: (Float) -> Unit = {},
    onImportTask: (String, String, Int, Boolean) -> Unit = { _, _, _, _ -> },
    onImportScheduleDrafts: (List<ImportedScheduleDraft>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isScanningOcr by remember { mutableStateOf(false) }
    var ocrDrafts by remember { mutableStateOf<List<ImportedScheduleDraft>>(emptyList()) }
    var showOcrReviewDialog by remember { mutableStateOf(false) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isScanningOcr = true
            scanErrorMessage = null
            coroutineScope.launch {
                val result = ScheduleOcrParser.recognizeTextFromUri(context, uri)
                result.onSuccess { visionText ->
                    val drafts = ScheduleOcrParser.parseScheduleItems(visionText)
                    ocrDrafts = drafts
                    showOcrReviewDialog = true
                    isScanningOcr = false
                }.onFailure { error ->
                    scanErrorMessage = "تعذر قراءة الصورة: ${error.localizedMessage ?: "خطأ غير متوقع"}"
                    isScanningOcr = false
                }
            }
        }
    }

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

    val displayedDays = remember(days, selectedDateMillis) {
        if (days.isNotEmpty()) days
        else {
            val cal = Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                cal.add(Calendar.DAY_OF_MONTH, -1)
            }
            val names = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
            val todayCal = Calendar.getInstance()
            val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            (0..6).map { i ->
                val ts = cal.timeInMillis
                val num = cal.get(Calendar.DAY_OF_MONTH)
                val name = names[i]
                val isSel = (cal.get(Calendar.YEAR) == selCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == selCal.get(Calendar.DAY_OF_YEAR))
                val isTod = (cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR))
                cal.add(Calendar.DAY_OF_MONTH, 1)
                DayItem(
                    dayName = name,
                    dayNumber = num,
                    isSelected = isSel,
                    isToday = isTod,
                    timestamp = ts
                )
            }
        }
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // OCR Schedule Import Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("ocr_schedule_import_btn"),
                        shape = RoundedCornerShape(50),
                        color = WhackaPrimaryAccent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, WhackaPrimaryAccent.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "استيراد صورة",
                                tint = WhackaPrimaryAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "استيراد صورة",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = WhackaPrimaryAccent
                            )
                        }
                    }

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
            }

            // Highlighted Header Container with Horizontal Week-Day Strip & Selected Day Quick Action
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top row: Full date + Quick Action (+ موعد جديد لليوم)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = dateHeaderStr,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (taskCount == 0) "لا توجد فترات مجدولة (يوم متاح)" else "$taskCount فترات مجدولة",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (taskCount == 0) WhackaEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick Action to add event for this selected day
                        Button(
                            onClick = onQuickAddEvent,
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("schedule_quick_add_event_btn"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhackaAmber,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ موعد جديد",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Horizontal Week-Day Strip (LazyRow)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(displayedDays, key = { it.timestamp }) { day ->
                            val isSelected = FocusViewModel.isSameDay(day.timestamp, selectedDateMillis)
                            val isToday = day.isToday

                            Surface(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onSelectDay(day)
                                        onSelectDateMillis(day.timestamp)
                                    }
                                    .testTag("schedule_day_pill_${day.dayName}"),
                                shape = RoundedCornerShape(16.dp),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else if (isToday) 1.5.dp else 1.dp,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    }
                                ),
                                shadowElevation = if (isSelected) 3.dp else 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = day.dayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = when {
                                            isSelected -> Color.White
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )

                                    Text(
                                        text = "${day.dayNumber}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = when {
                                            isSelected -> Color.White
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> Color.White
                                                    isToday -> MaterialTheme.colorScheme.primary
                                                    day.bookedMinutes > 0 -> WhackaAmber
                                                    else -> Color.Transparent
                                                }
                                            )
                                    )
                                }
                            }
                        }
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
                                    },
                                    onDelete = {
                                        item.originalTaskId?.let { onDeleteTask(it) }
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

    // OCR Review Dialog
    if (showOcrReviewDialog) {
        ImportScheduleReviewDialog(
            initialDrafts = ocrDrafts,
            onDismiss = { showOcrReviewDialog = false },
            onConfirmImport = { selectedDrafts ->
                onImportScheduleDrafts(selectedDrafts)
                showOcrReviewDialog = false
            }
        )
    }

    // OCR Scanning Progress Dialog
    if (isScanningOcr) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Text(
                    text = "جارٍ استيراد الجدول من الصورة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "يتم تحليل النص عبر ML Kit واستخراج الأوقات والمواعيد...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Scan Error Dialog
    if (scanErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { scanErrorMessage = null },
            confirmButton = {
                TextButton(onClick = { scanErrorMessage = null }) {
                    Text("حسناً")
                }
            },
            title = { Text("تنبيه") },
            text = { Text(scanErrorMessage ?: "") },
            shape = RoundedCornerShape(20.dp)
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
    onClick: () -> Unit = {},
    onDelete: (() -> Unit)? = null
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
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
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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

                        if (onDelete != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { onDelete() }
                                    .testTag("delete_task_quick_${block.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف الموعد",
                                    tint = WhackaRed.copy(alpha = 0.8f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
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
