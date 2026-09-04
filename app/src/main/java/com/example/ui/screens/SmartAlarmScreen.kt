package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.FocusAlarmSoundManager
import com.example.ui.theme.DeepBackground
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoPrimaryFixedDim
import com.example.ui.theme.TextOnSurfaceVariant

@Composable
fun SmartAlarmScreen(
    timeText: String = "09:00",
    taskTitle: String = "مشروع التخرج",
    taskSubtitle: String = "تصميم واجهة المستخدم",
    onStartTask: () -> Unit,
    onSnooze: () -> Unit,
    onOpenTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localContext = LocalContext.current
    DisposableEffect(Unit) {
        FocusAlarmSoundManager.startAlarm(localContext)
        onDispose {
            FocusAlarmSoundManager.stopAlarm()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bellPulse")
    
    // Pulsing animation for the Bell
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("smart_alarm_screen")
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            IndigoPrimary.copy(alpha = 0.15f),
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
            // Top: Time and Task Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 46.sp,
                        letterSpacing = 1.sp
                    ),
                    color = IndigoPrimaryFixedDim
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "حان وقت",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        letterSpacing = 2.sp
                    ),
                    color = TextOnSurfaceVariant
                )

                Text(
                    text = taskTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Fix: Use light gray or muted purple/indigo for subtitle, NOT neon green
                Text(
                    text = taskSubtitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = TextOnSurfaceVariant
                )
            }

            // Center: Pulsating Bell Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(170.dp)
                ) {
                    // Outer pulsating ring
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .border(
                                BorderStroke(
                                    3.dp,
                                    IndigoPrimary.copy(alpha = ringAlpha)
                                ),
                                CircleShape
                            )
                    )

                    // Bell Container
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "المنبه نشط",
                            tint = IndigoPrimary,
                            modifier = Modifier
                                .size(56.dp)
                                .scale(pulseScale)
                        )
                    }
                }

                // Alert description pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = TextOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "لن يتوقف المنبه حتى تتفاعل",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = TextOnSurfaceVariant
                    )
                }
            }

            // Bottom Actions: 3 stacked full-width buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Primary filled: "بدأت المهمة"
                Button(
                    onClick = onStartTask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("alarm_start_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoPrimary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بدأت المهمة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                // 2. Outlined: "تذكير بعد 10 دقائق"
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("alarm_snooze_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تذكير بعد 10 دقائق",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }

                // 3. Text button: "فتح المهمة"
                TextButton(
                    onClick = onOpenTask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("alarm_open_task_button")
                ) {
                    Text(
                        text = "فتح المهمة",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        ),
                        color = TextOnSurfaceVariant
                    )
                }
            }
        }
    }
}
