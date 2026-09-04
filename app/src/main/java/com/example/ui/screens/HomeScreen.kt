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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockColor
import com.example.model.DailyCapacityState
import com.example.model.RescheduleSuggestion
import com.example.model.TaskItem
import com.example.ui.theme.WhackaAmber
import com.example.ui.theme.WhackaAmberBg
import com.example.ui.theme.WhackaAmberDark
import com.example.ui.theme.WhackaEmerald
import com.example.ui.theme.WhackaEmeraldBg
import com.example.ui.theme.WhackaPrimaryAccent
import com.example.ui.theme.WhackaPrimaryShadow
import com.example.ui.theme.WhackaSecondaryAccent

@Composable
fun HomeScreen(
    userName: String,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    currentFocusTask: TaskItem,
    tasks: List<TaskItem>,
    dailyCapacity: DailyCapacityState = DailyCapacityState(),
    selectedCapacityMinutes: Int = 180,
    selectedEnergyLevel: String = "طبيعية",
    isDayUnavailable: Boolean = false,
    adaptiveSuggestions: List<RescheduleSuggestion> = emptyList(),
    isAdaptiveBannerDismissed: Boolean = false,
    onSaveCapacityPreferences: (Int, String) -> Unit = { _, _ -> },
    onSetDayUnavailable: (Boolean) -> Unit = {},
    onOpenAdaptiveSheet: () -> Unit = {},
    onDismissAdaptiveBanner: () -> Unit = {},
    onToggleTask: (String) -> Unit,
    onStartFocus: () -> Unit,
    onRequestBatteryOptimization: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size
    val progressRatio = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    var localMinutes by remember(selectedCapacityMinutes) { mutableIntStateOf(selectedCapacityMinutes) }
    var localEnergy by remember(selectedEnergyLevel) { mutableStateOf(selectedEnergyLevel) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. Header (Greeting, Avatar, Theme Toggle, Progress)
        // ==========================================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // User Avatar (Whacka 16dp rounded box)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(WhackaPrimaryAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.take(1).ifBlank { "م" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = WhackaPrimaryAccent
                                )
                            )
                        }

                        Column {
                            Text(
                                text = "أهلاً بك، $userName 👋",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "خطة اليوم وإدارة التركيز",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Theme Toggle Button
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .testTag("theme_toggle_button")
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "تبديل المظهر",
                            tint = WhackaPrimaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Daily Progress Bar Card (WhackaCard 24dp)
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إنجاز اليوم",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "$completedCount من $totalCount مهام (${(progressRatio * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = WhackaPrimaryAccent
                                )
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .testTag("today_progress_bar"),
                            color = WhackaPrimaryAccent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. Warning / Attention Banner
        // ==========================================
        if (adaptiveSuggestions.isNotEmpty() && !isAdaptiveBannerDismissed) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = WhackaSecondaryAccent.copy(alpha = 0.18f),
                            spotColor = WhackaSecondaryAccent.copy(alpha = 0.18f)
                        )
                        .testTag("warning_banner"),
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDarkMode) WhackaAmberBg.copy(alpha = 0.15f) else WhackaAmberBg,
                    border = BorderStroke(1.dp, WhackaAmber.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WhackaAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = WhackaAmberDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "تحتاج انتباه: ${adaptiveSuggestions.size} مهام بحاجة لإعادة جدولة",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF92400E)
                                    )
                                )
                                Text(
                                    text = "تكييف الخطة وتفادي التراكم فوراً",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color(0xFFFCD34D) else Color(0xFFB45309)
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenAdaptiveSheet,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(WhackaAmberDark.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تكييف الخطة",
                                tint = WhackaAmberDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. Daily Capacity Widget ("كيف يبدو وقتك اليوم؟")
        // ==========================================
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = WhackaPrimaryShadow,
                        spotColor = WhackaPrimaryShadow
                    )
                    .testTag("capacity_widget_card"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "كيف يبدو وقتك اليوم؟",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        if (isDayUnavailable) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "اليوم غير متاح",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Available Time Horizontal Chips Row
                    Text(
                        text = "الوقت المتاح للتركيز:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    val timeOptions = listOf(
                        30 to "30 دقيقة",
                        60 to "ساعة",
                        120 to "ساعتان",
                        180 to "3 ساعات",
                        240 to "4 ساعات",
                        300 to "5+ ساعات"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(timeOptions) { (mins, label) ->
                            val isSelected = localMinutes == mins && !isDayUnavailable
                            val chipBg = if (isSelected) WhackaPrimaryAccent else Color.Transparent
                            val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            val chipBorder = if (isSelected) WhackaPrimaryAccent else MaterialTheme.colorScheme.outline

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(chipBg)
                                    .border(BorderStroke(1.dp, chipBorder), RoundedCornerShape(50))
                                    .clickable {
                                        localMinutes = mins
                                        if (isDayUnavailable) onSetDayUnavailable(false)
                                        onSaveCapacityPreferences(mins, localEnergy)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = chipText
                                    )
                                )
                            }
                        }
                    }

                    // Energy Level Selector Row
                    Text(
                        text = "مستوى طاقتك ومزاجك:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    val energyOptions = listOf("منخفضة", "طبيعية", "عالية")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        energyOptions.forEach { energy ->
                            val isSelected = localEnergy == energy && !isDayUnavailable
                            val chipBg = if (isSelected) WhackaPrimaryAccent else Color.Transparent
                            val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            val chipBorder = if (isSelected) WhackaPrimaryAccent else MaterialTheme.colorScheme.outline

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(chipBg)
                                    .border(BorderStroke(1.dp, chipBorder), RoundedCornerShape(50))
                                    .clickable {
                                        localEnergy = energy
                                        if (isDayUnavailable) onSetDayUnavailable(false)
                                        onSaveCapacityPreferences(localMinutes, energy)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = energy,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = chipText
                                    )
                                )
                            }
                        }
                    }

                    // Action Buttons (Save & Mark Unavailable)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onSaveCapacityPreferences(localMinutes, localEnergy)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("save_capacity_btn"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhackaPrimaryAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "حفظ التفضيلات",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onSetDayUnavailable(!isDayUnavailable)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("toggle_day_unavailable_btn"),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = if (isDayUnavailable) "إلغاء التعطيل" else "اليوم غير متاح",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. Active Task Card (Current Focus)
        // ==========================================
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = WhackaPrimaryShadow,
                        spotColor = WhackaPrimaryShadow
                    )
                    .testTag("active_task_card"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = WhackaPrimaryAccent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "المهمة النشطة الآن",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = WhackaPrimaryAccent
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${currentFocusTask.durationMinutes} دقيقة",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentFocusTask.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = currentFocusTask.subtitle.ifBlank { "جلسة تركيز بدون مقاطعات" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        // Prominent circular Play button
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(WhackaPrimaryAccent)
                                .clickable { onStartFocus() }
                                .testTag("start_focus_circular_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "بدء التركيز",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Full Pill Button
                    Button(
                        onClick = onStartFocus,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("start_focus_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhackaPrimaryAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "بدء جلسة التركيز الآن",
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

        // ==========================================
        // 5. Today's Tasks Section
        // ==========================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مهام اليوم",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text(
                    text = "$totalCount مهام",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        if (tasks.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "لا توجد مهام مجدولة لليوم",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "انقر على زر + بالأسفل لإضافة مهمة أو موعد جديد",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                WhackaTaskCard(
                    task = task,
                    onToggle = { onToggleTask(task.id) }
                )
            }
        }
    }
}

/**
 * Task card matching Whacka specifications:
 * - 24.dp rounded corners
 * - Primary accent elevation/shadow (alpha 0.18)
 * - 4.dp left/right accent border matching category color
 */
@Composable
fun WhackaTaskCard(
    task: TaskItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            .clickable { onToggle() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 4.dp Category Accent Border stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(categoryColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Checkbox icon
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
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (task.subtitle.isNotBlank()) {
                                Text(
                                    text = task.subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            if (task.timeText.isNotBlank()) {
                                Text(
                                    text = "•  ${task.timeText}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // Duration badge (Outlined pill)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${task.durationMinutes}د",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
