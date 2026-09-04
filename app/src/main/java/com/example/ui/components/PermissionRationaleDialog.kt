package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SuccessSecondary

enum class PermissionType {
    NOTIFICATION,
    EXACT_ALARM,
    BATTERY_OPTIMIZATION
}

@Composable
fun PermissionRationaleDialog(
    permissionType: PermissionType,
    isPermanentlyDenied: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title: String
    val description: String
    val icon: ImageVector
    val iconTint: Color
    val confirmButtonText: String

    when (permissionType) {
        PermissionType.NOTIFICATION -> {
            title = "صلاحية الإشعارات والتنبيهات"
            description = "يحتاج FocusCraft لصلاحية التنبيهات لضمان تذكيرك بالمهام المجدولة وإرسال إشعارات التركيز في الوقت المحدد."
            icon = Icons.Default.NotificationsActive
            iconTint = IndigoPrimary
            confirmButtonText = if (isPermanentlyDenied) "فتح إعدادات التطبيق ⚙️" else "منح الإذن الآن"
        }
        PermissionType.EXACT_ALARM -> {
            title = "صلاحية المنبه الدقيق (Exact Alarm)"
            description = "يحتاج FocusCraft لصلاحية المنبه الدقيق لإيقاظ شاشة التركيز وتشغيل التنبيه في الثانية المحددة بدقة تامة دون تأخير من النظام."
            icon = Icons.Default.Alarm
            iconTint = AmberTertiary
            confirmButtonText = "ضبط إذن المنبه ⏰"
        }
        PermissionType.BATTERY_OPTIMIZATION -> {
            title = "حماية التنبيهات من إيقاف النظام"
            description = "تقوم بعض أنظمة أندرويد بإيقاف المنبهات في الخلفية لتوفير البطارية. يُفضل استثناء FocusCraft لضمان عدم تفويت أي مهمة."
            icon = Icons.Default.BatteryAlert
            iconTint = AmberTertiary
            confirmButtonText = "إلغاء قيود البطارية 🔋"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("permission_rationale_dialog"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (isPermanentlyDenied) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmberTertiary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = AmberTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تم تعطيل الإذن سابقاً، يمكنك تفعيله يدوياً من إعدادات النظام.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = AmberTertiary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iconTint),
                modifier = Modifier.testTag("permission_confirm_button")
            ) {
                Text(
                    text = confirmButtonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (iconTint == AmberTertiary) Color.Black else Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("permission_dismiss_button")
            ) {
                Text(
                    text = "لاحقاً",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
