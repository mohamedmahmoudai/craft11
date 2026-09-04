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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockColor
import com.example.model.DayCapacityLevel
import com.example.model.DayItem
import com.example.model.TimelineItem
import com.example.ui.theme.WhackaAmber
import com.example.ui.theme.WhackaEmerald
import com.example.ui.theme.WhackaPrimaryAccent
import com.example.ui.theme.WhackaPrimaryShadow
import com.example.ui.theme.WhackaRed
import com.example.ui.theme.WhackaSecondaryAccent
import java.util.Calendar
import java.util.Locale

@Composable
fun WeekScreen(
    days: List<DayItem>,
    weekTitle: String,
    timelineItems: List<TimelineItem>,
    onSelectDay: (DayItem) -> Unit,
    onPrevWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    onBlockClick: (TimelineItem.TaskBlock) -> Unit = {},
    onGapClick: (TimelineItem.FocusGap) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val hourHeightDp = 80.dp
    val hourHeightVal = 80f
    val timelineStartHour = 6.0f
    val timelineEndHour = 24.0f
    val totalHours = (timelineEndHour - timelineStartHour).toInt()

    val hours = remember {
        (6..24).map { h ->
            String.format(Locale.US, "%02d:00", if (h == 24) 0 else h)
        }
    }

    val currentCal = Calendar.getInstance()
    val currentHourFloat = currentCal.get(Calendar.HOUR_OF_DAY) + currentCal.get(Calendar.MINUTE) / 60f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Week Navigator Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "الأسبوع",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = weekTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Prev / Next Week buttons (Whacka pill buttons)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevWeek,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "الأسبوع السابق",
                        tint = WhackaPrimaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onNextWeek,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "الأسبوع القادم",
                        tint = WhackaPrimaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Horizontal Days of Week Selector (WhackaCard 24dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = WhackaPrimaryShadow,
                    spotColor = WhackaPrimaryShadow
                ),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(days) { day ->
                    val isSelected = day.isSelected
                    val isToday = day.isToday

                    val dayBg = when {
                        isSelected -> WhackaPrimaryAccent
                        isToday -> WhackaPrimaryAccent.copy(alpha = 0.12f)
                        else -> Color.Transparent
                    }

                    val textColor = when {
                        isSelected -> Color.White
                        isToday -> WhackaPrimaryAccent
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    val subTextColor = when {
                        isSelected -> Color.White.copy(alpha = 0.8f)
                        isToday -> WhackaPrimaryAccent.copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val capacityBadgeColor = when (day.capacityLevel) {
                        DayCapacityLevel.PACKED -> WhackaRed
                        DayCapacityLevel.BALANCED -> WhackaAmber
                        DayCapacityLevel.LIGHT -> WhackaEmerald
                    }

                    Surface(
                        modifier = Modifier
                            .width(52.dp)
                            .height(78.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectDay(day) },
                        shape = RoundedCornerShape(16.dp),
                        color = dayBg,
                        border = if (!isSelected && isToday) BorderStroke(1.dp, WhackaPrimaryAccent) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = day.dayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = subTextColor
                            )

                            Text(
                                text = day.dayNumber.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = textColor
                            )

                            // Capacity Dot Indicator
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else capacityBadgeColor)
                            )
                        }
                    }
                }
            }
        }

        // Timeline Scroll Area (WhackaCard 24dp)
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
                .testTag("week_timeline_container"),
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
                // Hour Markers Column
                Column(
                    modifier = Modifier
                        .width(62.dp)
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    hours.forEach { hour ->
                        Box(
                            modifier = Modifier
                                .height(hourHeightDp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = hour,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Grid & Timeline Items Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((hourHeightVal * totalHours + hourHeightVal).dp)
                        .padding(end = 12.dp)
                ) {
                    // Divider lines
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

                    // Focus Gaps & Task Blocks
                    timelineItems.forEach { item ->
                        when (item) {
                            is TimelineItem.FocusGap -> {
                                val topOffset = (hourHeightVal * (item.startHour - timelineStartHour).coerceAtLeast(0f)).dp
                                val blockHeight = ((hourHeightVal * item.durationHours) - 8f).coerceAtLeast(42f).dp

                                AvailableWeekGapBlock(
                                    title = "وقت متاح للتركيز (${(item.durationHours * 60).toInt()} د)",
                                    timeRange = item.timeRange,
                                    color = WhackaPrimaryAccent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = topOffset + 4.dp)
                                        .height(blockHeight),
                                    onClick = { onGapClick(item) }
                                )
                            }

                            is TimelineItem.TaskBlock -> {
                                val topOffset = (hourHeightVal * (item.startHour - timelineStartHour).coerceAtLeast(0f)).dp
                                val blockHeight = ((hourHeightVal * item.durationHours) - 8f).coerceAtLeast(54f).dp

                                WeekTaskBlock(
                                    block = item,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = topOffset + 4.dp)
                                        .height(blockHeight),
                                    onClick = { onBlockClick(item) }
                                )
                            }
                        }
                    }

                    // Current Time Line
                    if (currentHourFloat in timelineStartHour..timelineEndHour) {
                        val currentOffset = (hourHeightVal * (currentHourFloat - timelineStartHour)).dp
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
}

@Composable
fun WeekTaskBlock(
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
            .clickable { onClick() },
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

@Composable
fun AvailableWeekGapBlock(
    title: String,
    timeRange: String,
    color: Color,
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = color.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
