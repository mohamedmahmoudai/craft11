package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockColor
import com.example.model.FocusTimerState
import com.example.model.TimerStatus
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.DeepBackground
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoPrimaryFixedDim
import com.example.ui.theme.SuccessSecondary
import com.example.ui.theme.TextOnSurfaceVariant

@Composable
fun FocusTimerScreen(
    timerState: FocusTimerState,
    onPlayPause: () -> Unit,
    onStopAndSave: () -> Unit,
    onComplete: () -> Unit,
    onMinimize: () -> Unit,
    onReset: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")

    // Pulsing ambient ring during RUNNING state
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (timerState.status == TimerStatus.RUNNING) 0.35f else 0.10f,
        targetValue = if (timerState.status == TimerStatus.RUNNING) 0.05f else 0.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = timerState.progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "timerProgressAnimation"
    )

    val themeAccentColor = when (timerState.colorType) {
        BlockColor.ORANGE -> AmberTertiary
        BlockColor.GREEN -> SuccessSecondary
        BlockColor.INDIGO, BlockColor.TEAL -> IndigoPrimary
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("focus_timer_screen")
    ) {
        // Ambient background glow centered behind the circular timer
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            themeAccentColor.copy(alpha = if (timerState.status == TimerStatus.RUNNING) 0.22f else 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar: Minimize button, Mode Tag, and Status Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimize / Back button
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .testTag("timer_minimize_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "تصغير شاشة التركيز",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Mode Badge
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (timerState.status) {
                                        TimerStatus.RUNNING -> SuccessSecondary
                                        TimerStatus.PAUSED -> AmberTertiary
                                        TimerStatus.COMPLETED -> SuccessSecondary
                                        TimerStatus.IDLE -> IndigoPrimary
                                    }
                                )
                        )
                        Text(
                            text = when (timerState.status) {
                                TimerStatus.RUNNING -> "جلسة نشطة"
                                TimerStatus.PAUSED -> "مؤقت متوقف مؤقتاً"
                                TimerStatus.COMPLETED -> "تم إنهاء الجلسة"
                                TimerStatus.IDLE -> "جاهز للبدء"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Placeholder for symmetric alignment
                Spacer(modifier = Modifier.size(44.dp))
            }

            // 2. Active Task Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = timerState.taskTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                if (timerState.taskSubtitle.isNotBlank()) {
                    Text(
                        text = timerState.taskSubtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = TextOnSurfaceVariant
                    )
                }
            }

            // 3. Central Circular Progress Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
                    .testTag("timer_progress_indicator")
            ) {
                val trackBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                val strokeWidth = 14.dp

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = strokeWidth.toPx()

                    // Background Track
                    drawCircle(
                        color = trackBgColor,
                        style = Stroke(width = strokePx)
                    )

                    // Foreground Active Arc
                    val sweepAngle = animatedProgress * 360f
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                themeAccentColor.copy(alpha = 0.8f),
                                themeAccentColor,
                                themeAccentColor.copy(alpha = 0.8f)
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }

                // Inner Stats & Time Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timerState.formattedRemainingTime,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 54.sp,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.testTag("timer_display_text")
                    )

                    Text(
                        text = "من أصل ${timerState.plannedDurationMinutes} دقيقة",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextOnSurfaceVariant
                    )

                    // Actual time spent counter badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = themeAccentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "المنقضي: ${timerState.formattedElapsedTime}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 4. Motivational Quote Pill
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                borderColor = Color.White.copy(alpha = 0.06f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (timerState.status) {
                            TimerStatus.RUNNING -> "✨ حافظ على تركيزك وتجنب المشتتات"
                            TimerStatus.PAUSED -> "⏸️ خذ نفساً عميقاً ثم واصل التقدم"
                            TimerStatus.COMPLETED -> "🎉 عمل رائع! تم إنجاز المهمة بنجاح"
                            TimerStatus.IDLE -> "🚀 اضغط بدء للانطلاق في جلسة تركيز عميق"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 5. Interactive Control Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Action Button (Play/Pause)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause Big Button
                    Button(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("timer_play_pause_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timerState.status == TimerStatus.RUNNING) AmberTertiary else themeAccentColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (timerState.status == TimerStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (timerState.status) {
                                    TimerStatus.RUNNING -> "إيقاف مؤقت"
                                    TimerStatus.PAUSED -> "استئناف التركيز"
                                    TimerStatus.COMPLETED -> "إعادة الجلسة"
                                    TimerStatus.IDLE -> "بدء التركيز"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }

                    // Complete Button (Checkmark)
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("timer_complete_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessSecondary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "إكمال المهمة",
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "إكمال",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }

                // Secondary Action: "إنهاء وحفظ الوقت" (Stop and save spent minutes)
                OutlinedButton(
                    onClick = onStopAndSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("timer_stop_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            tint = AmberTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إنهاء وحفظ الوقت المنجز (${timerState.actualMinutesSpent} دقيقة)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
