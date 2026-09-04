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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
