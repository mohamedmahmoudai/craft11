package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockColor
import com.example.model.TaskItem
import com.example.ui.theme.WhackaAmber
import com.example.ui.theme.WhackaBackground
import com.example.ui.theme.WhackaBorder
import com.example.ui.theme.WhackaEmerald
import com.example.ui.theme.WhackaPrimaryAccent
import com.example.ui.theme.WhackaPrimaryShadow
import com.example.ui.theme.WhackaRed
import com.example.ui.theme.WhackaSecondaryAccent
import com.example.ui.theme.WhackaSurface
import com.example.ui.theme.WhackaTextPrimary
import com.example.ui.theme.WhackaTextSecondary

// ==========================================
// Whacka Design System - Reusable Components
// ==========================================

/**
 * Standard Whacka Card with 24.dp rounded corners, pure white background,
 * subtle border (0x1A000000), and optional primary accent shadow (alpha 0.18).
 */
@Composable
fun WhackaCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    hasAccentShadow: Boolean = true,
    shadowElevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shadowModifier = if (hasAccentShadow) {
        modifier.shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = WhackaPrimaryShadow,
            spotColor = WhackaPrimaryShadow
        )
    } else {
        modifier
    }

    val clickableModifier = if (onClick != null) {
        shadowModifier.clickable { onClick() }
    } else {
        shadowModifier
    }

    Surface(
        modifier = clickableModifier
            .border(BorderStroke(1.dp, borderColor), shape),
        shape = shape,
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        content()
    }
}

/**
 * Standard Whacka Task Card:
 * - 24.dp rounded corners
 * - Primary accent elevation/shadow (alpha 0.18)
 * - 4.dp vertical accent border matching category color
 * - Flat input & clean typography
 */
@Composable
fun WhackaTaskCardItem(
    task: TaskItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
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
            .clickable { onClick?.invoke() ?: onToggle() }
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 4.dp Category Accent Border stripe on the edge
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
                    // Checkbox toggle
                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (task.isCompleted) "مكتملة" else "غير مكتملة",
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
                                    fontSize = 14.sp,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
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
                                            fontWeight = FontWeight.Bold,
                                            color = WhackaEmerald
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

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

                if (trailingContent != null) {
                    trailingContent()
                } else {
                    // Duration Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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
}

/**
 * Whacka Clean Flat Input Field:
 * - 16.dp rounded corners
 * - Flat background without default underline
 * - 10% black border (0x1A000000)
 */
@Composable
fun WhackaFlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = singleLine,
        maxLines = maxLines,
        label = label?.let { { Text(it) } },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = WhackaPrimaryAccent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = WhackaPrimaryAccent
        )
    )
}

/**
 * Whacka Pill Button:
 * - Fully rounded shape (50)
 * - Primary accent or outlined variant
 */
@Composable
fun WhackaPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(46.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = WhackaPrimaryAccent,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.invoke()
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    } else {
        Surface(
            modifier = modifier
                .height(46.dp)
                .clip(RoundedCornerShape(50))
                .clickable(enabled = enabled) { onClick() }
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(50)),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                icon?.invoke()
                if (icon != null) Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

/**
 * Backward compatibility GlassCard -> mapped to WhackaCard
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    WhackaCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        hasAccentShadow = true,
        onClick = onClick,
        content = content
    )
}

@Composable
fun FocusProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = animatedProgress)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            progressColor.copy(alpha = 0.85f),
                            progressColor
                        )
                    )
                )
        )
    }
}

@Composable
fun CircularProgressCard(
    percentage: Int,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    progressColor: Color = WhackaPrimaryAccent,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (percentage / 100f) * 360f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "circularSweep"
    )

    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(96.dp)) {
            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TaskRadioButton(
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isCompleted) WhackaPrimaryAccent else Color.Transparent,
        animationSpec = tween(200),
        label = "radioBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isCompleted) WhackaPrimaryAccent else MaterialTheme.colorScheme.outline,
        animationSpec = tween(200),
        label = "radioBorder"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(animatedBg)
            .border(2.dp, animatedBorder, CircleShape)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "مكتملة",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun SegmentedTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            val bg by animateColorAsState(
                targetValue = if (isSelected) WhackaPrimaryAccent else Color.Transparent,
                animationSpec = tween(200),
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "tabText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}

// ==========================================
// Strict 12-Hour Universal Time Formatting & Picker
// ==========================================

object Time12HourUtils {
    /**
     * Format hour (0-23) and minute (0-59) to strict 12-hour AM/PM Arabic format ("01:00 م" or "10:30 ص")
     */
    fun formatHourMinuteTo12Hour(hourOfDay: Int, minute: Int): String {
        val period = if (hourOfDay < 12) "ص" else "م"
        val hour12 = when (hourOfDay) {
            0 -> 12
            in 1..12 -> hourOfDay
            else -> hourOfDay - 12
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, period)
    }

    /**
     * Parse any time string ("01:00 م", "13:00", "01:00 PM") into Pair(hour24, minute)
     */
    fun parseToHourMinute(timeStr: String?): Pair<Int, Int> {
        if (timeStr.isNullOrBlank()) return Pair(10, 0)
        val clean = timeStr.trim()
        val colonParts = clean.split(":")
        if (colonParts.size < 2) return Pair(10, 0)
        val h = colonParts[0].filter { it.isDigit() }.toIntOrNull() ?: 10
        val m = colonParts[1].take(2).filter { it.isDigit() }.toIntOrNull() ?: 0
        val isPM = clean.contains("م") || clean.lowercase(Locale.US).contains("pm")
        val isAM = clean.contains("ص") || clean.lowercase(Locale.US).contains("am")
        val hour24 = if (isPM) {
            if (h < 12) h + 12 else h
        } else if (isAM) {
            if (h == 12) 0 else h
        } else {
            h
        }
        return Pair(hour24.coerceIn(0, 23), m.coerceIn(0, 59))
    }

    /**
     * Normalize any time string or range to strict 12-hour format ("01:00 م - 02:00 م")
     */
    fun normalizeTo12Hour(timeStr: String?): String {
        if (timeStr.isNullOrBlank()) return "10:00 ص"
        if (timeStr.contains(" - ")) {
            val parts = timeStr.split(" - ")
            val start = normalizeTo12Hour(parts.getOrNull(0))
            val end = normalizeTo12Hour(parts.getOrNull(1))
            return "$start - $end"
        } else if (timeStr.contains("-")) {
            val parts = timeStr.split("-")
            val start = normalizeTo12Hour(parts.getOrNull(0))
            val end = normalizeTo12Hour(parts.getOrNull(1))
            return "$start - $end"
        }
        val (h, m) = parseToHourMinute(timeStr)
        return formatHourMinuteTo12Hour(h, m)
    }

    /**
     * Formats start and end hour/minute into strict 12-hour range: "01:00 م - 02:00 م"
     */
    fun formatTimeRange12Hour(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): String {
        return "${formatHourMinuteTo12Hour(startHour, startMinute)} - ${formatHourMinuteTo12Hour(endHour, endMinute)}"
    }
}

/**
 * Universal Native Time Picker Field with strict 12-hour format display ("01:00 م")
 * and >= 48dp touch target, opening the native TimePickerDialog.
 */
@Composable
fun WhackaTimePickerField(
    value: String,
    onTimeSelected: (String) -> Unit,
    label: String,
    placeholder: String = "10:00 ص",
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val (initialHour, initialMinute) = remember(value) {
        Time12HourUtils.parseToHourMinute(value)
    }

    val displayValue = remember(value) {
        if (value.isBlank()) placeholder else Time12HourUtils.normalizeTo12Hour(value)
    }

    val timePickerDialog = remember(context, initialHour, initialMinute) {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = Time12HourUtils.formatHourMinuteTo12Hour(hourOfDay, minute)
                onTimeSelected(formatted)
            },
            initialHour,
            initialMinute,
            false // False specifies 12-hour format with AM/PM picker
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Surface(
            onClick = { timePickerDialog.show() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .testTag("time_picker_${label.replace(" ", "_")}"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )

                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

